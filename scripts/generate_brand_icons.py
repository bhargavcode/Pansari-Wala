#!/usr/bin/env python3
"""Regenerate every Jansho app icon / logo from branding/jansho-logo-source.jpg.

Usage: python3 scripts/generate_brand_icons.py   (needs Pillow)
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "branding" / "jansho-logo-source.jpg"
ANDROID_RES = ROOT / "androidApp" / "src"
FONT = "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf"

WHITE = (255, 255, 255, 255)
NAVY = (0, 41, 133, 255)
ORANGE = (232, 84, 0, 255)
DEBUG_RED = (211, 47, 47, 255)

# Logo artwork bounds inside the 1024px source (mark + wordmark).
LOGO_BOX = (96, 186, 934, 836)
CANVAS = 1024

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FLAVORS = {
    "main": None,
    "user": None,
    "userDebug": None,
    "pos": "PoS",
    "posDebug": "PoS",
    "delivery": "Partner",
    "deliveryDebug": "Partner",
}
LABEL_COLORS = {"PoS": NAVY, "Partner": ORANGE}


def logo() -> Image.Image:
    return Image.open(SOURCE).convert("RGBA").crop(LOGO_BOX)


def fit(img: Image.Image, w: int, h: int) -> Image.Image:
    scale = min(w / img.width, h / img.height)
    return img.resize((round(img.width * scale), round(img.height * scale)), Image.LANCZOS)


def compose(label: str | None, debug: bool, content_ratio: float) -> Image.Image:
    """Square white artwork: logo (+ variant pill) centered inside `content_ratio` of the canvas."""
    canvas = Image.new("RGBA", (CANVAS, CANVAS), WHITE)
    box = round(CANVAS * content_ratio)
    origin = (CANVAS - box) // 2
    draw = ImageDraw.Draw(canvas)
    if label is None:
        art = fit(logo(), box, box)
        canvas.alpha_composite(art, ((CANVAS - art.width) // 2, (CANVAS - art.height) // 2))
    else:
        pill_h = round(box * 0.2)
        gap = round(box * 0.05)
        art = fit(logo(), box, box - pill_h - gap)
        top = origin + (box - art.height - gap - pill_h) // 2
        canvas.alpha_composite(art, ((CANVAS - art.width) // 2, top))
        font = ImageFont.truetype(FONT, round(pill_h * 0.66))
        tw = draw.textlength(label, font=font)
        pill_w = round(tw + pill_h * 1.1)
        px = (CANVAS - pill_w) // 2
        py = top + art.height + gap
        draw.rounded_rectangle((px, py, px + pill_w, py + pill_h), radius=pill_h // 2, fill=LABEL_COLORS[label])
        draw.text((CANVAS / 2, py + pill_h / 2), label, font=font, fill=WHITE, anchor="mm")
    if debug:
        band = round(CANVAS * 0.11)
        strip = Image.new("RGBA", (round(CANVAS * 0.8), band), DEBUG_RED)
        ImageDraw.Draw(strip).text(
            (strip.width / 2, band / 2), "DEBUG",
            font=ImageFont.truetype(FONT, round(band * 0.62)), fill=WHITE, anchor="mm",
        )
        strip = strip.rotate(-45, resample=Image.BICUBIC, expand=True)
        cx, cy = round(CANVAS * 0.8), round(CANVAS * 0.2)
        canvas.alpha_composite(strip, (cx - strip.width // 2, cy - strip.height // 2))
    return canvas


def rounded(img: Image.Image, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *img.size), radius=round(img.width * radius_ratio), fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


def circle(img: Image.Image) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, *img.size), fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


def save(img: Image.Image, path: Path, size: int, flatten: bool = False) -> None:
    out = img.resize((size, size), Image.LANCZOS)
    if flatten:
        bg = Image.new("RGB", out.size, (255, 255, 255))
        bg.paste(out, mask=out.split()[3])
        out = bg
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path, optimize=True)


def android() -> None:
    for flavor, label in FLAVORS.items():
        debug = flavor.endswith("Debug")
        legacy = compose(label, debug, 0.86)
        # Adaptive foreground: keep artwork inside the 66/108 safe zone.
        foreground = compose(label, debug, 0.6)
        for density, size in DENSITIES.items():
            res = ANDROID_RES / flavor / "res" / f"mipmap-{density}"
            save(rounded(legacy, 0.18), res / "ic_launcher.png", size)
            save(circle(legacy), res / "ic_launcher_round.png", size)
            save(foreground, res / "ic_launcher_foreground.png", round(size * 2.25))


def ios_web_shared() -> None:
    icon = compose(None, False, 0.84)
    save(icon, ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png", 1024, flatten=True)
    save(icon, ROOT / "webApp/src/webMain/resources/favicon.png", 420)
    save(compose(None, False, 0.96), ROOT / "shared/src/commonMain/composeResources/drawable/jansho_logo.png", 420)


if __name__ == "__main__":
    android()
    ios_web_shared()
    print("Jansho icons regenerated.")
