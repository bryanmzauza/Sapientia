"""Texture specs for every Sapientia item and block.

`lookup(id)` returns a Spec whose `render()` produces either a flat item sprite
or the faces of a block model. Masks and glyphs use the character legend in
art.py.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from typing import Callable

from PIL import Image

import art
from art import NAMED_RGBA, Ramp, outline, ramp, render_mask, tier_ramp

GENERATED = "minecraft:item/generated"
HANDHELD = "minecraft:item/handheld"


@dataclass
class Rendered:
    kind: str  # "item" | "block"
    sprite: Image.Image | None = None
    parent: str = GENERATED
    faces: dict[str, Image.Image] = field(default_factory=dict)


@dataclass
class Spec:
    render: Callable[[], Rendered]


def item(fn: Callable[[], Image.Image], parent: str = GENERATED) -> Spec:
    return Spec(lambda: Rendered("item", sprite=fn(), parent=parent))


def block(fn: Callable[[], dict[str, Image.Image]]) -> Spec:
    return Spec(lambda: Rendered("block", faces=fn()))


def recolor(mask: list[str], mapping: dict[str, str]) -> list[str]:
    return ["".join(mapping.get(c, c) for c in row) for row in mask]


def pips(img: Image.Image, tier: int, y: int = 15) -> Image.Image:
    """Tier indicator: one white dot per tier along the bottom-left edge."""
    px = img.load()
    for i in range(tier):
        px[1 + i * 2, y] = NAMED_RGBA["w"]
    return img


# =============================================================================
# Metals
# =============================================================================

METAL_COLORS = {
    "copper": "#c8703a",
    "tin": "#b9c3c9",
    "zinc": "#98b0b6",
    "lead": "#6b6f8a",
    "silver": "#d8dde3",
    "nickel": "#c9c29a",
    "aluminum": "#c7ccd4",
    "silicon": "#5a6478",
    "titanium": "#a9a0b8",
    "lithium": "#d9c4cc",
    "bronze": "#b07a3a",
    "brass": "#d4b24c",
    "electrum": "#d8cf7a",
    "stainless_steel": "#a6adb4",
    "damascus_steel": "#77716d",
    "nichrome": "#8c9a8e",
}

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "....00000000....",
    "...0555555440...",
    "..054444444430..",
    ".05444444443330.",
    ".03333333333220.",
    ".02222222222210.",
    ".02222222222210.",
    ".01111111111110.",
    "..000000000000..",
    "................",
    "................",
    "................",
]

RAW = [
    "................",
    "................",
    "................",
    ".....0000.......",
    "....045540......",
    "...04554430.00..",
    "..0455443330450.",
    "..0544332330540.",
    ".04433232222330.",
    ".04332222322210.",
    ".03322122221110.",
    ".02221111211100.",
    "..002111111100..",
    "....0000000.....",
    "................",
    "................",
]

DUST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......00.......",
    ".....004500.....",
    "....04544430....",
    "...0454433330...",
    "..044433332220..",
    ".04333322221110.",
    ".01111111111110.",
    "..000000000000..",
    "................",
    "................",
    "................",
]

WIRE = [
    "................",
    "................",
    "................",
    "....00000000....",
    "...0xssssssx0...",
    "....05555550....",
    "....02222220....",
    "....04444440000.",
    "....022222204430",
    "....03333330.00.",
    "....01111110....",
    "...0xssssssx0...",
    "....00000000....",
    "................",
    "................",
    "................",
]

ROD = [
    "................",
    ".............00.",
    "............0540",
    "...........05430",
    "..........05430.",
    ".........05430..",
    "........05430...",
    ".......05430....",
    "......05430.....",
    ".....05430......",
    "....05430.......",
    "...05430........",
    "..05430.........",
    ".02430..........",
    ".0220...........",
    "..00............",
]


def _canvas() -> Image.Image:
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _segment_dist(px: float, py: float, ax: float, ay: float, bx: float, by: float) -> tuple[float, float, float]:
    """Distance from p to segment ab, the parameter t along it, and the signed side."""
    vx, vy = bx - ax, by - ay
    length2 = vx * vx + vy * vy
    t = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / length2))
    cx, cy = ax + t * vx, ay + t * vy
    side = (vx * (py - ay) - vy * (px - ax)) / math.sqrt(length2)
    return math.hypot(px - cx, py - cy), t, side


def tube(color_at: Callable[[float, float], tuple], start=(2.0, 13.0), end=(13.0, 2.0),
         half_width: float = 1.9, edge=None) -> Image.Image:
    """Diagonal tube; color_at(t, side) returns an RGBA for each pixel."""
    img = _canvas()
    p = img.load()
    for y in range(16):
        for x in range(16):
            d, t, side = _segment_dist(x, y, *start, *end)
            if d <= half_width:
                p[x, y] = color_at(t, side / half_width)
    return outline(img, edge or NAMED_RGBA["k"])


def shaded(r: Ramp, side: float) -> tuple:
    # side < 0 is the upper-left (lit) edge of a bottom-left -> top-right tube.
    if side < -0.45:
        return r[5]
    if side < 0.05:
        return r[4]
    if side < 0.5:
        return r[3]
    return r[2]


def metal_plate(r: Ramp) -> Image.Image:
    img = _canvas()
    p = img.load()
    for y in range(4, 11):
        off = (10 - y) // 2
        for x in range(1 + off, 12 + off):
            u = (x - 1 - off) / 10
            v = (y - 4) / 6
            light = 1 - (u + v) / 2
            p[x, y] = r[5] if (y == 4 or x == 1 + off) else r[3 + round(light)]
    for y in (11, 12):
        for x in range(1, 12):
            p[x, y] = r[2] if y == 11 else r[1]
    for y in range(8, 12):
        x = 12 + (10 - min(y, 10)) // 2
        if y >= 11:
            x = 12
        p[x, y] = r[1]
    return outline(img, r[0])


def metal_screw(r: Ramp) -> Image.Image:
    img = _canvas()
    p = img.load()
    for y in range(16):
        for x in range(16):
            d, t, side = _segment_dist(x, y, 3, 12.5, 10, 5.5)
            if d <= 1.25:
                thread = int(t * 9) % 2 == 0
                p[x, y] = r[4] if thread and side < 0 else (r[3] if thread else r[2])
    cx, cy = 11.2, 4.3
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) <= 2.9:
                light = -((x - cx) + (y - cy)) / 5.8
                p[x, y] = r[max(2, min(5, 4 + round(light * 2)))]
    for i in range(-2, 3):
        p[round(cx + i * 0.7), round(cy + i * 0.7)] = r[1]
    return outline(img, r[0])


def metal_block_face(r: Ramp, seed: int) -> Image.Image:
    img = Image.new("RGBA", (16, 16), r[3])
    p = img.load()
    for y in range(16):
        for x in range(16):
            n = art._noise(x, y, seed)
            if n < 30:
                p[x, y] = r[2]
            elif n > 235:
                p[x, y] = r[4]
    for i in range(16):
        p[i, 0] = r[4]
        p[0, i] = r[4]
        p[i, 15] = r[1]
        p[15, i] = r[1]
    for i in range(1, 15):
        p[i, 1] = r[5]
        p[1, i] = r[5]
        p[i, 14] = r[2]
        p[14, i] = r[2]
    for i in range(4, 12):
        p[i, 4] = r[2]
        p[4, i] = r[2]
        p[i, 11] = r[4]
        p[11, i] = r[4]
    return img


def damascus(img: Image.Image, r: Ramp) -> Image.Image:
    """Folded-steel waves: darkens pixels along a sine band."""
    p = img.load()
    index = {c: i for i, c in enumerate(r)}
    for y in range(16):
        for x in range(16):
            c = p[x, y]
            i = index.get(c)
            if i is None or i < 2:
                continue
            if math.sin((x + 2.2 * math.sin(y / 2.3)) * 1.4) > 0.55:
                p[x, y] = r[i - 1]
    return img


def metal_spec(metal: str, form: str) -> Spec:
    r = ramp(METAL_COLORS[metal])

    def finish(img: Image.Image) -> Image.Image:
        return damascus(img, r) if metal == "damascus_steel" else img

    if form == "block":
        return block(lambda: {"all": finish(metal_block_face(r, sum(map(ord, metal))))})
    makers = {
        "ingot": lambda: render_mask(INGOT, r),
        "dust": lambda: render_mask(DUST, r),
        "wire": lambda: render_mask(WIRE, r),
        "rod": lambda: render_mask(ROD, r),
        "plate": lambda: metal_plate(r),
        "gear": lambda: art.gear_sprite(r),
        "screw": lambda: metal_screw(r),
    }
    make = makers[form]
    return item(lambda: finish(make()))


# =============================================================================
# Minerals: fragments (rock chunk with specks) and tailings (greyish heap)
# =============================================================================

MINERAL_COLORS = {
    "native_copper": ("#c8703a", "#f0a868"), "malachite": ("#2e8b57", "#8fe0b0"),
    "native_gold": ("#8a7a5a", "#ffd84a"), "native_silver": ("#8a8e94", "#eef2f6"),
    "chalcopyrite": ("#b8963a", "#6fa86a"), "cassiterite": ("#4a3a32", "#b09a80"),
    "arsenopyrite": ("#a8a8a0", "#e8e8d8"), "hematite": ("#8a3a30", "#d8705a"),
    "magnetite": ("#3a3a40", "#8a8a98"), "galena": ("#6b6f8a", "#c8cce0"),
    "cinnabar": ("#a8302a", "#f06a5a"), "sphalerite": ("#6a4a2a", "#d8a860"),
    "stibnite": ("#707880", "#c8d0d8"), "bismuthinite": ("#7a7a8a", "#d890e8"),
    "pyrolusite": ("#2e2e2e", "#707070"), "cobaltite": ("#a8a0b0", "#5a78d8"),
    "native_platinum": ("#9a9ea4", "#f4f6f8"), "pentlandite": ("#a89058", "#e0d090"),
    "chromite": ("#2a2a30", "#7a6a5a"), "wolframite": ("#3a3028", "#8a7a60"),
    "molybdenite": ("#6a7280", "#c0c8d4"), "ilmenite": ("#2e2a2c", "#7a6a70"),
    "bauxite": ("#b86a3a", "#e8b080"), "magnesite": ("#d8d4c8", "#9a9280"),
    "vanadinite": ("#b8401a", "#ff8a4a"), "beryl": ("#5ab890", "#b8f0dc"),
    "spodumene": ("#c8a8c0", "#f0dcec"), "uraninite": ("#1f2a1f", "#7af05a"),
    "thorianite": ("#4a4038", "#a89878"), "zircon": ("#a85a3a", "#f0b080"),
    "rutile": ("#7a321a", "#d87a4a"), "coltan": ("#2a2830", "#7a78a0"),
    "lepidolite": ("#a880b8", "#f0d8ff"), "monazite": ("#b88a48", "#f0cc88"),
    "bastnasite": ("#b87a4a", "#f0c49a"), "xenotime": ("#7a5a30", "#d8b878"),
    "calaverite": ("#b8b07a", "#fff4c0"), "meteorite": ("#3a3638", "#a89ca4"),
    "halite": ("#d8d8e4", "#a8c0f0"), "native_sulfur": ("#c8b820", "#fff48a"),
    "saltpeter": ("#e4e4dc", "#b8b0a0"), "phosphorite": ("#6a5a4a", "#c0b0a0"),
    "graphite": ("#2a2a2e", "#6a6a7a"), "sylvite": ("#d8a8a0", "#fff0ec"),
    "fluorite": ("#7a4ab8", "#6ad8c0"), "borax": ("#e4e4e8", "#9ab8d8"),
}


def _speckle(img: Image.Image, mask: list[str], color: str, seed: int, every: int = 5) -> Image.Image:
    """Scatters `color` over the interior shades (2 to 5) of a mask, deterministically per seed."""
    px = img.load()
    rgba = art.hex_rgba(color)
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            h = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
            h = (h ^ (h >> 13)) * 0x5BD1E995 & 0xFFFFFFFF
            if ch in "2345" and (h >> 7) % every == 0:
                px[x, y] = rgba
    return img


def _greyed(base: str, amount: float = 0.55) -> str:
    r, g, b, _ = art.hex_rgba(base)
    grey = (r + g + b) / 3
    mix = [round(c * (1 - amount) + grey * amount * 0.85) for c in (r, g, b)]
    return "#%02x%02x%02x" % tuple(max(0, min(255, c)) for c in mix)


def mineral_spec(mineral: str, kind: str) -> Spec:
    base, speck = MINERAL_COLORS[mineral]
    seed = sum(map(ord, mineral))
    if kind == "fragment":
        return item(lambda: _speckle(render_mask(RAW, ramp(base)), RAW, speck, seed))
    return item(lambda: _speckle(render_mask(DUST, ramp(_greyed(base))), DUST, speck, seed, every=7))


# =============================================================================
# Electronics components
# =============================================================================

BOARD_COLORS = {1: "#2f8f3a", 2: "#2a5db0", 3: "#7b3fb0"}

MOTOR = [
    "................",
    "................",
    "................",
    "................",
    "..0000000000....",
    ".0wwwwDDwwww0...",
    ".0ssssCCssss0...",
    ".0ssssCCssss000.",
    ".0ssssCCssss0wx0",
    ".0xxxxBBxxxx000.",
    ".0xxxxBBxxxx0...",
    "..0000000000....",
    "..0k0....0k0....",
    "..000....000....",
    "................",
    "................",
]

CIRCUIT = [
    "................",
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kEEEEEEEEEEEEk.",
    ".kEyyyEEEEyyyDk.",
    ".kEDDywwwwyDDDk.",
    ".kEDDwhhhhwDDDk.",
    ".kEyywhhhhwyyDk.",
    ".kEDDwhhhhwDDDk.",
    ".kEDDywwwwyDDDk.",
    ".kEyyyDDDDyyyDk.",
    ".kEDDDDDDDDDDCk.",
    ".kCCCCCCCCCCCCk.",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
]

PROCESSOR = [
    "................",
    "................",
    "....y.y.y.y.....",
    "...kkkkkkkkkk...",
    "..ykxnnnnnnxky..",
    "...knnnnnnnnk...",
    "..yknnDDDDnnky..",
    "...knnDFFDnnk...",
    "..yknnDFFDnnky..",
    "...knnDDDDnnk...",
    "..yknnnnnnnnky..",
    "...kxnnnnnnxk...",
    "...kkkkkkkkkk...",
    "....y.y.y.y.....",
    "................",
    "................",
]

COIL = [
    "................",
    "................",
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    ".kDx53535353xDk.",
    ".kCx42424242xCk.",
    ".kCx42424242xCk.",
    ".kCx42424242xCk.",
    ".kCx31313131xCk.",
    ".kBx31313131xBk.",
    "..kkkkkkkkkkkk..",
    "...k........k...",
    "...o........o...",
    "................",
    "................",
]

RAM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "kkkkkkkkkkkkkkkk",
    "kEEEEEEEEEEEEEEk",
    "kEhhEhhEhhEhhDDk",
    "kDhhDhhDhhDhhDDk",
    "kCCCCCCCCCCCCCCk",
    "kyCyCyCyCyCyCyCk",
    "kkkkkkkkkkkkkkkk",
    "................",
    "................",
    "................",
    "................",
]

HDD = [
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    "..kssssssssssk..",
    "..ksiiiiiiiisk..",
    "..ksi..ss..isk..",
    "..ksi.swws.isk..",
    "..ksisw..wsisk..",
    "..ksisw..wsisk..",
    "..ksi.swwsxisk..",
    "..ksi..ssx.isk..",
    "..ksiiiiixiisk..",
    "..kssssssssgsk..",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
]

SSD = [
    "................",
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    "..khhhhhhhhhhk..",
    "..khEEEEEEEEhk..",
    "..khEwwwwwwEhk..",
    "..khEEEEEEEEhk..",
    "..khhhhhhhhhhk..",
    "..khhhhhhhhhhk..",
    "..khhhhhhhhhgk..",
    "..kkkkkkkkkkkk..",
    "...yyyyyyyyyy...",
    "................",
    "................",
    "................",
]


def wafer() -> Image.Image:
    r = ramp(METAL_COLORS["silicon"])
    img = _canvas()
    p = img.load()
    c = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - c, y - c)
            if d > 6.8 or y > 13:
                continue
            grid = (x % 3 == 0) or (y % 3 == 0)
            light = -((x - c) + (y - c)) / 13.6
            base = 3 + round(light * 2)
            p[x, y] = r[max(1, min(5, base + (1 if grid else 0)))]
    for y in range(2, 13):
        for x in range(2, 13):
            if p[x, y][3] and (x + y) % 11 == 0:
                p[x, y] = NAMED_RGBA["v"]
    return outline(img, r[0])


def component_spec(cid: str) -> Spec | None:
    if cid == "silicon_wafer":
        return item(wafer)
    if cid == "storage_hdd":
        return item(lambda: render_mask(HDD))
    if cid == "storage_ssd":
        return item(lambda: render_mask(SSD, accent=tier_ramp(2)))
    kind, _, tier_s = cid.rpartition("_t")
    if not tier_s.isdigit():
        return None
    tier = int(tier_s)
    steel = ramp("#8a929b")
    if kind == "motor":
        return item(lambda: pips(render_mask(MOTOR, steel, tier_ramp(tier)), tier))
    if kind == "circuit":
        return item(lambda: pips(render_mask(CIRCUIT, accent=ramp(BOARD_COLORS[tier])), tier, 14))
    if kind == "processor":
        return item(lambda: pips(render_mask(PROCESSOR, accent=tier_ramp(tier)), tier))
    if kind == "coil":
        return item(lambda: pips(render_mask(COIL, ramp(METAL_COLORS["copper"]), tier_ramp(tier)), tier))
    if kind == "ram":
        return item(lambda: pips(render_mask(RAM, accent=ramp(BOARD_COLORS[tier])), tier, 13))
    return None


# =============================================================================
# Android upgrades
# =============================================================================

AI_CHIP = recolor(PROCESSOR, {}).copy()
AI_CHIP[6] = "..yknnDDDDnnky.."
AI_CHIP[7] = "...knDEwwEDnk..."
AI_CHIP[8] = "..yknDEkkEDnky.."
AI_CHIP[9] = "...knnDDDDnnk..."

MOTOR_CHIP = PROCESSOR.copy()
MOTOR_CHIP[5] = "...knnnnnnnnk..."
MOTOR_CHIP[6] = "..yknnEnnEnnky.."
MOTOR_CHIP[7] = "...knnEEEEnnk..."
MOTOR_CHIP[8] = "..yknEEnnEEnky.."
MOTOR_CHIP[9] = "...knnEEEEnnk..."
MOTOR_CHIP[10] = "..yknnEnnEnnky.."

ARMOUR_PLATE = [
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    "..kD55555544Dk..",
    "..kD54444433Dk..",
    "..kD54444433Dk..",
    "..kD44444333Dk..",
    "..kD44443332Dk..",
    "...kD443332Dk...",
    "...kD433322Dk...",
    "....kD3322Dk....",
    ".....kD22Dk.....",
    "......kDDk......",
    ".......kk.......",
    "................",
    "................",
]

FUEL_MODULE = [
    "................",
    "................",
    "....kkk.kkkk....",
    "....ksk.kssk....",
    "...kkkkkkkkkk...",
    "...kEEEEEEEDk...",
    "...kECDDDDCCk...",
    "...kEDCDDCDCk...",
    "...kEDDCCDDCk...",
    "...kEDDCCDDCk...",
    "...kEDCDDCDCk...",
    "...kECDDDDCCk...",
    "...kDCCCCCCBk...",
    "...kkkkkkkkkk...",
    "................",
    "................",
]


def upgrade_spec(uid: str) -> Spec | None:
    kind, _, tier_s = uid.rpartition("_t")
    if not tier_s.isdigit():
        return None
    tier = int(tier_s)
    acc = tier_ramp(tier)
    masks = {"ai_chip": AI_CHIP, "motor_chip": MOTOR_CHIP, "fuel_module": FUEL_MODULE}
    if kind in masks:
        return item(lambda: pips(render_mask(masks[kind], accent=acc), tier))
    if kind == "armour_plate":
        return item(lambda: pips(render_mask(ARMOUR_PLATE, ramp("#a6adb4"), acc), tier))
    return None


# =============================================================================
# Tools
# =============================================================================

GUIDE = [
    "................",
    "................",
    "..kkkkkkkkkkk...",
    "..kCDDDDDDDDk...",
    "..kCDDDDDDDDk...",
    "..kCDDDyyDDDk...",
    "..kCDDyDDyDDk...",
    "..kCDDyDDyDDk...",
    "..kCDDDyyDDDk...",
    "..kCDDDDDDDDk...",
    "..kCDDDDDDDDk...",
    "..kCEEEEEEEEk...",
    "..kCwwwwwwwwk...",
    "..kkkkkkkkkkk...",
    "................",
    "................",
]

ALMANAC = [
    "................",
    "................",
    "..kkkkkkkkkkk...",
    "..kCDDDDDDDDk...",
    "..kCDyyyyyyDk...",
    "..kCDDyDDyDDk...",
    "..kCDDDyyDDDk...",
    "..kCDDDyyDDDk...",
    "..kCDDyDDyDDk...",
    "..kCDyyyyyyDk...",
    "..kCDDDDDDDDk...",
    "..kCEEEEEEEEk...",
    "..kCwwwwwwwwk...",
    "..kkkkkkkkkkk...",
    "................",
    "................",
]

PROSPECTOR = [
    "................",
    "...........kk...",
    "...........kr...",
    "...........ks...",
    "...kkkkkkkkks...",
    "...kxxxxxxxxk...",
    "...kxjjjjjjxk...",
    "...kxjlljljxk...",
    "...kxjjljjlxk...",
    "...kxjjjjjjxk...",
    "...kxxxxxxxxk...",
    "...kxEExxrrxk...",
    "...kxxxxxxxxk...",
    "...kkkkkkkkkk...",
    "................",
    "................",
]

GPS_MAP = [
    "................",
    "................",
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kxxxxxxxxxxxxk.",
    ".kxggglbbbgggxk.",
    ".kxgglbbbbgglxk.",
    ".kxgggbbrbgggxk.",
    ".kxlggbbbggggxk.",
    ".kxgggglbbgggxk.",
    ".kxxxxxxxxxxxxk.",
    ".kxEExxxxxwwxxk.",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
    "................",
]


def wrench() -> Image.Image:
    steel = ramp("#a6adb4")
    grip = tier_ramp(1)
    img = _canvas()
    p = img.load()
    for y in range(16):
        for x in range(16):
            d, t, side = _segment_dist(x, y, 2.5, 13.5, 10.5, 5.5)
            if d <= 1.3:
                p[x, y] = shaded(grip if t < 0.42 else steel, side * 1.6)
    cx, cy = 11.6, 4.4
    for y in range(16):
        for x in range(16):
            dist = math.hypot(x - cx, y - cy)
            jaw = (x - cx) + (cy - y) > 0.8 and abs((x - cx) - (cy - y)) < 2.2
            if dist <= 3.3 and not jaw:
                p[x, y] = steel[4] if (x - cx) + (y - cy) < 0 else steel[3]
    return outline(img, steel[0])


TOOLS: dict[str, Spec] = {
    "wrench": item(wrench, HANDHELD),
    "guide": item(lambda: render_mask(GUIDE, accent=ramp("#2f8fa8"))),
    "era_almanac": item(lambda: render_mask(ALMANAC, accent=ramp("#8a5a2f"))),
    "prospector": item(lambda: render_mask(PROSPECTOR, accent=tier_ramp(3)), HANDHELD),
    "gps_handheld_map": item(lambda: render_mask(GPS_MAP, accent=tier_ramp(2))),
}


# =============================================================================
# Machine glyphs (10x10, drawn on the dark front panel)
# =============================================================================

G: dict[str, list[str]] = {
    "flame": [
        "....o.....", "...oo.....", "...omo..o.", "..omyo.oo.", "..omyyoom.",
        ".omyyyymo.", ".omywwymo.", ".omywwymo.", "..myyyym..", "..xxxxxx..",
    ],
    "battery": [
        "...kkkk...", "..ssssss..", "..sDDDDs..", "..sEEEEs..", "..sDDDDs..",
        "..sEEEEs..", "..sDDDDs..", "..sEEEEs..", "..sCCCCs..", "..ssssss..",
    ],
    "bulb": [
        "...yyyy...", "..ywwwyy..", ".ywwyyyyy.", ".ywyyyyyy.", ".yyyyyyyy.",
        "..yyyyyy..", "...ssss...", "...xxxx...", "...ssss...", "....xx....",
    ],
    "screen": [
        "..........", ".vvvv.vv..", "..........", ".vv.vvvv..", "..........",
        ".vvvvv.v..", "..........", ".vvv.vv...", "..........", ".v........",
    ],
    "arrow_out": [
        "....EE....", "...EEEE...", "..EEEEEE..", "....EE....", "....EE....",
        ".dddddddd.", ".dzzzzzzd.", ".dzdzzdzd.", ".dzzzzzzd.", ".dddddddd.",
    ],
    "arrow_in": [
        "....EE....", "....EE....", "..EEEEEE..", "...EEEE...", "....EE....",
        ".dddddddd.", ".dzzzzzzd.", ".dzdzzdzd.", ".dzzzzzzd.", ".dddddddd.",
    ],
    "funnel": [
        ".r..g..b..", "..........", "ssssssssss", ".sxxxxxxs.", "..sxxxxs..",
        "...sxxs...", "....ss....", "....ss....", "..........", "....g.....",
    ],
    "pump": [
        "....u...E.", "...uub.EEE", "..uuubb.E.", ".uuuubbbE.", ".uwuubbbE.",
        ".uwuubbbE.", "..uubbb.E.", "...bbb..E.", "..........", "..........",
    ],
    "drain": [
        ".ssssssss.", ".skkkkkks.", ".ssssssss.", ".skkkkkks.", ".ssssssss.",
        "....u.....", "...uub....", "...uwb....", "....b.....", "..........",
    ],
    "transformer": [
        "..........", ".xx....xx.", ".oo....EE.", ".tt....DD.", ".oo....EE.",
        ".tt....DD.", ".oo....EE.", ".xxxxxxxx.", "..........", "..........",
    ],
    "crusher": [
        "iiiiiiiiii", "isisisisis", ".s.s.s.s.s", "..........", "...zoz....",
        "...ozo....", "..........", "s.s.s.s.s.", "sisisisisi", "iiiiiiiiii",
    ],
    "washer": [
        ".u...u..u.", "..u.u..u..", ".u..u.u...", "..........", "...iiii...",
        "..iioiii..", "..ixiiox..", "..iiiixi..", "...xxxx...", ".bbbbbbbb.",
    ],
    "heater": [
        "..........", ".oooooooo.", ".mmmmmmmm.", "..........", ".oooooooo.",
        ".mmmmmmmm.", "..........", ".yyyyyyyy.", ".oooooooo.", "..........",
    ],
    "mixer": [
        "....ss....", "....ss....", "...s..s...", "..s.ss.s..", "..s.ss.s..",
        "...ssss...", "iiiiiiiiii", "iDEDEDEDEi", ".iDDDDDDi.", "..iiiiii..",
    ],
    "compress": [
        "..EEEEEE..", "...EEEE...", "....EE....", "..........", "..ssssss..",
        "..sxxxxs..", "..........", "....EE....", "...EEEE...", "..EEEEEE..",
    ],
    "saw": [
        "..s.ss.s..", ".ssssssss.", "sssiiiisss", ".sii..iis.", "ssi.xx.iss",
        "ssi.xx.iss", ".sii..iis.", "sssiiiisss", ".ssssssss.", "..s.ss.s..",
    ],
    "press": [
        "xxxxxxxxxx", "..iiiiii..", "...ssss...", "...ssss...", "..EEEEEE..",
        "..........", "..........", ".DDDDDDDD.", "iiiiiiiiii", "xxxxxxxxxx",
    ],
    "extract": [
        "ssssssssss", ".sxxxxxxs.", "..sxxxxs..", "...sxxs...", "....ss....",
        "....EE....", "...EEFE...", "...EEEE...", "....EE....", "..........",
    ],
    "induction": [
        "..........", ".oooooooo.", "..myyyym..", ".oooooooo.", "..myyyym..",
        ".oooooooo.", "..myyyym..", ".oooooooo.", "..........", ".xxxxxxxx.",
    ],
    "oil": [
        "....i.....", "...ihi....", "...ihhi...", "..ihhhhi..", ".ihhhhhhi.",
        ".ihphhhhi.", ".ihphhhhi.", "..ihhhhi..", "...iiii...", "..........",
    ],
    "refinery": [
        "..i.......", ".ihi..yyy.", "ihhhi.....", "ihphi.ooo.", ".iii......",
        "......bbb.", "..........", "ssssssssss", "sxsxsxsxsx", "ssssssssss",
    ],
    "cracker": [
        "..........", "..rr......", ".rrrr.....", ".rrrrs....", "..rr.y....",
        ".....s.bb.", "......bbbb", "......bbbb", ".......bb.", "..........",
    ],
    "ferment": [
        "..........", "..w....w..", ".....w....", "..w.......", "iiiiiiiiii",
        "illwllllli", "iglgglwggi", "iggggggggi", "ijgjgjgjgi", "iiiiiiiiii",
    ],
    "still": [
        "....ss....", "....ss....", "...s..s...", "..s....s..", ".s......s.",
        ".syyyyyys.", ".syooooys.", "..syyyys..", "...ssss...", ".mmmmmmmm.",
    ],
    "leaf": [
        "..........", "...gggg...", "..glllgg..", ".gllwllgg.", ".glllllgj.",
        ".gllllggj.", "..glggjj..", "...gjjj...", "....t.....", "...t......",
    ],
    "lava": [
        "mmhhmmmhhm", "hmmhhomhmm", "hhommyohhm", "mhhoyyomhh", "mmhhoommhh",
        "hmmhhmhhmm", "hhmooyohmm", "mhmoyyyohm", "mhhmoomhhm", "hmmhhhmmhh",
    ],
    "turbine": [
        "..ss......", "..sss.....", "...ss...ss", "....s.ssss", "....xx....",
        "....xx....", "ssss.s....", "ss...ss...", ".....sss..", "......ss..",
    ],
    "radiation": [
        "yyyyyyyyyy", "ykkyyyykky", "ykkkyykkky", "yykkyykkyy", "yyyykkyyyy",
        "yyyykkyyyy", "yyyyyyyyyy", "yyykkkkyyy", "yyykkkkyyy", "yyyyyyyyyy",
    ],
    "electrolysis": [
        ".r......x.", ".r..w...x.", ".r....w.x.", "brbbwbbbxb", "brbbbbwbxb",
        "brbwbbbbxb", "brbbbbbbxb", "bbbbbbbbbb", "iiiiiiiiii", "..........",
    ],
    "rollers": [
        "..iiiiii..", ".isswsssi.", ".issssssi.", "..iiiiii..", "EEEEEEEEEE",
        "..iiiiii..", ".isswsssi.", ".issssssi.", "..iiiiii..", "..........",
    ],
    "laser": [
        "..xxxxxx..", "..xiiiix..", "...xrrx...", "....rr....", "....rr....",
        "....rr....", "...wrrw...", "..y.ww.y..", "iiiiiiiiii", "xxxxxxxxxx",
    ],
    "flask": [
        "..........", "...ssss...", "....ss....", "....ss....", "...s..s...",
        "..s.w..s..", ".sllllwls.", "sllgllllls", "ssssssssss", "..........",
    ],
    "gas_in": [
        "EE......EE", ".EE....EE.", "..........", "...cccc...", "..cccccc..",
        ".cccccccc.", ".csccccsc.", "..........", ".EE....EE.", "EE......EE",
    ],
    "boiler": [
        ".w..w..w..", "..w..w..w.", "iiiiiiiiii", "ibbbbbbbbi", "ibubbubbbi",
        "iiiiiiiiii", "..........", "..o.o..o..", ".omomoomo.", ".myyyyyym.",
    ],
    "condense": [
        "..w..w....", ".w..w..w..", "..........", "EEEEEEEEEE", "..........",
        "EEEEEEEEEE", "..........", "...u...u..", "..ub..ub..", "...b...b..",
    ],
    "snow": [
        "....u.....", ".u..u..u..", "..u.u.u...", "...cuc....", "uuuucuuuu.",
        "...cuc....", "..u.u.u...", ".u..u..u..", "....u.....", "..........",
    ],
    "layers": [
        "iiiiiiiiii", "icccccccci", "icwccccwci", "iiiiiiiiii", "iyyyyyyyyi",
        "iyoyyyoyyi", "iiiiiiiiii", "ibbbbbbbbi", "ibubbbbubi", "iiiiiiiiii",
    ],
    "pickaxe": [
        "...ssss...", ".ss....ss.", "s...tt...s", "....tt....", "....tt....",
        "....tt....", "....tt....", "....tt....", "....dd....", "..........",
    ],
    "drill": [
        "..xxxxxx..", "..iiiiii..", "..sssxss..", "...ssxs...", "...sxss...",
        "....sxs...", "....ss....", "....s.....", "....s.....", "..........",
    ],
    "desalinate": [
        "....u.....", "...uub....", "..uuubb...", ".uwuubbb..", ".uwuubbb..",
        "..uubbb...", "...bbb....", "..........", ".w.ww.w.w.", "wwwwwwwwww",
    ],
    "gas_out": [
        "....EE....", "...EEEE...", "..EEEEEE..", "....EE....", "....EE....",
        "..cccc....", ".ccccccc..", "cccccccccc", ".cscccscc.", "..........",
    ],
    "wind": [
        "..........", "cccccccc..", "........c.", "......cc..", "..........",
        "ccccccccc.", ".........c", ".......cc.", "..........", "ccccc.....",
    ],
    "antenna": [
        ".E.....E..", "E..E.E..E.", "E.E...E.E.", "E..E.E..E.", ".E..r..E..",
        "....s.....", "...sss....", "...s.s....", "..s...s...", ".iiiiiii..",
    ],
    "crates": [
        ".dddd.dddd", ".dzzd.dzzd", ".dzzd.dzzd", ".dddd.dddd", "..........",
        ".dddd.dddd", ".dzzd.dzzd", ".dzzd.dzzd", ".dddd.dddd", "..........",
    ],
    "split": [
        "EE......EE", ".EE....EE.", "..EE..EE..", "...EEEE...", "....EE....",
        "....EE....", "....EE....", "....EE....", "....EE....", "..........",
    ],
    "filters": [
        "ssssssssss", "s.x.x.x.xs", "ssssssssss", "sx.x.x.x.s", "ssssssssss",
        "s.x.x.x.xs", "ssssssssss", "..........", "....EE....", "....EE....",
    ],
    "overflow": [
        "..........", "iiiiii....", "iDDDDiE...", "iDDDDiEE..", "iDDDDiEEE.",
        "iDDDDiEE..", "iDDDDiE...", "iiiiii....", "..........", "..........",
    ],
    "comparator": [
        "..........", "..r....r..", ".rqr..rqr.", "..q....q..", "..........",
        "....r.....", "...rqr....", "....q.....", "ssssssssss", "iiiiiiiiii",
    ],
    "package": [
        "..........", ".dddddddd.", ".dzzyyzzd.", ".dzzyyzzd.", ".dyyyyyyd.",
        ".dzzyyzzd.", ".dzzyyzzd.", ".dddddddd.", "..........", "..........",
    ],
    "unpackage": [
        "....E.....", "...EEE....", "....E.....", "d..dddd..d", ".d.dzzd.d.",
        "..dzzzzd..", "..dzzzzd..", "..dzzzzd..", "..dddddd..", "..........",
    ],
    "valve": [
        "...rrrr...", ".rr.rr.rr.", ".r..rr..r.", "rrrrxxrrrr", ".r..rr..r.",
        ".rr.rr.rr.", "...rrrr...", "....ss....", "iiiissiiii", "iiiiiiiiii",
    ],
    "gauge": [
        ".iiiiiiii.", ".i......iw", ".i......i.", ".ibbbbbbiw", ".iuuuuuui.",
        ".ibbbbbbiw", ".ibbbbbbi.", ".iiiiiiii.", "..........", "..EEEEEE..",
    ],
    "gear": [
        "....ss....", ".s.ssss.s.", "..ssssss..", ".sssiisss.", "sssi..isss",
        "sssi..isss", ".sssiisss.", "..ssssss..", ".s.ssss.s.", "....ss....",
    ],
}

GREEN_FLAME = recolor(G["flame"], {"o": "g", "m": "j", "y": "l", "w": "l"})
FUEL_FLAME = recolor(G["flame"], {"x": "h"})
TRANSFORMER_HV = recolor(G["transformer"], {"o": "u", "t": "b"})


def machine(tier: int, glyph: list[str], seed: int, **kw) -> Spec:
    if len(glyph) != 10 or any(len(row) != 10 for row in glyph):
        raise ValueError(f"glyph for seed {seed} must be 10x10: {[len(r) for r in glyph]}")
    return block(lambda: art.machine_faces(tier, glyph, seed, **kw))


# =============================================================================
# Special blocks
# =============================================================================

def casing_block(tier: int, seed: int, body: str | None = None, brushed: bool = False) -> Spec:
    def faces() -> dict[str, Image.Image]:
        img = art.casing(tier, seed, body=body)
        p = img.load()
        r = ramp(body or art.CASING_BODY[tier], hue_shift=0.02)
        if brushed:
            for y in range(3, 13, 2):
                for x in range(3, 13):
                    p[x, y] = r[4]
        else:
            for i in range(3, 13):
                p[i, i] = r[2]
                p[15 - i, i] = r[2]
        art.trim(img, tier, 0, height=1, x0=3, x1=12)
        return {"all": img}
    return block(faces)


def fluid_tank() -> dict[str, Image.Image]:
    side = art.casing(1, 71)
    p = side.load()
    for y in range(3, 13):
        for x in range(3, 13):
            if y < 7:
                p[x, y] = NAMED_RGBA["c"]
            else:
                p[x, y] = NAMED_RGBA["b"] if y > 7 else NAMED_RGBA["u"]
    for x, y in ((5, 9), (9, 10), (7, 11)):
        p[x, y] = NAMED_RGBA["u"]
    p[4, 4] = NAMED_RGBA["w"]
    p[5, 4] = NAMED_RGBA["w"]
    p[4, 5] = NAMED_RGBA["w"]
    top = art.casing(1, 72)
    art.panel(top, 5, 5, 10, 10)
    return {"front": side, "side": side, "top": top}


def workbench() -> dict[str, Image.Image]:
    top = art.casing(0, 81)
    art.panel(top, 2, 2, 13, 13)
    p = top.load()
    for i in (5, 9):
        for j in range(2, 14):
            p[i, j] = NAMED_RGBA["x"]
            p[j, i] = NAMED_RGBA["x"]
    side = art.casing(0, 82)
    art.trim(side, 1, 3, height=2)
    art.paste_mask(side, [
        "..........",
        "..ss......",
        "..sss.....",
        "...sst....",
        "....st....",
        ".....t....",
        "......t...",
    ], 3, 7)
    return {"top": top, "front": side, "side": side}


def pedestal() -> dict[str, Image.Image]:
    body = "#c9c2b6"
    top = art.casing(0, 91, rivets=False, body=body)
    art.panel(top, 4, 4, 11, 11)
    p = top.load()
    a = tier_ramp(2)
    for x, y in ((6, 6), (9, 6), (6, 9), (9, 9), (7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        p[x, y] = a[4]
    side = art.casing(0, 92, rivets=False, body=body)
    sp = side.load()
    r = ramp(body)
    for y in range(3, 13):
        for x in (4, 7, 8, 11):
            sp[x, y] = r[2]
    return {"top": top, "front": side, "side": side}


ANDROID_ACCENT = {
    "farmer": "#d4b24c", "lumberjack": "#6b8e3a", "miner": "#8a929b", "fisherman": "#3a7be0",
    "butcher": "#d63a2f", "builder": "#e07b39", "slayer": "#8e1f1a", "trader": "#2ecc71",
}

EMBLEMS = {
    "farmer": [".y..y.", "yyyyyy", ".yyyy.", "..gg..", "..gg.."],
    "lumberjack": ["ss....", "sst...", ".st...", "..t...", "...t.."],
    "miner": ["ssss..", "....t.", "...t.s", "..t...", ".t...."],
    "fisherman": ["......", ".bbb.b", "bbwbbb", ".bbb.b", "......"],
    "butcher": ["ssss..", "sssst.", "ssss.t", "......", "......"],
    "builder": ["rrqrrq", "qqqqqq", "rqrrqr", "qqqqqq", "rrqrrq"],
    "slayer": ["....s.", "...s..", "..s...", "tts...", ".t...."],
    "trader": ["..gg..", ".glgg.", "gglgjg", ".gjjg.", "..jj.."],
}


def android(kind: str) -> Spec:
    def faces() -> dict[str, Image.Image]:
        acc = ramp(ANDROID_ACCENT[kind])
        front = art.casing(0, 101 + len(kind))
        p = front.load()
        art.panel(front, 2, 3, 13, 7)
        for x in range(3, 13):
            p[x, 3] = acc[2]
        for ex in (5, 9):
            p[ex, 5] = NAMED_RGBA["v"]
            p[ex + 1, 5] = NAMED_RGBA["v"]
            p[ex, 6] = (40, 150, 140, 255)
            p[ex + 1, 6] = (40, 150, 140, 255)
        art.panel(front, 4, 9, 11, 13)
        art.paste_mask(front, EMBLEMS[kind], 5, 9)
        top = art.casing(0, 111)
        tp = top.load()
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            tp[x, y] = acc[4]
        side = art.casing(0, 121)
        sp = side.load()
        for y in range(3, 13):
            sp[7, y] = acc[3]
            sp[8, y] = acc[2]
        return {"front": front, "top": top, "side": side}
    return block(faces)


def cable(tier: int) -> Spec:
    acc = tier_ramp(tier)
    rubber = ramp("#3d434b")
    copper = ramp(METAL_COLORS["copper"])

    def color(t: float, side: float) -> tuple:
        if t < 0.08 or t > 0.92:
            return shaded(copper, side)
        if abs(side) < 0.3:
            return acc[4] if int(t * 12) % 2 == 0 else acc[3]
        return shaded(rubber, side)
    return item(lambda: tube(color, half_width=1.9))


def pipe(body: str, band: str, band_every: float = 0.33, hazard: bool = False) -> Spec:
    r = ramp(body)
    b = ramp(band)

    def color(t: float, side: float) -> tuple:
        if hazard:
            return shaded(b if int(t * 10) % 2 == 0 else r, side)
        on_band = abs((t / band_every) - round(t / band_every)) < 0.12 and 0.05 < t < 0.95
        return shaded(b if on_band else r, side)
    return item(lambda: tube(color, half_width=2.2))


def item_cable() -> Spec:
    glass = ramp("#9aa1a8")

    def color(t: float, side: float) -> tuple:
        slot = int(t * 9)
        if abs(side) < 0.5 and slot % 2 == 1 and 0 < slot < 8:
            return NAMED_RGBA["yrgb"[(slot // 2) % 4]]
        return shaded(glass, side)
    return item(lambda: tube(color, half_width=2.0))


CONVEYOR = [
    "................",
    "................",
    "................",
    "................",
    "kkkkkkkkkkkkkkkk",
    "kiiiiiiiiiiiiiik",
    "knynnnynnnynnnnk",
    "knnynnnynnnynnnk",
    "knynnnynnnynnnnk",
    "kiiiiiiiiiiiiiik",
    "kkkkkkkkkkkkkkkk",
    ".xx..........xx.",
    ".kk..........kk.",
    "................",
    "................",
    "................",
]

GPS_MARKER = [
    "................",
    "................",
    "..E..........E..",
    ".E..E......E..E.",
    ".E.E...rr...E.E.",
    ".E.E..rwrr..E.E.",
    ".E..E.rrrr.E..E.",
    "..E....rr....E..",
    ".......ss.......",
    ".......ss.......",
    ".......ss.......",
    ".......ss.......",
    "......ssss......",
    ".....xxxxxx.....",
    "................",
    "................",
]


# =============================================================================
# Block table
# =============================================================================

LV, MV, HV = 1, 2, 3

BLOCKS: dict[str, Spec] = {
    # Core / demo
    "pedestal": block(pedestal),
    "console": machine(0, G["screen"], 3, top="plain"),
    "workbench": block(workbench),
    # Energy
    "generator": machine(LV, G["flame"], 10),
    "combustion_gen": machine(MV, FUEL_FLAME, 11),
    "biogas_gen": machine(LV, GREEN_FLAME, 12),
    "geothermal_gen": machine(HV, G["lava"], 13),
    "gas_turbine": machine(HV, G["turbine"], 14, side="vent"),
    "rtg": machine(HV, G["radiation"], 15),
    "capacitor": machine(LV, G["battery"], 16, top="plain"),
    "capacitor_t2": machine(MV, G["battery"], 17, top="plain"),
    "capacitor_t3": machine(HV, G["battery"], 18, top="plain"),
    "consumer": machine(LV, G["bulb"], 19),
    "transformer_lv_mv": machine(MV, G["transformer"], 20),
    "transformer_mv_hv": machine(HV, TRANSFORMER_HV, 21),
    "cable": cable(LV),
    "cable_t2": cable(MV),
    "cable_t3": cable(HV),
    # Casings
    "machine_casing": casing_block(LV, 30),
    "machine_casing_mv": casing_block(MV, 31),
    "stainless_steel_casing": casing_block(0, 32, body="#a6adb4", brushed=True),
    # Machines
    "macerator": machine(LV, G["crusher"], 40),
    "ore_washer": machine(LV, G["washer"], 41),
    "electric_furnace": machine(LV, G["heater"], 42),
    "bench_saw": machine(LV, G["saw"], 43),
    "mixer": machine(MV, G["mixer"], 44),
    "compressor": machine(MV, G["compress"], 45),
    "plate_press": machine(MV, G["press"], 46),
    "extractor": machine(MV, G["extract"], 47),
    "electrolyzer": machine(HV, G["electrolysis"], 48),
    "rolling_mill": machine(HV, G["rollers"], 49),
    "laser_cutter": machine(HV, G["laser"], 50),
    "chemical_reactor": machine(HV, G["flask"], 51),
    # Multiblock controllers
    "induction_furnace_controller": machine(MV, G["induction"], 60, top="hatch"),
    "oil_refinery_controller": machine(MV, G["refinery"], 61, top="hatch"),
    "quarry_controller": machine(HV, G["pickaxe"], 62, top="hatch"),
    "drill_rig_controller": machine(HV, G["drill"], 63, top="hatch"),
    "desalinator_controller": machine(HV, G["desalinate"], 64, top="hatch"),
    # Petroleum & chemistry
    "pumpjack": machine(MV, G["oil"], 70),
    "cracker": machine(MV, G["cracker"], 71),
    "fermenter": machine(MV, G["ferment"], 72),
    "still": machine(MV, G["still"], 73),
    "bioreactor": machine(LV, G["leaf"], 74),
    # Gas handling
    "gas_compressor": machine(MV, G["gas_in"], 80),
    "boiler": machine(MV, G["boiler"], 81),
    "condenser": machine(MV, G["condense"], 82),
    "liquefier": machine(HV, G["snow"], 83),
    "phase_separator": machine(HV, G["layers"], 84),
    "pressurized_pipe": pipe("#4f565e", "#f5d547", hazard=True),
    # Geo & atmosphere
    "gas_extractor": machine(MV, G["gas_out"], 90),
    "atmospheric_collector": machine(MV, G["wind"], 91, top="vent", side="vent"),
    "gps_transmitter": machine(0, G["antenna"], 92, accent=tier_ramp(2)),
    "gps_marker": item(lambda: render_mask(GPS_MARKER, accent=tier_ramp(2))),
    # Item logistics
    "item_cable": item_cable(),
    "item_producer": machine(0, G["arrow_out"], 100, accent=tier_ramp(1)),
    "item_consumer": machine(0, G["arrow_in"], 101, accent=tier_ramp(1)),
    "item_filter": machine(0, G["funnel"], 102),
    "item_buffer": machine(0, G["crates"], 103),
    "item_splitter": machine(0, G["split"], 104, accent=tier_ramp(1)),
    "filter_chamber": machine(0, G["filters"], 105, accent=tier_ramp(1)),
    "overflow_module": machine(0, G["overflow"], 106, accent=tier_ramp(1)),
    "comparator_sensor": machine(0, G["comparator"], 107),
    "packager": machine(0, G["package"], 108),
    "unpackager": machine(0, G["unpackage"], 109, accent=tier_ramp(1)),
    "conveyor_belt": item(lambda: render_mask(CONVEYOR)),
    # Fluid logistics
    "fluid_pipe": pipe("#8a929b", "#3a7be0"),
    "fluid_pump": machine(0, G["pump"], 110, accent=tier_ramp(2)),
    "fluid_tank": block(fluid_tank),
    "fluid_drain": machine(0, G["drain"], 111),
    "fluid_valve": machine(0, G["valve"], 112),
    "fluid_level_sensor": machine(0, G["gauge"], 113, accent=tier_ramp(2)),
}

for _kind in ANDROID_ACCENT:
    BLOCKS[f"android_{_kind}"] = android(_kind)


def lookup(item_id: str) -> Spec | None:
    if item_id in TOOLS:
        return TOOLS[item_id]
    if item_id in BLOCKS:
        return BLOCKS[item_id]
    for metal in METAL_COLORS:
        prefix = metal + "_"
        if item_id.startswith(prefix):
            form = item_id[len(prefix):]
            if form in ("dust", "ingot", "block", "plate", "wire", "rod", "gear", "screw"):
                return metal_spec(metal, form)
    for kind in ("fragment", "tailings"):
        suffix = "_" + kind
        if item_id.endswith(suffix) and item_id[: -len(suffix)] in MINERAL_COLORS:
            return mineral_spec(item_id[: -len(suffix)], kind)
    return component_spec(item_id) or upgrade_spec(item_id)
