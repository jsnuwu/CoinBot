from dataclasses import replace
import json

import httpx
import pytest
from solders.keypair import Keypair

from xpump.free_pump import FreePumpCreator, API
from xpump.store import Blocked
from xpump.pipeline import Pipeline
from xpump.ideas import TemplateIdeas


@pytest.fixture
def free_cfg(cfg, monkeypatch):
    monkeypatch.setenv("PUMP_ACCESS_TOKEN", "test-session")
    return replace(cfg, free_pump_enabled=True, pump_creator_address=str(Keypair().pubkey()))


def provider(cfg, mint, *, timeout=False, wrong_owner=False):
    calls = []

    async def handler(req):
        calls.append(req.url.path)
        assert str(req.url).startswith(API + "/")
        assert req.headers["Authorization"] == "Bearer test-session"
        if req.url.path == "/auth/my-profile":
            return httpx.Response(200, json={"address": "wrong-owner" if wrong_owner else cfg.pump_creator_address})
        if req.url.path == "/ipfs/token-metadata":
            assert b"image/png" in req.content
            return httpx.Response(200, json={"metadataUri": "ipfs://metadata", "metadata": {"image": "ipfs://image"}})
        assert req.url.path == "/coins/create-v2" and req.method == "POST"
        body = json.loads(req.content)
        assert set(body) == {"name", "ticker", "description", "website", "showName", "metadataUri", "image", "tokenProgram"}
        if timeout:
            raise httpx.ReadTimeout("provider detail must not be logged")
        return httpx.Response(200, json={"mint": mint})
    return calls, handler


async def test_free_listing_never_uses_rpc_trading_or_paid_metadata(free_cfg, store, post, idea):
    mint = str(Keypair().pubkey())
    calls, handler = provider(free_cfg, mint)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        creator = FreePumpCreator(free_cfg, store, client)
        result = await creator.create(post, idea)
        assert result == {"status": "listed", "mint": mint, "coin_url": "https://pump.fun/coin/" + mint}
        assert creator.snapshot()["records"][0]["status"] == "listed"
        assert "test-session" not in str(creator.snapshot())
        with pytest.raises(Blocked, match="duplicate"):
            await creator.create(post, idea)
    assert calls == ["/auth/my-profile", "/ipfs/token-metadata", "/coins/create-v2"]


async def test_unknown_creation_is_never_retried(free_cfg, store, post, idea):
    calls, handler = provider(free_cfg, str(Keypair().pubkey()), timeout=True)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        creator = FreePumpCreator(free_cfg, store, client)
        assert (await creator.create(post, idea))["status"] == "free_creation_unknown"
        restarted = FreePumpCreator(free_cfg, store, client)
        with pytest.raises(Blocked, match="previous_creation_unknown"):
            await restarted.create(replace(post, id="101", root_id="101"), idea)
    assert calls.count("/coins/create-v2") == 1


async def test_wrong_account_cannot_publish(free_cfg, store, post, idea):
    calls, handler = provider(free_cfg, str(Keypair().pubkey()), wrong_owner=True)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(Blocked, match="wrong_creator"):
            await FreePumpCreator(free_cfg, store, client).create(post, idea)
    assert calls == ["/auth/my-profile"]


async def test_missing_session_and_disabled_feature_make_no_requests(free_cfg, store, post, idea, monkeypatch):
    monkeypatch.delenv("PUMP_ACCESS_TOKEN")
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda req: pytest.fail("Unexpected request"))) as client:
        with pytest.raises(Blocked, match="authentication_required"):
            await FreePumpCreator(free_cfg, store, client).create(post, idea)
        with pytest.raises(Blocked, match="free_pump_disabled"):
            await FreePumpCreator(replace(free_cfg, free_pump_enabled=False), store, client).create(post, idea)


async def test_detected_post_automatically_calls_free_listing(free_cfg, store, post):
    mint = str(Keypair().pubkey())
    calls, handler = provider(free_cfg, mint)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        creator = FreePumpCreator(free_cfg, store, client)
        pipeline = Pipeline(free_cfg, store, TemplateIdeas(), started_at=post.created_at, free_creator=creator)
        result = await pipeline.process(post)
        assert result["status"] == "listed" and result["mint"] == mint
        assert (await pipeline.process(post))["status"] == "duplicate"
    assert calls.count("/coins/create-v2") == 1
