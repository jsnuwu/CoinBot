import asyncio
import json
import os
import random

import httpx

from .models import Post, utcnow
from .store import Blocked

BASE = "https://api.x.com/2/tweets/search/stream"
TAG = "xpump-v1"


def headers():
    if not os.getenv("X_BEARER_TOKEN"):
        raise Blocked("x_bearer_token_missing")
    return {"Authorization": "Bearer " + os.environ["X_BEARER_TOKEN"]}


async def configure_rules(cfg, client):
    if not cfg.x_enabled or not cfg.author_ids:
        raise Blocked("x_stream_not_configured")
    r = await client.get(BASE + "/rules", headers=headers())
    r.raise_for_status()
    rules = r.json().get("data", [])
    # Don't attach to unrelated rules: their traffic can incur unrelated X bills.
    if any(rule.get("tag") != TAG for rule in rules):
        raise Blocked("use_a_dedicated_x_app_existing_foreign_rules")
    value = "(" + " OR ".join("from:" + author for author in cfg.author_ids) + ") -is:retweet -is:reply -is:quote"
    if len(value) > 1024:
        raise Blocked("too_many_authors_for_one_rule")
    if len(rules) == 1 and rules[0]["value"] == value:
        return
    if rules:
        r = await client.post(BASE + "/rules", headers=headers(), json={"delete": {"ids": [r["id"] for r in rules]}})
        r.raise_for_status()
        if r.json().get("errors"):
            raise Blocked("x_rule_update_failed")
    r = await client.post(BASE + "/rules", headers=headers(), json={"add": [{"value": value, "tag": TAG}]})
    r.raise_for_status()
    if r.json().get("errors"):
        raise Blocked("x_rule_update_failed")


async def stream(cfg, store, queue):
    if not cfg.x_enabled or not cfg.author_ids:
        raise Blocked("x_stream_not_configured")
    backoff = 1
    async with httpx.AsyncClient(timeout=httpx.Timeout(25, connect=10), follow_redirects=False) as client:
        # Read-only verification. Rule changes happen only via the explicit CLI command.
        r = await client.get(BASE + "/rules", headers=headers())
        r.raise_for_status()
        rules = r.json().get("data", [])
        expected = "(" + " OR ".join("from:" + a for a in cfg.author_ids) + ") -is:retweet -is:reply -is:quote"
        if len(rules) != 1 or rules[0].get("tag") != TAG or rules[0].get("value") != expected:
            raise Blocked("run_configure_x_rules_first")
        while True:
            try:
                async with client.stream("GET", BASE, headers=headers(), params={
                    "tweet.fields": "author_id,created_at,referenced_tweets,edit_history_tweet_ids",
                }) as response:
                    if response.status_code in (401, 402, 403):
                        raise Blocked("x_auth_access_or_credits_missing")
                    response.raise_for_status()
                    store.health("feed", {"status": "connected", "time": utcnow().isoformat()})
                    async for line in response.aiter_lines():
                        store.health("feed_heartbeat", utcnow().isoformat())
                        backoff = 1
                        if not line:
                            continue
                        if len(line) > 100000:
                            store.health("feed_warning", "oversized_event")
                            continue
                        try:
                            payload = json.loads(line)
                            if "data" not in payload:
                                store.health("feed_warning", "non_post_event")
                                continue
                            post = Post.from_x(payload)
                        except (ValueError, KeyError, TypeError):
                            store.health("feed_warning", "malformed_post")
                            continue
                        if queue.full():
                            queue.get_nowait()
                            queue.task_done()
                            store.health("feed_warning", "queue_full_dropped_oldest")
                        queue.put_nowait(post)
            except (httpx.HTTPError, OSError):
                store.health("feed", {"status": "reconnecting", "time": utcnow().isoformat()})
            else:
                store.health("feed", {"status": "disconnected", "time": utcnow().isoformat()})
            await asyncio.sleep(backoff + random.random())
            backoff = min(backoff * 2, 60)
