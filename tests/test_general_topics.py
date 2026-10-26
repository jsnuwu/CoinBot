from dataclasses import replace
from pathlib import Path
from io import BytesIO

from PIL import Image
import pytest

from xpump.art import render
from xpump.ideas import TemplateIdeas
from xpump.models import Idea

CASES = [line.split("\t") for line in (Path(__file__).parents[1] / "android/topic-cases.tsv").read_text().splitlines()
         if line.strip() and not line.startswith("# ")]


@pytest.mark.parametrize("text,genre,subject", CASES)
async def test_news_topics_do_not_require_a_meme_keyword(post, text, genre, subject):
    idea = await TemplateIdeas().generate(replace(post, text=text))
    assert idea is not None
    assert idea.theme == genre
    assert idea.name.startswith(subject + " ")
    assert len(idea.name) <= 25 and len(idea.symbol) <= 10
    assert "Unofficial" in idea.metadata(post, "meme.png")["description"]


@pytest.mark.parametrize("text", ["ok thanks", "Hello @doge", "See https://example.test/rocket", "private key: rocket", "seed phrase: secret", "password=secret", "api_key: secret"])
async def test_filler_and_credentials_still_have_no_public_metadata(post, text):
    assert await TemplateIdeas().generate(replace(post, text=text)) is None


def test_new_categories_have_distinct_neutral_illustrations():
    images = set()
    for theme in {case[1] for case in CASES}:
        png = render(Idea("Example Signal", "EXAMPLE", "editorial illustration", 0.9, theme))
        with Image.open(BytesIO(png)) as image:
            assert image.size == (768, 768)
        images.add(png)
    assert len(images) == len({case[1] for case in CASES})
