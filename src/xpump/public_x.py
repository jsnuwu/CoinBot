"""Best-effort reader of X's public embed view. No authentication or access bypass."""
import asyncio
from datetime import datetime, timezone
from html.parser import HTMLParser
import json
import re
import time

import httpx

from .models import Post, utcnow


class PublicXError(Exception):
    def __init__(self, code, *, http_status=None):
        super().__init__(code)
        self.http_status = http_status


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
            raise PublicXError("public_access_limited", http_status=response.status_code)
        if response.status_code != 200:
            raise PublicXError("public_view_unavailable", http_status=response.status_code)
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
        self.next_due = {handle: 0.0 for handle, _ in self.accounts}
        self.failures = {handle: 0 for handle, _ in self.accounts}
        self.account_state = {handle: {"handle": handle, "status": "starting"} for handle, _ in self.accounts}
        self.recent = {}
        self.state = {"status": "starting" if self.accounts else "not_configured", "checks": 0,
                      "started_at": utcnow().isoformat(), "last_checked": None, "latest_post_at": None,
                      "accounts": [], "recent_posts": []}

    async def check(self, client, *, now=None):
        now = time.time() if now is None else now
        due = [(handle, author) for handle, author in self.accounts if self.next_due[handle] <= now]
        if not due:
            return self.state["status"] == "degraded"

        async def read(handle, author_id):
            try:
                return handle, await read_timeline(client, handle, author_id), None
            except PublicXError as exc:
                return handle, [], {"status": "unavailable", "error_code": str(exc),
                                    "http_status": exc.http_status}
            except httpx.TimeoutException:
                return handle, [], {"status": "unavailable", "error_code": "request_timeout"}
            except httpx.HTTPError:
                return handle, [], {"status": "unavailable", "error_code": "network_error"}
            except UnicodeError:
                return handle, [], {"status": "unavailable", "error_code": "invalid_response_encoding"}
            except Exception:
                return handle, [], {"status": "processing_error", "error_code": "reader_error"}

        # One limited account must not serialize network reads or stall another account's cadence.
        for handle, posts, error in await asyncio.gather(*(read(h, a) for h, a in due)):
            if error is None:
                account = {"handle": handle, "status": "readable" if posts else "no_posts_returned",
                           "returned_posts": len(posts)}
                recent = []
                try:
                    # Feed order can be pinned/unsorted. Give the newest eligible post first access to limits.
                    for post in reversed(posts):
                        outcome = await self.pipeline.process(post)
                        recent.append({"id": post.id, "url": post.url, "handle": handle,
                                       "created_at": post.created_at.isoformat(), "text": post.text[:600],
                                       "result": outcome})
                    self.recent[handle] = recent[:10]
                except Exception:
                    error = {"status": "processing_error", "error_code": "pipeline_error"}
            if error:
                account = {"handle": handle, **error}
                self.failures[handle] += 1
                delay = min(900 * 2 ** min(self.failures[handle] - 1, 2), 3600)
            else:
                self.failures[handle] = 0
                delay = self.cfg.public_x_poll_seconds
            self.next_due[handle] = now + delay
            account.update({"last_checked": utcnow().isoformat(),
                            "next_check_at": datetime.fromtimestamp(now + delay, timezone.utc).isoformat()})
            self.account_state[handle] = account
        results = list(self.account_state.values())
        failed = any(a["status"] in ("unavailable", "processing_error") for a in results)
        recent = [post for account_posts in self.recent.values() for post in account_posts]
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
        async with httpx.AsyncClient(timeout=15, follow_redirects=False) as client:
            while True:
                await self.check(client)
                delay = max(1, min(self.next_due.values()) - time.time())
                self.state["next_check_seconds"] = delay
                await asyncio.sleep(delay)
