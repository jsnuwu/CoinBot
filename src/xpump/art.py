import hashlib
from io import BytesIO

from PIL import Image, ImageDraw, ImageFont

from .models import Idea


def render(idea: Idea) -> bytes:
    """Local template: no paid image request, portraits, logos or external fonts."""
    digest = hashlib.sha256(idea.symbol.encode()).digest()
    accent = (120 + digest[0] % 100, 70 + digest[1] % 80, 220)
    im = Image.new("RGB", (768, 768), (13, 12, 24))
    draw = ImageDraw.Draw(im)
    for i in range(24):
        x, y = (digest[i] * 3) % 768, (digest[(i + 5) % 32] * 3) % 768
        draw.ellipse((x, y, x + 4, y + 4), fill=(100, 100, 140))
    draw.ellipse((180, 140, 588, 548), fill=accent)
    draw.ellipse((260, 250, 300, 305), fill=(13, 12, 24))
    draw.ellipse((468, 250, 508, 305), fill=(13, 12, 24))
    draw.arc((290, 310, 478, 435), 0, 180, fill=(13, 12, 24), width=12)
    draw.text((384, 575), idea.symbol, anchor="mt", font=ImageFont.load_default(size=58), fill="white")
    draw.text((384, 665), "UNOFFICIAL COMMUNITY MEME", anchor="mt",
              font=ImageFont.load_default(size=22), fill=(180, 174, 202))
    out = BytesIO()
    im.save(out, format="PNG")
    return out.getvalue()
