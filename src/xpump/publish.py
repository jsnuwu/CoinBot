import base64
import json
import os
from pathlib import Path
import re
import stat
import time
from urllib.parse import urlparse

import httpx
from solders.keypair import Keypair
from solders.message import to_bytes_versioned
from solders.transaction import VersionedTransaction

from .art import render
from .config import lamports
from .store import Blocked
from .transaction import validate_transaction


class Publisher:
    def __init__(self, cfg, store, client: httpx.AsyncClient):
        if cfg.mode != "live" or os.getenv("XPUMP_ENABLE_MAINNET") != "true":
            raise Blocked("mainnet_not_enabled")
        missing = [key for key in ("SOLANA_RPC_URL", "PINATA_JWT", "XPUMP_WALLET_FILE") if not os.getenv(key)]
        if missing:
            raise Blocked("missing_settings:" + ",".join(missing))
        self.cfg, self.store, self.client = cfg, store, client
        self.rpc_url = os.environ["SOLANA_RPC_URL"]
        parsed = urlparse(self.rpc_url)
        if parsed.scheme != "https" or not parsed.hostname or parsed.username:
            raise Blocked("rpc_requires_https")
        self.pinata = os.environ["PINATA_JWT"]
        if not self.pinata:
            raise Blocked("pinata_key_missing")
        wallet_file = Path(os.environ["XPUMP_WALLET_FILE"])
        if stat.S_IMODE(wallet_file.stat().st_mode) & 0o077:
            raise Blocked("wallet_file_permissions_must_be_600")
        self.wallet = Keypair.from_bytes(bytes(json.loads(wallet_file.read_text())))

    async def rpc(self, method, params):
        r = await self.client.post(self.rpc_url, json={"jsonrpc": "2.0", "id": 1,
                                                      "method": method, "params": params})
        r.raise_for_status()
        value = r.json()
        if "error" in value or "result" not in value:
            raise Blocked("rpc_request_failed")
        return value["result"]

    async def upload(self, content: bytes, filename, mime):
        r = await self.client.post("https://uploads.pinata.cloud/v3/files",
                                   headers={"Authorization": f"Bearer {self.pinata}"},
                                   data={"network": "public"}, files={"file": (filename, content, mime)})
        r.raise_for_status()
        cid = r.json()["data"]["cid"]
        if not isinstance(cid, str) or not re.fullmatch(r"[A-Za-z0-9]{32,100}", cid):
            raise Blocked("invalid_ipfs_cid")
        return "https://ipfs.io/ipfs/" + cid

    async def publish(self, post, idea):
        self.store.reserve(post.root_id, self.cfg, time.time())
        try:
            if await self.rpc("getGenesisHash", []) != "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp":
                raise Blocked("rpc_is_not_mainnet")
            # Same lock/runner owns the wallet for the whole operation.
            wallet = self.wallet.pubkey()
            balance = await self.rpc("getBalance", [str(wallet), {"commitment": "confirmed"}])
            cap = lamports(self.cfg.max_launch_sol)
            reserve = lamports(self.cfg.wallet_reserve_sol)
            if balance["value"] < reserve + cap:
                raise Blocked("wallet_reserve")
            image_uri = await self.upload(render(idea), "meme.png", "image/png")
            uri = await self.upload(json.dumps(idea.metadata(post, image_uri)).encode(),
                                    "metadata.json", "application/json")
            mint = Keypair()
            response = await self.client.post("https://pumpportal.fun/api/trade-local", json={
                "publicKey": str(wallet), "action": "create",
                "tokenMetadata": {"name": idea.name, "symbol": idea.symbol, "uri": uri},
                "mint": str(mint.pubkey()), "denominatedInSol": "true", "amount": 0,
                "slippage": 0, "priorityFee": float(self.cfg.priority_fee_sol), "pool": "pump",
            })
            response.raise_for_status()
            if len(response.content) > 1232:
                raise Blocked("oversized_transaction")
            unsigned = validate_transaction(response.content, wallet, mint.pubkey(), idea.name,
                                            idea.symbol, uri, lamports(self.cfg.priority_fee_sol))
            signed = VersionedTransaction(unsigned.message, [self.wallet, mint])
            encoded = base64.b64encode(bytes(signed)).decode()
            simulation = await self.rpc("simulateTransaction", [encoded, {
                "encoding": "base64", "commitment": "confirmed", "sigVerify": True,
                "minContextSlot": balance["context"]["slot"],
                "accounts": {"encoding": "base64", "addresses": [str(wallet)]},
            }])
            result = simulation["value"]
            if result["err"] is not None or not result.get("accounts") or result["accounts"][0] is None:
                raise Blocked("simulation_failed")
            after = result["accounts"][0]["lamports"]
            # Extra fee margin is conservative even if the simulator already deducts it.
            fee = await self.rpc("getFeeForMessage", [base64.b64encode(to_bytes_versioned(unsigned.message)).decode(),
                                                     {"commitment": "confirmed"}])
            if fee["value"] is None:
                raise Blocked("fee_unavailable")
            estimated = max(0, balance["value"] - after) + fee["value"]
            if estimated > cap or after - fee["value"] < reserve:
                raise Blocked("simulated_cost_exceeds_budget")
            if (self.store.root / "STOP").exists():
                raise Blocked("kill_switch")
            if time.time() - post.created_at.timestamp() > self.cfg.max_post_age_seconds:
                raise Blocked("post_expired_during_preparation")
            signature = str(signed.signatures[0])
            self.store.prepared(post.root_id, str(mint.pubkey()), signature)
            # No fresh mint / transaction on retries, timeout or process restart.
            returned = await self.rpc("sendTransaction", [encoded, {
                "encoding": "base64", "skipPreflight": False, "preflightCommitment": "confirmed",
                "maxRetries": 0,
            }])
            if returned != signature:
                raise Blocked("unexpected_rpc_signature")
            return {"mint": str(mint.pubkey()), "signature": signature, "status": "pending"}
        except BaseException:
            row = next((r for r in self.store.pending() if r["root_id"] == post.root_id), None)
            if row and row["signature"] is None:
                self.store.outcome(post.root_id, "aborted")
            # Signed/broadcast-uncertain attempts remain pending and freeze new spending.
            raise

    async def reconcile(self):
        for row in self.store.pending():
            if not row["signature"]:
                # A crash before durable signature persistence cannot have sent a tx.
                self.store.outcome(row["root_id"], "aborted")
                continue
            result = await self.rpc("getSignatureStatuses", [[row["signature"]], {"searchTransactionHistory": True}])
            status = result["value"][0]
            if status is None or status.get("confirmationStatus") != "finalized":
                continue
            tx = await self.rpc("getTransaction", [row["signature"], {
                "commitment": "finalized", "encoding": "json", "maxSupportedTransactionVersion": 0,
            }])
            if tx is None or tx.get("meta") is None:
                continue
            meta = tx["meta"]
            debit = max(0, meta["preBalances"][0] - meta["postBalances"][0])
            self.store.outcome(row["root_id"], "failed" if meta["err"] is not None else "finalized", debit)
            if debit > row["reserved"]:
                (self.store.root / "STOP").touch()
