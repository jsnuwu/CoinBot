"""Cheap local topic extraction. This is a heuristic, not a language model or trend score."""
import re
import unicodedata

STOP = set((
    "the a an and or but to of in on at for from with without by as is are was were be been being "
    "this that these those it its they them their we us our you your he she his her i my me "
    "new latest breaking just news update says said say will would could should may might can "
    "has have had do does did not no yes ok okay hey now today tonight tomorrow yesterday very more most "
    "some any all much many other another about over under into out than then here there "
    "announces announced announce opens opened launches launched reveals revealed after before "
    "please follow subscribe retweet share like thanks thank hello good morning evening see read "
    "official endorsed guarantee guaranteed profit investment free claim wallet seed password "
    "trump donald president elon musk mario nawfal "
    "der die das ein eine einer eines einem einen und oder aber mit ohne von vom zum zur am im "
    "ist sind war waren wird werden wurde wurden hat haben hatte den dem des dieser diese dieses "
    "heute jetzt gestern morgen neu neue neuen neues sagt sagte meldet berichtet bekannt gibt "
    "bitte danke hallo mehr auch noch nur nicht kein keine fuer fur uber ueber"
).split())

GENRES = (
    ("sports", "football|soccer|basketball|tennis|olympics|champion|tournament|sport|fussball", "Kick"),
    ("transport", "train|railway|rail|railroad|metro|airport|airline|flight|transport|bahn|zug", "Rail"),
    ("finance", "bitcoin|ethereum|crypto|bank|banking|stock|stocks|inflation|market|markets|economy|gdp|finance|wirtschaft", "Pulse"),
    ("science", "quantum|fusion|physics|research|scientist|science|energy|forschung", "Lab"),
    ("weather", "storm|hurricane|flood|weather|rain|snow|climate|wetter|sturm", "Cloud"),
    ("health", "hospital|vaccine|medicine|medical|health|healthcare|clinic|gesundheit", "Care"),
    ("politics", "senate|election|elections|government|parliament|congress|politics|minister|wahl|wahlen|regierung|parlament|abstimmung|midterms|verteidigungsminister", "Brief"),
    ("technology", "chip|chips|software|smartphone|iphone|android|computer|technology|technologie|internet", "Byte"),
    ("news", ".", "Signal"),
)
SERIOUS_NEWS = re.compile(r"\b(killed|dead|death|shooting|attack|war|suicide|murder|rape|krieg|get[oö]tet|angriff|angreifen|vergeltung)\b", re.I)
ENTITIES = set("iran israel usa china japan india indien russia russland ukraine germany deutschland seoul berlin france frankreich brazil brasilien canada kanada london paris tokyo bitcoin ethereum solana nvidia samsung tesla apple google openai nato nasa".split())


def general_topic(text):
    normalized = "".join(c for c in unicodedata.normalize("NFD", text) if not unicodedata.combining(c)).replace("ß", "ss")
    subject, best = None, -1
    for match in re.finditer(r"#?[A-Za-z][A-Za-z0-9]{1,23}", normalized):
        raw = match.group()
        word = raw.lstrip("#")
        if word.lower() in STOP or len(word) < 3 and not re.fullmatch(r"[A-Z]{2,6}", word):
            continue
        score = (40 if raw.startswith("#") else 0) + (15 if re.fullmatch(r"[A-Z]{2,6}", word) else 0)
        score += (20 if re.fullmatch(r"[A-Z][a-z]+", word) else 0) + min(len(word), 10)
        if word.lower() in ENTITIES:
            score = 75
        if raw.startswith("#"):
            score += 100
        if score > best:
            subject, best = word, score
    if subject is None:
        return None
    genre = GENRES[-1]
    genre = next((g for g in GENRES if (not SERIOUS_NEWS.search(text) or g[0] == "politics")
                  and re.search(r"\b(" + g[1] + r")\b", normalized, re.I)), genre)
    return subject, genre[0], genre[2]
