import json
import time

import httpx
import pytest
from solders.keypair import Keypair
from solders.pubkey import Pubkey

from xpump.publish import Publisher
from xpump.store import Blocked

from test_transactions import create_raw


@pytest.fixture
def wallet_env(monkeypatch, tmp_path):
    key = Keypair()
    path = tmp_path / "wallet.json"
    path.write_text(json.dumps(list(bytes(key))))
    path.chmod(0o600)
    monkeypatch.setenv("XPUMP_WALLET_FILE", str(path))
    monkeypatch.setenv("XPUMP_ENABLE_MAINNET", "true")
    monkeypatch.setenv("SOLANA_RPC_URL", "https://rpc.example.test")
    monkeypatch.setenv("PINATA_JWT", "test-only")
    return key


class Provider:
    def __init__(self, store, *, timeout=False, debit=10_000_000, finalized=False, failed=False):
        self.store, self.timeout, self.debit = store, timeout, debit
        self.finalized, self.failed = finalized, failed
        self.calls, self.sent = [], 0

    async def __call__(self, req):
        self.calls.append(str(req.url))
        if req.url.host == "uploads.pinata.cloud":
            return httpx.Response(200, json={"data": {"cid": "a" * 46}})
        body = json.loads(req.content)
        if req.url.host == "pumpportal.fun":
            assert body["amount"] == 0
            assert "privateKey" not in body
            meta = body["tokenMetadata"]
            return httpx.Response(200, content=create_raw(Pubkey.from_string(body["publicKey"]),
                Pubkey.from_string(body["mint"]), meta["name"], meta["symbol"], meta["uri"]))
        method = body["method"]
        if method == "getGenesisHash":
            result = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
        elif method == "getBalance":
            result = {"context": {"slot": 123}, "value": 100_000_000}
        elif method == "simulateTransaction":
            assert body["params"][1]["sigVerify"] is True
            result = {"value": {"err": None, "accounts": [{"lamports": 100_000_000 - self.debit}]}}
        elif method == "getFeeForMessage":
            result = {"value": 10000}
        elif method == "sendTransaction":
            self.sent += 1
            pending = self.store.pending()
            assert len(pending) == 1 and pending[0]["signature"]
            if self.timeout:
                raise httpx.ReadTimeout("DO_NOT_LOG_SECRET")
            assert body["params"][1]["skipPreflight"] is False
            result = pending[0]["signature"]
        elif method == "getSignatureStatuses":
            result = {"value": [{"confirmationStatus": "finalized"} if self.finalized else None]}
        elif method == "getTransaction":
            result = {"meta": {"err": {"code": 1} if self.failed else None,
                               "preBalances": [100_000_000], "postBalances": [100_000_000 - self.debit]}}
        else:
            pytest.fail("Unexpected RPC: " + method)
        return httpx.Response(200, json={"jsonrpc": "2.0", "id": 1, "result": result})


def test_mainnet_gate_required(live_cfg, store, monkeypatch):
    monkeypatch.delenv("XPUMP_ENABLE_MAINNET", raising=False)
    with pytest.raises(Blocked, match="mainnet_not_enabled"):
        Publisher(live_cfg, store, None)


async def test_launch_and_reconciliation(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        pub = Publisher(live_cfg, store, client)
        store.claim(post, time.time())
        result = await pub.publish(post, idea)
        assert result["status"] == "pending" and result["mint"] and result["signature"]
        await pub.reconcile()
        assert len(store.pending()) == 1
        provider.finalized = True
        await pub.reconcile()
        assert not store.pending()
        assert store.snapshot()["launches"][0]["status"] == "finalized"
        assert provider.sent == 1


async def test_timeout_does_not_create_a_second_token(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store, timeout=True)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        pub = Publisher(live_cfg, store, client)
        with pytest.raises(httpx.ReadTimeout):
            await pub.publish(post, idea)
        await pub.reconcile()
        with pytest.raises(Blocked, match="unresolved"):
            await pub.publish(post, idea)
        assert provider.sent == 1
        assert store.pending()[0]["signature"]


async def test_excess_cost_never_sent(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store, debit=50_000_000)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        with pytest.raises(Blocked, match="simulated_cost"):
            await Publisher(live_cfg, store, client).publish(post, idea)
    assert provider.sent == 0
    assert store.snapshot()["launches"][0]["status"] == "aborted"


async def test_crashed_presign_reservation_recovers_but_keeps_budget(live_cfg, store, wallet_env):
    store.reserve("1", live_cfg, time.time())
    provider = Provider(store)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        await Publisher(live_cfg, store, client).reconcile()
    assert not store.pending()
    assert store.snapshot()["reserved_lamports_total"] == 20_000_000


async def test_finalized_failure_still_consumes_budget(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store, finalized=True, failed=True)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        pub = Publisher(live_cfg, store, client)
        await pub.publish(post, idea)
        await pub.reconcile()
    assert store.snapshot()["launches"][0]["status"] == "failed"
    assert store.snapshot()["reserved_lamports_total"] == 20_000_000


async def test_kill_switch_during_preparation_never_sends(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store)

    async def transport(req):
        response = await provider(req)
        if req.url.host == "pumpportal.fun":
            (store.root / "STOP").touch()
        return response
    async with httpx.AsyncClient(transport=httpx.MockTransport(transport)) as client:
        with pytest.raises(Blocked, match="kill_switch"):
            await Publisher(live_cfg, store, client).publish(post, idea)
    assert provider.sent == 0


async def test_actual_overspend_stops_future_launches(live_cfg, store, post, idea, wallet_env):
    provider = Provider(store)
    async with httpx.AsyncClient(transport=httpx.MockTransport(provider)) as client:
        pub = Publisher(live_cfg, store, client)
        await pub.publish(post, idea)
        provider.finalized, provider.debit = True, 30_000_000
        await pub.reconcile()
    assert (store.root / "STOP").exists()
