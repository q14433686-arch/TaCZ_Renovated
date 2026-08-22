#!/usr/bin/env python3
"""Generate original TaCZ: Renovated brand marks.

Produces:
  src/main/resources/icon.png  — square Mods / GitHub avatar
  src/main/resources/logo.png  — wide NeoForge logoFile banner

Original artwork for this unofficial port. Not derived from official TaCZ
icon.png / logo.png (those are CC BY-NC-ND 4.0 and must not be remixed).

Regenerate:
  python3 -m venv /tmp/branding-venv
  /tmp/branding-venv/bin/pip install pillow
  /tmp/branding-venv/bin/python scripts/generate_branding.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "src" / "main" / "resources"
FONT_BOLD = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")

# Palette — cyan reticle family language, copper = "renovated / forge"
BG = (26, 29, 34, 255)
CYAN = (27, 184, 224, 255)
COPPER = (209, 122, 42, 255)
SILVER = (198, 200, 202, 255)


def _polar(cx: float, cy: float, r: float, deg: float) -> tuple[float, float]:
    a = math.radians(deg)
    return (cx + r * math.cos(a), cy + r * math.sin(a))


def _thick_arc(
    cx: float,
    cy: float,
    r_mid: float,
    half_w: float,
    start: float,
    end: float,
    n: int = 160,
) -> list[tuple[float, float]]:
    outer = r_mid + half_w
    inner = r_mid - half_w
    pts: list[tuple[float, float]] = []
    for i in range(n + 1):
        t = start + (end - start) * i / n
        pts.append(_polar(cx, cy, outer, t))
    for i in range(n + 1):
        t = end - (end - start) * i / n
        pts.append(_polar(cx, cy, inner, t))
    return pts


def draw_reticle(draw: ImageDraw.ImageDraw, cx: float, cy: float, r_mid: float, half_w: float) -> None:
    """Four-segment targeting ring. Gaps sit on the cardinals."""
    # math degrees: 0 = east, CCW. Radial (butt) caps = scope-reticle cuts.
    gap = 32.0
    span = 90.0 - gap
    centres = (45.0, 135.0, 225.0, 315.0)
    for mid in centres:
        start = mid - span / 2.0
        end = mid + span / 2.0
        draw.polygon(_thick_arc(cx, cy, r_mid, half_w, start, end), fill=CYAN)


def _load(path: Path, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(path), size)


def draw_centered_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    cx: float,
    cy: float,
    font: ImageFont.FreeTypeFont,
    fill,
    tracking: float = 0.0,
) -> None:
    """Draw text centered on (cx, cy) with optional extra letter-spacing."""
    if tracking == 0.0:
        bbox = draw.textbbox((0, 0), text, font=font)
        w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
        draw.text((cx - w / 2.0 - bbox[0], cy - h / 2.0 - bbox[1]), text, font=font, fill=fill)
        return
    widths = []
    for ch in text:
        bb = draw.textbbox((0, 0), ch, font=font)
        widths.append(bb[2] - bb[0])
    total = sum(widths) + tracking * (len(text) - 1)
    x = cx - total / 2.0
    bb0 = draw.textbbox((0, 0), text, font=font)
    h = bb0[3] - bb0[1]
    y = cy - h / 2.0 - bb0[1]
    for ch, w in zip(text, widths):
        draw.text((x, y), ch, font=font, fill=fill)
        x += w + tracking


def _render_icon_raw(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), BG)
    draw = ImageDraw.Draw(img)
    s = size / 1024.0

    cx, cy = size * 0.50, size * 0.42
    r_mid = size * 0.248
    half_w = size * 0.040
    draw_reticle(draw, cx, cy, r_mid, half_w)

    r_font = _load(FONT_BOLD, int(round(268 * s)))
    draw_centered_text(draw, "R", cx, cy + size * 0.012, r_font, COPPER)

    tacz_font = _load(FONT_BOLD, int(round(72 * s)))
    reno_font = _load(FONT_BOLD, int(round(36 * s)))
    draw_centered_text(draw, "TACZ", size * 0.50, size * 0.78, tacz_font, SILVER, tracking=10 * s)
    draw_centered_text(draw, "RENOVATED", size * 0.50, size * 0.86, reno_font, COPPER, tracking=8 * s)
    return img


def _render_logo_raw(width: int, height: int) -> Image.Image:
    img = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(img)

    cx, cy = height * 0.50, height * 0.50
    r_mid = height * 0.32
    half_w = height * 0.058
    draw_reticle(draw, cx, cy, r_mid, half_w)
    r_font = _load(FONT_BOLD, int(round(height * 0.38)))
    draw_centered_text(draw, "R", cx, cy + height * 0.015, r_font, COPPER)

    text_left = height * 1.08
    tacz_font = _load(FONT_BOLD, int(round(height * 0.13)))
    reno_font = _load(FONT_BOLD, int(round(height * 0.28)))
    draw.text((text_left, height * 0.18), "TACZ", font=tacz_font, fill=CYAN)
    reno = "RENOVATED"
    draw.text((text_left, height * 0.36), reno, font=reno_font, fill=SILVER)
    reno_bb = draw.textbbox((text_left, height * 0.36), reno, font=reno_font)
    rule_y = height * 0.78
    draw.rectangle((text_left, rule_y, reno_bb[2], rule_y + height * 0.018), fill=COPPER)
    return img


def render_icon(size: int = 512, ssaa: int = 4) -> Image.Image:
    raw = _render_icon_raw(size * ssaa)
    return raw.resize((size, size), Image.Resampling.LANCZOS)


def render_logo(width: int = 1280, height: int = 360, ssaa: int = 4) -> Image.Image:
    raw = _render_logo_raw(width * ssaa, height * ssaa)
    return raw.resize((width, height), Image.Resampling.LANCZOS)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    icon = render_icon(512)
    logo = render_logo(1280, 360)
    icon.save(OUT_DIR / "icon.png", "PNG", optimize=True)
    logo.save(OUT_DIR / "logo.png", "PNG", optimize=True)
    print(f"wrote {OUT_DIR / 'icon.png'} {icon.size}")
    print(f"wrote {OUT_DIR / 'logo.png'} {logo.size}")


if __name__ == "__main__":
    main()
