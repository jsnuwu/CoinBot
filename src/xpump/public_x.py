"""Best-effort reader of X's public embed view. No authentication or access bypass."""
import asyncio
from datetime import datetime, timezone
from html.parser import HTMLParser
import json
import re

import httpx

from .models import Post, utcnow


class PublicXError(Exception):
    pass


class EmbedData(HTMLParser):
    def __init__(self):
        super().__init__()
        self.capture = False
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag == "script":
            self.capture = dict(attrs).get("id") == "__NEXT_DATA__"

    def handle_endtag(self, tag):
        if tag == "script":
            self.capture = False

    def handle_data(self, text):
        if self.capture:
            self.parts.append(text)


def parse_timeline(html, handle, author_id):
    parser = EmbedData()
    parser.feed(html)
    try:
        data = json.loads("".join(parser.parts))["props"]["pageProps"]
        entries = data["timeline"]["entries"]
        if not isinstance(entries, list):
            raise ValueError()
    except (ValueError, KeyError, TypeError) as exc:
        raise PublicXError("public_view_unavailable") from exc
    posts = {}
    for entry in entries[:100]:
        try:
            if entry.get("type") != "tweet":
                continue
            tweet = entry["content"]["tweet"]
            user = tweet["user"]
            if user["id_str"] != author_id or user["screen_name"].casefold() != handle.casefold():
                continue
            post_id = tweet["id_str"]
            text = tweet.get("full_text", tweet.get("text", ""))
            if not re.fullmatch(r"[0-9]{1,24}", post_id) or not isinstance(text, str) or len(text) > 25000:
                continue
            created = datetime.strptime(tweet["created_at"], "%a %b %d %H:%M:%S %z %Y").astimezone(timezone.utc)
            history = tweet.get("edit_control", {}).get("edit_tweet_ids", [post_id])
            original = not (tweet.get("retweeted_status") or tweet.get("in_reply_to_status_id_str")
                            or tweet.get("quoted_status_id_str") or tweet.get("is_quote_status")
                            or len(history) != 1 or tweet.get("possibly_sensitive"))
            posts[post_id] = Post(post_id, post_id, author_id, text, created, original)
        except (ValueError, KeyError, TypeError, AttributeError):
            continue
    return sorted(posts.values(), key=lambda p: p.created_at)


async def read_timeline(client, handle, author_id):
    if not re.fullmatch(r"[A-Za-z0-9_]{1,15}", handle):
        raise PublicXError("invalid_handle")
    # Fixed host; no arbitrary URL, cookies, login, proxy rotation or challenge bypass.
    url = f"https://syndication.twitter.com/srv/timeline-profile/screen-name/{handle}"
    async with client.stream("GET", url, headers={"User-Agent": "CoinBot-PublicPreview/0.1"}) as response:
        if response.status_code in (401, 403, 429):
            raise PublicXError("public_access_limited")
        if response.status_code != 200:
            raise PublicXError("public_view_unavailable")
        body = bytearray()
        async for chunk in response.aiter_bytes():
            body.extend(chunk)
            if len(body) > 2_000_000:
                raise PublicXError("public_view_too_large")
    return parse_timeline(body.decode("utf-8"), handle, author_id)


class PublicMonitor:
    def __init__(self, cfg, pipeline):
        self.cfg, self.pipeline = cfg, pipeline
        self.accounts = [tuple(a.strip().split(":")) for a in cfg.public_x_accounts.split(",") if a.strip()]
        self.state = {"status": "starting" if self.accounts else "not_configured", "checks": 0,
                      "last_checked": None, "latest_post_at": None, "accounts": [], "recent_posts": []}

    async def check(self, client):
        results, recent, failed = [], [], False
        for handle, author_id in self.accounts:
            try:
                posts = await read_timeline(client, handle, author_id)
                account = {"handle": handle, "status": "readable" if posts else "no_posts_returned",
                           "returned_posts": len(posts)}
                for post in posts:
                    outcome = await self.pipeline.process(post)
                    recent.append({"id": post.id, "url": post.url, "handle": handle,
                                   "created_at": post.created_at.isoformat(), "text": post.text[:600],
                                   "result": outcome})
                results.append(account)
            except (PublicXError, httpx.HTTPError, UnicodeError):
                failed = True
                results.append({"handle": handle, "status": "unavailable"})
            except Exception:
                failed = True
                results.append({"handle": handle, "status": "processing_error"})
        recent.sort(key=lambda p: p["created_at"], reverse=True)
        self.state.update({"status": "degraded" if failed else "polling", "checks": self.state["checks"] + 1,
                           "last_checked": utcnow().isoformat(), "accounts": results})
        if recent:
            self.state.update({"latest_post_at": recent[0]["created_at"], "recent_posts": recent[:10]})
        self.pipeline.store.health("public_x", {k: v for k, v in self.state.items() if k != "recent_posts"})
        return failed

    async def run(self):
        if not self.accounts:
            return
        delay = self.cfg.public_x_poll_seconds
        async with httpx.AsyncClient(timeout=15, follow_redirects=False) as client:
            while True:
                failed = await self.check(client)
                delay = min(max(900, delay * 2), 3600) if failed else self.cfg.public_x_poll_seconds
                self.state["next_check_seconds"] = delay
                await asyncio.sleep(delay)
