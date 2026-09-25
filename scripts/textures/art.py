"""Drawing primitives shared by every Sapientia texture.

Colours are organised in six-step ramps (index 0 = outline, 5 = highlight).
Masks are lists of strings where each character selects a colour:

    .        transparent
    0-5      primary ramp
    A-F      accent ramp (index 0-5)
    a-z      fixed named colours (see NAMED)
"""
from __future__ import annotations

import colorsys
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

Color = tuple[int, int, int, int]
Ramp = list[Color]

NAMED: dict[str, str] = {
    "k": "#16161c",  # near black
    "n": "#23272e",  # panel background
    "w": "#f2f2f2",  # white
    "s": "#b8c0c8",  # light steel
    "i": "#8a929b",  # steel
    "x": "#5a626b",  # dark steel
    "y": "#f5d547",  # yellow / gold
    "o": "#f08a24",  # orange
    "m": "#e8481c",  # hot orange-red
    "r": "#d63a2f",  # red
    "q": "#8e1f1a",  # dark red
    "g": "#3dae45",  # green
    "l": "#8ee06a",  # light green
    "j": "#1f6b2a",  # dark green
    "b": "#3a7be0",  # blue
    "u": "#7fc8f8",  # light blue
    "v": "#5ff0e0",  # cyan glow
    "p": "#a45be0",  # purple
    "t": "#7a5230",  # brown
    "d": "#4a3222",  # dark brown
    "h": "#2a2622",  # oil black
    "c": "#d7eef8",  # glass / ice
    "z": "#e8c07a",  # sand / skin
}

TIER_ACCENTS = {
    1: "#e07b39",  # LV — copper orange
    2: "#3fb8d6",  # MV — cyan
    3: "#e8c547",  # HV — gold
    4: "#a45be0",  # EV — purple
}


def hex_rgba(value: str, alpha: int = 255) -> Color:
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def ramp(base: str, hue_shift: float = 0.035) -> Ramp:
    """Six shades from a base colour. Darker steps drift cooler, lighter warmer."""
    r, g, b, _ = hex_rgba(base)
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    steps = [(0.30, -2), (0.55, -1.2), (0.78, -0.5), (1.0, 0), (None, 0.4), (None, 0.8)]
    out: Ramp = []
    for factor, shift in steps:
        if factor is not None:
            nl = l * factor
        else:
            nl = l + (1 - l) * (0.38 if shift < 0.5 else 0.72)
        nh = (h - hue_shift * shift / 2) % 1.0
        ns = min(1.0, s * (1.05 if factor and factor < 1 else 0.9))
        rr, gg, bb = colorsys.hls_to_rgb(nh, max(0.0, min(1.0, nl)), ns)
        out.append((round(rr * 255), round(gg * 255), round(bb * 255), 255))
    return out


def tier_ramp(tier: int) -> Ramp:
    return ramp(TIER_ACCENTS[tier])


NAMED_RGBA = {k: hex_rgba(v) for k, v in NAMED.items()}


def render_mask(mask: list[str], primary: Ramp | None = None, accent: Ramp | None = None,
                size: int = 16) -> Image.Image:
    if len(mask) != size or any(len(row) != size for row in mask):
        bad = [f"row {i}: {len(r)}" for i, r in enumerate(mask) if len(r) != size]
        raise ValueError(f"mask must be {size}x{size}; got {len(mask)} rows {bad}")
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            color = resolve(ch, primary, accent)
            if color is not None:
                px[x, y] = color
    return img


def resolve(ch: str, primary: Ramp | None, accent: Ramp | None) -> Color | None:
    if ch == ".":
        return None
    if ch.isdigit():
        if primary is None:
            raise ValueError("mask uses primary ramp but none given")
        return primary[int(ch)]
    if "A" <= ch <= "F":
        if accent is None:
            raise ValueError("mask uses accent ramp but none given")
        return accent[ord(ch) - ord("A")]
    if ch in NAMED_RGBA:
        return NAMED_RGBA[ch]
    raise ValueError(f"unknown mask character {ch!r}")


def outline(img: Image.Image, color: Color) -> Image.Image:
    """Adds a 1px outline around every opaque region (4-neighbourhood)."""
    w, h = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3]:
                    dst[x, y] = color
                    break
    return out


def paste_mask(base: Image.Image, mask: list[str], ox: int, oy: int,
               primary: Ramp | None = None, accent: Ramp | None = None) -> None:
    px = base.load()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            color = resolve(ch, primary, accent)
            if color is not None:
                px[ox + x, oy + y] = color


# --- Machine casings ---------------------------------------------------------

CASING_BODY = {
    0: "#9aa1a8",  # neutral / non-powered
    1: "#8e969e",  # LV
    2: "#6f7c88",  # MV
    3: "#4f565e",  # HV
}


def _noise(x: int, y: int, seed: int) -> int:
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n >> 16) & 0xFF


def casing(tier: int, seed: int = 0, rivets: bool = True, body: str | None = None) -> Image.Image:
    """Bevelled metal plate used as the base of every machine face."""
    r = ramp(body or CASING_BODY[tier], hue_shift=0.02)
    img = Image.new("RGBA", (16, 16), r[3])
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = _noise(x, y, seed)
            if n < 22:
                px[x, y] = r[2]
            elif n > 238:
                px[x, y] = r[4]
    for i in range(16):
        px[i, 0] = r[1]
        px[0, i] = r[1]
        px[i, 15] = r[0]
        px[15, i] = r[0]
    for i in range(1, 15):
        px[i, 1] = r[4]
        px[1, i] = r[4]
        px[i, 14] = r[2]
        px[14, i] = r[2]
    if rivets:
        for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
            px[cx, cy] = r[5]
            if cx + 1 < 15 and cy + 1 < 15:
                px[cx + 1, cy + 1] = r[1]
    return img


def panel(img: Image.Image, x0: int, y0: int, x1: int, y1: int) -> None:
    """Recessed dark panel with an inner shadow, inclusive bounds."""
    px = img.load()
    bg = NAMED_RGBA["n"]
    dark = NAMED_RGBA["k"]
    light = (86, 94, 104, 255)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = bg
    for x in range(x0, x1 + 1):
        px[x, y0] = dark
        px[x, y1 + 1 if y1 + 1 < 16 else y1] = light
    for y in range(y0, y1 + 1):
        px[x0, y] = dark
        px[x1 + 1 if x1 + 1 < 16 else x1, y] = light


def trim(img: Image.Image, tier: int, row: int, height: int = 2, x0: int = 1, x1: int = 14) -> None:
    """Horizontal accent stripe in the tier colour."""
    if tier == 0:
        return
    a = tier_ramp(tier)
    px = img.load()
    for x in range(x0, x1 + 1):
        px[x, row] = a[4]
        for dy in range(1, height):
            px[x, row + dy] = a[3]
        if row + height < 15:
            px[x, row + height] = a[1]


def vents(img: Image.Image, x0: int, y0: int, x1: int, y1: int, step: int = 2) -> None:
    px = img.load()
    for y in range(y0, y1 + 1, step):
        for x in range(x0, x1 + 1):
            px[x, y] = NAMED_RGBA["k"]
            if y + 1 <= y1 + 1:
                px[x, y + 1] = (120, 128, 138, 255)


def machine_faces(tier: int, glyph: list[str] | None, seed: int, accent: Ramp | None = None,
                  top: str = "vent", side: str = "trim", body: str | None = None) -> dict[str, Image.Image]:
    """Standard orientable machine: front shows a glyph panel, top/side are casing."""
    accent = accent or (tier_ramp(tier) if tier else ramp("#9aa1a8"))

    front = casing(tier, seed, body=body)
    panel(front, 3, 3, 12, 12)
    if glyph:
        paste_mask(front, glyph, 3, 3, accent=accent)
    lamp = accent[4] if tier else NAMED_RGBA["g"]
    front.load()[12, 14] = lamp

    top_img = casing(tier, seed + 1, body=body)
    if top == "vent":
        panel(top_img, 5, 5, 10, 10)
        vents(top_img, 6, 6, 9, 8)
    elif top == "plain":
        pass
    elif top == "hatch":
        panel(top_img, 3, 3, 12, 12)
        p = top_img.load()
        for i in range(4, 12):
            p[i, i] = NAMED_RGBA["x"]
            p[15 - i, i] = NAMED_RGBA["x"]

    side_img = casing(tier, seed + 2, body=body)
    if side == "trim":
        trim(side_img, tier, 7)
        vents(side_img, 4, 3, 11, 4)
        vents(side_img, 4, 11, 11, 12)
    elif side == "vent":
        panel(side_img, 3, 4, 12, 11)
        vents(side_img, 4, 5, 11, 10)

    return {"front": front, "top": top_img, "side": side_img}


# --- Isometric icon (Bedrock inventory) ---------------------------------------

def _affine_face(tex: Image.Image, size: int, quad: tuple, shade: float) -> Image.Image:
    """Maps a square texture onto the parallelogram (o, u, v) in a size x size canvas."""
    (ox, oy), (ux, uy), (vx, vy) = quad
    # texture (s, t) in [0,16) -> canvas p = o + s/16*(u-o) + t/16*(v-o); invert for PIL.
    ax, ay = (ux - ox) / 16, (uy - oy) / 16
    bx, by = (vx - ox) / 16, (vy - oy) / 16
    det = ax * by - ay * bx
    ia, ib = by / det, -bx / det
    ic, id_ = -ay / det, ax / det
    coeffs = (ia, ib, -(ia * ox + ib * oy), ic, id_, -(ic * ox + id_ * oy))
    shaded = tex.copy()
    if shade != 1.0:
        p = shaded.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = p[x, y]
                p[x, y] = (int(r * shade), int(g * shade), int(b * shade), a)
    return shaded.transform((size, size), Image.AFFINE, coeffs, resample=Image.NEAREST)


def isometric_icon(faces: dict[str, Image.Image], size: int) -> Image.Image:
    top = faces.get("top", faces.get("all"))
    front = faces.get("front", faces.get("all"))
    side = faces.get("side", faces.get("all"))
    s = size
    h = s / 2
    q = s / 4
    canvas = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    # Left face (front texture), right face (side texture), top.
    left = _affine_face(front, s, ((0, q), (h, h), (0, s - q)), 0.94)
    right = _affine_face(side, s, ((h, h), (s, q), (h, s)), 0.74)
    top_face = _affine_face(top, s, ((0, q), (h, 0), (h, h)), 1.0)
    for layer in (left, right, top_face):
        canvas.alpha_composite(layer)
    return canvas


# --- Misc ----------------------------------------------------------------------

def pack_logo() -> Image.Image:
    """64x64 pack icon: a gear over a tier-coloured plate."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((1, 1, 30, 30), radius=5, fill=hex_rgba("#23272e"), outline=hex_rgba("#16161c"))
    d.rounded_rectangle((3, 3, 28, 28), radius=4, outline=hex_rgba("#3fb8d6"))
    gear = gear_sprite(ramp("#e8c547"), radius=11.5, teeth=10, hole=3.2, size=32)
    img.alpha_composite(gear)
    return img.resize((64, 64), Image.NEAREST)


def gear_sprite(r: Ramp, radius: float = 7.2, teeth: int = 8, hole: float = 2.0,
                size: int = 16) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    inner = radius * 0.74
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            dist = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = (math.cos(ang * teeth) > 0.15)
            if dist <= hole or dist > radius or (dist > inner and not tooth):
                continue
            light = -(dx + dy) / (2 * radius)  # top-left is lit
            idx = 3 + round(light * 2)
            if dist > inner - 1:
                idx -= 1
            px[x, y] = r[max(1, min(5, idx))]
    return outline(img, r[0])


def contact_sheet(icons: list[tuple[str, Image.Image]], path: Path, scale: int = 3, cols: int = 12) -> None:
    cell_w, cell_h = 32 * scale + 8, 32 * scale + 22
    rows = math.ceil(len(icons) / cols)
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (60, 63, 70, 255))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 11)
    except OSError:
        font = ImageFont.load_default()
    for i, (name, icon) in enumerate(icons):
        cx, cy = (i % cols) * cell_w, (i // cols) * cell_h
        d.rectangle((cx + 2, cy + 2, cx + cell_w - 3, cy + 32 * scale + 5), fill=(139, 139, 139, 255))
        big = icon.resize((32 * scale, 32 * scale), Image.NEAREST)
        sheet.alpha_composite(big, (cx + 4, cy + 4))
        d.text((cx + 4, cy + 32 * scale + 7), name[:18], fill=(235, 235, 235, 255), font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
