import json

import httpx
import pytest

from dataclasses import replace

from xpump.store import Blocked
from xpump.xfeed import configure_rules


async def test_disabled_feed_makes_no_requests(cfg):
    async def handler(req):
        pytest.fail("Disabled X integration attempted a request")
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(Blocked, match="not_configured"):
            await configure_rules(cfg, client)


async def test_foreign_rules_are_not_deleted(cfg, monkeypatch):
    monkeypatch.setenv("X_BEARER_TOKEN", "test")
    calls = []

    async def handler(req):
        calls.append(req.method)
        return httpx.Response(200, json={"data": [{"id": "1", "tag": "other-project", "value": "bitcoin"}]})
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(Blocked, match="dedicated_x_app"):
            await configure_rules(replace(cfg, x_enabled=True), client)
    assert calls == ["GET"]


async def test_only_selected_authors_in_rule(cfg, monkeypatch):
    monkeypatch.setenv("X_BEARER_TOKEN", "test")
    calls = []

    async def handler(req):
        calls.append(req.method)
        if req.method == "POST":
            body = json.loads(req.content)
            assert body == {"add": [{"value": "(from:123) -is:retweet -is:reply -is:quote", "tag": "xpump-v1"}]}
        return httpx.Response(200, json={"data": []})
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await configure_rules(replace(cfg, x_enabled=True), client)
    assert calls == ["GET", "POST"]


async def test_provider_rejects_rule(cfg, monkeypatch):
    monkeypatch.setenv("X_BEARER_TOKEN", "test")

    async def handler(req):
        return httpx.Response(200, json={"errors": [{"message": "sensitive provider detail"}]}
                              if req.method == "POST" else {"data": []})
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(Blocked, match="x_rule_update_failed"):
            await configure_rules(replace(cfg, x_enabled=True), client)
