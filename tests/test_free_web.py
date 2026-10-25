import base64
from dataclasses import replace
from datetime import timedelta
import json

import httpx
import pytest
from starlette.testclient import TestClient

from xpump.ideas import TemplateIdeas
from xpump.models import utcnow
from xpump.pipeline import Pipeline
from xpump.public_x import PublicMonitor, PublicXError, parse_timeline, read_timeline
from xpump.web import create_app


def embed(*tweets):
    data = {"props": {"pageProps": {"timeline": {"entries": [
        {"type": "tweet", "content": {"tweet": t}} for t in tweets
    ]}}}}
    return '<html><script id="__NEXT_DATA__" type="application/json">' + json.dumps(data) + '</script></html>'


def tweet(**changes):
    return {"id_str": "12345", "full_text": "Coffee on the moon!",
            "created_at": utcnow().strftime("%a %b %d %H:%M:%S %z %Y"),
            "user": {"id_str": "123", "screen_name": "testaccount"}, **changes}


def test_public_view_verifies_author_and_never_executes_html():
    posts = parse_timeline(embed(tweet(), tweet(id_str="12346", user={"id_str": "999", "screen_name": "testaccount"})),
                           "testaccount", "123")
    assert len(posts) == 1 and posts[0].id == "12345"
    assert posts[0].url == "https://x.com/i/web/status/12345"


@pytest.mark.parametrize("fields", [{"in_reply_to_status_id_str": "1"}, {"retweeted_status": {"id": "1"}},
                                    {"is_quote_status": True}, {"possibly_sensitive": True},
                                    {"edit_control": {"edit_tweet_ids": ["1", "2"]}}])
def test_public_quotes_replies_edits_are_not_launch_candidates(fields):
    posts = parse_timeline(embed(tweet(**fields)), "testaccount", "123")
    assert len(posts) == 1 and not posts[0].original


def test_public_login_wall_is_error_not_empty_success():
    with pytest.raises(PublicXError, match="unavailable"):
        parse_timeline('<html>Please log in<script>alert(1)</script></html>', "testaccount", "123")


@pytest.mark.parametrize("status", [401, 403, 429])
async def test_public_access_denied_no_bypass(status):
    calls = []

    async def handler(request):
        calls.append(str(request.url))
        assert "authorization" not in request.headers and "cookie" not in request.headers
        return httpx.Response(status)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(PublicXError, match="limited"):
            await read_timeline(client, "testaccount", "123")
    assert calls == ["https://syndication.twitter.com/srv/timeline-profile/screen-name/testaccount"]


async def test_public_monitor_generates_once_then_deduplicates(cfg, store):
    cfg = replace(cfg, public_x_accounts="testaccount:123", free_hosting=True)
    pipeline = Pipeline(cfg, store, TemplateIdeas(), started_at=utcnow() - timedelta(seconds=5))
    monitor = PublicMonitor(cfg, pipeline)

    async def handler(request):
        return httpx.Response(200, text=embed(tweet()))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        assert await monitor.check(client) is False
        assert await monitor.check(client) is False
    assert monitor.state["checks"] == 2 and monitor.state["status"] == "polling"
    assert len(store.snapshot()["launches"]) == 1
    assert store.snapshot()["launches"][0]["status"] == "preview"
    assert store.snapshot()["launches"][0]["signature"] is None


async def test_cached_old_posts_are_shown_but_never_processed(cfg, store):
    cfg = replace(cfg, public_x_accounts="testaccount:123")
    pipeline = Pipeline(cfg, store, TemplateIdeas())
    monitor = PublicMonitor(cfg, pipeline)
    old = (utcnow() - timedelta(days=365)).strftime("%a %b %d %H:%M:%S %z %Y")

    async def handler(request):
        return httpx.Response(200, text=embed(tweet(created_at=old)))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await monitor.check(client)
    assert monitor.state["recent_posts"][0]["result"]["reason"] == "old_post"
    assert not store.snapshot()["launches"]


def test_website_preview_is_useful_without_any_network(cfg, monkeypatch):
    cfg = replace(cfg, free_hosting=True)

    async def fail_network(*args, **kwargs):
        pytest.fail("Manual preview attempted a network request")
    monkeypatch.setattr(httpx.AsyncClient, "send", fail_network)
    with TestClient(create_app(cfg)) as client:
        assert client.get("/").status_code == 200
        assert client.get("/assets/app.js").status_code == 200
        assert client.get("/health").json()["launch_enabled"] is False
        state = client.get("/api/status").json()
        assert state["monitor"]["status"] == "not_configured" and state["ephemeral_storage"] is True
        result = client.post("/api/preview", json={"text": "Coffee on the moon!"})
        assert result.status_code == 200
        body = result.json()
        assert base64.b64decode(body["image"]).startswith(b"\x89PNG")
        assert "source_post" not in body["metadata"]  # No made-up X post URL.
        assert body["origin"] == "user_supplied_text"
        assert client.post("/api/launch", json={}).status_code == 404


@pytest.mark.parametrize("payload", [
    {"text": ""}, {"text": "moon", "url": "http://169.254.169.254/"},
    {"text": "moon", "url": "https://x.com.attacker.test/a/status/1"},
    {"text": "x" * 4001}, [],
])
def test_web_rejects_invalid_input_and_external_urls(cfg, payload):
    with TestClient(create_app(cfg)) as client:
        assert client.post("/api/preview", json=payload).status_code == 400


def test_web_request_limits_and_sensitive_content(cfg):
    with TestClient(create_app(cfg)) as client:
        assert client.post("/api/preview", content="a" * 16001).status_code == 413
        assert client.post("/api/preview", json={"text": "People killed on the moon"}).status_code == 422
        assert client.post("/api/preview", json={"text": "Meeting at noon"}).status_code == 422
        for _ in range(17):
            client.post("/api/preview", json={"text": ""})
        assert client.post("/api/preview", json={"text": "moon"}).status_code == 429


@pytest.mark.parametrize("change", [dict(x_enabled=True), dict(idea_provider="ollama")])
def test_free_profile_cannot_enable_paid_sources(cfg, change):
    with pytest.raises(ValueError, match="Free hosting"):
        replace(cfg, free_hosting=True, **change)


def test_website_refuses_live_config(live_cfg):
    with pytest.raises(ValueError):
        create_app(live_cfg)
    with pytest.raises(ValueError):
        replace(live_cfg, free_hosting=True)


@pytest.mark.parametrize("change", [dict(public_x_poll_seconds=1), dict(public_x_accounts="../a:1"),
                                    dict(public_x_accounts="realDonaldTrump:no-id")])
def test_public_feed_settings_are_bounded(cfg, change):
    with pytest.raises(ValueError):
        replace(cfg, **change)
