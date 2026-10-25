from dataclasses import asdict
from datetime import datetime
import json
import re
import time

from .art import render
from .models import utcnow
from .store import Blocked


class Pipeline:
    def __init__(self, cfg, store, ideas, publisher=None, started_at=None):
        self.cfg, self.store, self.ideas, self.publisher = cfg, store, ideas, publisher
        self.started_at = started_at or utcnow()
        if cfg.mode == "live" and publisher is None:
            raise ValueError("Live pipeline requires publisher")

    def exclusion(self, post, now: datetime):
        if post.author_id not in self.cfg.author_ids:
            return "author_not_allowed"
        if not post.original:
            return "reply_repost_quote_or_edit"
        age = (now - post.created_at).total_seconds()
        if age < -5:
            return "future_timestamp"
        if post.created_at < self.started_at or age > self.cfg.max_post_age_seconds:
            return "old_post"
        if not post.text.strip():
            return "empty_post"
        # Conservative prefilter applies even to the local template provider.
        if re.search(r"\b(killed|dead|death|shooting|attack|war|suicide|murder|rape|"
                     r"private key|seed phrase|guaranteed profit)\b", post.text, re.I):
            return "sensitive_content"
        if (self.store.root / "STOP").exists():
            return "kill_switch"
        return None

    async def process(self, post):
        now = utcnow()
        start = time.monotonic()
        reason = self.exclusion(post, now)
        if reason:
            self.store.health("last_ignored", {"reason": reason, "time": now.isoformat()})
            return {"status": "ignored", "reason": reason}
        if not self.store.claim(post, now.timestamp()):
            return {"status": "duplicate"}
        try:
            if self.store.candidates(now.timestamp()) > self.cfg.max_candidates_24h:
                raise Blocked("candidate_limit_24h")
            if self.store.pending():
                raise Blocked("unresolved_transaction")
            idea = await self.ideas.generate(post)
            if idea is None or idea.score < self.cfg.min_score:
                raise Blocked("insufficient_meme_relevance")
            if (utcnow() - post.created_at).total_seconds() > self.cfg.max_post_age_seconds:
                raise Blocked("post_expired_during_preparation")
            self.store.finish(post.root_id, "candidate", idea=asdict(idea))
            if self.cfg.mode == "preview":
                self.store.reserve(post.root_id, self.cfg, time.time())
                output = self.store.root / "previews" / post.root_id
                output.mkdir(parents=True, exist_ok=True)
                (output / "meme.png").write_bytes(render(idea))
                (output / "metadata.json").write_text(json.dumps(idea.metadata(post, "meme.png"), indent=2))
                result = {"status": "preview", "path": str(output)}
                self.store.outcome(post.root_id, "preview")
            else:
                result = await self.publisher.publish(post, idea)
            self.store.finish(post.root_id, result["status"],
                              preparation_ms=round((time.monotonic() - start) * 1000))
            self.store.health("last_result", {**result, "time": utcnow().isoformat()})
            return result
        except Exception as exc:
            # Never log response bodies, secret-bearing URLs, private keys or raw LLM output.
            reason = str(exc) if isinstance(exc, Blocked) else type(exc).__name__
            pending = any(r["root_id"] == post.root_id and r["signature"] for r in self.store.pending())
            status = "pending" if pending else "skipped"
            self.store.finish(post.root_id, status, reason)
            self.store.health("last_result", {"status": status, "reason": reason, "time": utcnow().isoformat()})
            return {"status": status, "reason": reason}
