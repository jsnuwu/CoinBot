import argparse
import asyncio
from contextlib import suppress
import json

import pytest

from xpump.cli import execute
from xpump.config import Config
from xpump.store import Store


def test_render_environment_overrides_config(tmp_path, monkeypatch):
    config = tmp_path / "config.toml"
    config.write_text('mode = "preview"\nx_enabled = false\n')
    monkeypatch.setenv("XPUMP_AUTHOR_IDS", "123, 456")
    monkeypatch.setenv("XPUMP_X_ENABLED", "true")
    monkeypatch.setenv("XPUMP_CONFIGURE_X_ON_START", "true")
    monkeypatch.setenv("XPUMP_DATA_DIR", str(tmp_path / "disk"))
    monkeypatch.setenv("XPUMP_MIN_SCORE", "0.93")
    monkeypatch.setenv("XPUMP_MAX_LAUNCHES_24H", "3")
    cfg = Config.load(str(config))
    assert cfg.author_ids == ("123", "456")
    assert cfg.x_enabled and cfg.configure_x_on_start
    assert cfg.min_score == 0.93 and cfg.max_launches_24h == 3
    assert cfg.data_dir == str(tmp_path / "disk")
    assert cfg.mode == "preview"


@pytest.mark.parametrize("key,value", [
    ("XPUMP_X_ENABLED", "yes"), ("XPUMP_CONFIGURE_X_ON_START", "on"),
    ("XPUMP_MAX_LAUNCHES_24H", "infinity"), ("XPUMP_AUTHOR_IDS", "123,"),
    ("XPUMP_MIN_SCORE", "NaN"), ("XPUMP_MODE", "live"),
])
def test_bad_host_settings_fail_before_network(tmp_path, monkeypatch, key, value):
    config = tmp_path / "config.toml"
    config.write_text("")
    monkeypatch.setenv(key, value)
    with pytest.raises(ValueError):
        Config.load(str(config))


async def test_first_deploy_waits_without_keys_or_network(tmp_path, monkeypatch, capsys):
    config = tmp_path / "config.toml"
    config.write_text("data_dir = " + json.dumps(str(tmp_path / "disk")) + "\n")
    monkeypatch.setenv("XPUMP_X_ENABLED", "false")
    monkeypatch.setenv("XPUMP_CONFIGURE_X_ON_START", "true")
    for key in ("X_BEARER_TOKEN", "PINATA_JWT", "XPUMP_WALLET_FILE", "SOLANA_RPC_URL"):
        monkeypatch.delenv(key, raising=False)

    async def no_network(*args, **kwargs):
        pytest.fail("First deploy must not contact external providers")
    monkeypatch.setattr("httpx.AsyncClient.send", no_network)
    monkeypatch.setattr("xpump.cli.configure_rules", no_network)
    task = asyncio.create_task(execute(argparse.Namespace(config=str(config), command="run")))
    store = None
    try:
        for _ in range(100):
            await asyncio.sleep(0.01)
            if task.done():
                await task
                pytest.fail("Idle worker exited")
            if (tmp_path / "disk/preview/state.sqlite3").exists():
                if store is None:
                    store = Store(str(tmp_path / "disk/preview"))
                status = store.snapshot()
                if status["health"].get("worker_heartbeat"):
                    break
        else:
            pytest.fail("Worker did not become ready")
        assert status["health"]["feed"]["status"] == "disabled"
        assert status["health"]["mode"] == "preview"
        assert status["launches"] == []
        assert "waiting for configuration" in capsys.readouterr().out
    finally:
        task.cancel()
        with suppress(asyncio.CancelledError):
            await task
        if store:
            store.db.close()
