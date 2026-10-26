from dataclasses import replace
from datetime import timedelta
from io import BytesIO
import json

import httpx
from PIL import Image
import pytest

from xpump.art import render
from xpump.ideas import TemplateIdeas
from xpump.models import Idea, utcnow
from xpump.pipeline import Pipeline
from xpump.public_x import PublicMonitor
from test_free_web import embed, tweet


@pytest.mark.parametrize("text,theme", [
    ("Starship to Mars", "space"), ("Cats everywhere", "cat"), ("Doge says hello", "dog"),
    ("Coffee on the moon", "coffee"), ("Taco Tuesday", "taco"), ("Penguins on ice", "penguin"),
    ("Golden morning", "gold"), ("AI and Optimus robots", "robot"), ("Pizza party", "pizza"),
    ("New tariffs announced", "tariff"),
])
async def test_topic_name_and_art_follow_post_content(post, text, theme):
    idea = await TemplateIdeas().generate(replace(post, text=text))
    assert idea.theme == theme
    assert 2 <= len(idea.symbol) <= 10 and len(idea.name.encode()) <= 32
    with Image.open(BytesIO(render(idea))) as image:
        assert image.format == "PNG" and image.size == (768, 768)


def test_each_topic_has_its_own_illustration():
    # Constant ticker fixes palette/background so distinct output must come from the drawing.
    images = {render(Idea("Test Meme", "TEST", "a cartoon", 0.9, theme=theme))
              for theme, *_ in TemplateIdeas.THEMES}
    assert len(images) == len(TemplateIdeas.THEMES)


@pytest.mark.parametrize("text", ["See https://example.org/rocket", "Hello @doge", "Private key: rocket"])
async def test_links_handles_and_sensitive_news_are_not_meme_evidence(post, text):
    assert await TemplateIdeas().generate(replace(post, text=text)) is None


async def test_distinct_topics_outrank_repeating_one_keyword(post):
    idea = await TemplateIdeas().generate(replace(post, text="Coffee coffee coffee! Starship rocket to Mars."))
    assert idea.theme == "space"


async def test_one_limited_account_does_not_delay_other_accounts(cfg, store):
    cfg = replace(cfg, public_x_accounts="testaccount:123,other:456", public_x_poll_seconds=60)
    monitor = PublicMonitor(cfg, Pipeline(cfg, store, TemplateIdeas()))
    calls = {"testaccount": 0, "other": 0}

    async def handler(request):
        handle = str(request.url).rsplit("/", 1)[1]
        calls[handle] += 1
        return httpx.Response(403) if handle == "other" else httpx.Response(200, text=embed(tweet()))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        assert await monitor.check(client, now=1000) is True
        assert await monitor.check(client, now=1060) is True
        await monitor.check(client, now=1061)  # Neither account is due; no extra request.
    assert calls == {"testaccount": 2, "other": 1}
    assert monitor.next_due == {"testaccount": 1120, "other": 1900}
    assert {a["handle"]: a["status"] for a in monitor.state["accounts"]} == {
        "testaccount": "readable", "other": "unavailable"}


async def test_newest_post_gets_preview_budget_first(cfg, store):
    cfg = replace(cfg, public_x_accounts="testaccount:123", max_launches_24h=1)
    start = utcnow() - timedelta(seconds=10)
    monitor = PublicMonitor(cfg, Pipeline(cfg, store, TemplateIdeas(), started_at=start))
    older = tweet(id_str="10", full_text="Coffee", created_at=(start + timedelta(seconds=1)).strftime("%a %b %d %H:%M:%S %z %Y"))
    newer = tweet(id_str="11", full_text="Mars rocket", created_at=(start + timedelta(seconds=5)).strftime("%a %b %d %H:%M:%S %z %Y"))

    async def handler(request):
        return httpx.Response(200, text=embed(older, newer))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await monitor.check(client)
    launches = store.snapshot()["launches"]
    assert len(launches) == 1 and launches[0]["root_id"] == "11"
    metadata = json.loads((store.root / "previews/11/metadata.json").read_text())
    assert "rocket" in metadata["description"]
