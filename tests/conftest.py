from dataclasses import replace
from datetime import timedelta

import pytest

from xpump.config import Config
from xpump.models import Idea, Post, utcnow
from xpump.store import Store


@pytest.fixture
def cfg(tmp_path):
    return Config(data_dir=str(tmp_path), author_ids=("123",), cooldown_seconds=0)


@pytest.fixture
def live_cfg(cfg):
    return replace(cfg, mode="live", max_launch_sol="0.02", max_24h_sol="0.04", max_total_sol="0.06")


@pytest.fixture
def store(tmp_path):
    s = Store(str(tmp_path))
    yield s
    s.db.close()


@pytest.fixture
def post():
    return Post("100", "100", "123", "Coffee on the moon!", utcnow() - timedelta(seconds=1))


@pytest.fixture
def idea():
    return Idea("Lunar Pop", "LPOP", "a cartoon rocket", 0.9)
