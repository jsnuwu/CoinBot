from contextlib import contextmanager
import fcntl
import json
from pathlib import Path
import sqlite3
import time

from .config import Config, lamports


class Blocked(Exception):
    """A controlled, public reason for not launching."""


class Store:
    def __init__(self, directory: str):
        root = Path(directory)
        root.mkdir(parents=True, exist_ok=True, mode=0o700)
        self.root = root
        self.db = sqlite3.connect(root / "state.sqlite3", isolation_level=None)
        self.db.row_factory = sqlite3.Row
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("PRAGMA synchronous=FULL")
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS posts (
                root_id TEXT PRIMARY KEY, post_id TEXT NOT NULL, author_id TEXT NOT NULL,
                seen REAL NOT NULL, status TEXT NOT NULL, reason TEXT, idea TEXT,
                detection_ms INTEGER, preparation_ms INTEGER
            );
            CREATE TABLE IF NOT EXISTS launches (
                root_id TEXT PRIMARY KEY REFERENCES posts(root_id), created REAL NOT NULL,
                reserved INTEGER NOT NULL, status TEXT NOT NULL, mint TEXT, signature TEXT,
                actual_debit INTEGER
            );
            CREATE TABLE IF NOT EXISTS health (key TEXT PRIMARY KEY, value TEXT NOT NULL);
        """)

    @contextmanager
    def single_runner(self):
        with (self.root / "runner.lock").open("a") as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as exc:
                raise Blocked("another_runner_owns_this_database") from exc
            yield

    def health(self, key, value):
        self.db.execute("INSERT OR REPLACE INTO health VALUES (?,?)", (key, json.dumps(value)))

    def claim(self, post, now: float) -> bool:
        result = self.db.execute(
            "INSERT OR IGNORE INTO posts(root_id,post_id,author_id,seen,status,detection_ms) "
            "VALUES (?,?,?,?,?,?)",
            (post.root_id, post.id, post.author_id, now, "observed",
             round((now - post.created_at.timestamp()) * 1000)),
        )
        return result.rowcount == 1

    def finish(self, root_id, status, reason=None, idea=None, preparation_ms=None):
        self.db.execute("UPDATE posts SET status=?,reason=?,idea=COALESCE(?,idea),"
                        "preparation_ms=COALESCE(?,preparation_ms) WHERE root_id=?",
                        (status, reason, json.dumps(idea) if idea else None, preparation_ms, root_id))

    def candidates(self, now):
        return self.db.execute("SELECT COUNT(*) FROM posts WHERE seen>=? AND status!='ignored'",
                               (now - 86400,)).fetchone()[0]

    def pending(self):
        return [dict(r) for r in self.db.execute(
            "SELECT * FROM launches WHERE status NOT IN ('finalized','failed','aborted','preview')")]

    def reserve(self, root_id: str, cfg: Config, now: float):
        """Full per-launch cap is charged to the ledger, including failed attempts."""
        self.db.execute("BEGIN IMMEDIATE")
        try:
            if (self.root / "STOP").exists():
                raise Blocked("kill_switch")
            if self.pending():
                raise Blocked("unresolved_transaction")
            recent = self.db.execute("SELECT COUNT(*),COALESCE(SUM(reserved),0),MAX(created) "
                                     "FROM launches WHERE created>=?", (now - 86400,)).fetchone()
            last = self.db.execute("SELECT MAX(created) FROM launches").fetchone()[0]
            total = self.db.execute("SELECT COALESCE(SUM(reserved),0) FROM launches").fetchone()[0]
            cap = lamports(cfg.max_launch_sol)
            if recent[0] >= cfg.max_launches_24h:
                raise Blocked("launch_count_24h")
            if last is not None and now - last < cfg.cooldown_seconds:
                raise Blocked("cooldown")
            if recent[1] + cap > lamports(cfg.max_24h_sol) or total + cap > lamports(cfg.max_total_sol):
                raise Blocked("sol_budget")
            self.db.execute("INSERT INTO launches(root_id,created,reserved,status) VALUES (?,?,?,?)",
                            (root_id, now, cap, "reserved"))
            self.db.execute("COMMIT")
        except BaseException:
            self.db.execute("ROLLBACK")
            raise

    def prepared(self, root_id, mint, signature):
        # Commit signature BEFORE handing the signed bytes to the RPC.
        self.db.execute("UPDATE launches SET status='pending',mint=?,signature=? WHERE root_id=?",
                        (mint, signature, root_id))

    def outcome(self, root_id, status, debit=None):
        self.db.execute("UPDATE launches SET status=?,actual_debit=? WHERE root_id=?",
                        (status, debit, root_id))
        self.finish(root_id, status)

    def snapshot(self):
        return {
            "health": {r["key"]: json.loads(r["value"]) for r in self.db.execute("SELECT * FROM health")},
            "recent_posts": [dict(r) for r in self.db.execute("SELECT * FROM posts ORDER BY seen DESC LIMIT 20")],
            "launches": [dict(r) for r in self.db.execute("SELECT * FROM launches ORDER BY created DESC LIMIT 20")],
            "reserved_lamports_total": self.db.execute("SELECT COALESCE(SUM(reserved),0) FROM launches").fetchone()[0],
            "kill_switch": (self.root / "STOP").exists(),
        }

    def prune(self):
        # Keep durable launch/idempotency records; old ignored posts need no text history.
        self.db.execute("DELETE FROM posts WHERE seen<? AND root_id NOT IN (SELECT root_id FROM launches)",
                        (time.time() - 30 * 86400,))
