import hashlib
from io import BytesIO

from PIL import Image, ImageDraw, ImageFont

from .models import Idea


def render(idea: Idea) -> bytes:
    """Draw a topic-specific original cartoon locally, without a model or paid request."""
    digest = hashlib.sha256(idea.symbol.encode()).digest()
    accent = (120 + digest[0] % 100, 70 + digest[1] % 80, 220)
    ink, cream, gold = (22, 19, 36), (252, 239, 215), (255, 194, 74)
    im = Image.new("RGB", (768, 768), (13, 12, 24))
    d = ImageDraw.Draw(im)
    for i in range(24):
        x, y = (digest[i] * 3) % 768, (digest[(i + 5) % 32] * 3) % 768
        d.ellipse((x, y, x + 4, y + 4), fill=(100, 100, 140))
    d.ellipse((100, 65, 668, 570), fill=(34, 27, 51))

    def eyes(y=300, left=314, right=454):
        for x in (left, right):
            d.ellipse((x - 15, y - 20, x + 15, y + 20), fill=ink)
            d.ellipse((x - 6, y - 13, x + 1, y - 6), fill="white")

    def smile(y=330):
        d.arc((344, y, 424, y + 50), 0, 180, fill=ink, width=8)

    theme = idea.theme
    if theme == "space":
        d.polygon([(335, 410), (384, 548), (434, 410)], fill=gold)
        d.polygon([(352, 410), (384, 500), (414, 410)], fill=(255, 109, 75))
        d.polygon([(300, 320), (237, 455), (320, 423)], fill=accent)
        d.polygon([(468, 320), (531, 455), (448, 423)], fill=accent)
        d.rounded_rectangle((300, 190, 468, 440), radius=75, fill=cream)
        d.polygon([(300, 230), (384, 98), (468, 230)], fill=accent)
        d.ellipse((330, 240, 438, 348), fill=ink)
        d.ellipse((343, 253, 425, 335), fill=(100, 201, 244))
        d.arc((351, 276, 417, 320), 0, 180, fill=ink, width=6)
    elif theme in ("cat", "dog"):
        if theme == "cat":
            d.polygon([(229, 270), (220, 125), (342, 206)], fill=accent)
            d.polygon([(426, 206), (548, 125), (539, 270)], fill=accent)
            d.polygon([(246, 220), (241, 163), (301, 208)], fill=(250, 159, 181))
            d.polygon([(467, 208), (527, 163), (522, 220)], fill=(250, 159, 181))
        else:
            d.ellipse((159, 207, 309, 461), fill=(161, 100, 57))
            d.ellipse((459, 207, 609, 461), fill=(161, 100, 57))
        d.ellipse((225, 184, 543, 483), fill=accent if theme == "cat" else gold)
        eyes()
        d.ellipse((345, 345, 423, 389), fill=ink)
        smile(388)
        if theme == "cat":
            for y in (350, 379):
                d.line((195, y - 15, 305, y), fill=cream, width=5)
                d.line((463, y, 573, y - 15), fill=cream, width=5)
    elif theme == "coffee":
        d.ellipse((458, 250, 611, 408), outline=cream, width=27)
        d.rounded_rectangle((225, 224, 510, 456), radius=36, fill=cream)
        d.ellipse((228, 208, 507, 266), fill=gold)
        d.ellipse((246, 220, 490, 254), fill=(99, 53, 45))
        eyes(330, 300, 435)
        smile(359)
        d.ellipse((200, 460, 558, 493), fill=accent)
        for x in (287, 363, 439):
            d.arc((x - 20, 105, x + 20, 197), 80, 265, fill=cream, width=7)
    elif theme == "robot":
        d.line((384, 145, 384, 207), fill=cream, width=12)
        d.ellipse((364, 109, 404, 149), fill=gold)
        d.rounded_rectangle((208, 225, 560, 470), radius=48, fill=accent)
        d.rectangle((174, 294, 208, 388), fill=gold)
        d.rectangle((560, 294, 594, 388), fill=gold)
        d.rounded_rectangle((250, 267, 518, 365), radius=22, fill=ink)
        for x in (315, 453):
            d.ellipse((x - 20, 291, x + 20, 333), fill=(113, 240, 220))
        d.rounded_rectangle((311, 394, 457, 425), radius=8, fill=cream)
        for x in (345, 384, 423):
            d.line((x, 394, x, 425), fill=ink, width=4)
    elif theme == "penguin":
        d.ellipse((216, 130, 552, 510), fill=ink, outline=accent, width=14)
        d.ellipse((269, 252, 499, 491), fill=cream)
        eyes(262, 327, 441)
        d.polygon([(355, 312), (413, 312), (384, 346)], fill=gold)
        d.ellipse((247, 477, 365, 522), fill=gold)
        d.ellipse((403, 477, 521, 522), fill=gold)
    elif theme == "pizza":
        d.polygon([(222, 195), (568, 195), (384, 516)], fill=gold)
        d.line((222, 195, 568, 195), fill=(196, 120, 69), width=43)
        for x, y in ((301, 252), (453, 262), (379, 396)):
            d.ellipse((x - 25, y - 25, x + 25, y + 25), fill=(215, 78, 71))
        eyes(320, 345, 425)
        smile(337)
    elif theme == "taco":
        d.pieslice((167, 166, 601, 557), 180, 360, fill=(104, 189, 117))
        for x, y in ((247, 299), (327, 244), (426, 238), (509, 294)):
            d.ellipse((x - 22, y - 22, x + 22, y + 22), fill=(236, 92, 72))
        d.pieslice((171, 227, 597, 585), 180, 360, fill=gold)
        eyes(339, 303, 465)
        smile(350)
    elif theme == "gold":
        d.ellipse((232, 284, 525, 485), fill=gold)
        d.rounded_rectangle((412, 190, 498, 403), radius=38, fill=gold)
        d.ellipse((392, 137, 520, 265), fill=gold)
        d.polygon([(503, 192), (583, 221), (503, 240)], fill=(240, 124, 59))
        d.ellipse((465, 177, 482, 197), fill=ink)
        d.arc((268, 313, 452, 440), 0, 170, fill=(195, 129, 51), width=9)
        for x in (308, 427):
            d.line((x, 469, x, 514), fill=(240, 124, 59), width=10)
            d.line((x - 12, 514, x + 32, 514), fill=(240, 124, 59), width=10)
    elif theme == "tariff":
        d.rectangle((225, 238, 543, 476), fill=(194, 134, 84))
        d.polygon([(225, 238), (384, 167), (543, 238), (384, 299)], fill=(230, 178, 120))
        d.line((384, 299, 384, 476), fill=(151, 91, 61), width=8)
        d.polygon([(225, 238), (171, 180), (325, 125), (384, 167)], fill=gold)
        d.polygon([(384, 167), (443, 125), (598, 180), (543, 238)], fill=accent)
        eyes(350)
        d.ellipse((366, 390, 402, 431), fill=ink)
    elif theme == "news":
        d.rounded_rectangle((195, 160, 573, 475), radius=16, fill=cream)
        d.rectangle((220, 185, 548, 235), fill=accent)
        for y in range(270, 440, 38):
            d.rectangle((225, y, 375, y + 9), fill=ink)
            d.rectangle((408, y, 540, y + 9), fill=ink)
    elif theme == "politics":
        d.polygon([(180, 245), (384, 130), (588, 245)], fill=cream)
        d.rectangle((180, 450, 588, 485), fill=accent)
        for x in range(240, 511, 90):
            d.rectangle((x, 260, x + 30, 448), fill=cream)
    elif theme == "finance":
        for x, y, color in ((210, 350, accent), (330, 270, cream), (450, 180, gold)):
            d.rounded_rectangle((x, y, x + 80, 475), radius=8, fill=color)
        d.rectangle((190, 490, 565, 500), fill=cream)
    elif theme == "sports":
        d.ellipse((214, 150, 554, 490), fill=cream)
        d.ellipse((329, 265, 439, 375), fill=ink)
        for x, y in ((384, 195), (503, 281), (458, 421), (310, 421), (265, 281)):
            d.ellipse((x - 24, y - 24, x + 24, y + 24), fill=ink)
    elif theme == "transport":
        d.rounded_rectangle((230, 140, 538, 465), radius=45, fill=accent)
        d.rounded_rectangle((265, 190, 503, 315), radius=12, fill=(100, 201, 244))
        for x in (295, 473):
            d.ellipse((x - 22, 363, x + 22, 407), fill=gold)
        for x in (255, 483):
            d.rectangle((x, 475, x + 30, 525), fill=cream)
    elif theme == "science":
        d.ellipse((205, 255, 563, 385), outline=accent, width=12)
        d.ellipse((310, 140, 458, 500), outline=cream, width=12)
        d.line((245, 200, 523, 440), fill=accent, width=10)
        d.ellipse((348, 284, 420, 356), fill=gold)
    elif theme == "weather":
        d.ellipse((205, 145, 375, 315), fill=gold)
        for x, y, size in ((340, 330, 86), (435, 290, 100), (515, 345, 70)):
            d.ellipse((x - size, y - size, x + size, y + size), fill=cream)
        d.rounded_rectangle((265, 330, 545, 410), radius=18, fill=cream)
        for x in range(300, 530, 75):
            d.rounded_rectangle((x, 440, x + 12, 495), radius=6, fill=(100, 201, 244))
    elif theme == "health":
        d.ellipse((214, 150, 554, 490), fill=accent)
        d.rectangle((350, 200, 418, 440), fill=cream)
        d.rectangle((265, 285, 503, 355), fill=cream)
    elif theme == "technology":
        d.rounded_rectangle((260, 195, 508, 445), radius=20, fill=accent)
        d.rounded_rectangle((290, 225, 478, 415), radius=10, fill=ink)
        for x in range(285, 500, 50):
            d.rectangle((x, 155, x + 15, 195), fill=cream)
            d.rectangle((x, 445, x + 15, 485), fill=cream)
        for y in range(235, 420, 50):
            d.rectangle((220, y, 260, y + 15), fill=cream)
            d.rectangle((508, y, 548, y + 15), fill=cream)
    else:
        d.ellipse((200, 130, 568, 510), fill=accent)
        eyes()
        smile(340)

    if theme in {"news", "politics", "finance", "sports", "transport", "science", "weather", "health", "technology"}:
        subject = idea.name.split()[0]
        headline = ImageFont.load_default(size=34)
        d.text((384, 520), subject, anchor="mt", font=headline, fill=cream)

    size = 58
    font = ImageFont.load_default(size=size)
    while d.textbbox((0, 0), idea.symbol, font=font)[2] > 660:
        size -= 2
        font = ImageFont.load_default(size=size)
    d.text((384, 586), idea.symbol, anchor="mt", font=font, fill="white")
    d.text((384, 674), "UNOFFICIAL COMMUNITY MEME", anchor="mt",
           font=ImageFont.load_default(size=22), fill=(180, 174, 202))
    out = BytesIO()
    im.save(out, format="PNG")
    return out.getvalue()
