"""Experimental zero-buy listing flow observed in Pump.fun's own website.

This adapter cannot sign/send Solana transactions or buy tokens. A website session
is required. Website compatibility must be verified with the owner's account.
"""
import os
import time

import httpx
from solders.pubkey import Pubkey

from .art import render
from .store import Blocked

API = "https://frontend-api-v3.pump.fun"
TOKEN_2022 = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"


class FreePumpCreator:
    def __init__(self, cfg, store, client):
        self.cfg, self.store, self.client = cfg, store, client
        self.token = os.getenv("PUMP_ACCESS_TOKEN", "")
        self.state = "disabled" if not cfg.free_pump_enabled else "authentication_required"
        if cfg.free_pump_enabled and self.token and cfg.pump_creator_address:
            self.state = "unverified_session"
        store.db.execute("CREATE TABLE IF NOT EXISTS free_creations ("
                         "root_id TEXT PRIMARY KEY, created REAL NOT NULL, status TEXT NOT NULL, mint TEXT)")
        if cfg.free_pump_enabled and store.db.execute(
                "SELECT 1 FROM free_creations WHERE status IN ('sending','unknown')").fetchone():
            self.state = "unknown_creation_blocks_queue"

    def snapshot(self):
        return {"enabled": self.cfg.free_pump_enabled, "state": self.state,
                "creator": self.cfg.pump_creator_address or None,
                "records": [dict(r) for r in self.store.db.execute(
                    "SELECT * FROM free_creations ORDER BY created DESC LIMIT 20")]}

    async def request(self, method, path, **kwargs):
        # Credentials go to this one first-party host; redirects are never followed.
        r = await self.client.request(method, API + path, follow_redirects=False,
                                      headers={"Authorization": "Bearer " + self.token,
                                               "Cookie": "auth_token=" + self.token,
                                               "Origin": "https://pump.fun"}, **kwargs)
        if r.status_code in (401, 403):
            self.state = "authentication_required"
            raise Blocked("pump_authentication_required")
        if r.status_code == 429:
            raise Blocked("pump_rate_limited")
        if not 200 <= r.status_code < 300:
            raise Blocked("pump_website_request_rejected")
        return r.json()

    async def create(self, post, idea):
        if not self.cfg.free_pump_enabled:
            raise Blocked("free_pump_disabled")
        if not self.token or not self.cfg.pump_creator_address:
            raise Blocked("pump_authentication_required")
        if self.store.db.execute("SELECT 1 FROM free_creations WHERE status IN ('sending','unknown')").fetchone():
            self.state = "unknown_creation_blocks_queue"
            raise Blocked("pump_previous_creation_unknown")
        if self.store.db.execute("SELECT 1 FROM free_creations WHERE root_id=?", (post.root_id,)).fetchone():
            raise Blocked("pump_duplicate_creation")
        profile = await self.request("GET", "/auth/my-profile")
        if profile.get("address") != self.cfg.pump_creator_address:
            self.state = "wrong_account"
            raise Blocked("pump_wrong_creator_account")
        self.state = "authenticated"
        metadata = idea.metadata(post, "")
        uploaded = await self.request("POST", "/ipfs/token-metadata", data={
            "name": idea.name, "symbol": idea.symbol, "description": metadata["description"],
            "showName": "true", "website": post.url,
        }, files={"file": ("meme.png", render(idea), "image/png")})
        uri = uploaded.get("metadataUri", "")
        image = uploaded.get("metadata", {}).get("image", "")
        if not isinstance(uri, str) or not uri.startswith(("https://", "ipfs://")) or not image:
            raise Blocked("pump_metadata_response_invalid")
        if (self.store.root / "STOP").exists():
            raise Blocked("kill_switch")
        if time.time() - post.created_at.timestamp() > self.cfg.max_post_age_seconds:
            raise Blocked("post_expired_during_preparation")
        # Durable intent before the only non-idempotent request. No automatic POST retry.
        self.store.db.execute("INSERT INTO free_creations VALUES (?,?,?,NULL)",
                              (post.root_id, time.time(), "sending"))
        try:
            response = await self.request("POST", "/coins/create-v2", json={
                "name": idea.name, "ticker": idea.symbol, "description": metadata["description"],
                "website": post.url, "showName": True, "metadataUri": uri,
                "image": image, "tokenProgram": TOKEN_2022,
            })
            mint = str(Pubkey.from_string(response["mint"]))
        except (httpx.HTTPError, ValueError, KeyError, TypeError):
            self.store.db.execute("UPDATE free_creations SET status='unknown' WHERE root_id=?", (post.root_id,))
            self.state = "unknown_creation_blocks_queue"
            return {"status": "free_creation_unknown"}
        except Blocked:
            # Even an error response is not proof a remote write did not take place.
            self.store.db.execute("UPDATE free_creations SET status='unknown' WHERE root_id=?", (post.root_id,))
            self.state = "unknown_creation_blocks_queue"
            raise
        self.store.db.execute("UPDATE free_creations SET status='listed',mint=? WHERE root_id=?", (mint, post.root_id))
        self.state = "listed"
        return {"status": "listed", "mint": mint, "coin_url": "https://pump.fun/coin/" + mint}
