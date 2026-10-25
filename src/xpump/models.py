from dataclasses import dataclass
from datetime import datetime, timezone
import re


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


@dataclass(frozen=True)
class Post:
    id: str
    root_id: str
    author_id: str
    text: str
    created_at: datetime
    original: bool = True

    @property
    def url(self):
        return f"https://x.com/i/web/status/{self.id}"

    @classmethod
    def from_x(cls, payload: dict):
        d = payload["data"]
        history = d.get("edit_history_tweet_ids") or [d["id"]]
        ids = [d["id"], d["author_id"], *history]
        if any(not isinstance(v, str) or not re.fullmatch(r"[0-9]{1,24}", v) for v in ids):
            raise ValueError("Invalid X identifiers")
        created = datetime.fromisoformat(d["created_at"].replace("Z", "+00:00"))
        if created.tzinfo is None or not isinstance(d["text"], str) or len(d["text"]) > 25000:
            raise ValueError("Invalid X timestamp or text")
        return cls(d["id"], history[0], d["author_id"], d["text"], created,
                   not d.get("referenced_tweets") and len(history) == 1)


@dataclass(frozen=True)
class Idea:
    name: str
    symbol: str
    motif: str
    score: float
    theme: str = "generic"

    def metadata(self, post: Post, image_uri: str):
        return {
            "name": self.name,
            "symbol": self.symbol,
            "description": (f"Independent community meme: {self.motif}. "
                            "Unofficial; not affiliated with or endorsed by the post author. "
                            "No promised value or returns."),
            "image": image_uri,
            "source_post": post.url,
            "showName": True,
        }
