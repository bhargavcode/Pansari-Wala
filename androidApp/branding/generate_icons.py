#!/usr/bin/env python3
"""Regenerate Android / iOS launcher icons from branding/icons masters."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
BRAND = Path(__file__).resolve().parent / "icons"
ANDROID = ROOT / "androidApp" / "src"
IOS_ICON = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset" / "app-icon-1024.png"

SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FG_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""

BG_COLOR_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
"""


def add_debug_label(im: Image.Image) -> Image.Image:
    out = im.copy()
    draw = ImageDraw.Draw(out)
    w, h = out.size
    band = max(int(h * 0.14), 48)
    draw.polygon([(w - band * 2.2, 0), (w, 0), (w, band * 2.2)], fill=(196, 48, 43, 255))
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", max(int(h * 0.055), 28))
    except OSError:
        font = ImageFont.load_default()
    tw, th = int(band * 2.4), int(band * 0.7)
    layer = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    bbox = ld.textbbox((0, 0), "DEBUG", font=font)
    twt, tht = bbox[2] - bbox[0], bbox[3] - bbox[1]
    ld.text(((tw - twt) / 2, (th - tht) / 2 - 2), "DEBUG", font=font, fill=(255, 255, 255, 255))
    layer = layer.rotate(-45, expand=True, resample=Image.Resampling.BICUBIC)
    out.alpha_composite(layer, (w - layer.width + int(band * 0.15), -int(band * 0.05)))
    return out


def write_android_set(src_set: str, icon: Image.Image) -> None:
    res = ANDROID / src_set / "res"
    for dens, px in SIZES.items():
        resized = icon.resize((px, px), Image.Resampling.LANCZOS)
        folder = res / f"mipmap-{dens}"
        folder.mkdir(parents=True, exist_ok=True)
        resized.save(folder / "ic_launcher.png", "PNG", optimize=True)
        resized.save(folder / "ic_launcher_round.png", "PNG", optimize=True)
    for dens, px in FG_SIZES.items():
        folder = res / f"mipmap-{dens}"
        folder.mkdir(parents=True, exist_ok=True)
        icon.resize((px, px), Image.Resampling.LANCZOS).save(
            folder / "ic_launcher_foreground.png", "PNG", optimize=True
        )
    anydpi = res / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (anydpi / name).write_text(ADAPTIVE_XML, encoding="utf-8")
    values = res / "values"
    values.mkdir(parents=True, exist_ok=True)
    (values / "ic_launcher_background.xml").write_text(BG_COLOR_XML, encoding="utf-8")


def main() -> None:
    mapping = {
        "user": BRAND / "user.png",
        "delivery": BRAND / "partner.png",
        "pos": BRAND / "pos.png",
    }
    for flavor, path in mapping.items():
        base = Image.open(path).convert("RGBA")
        write_android_set(flavor, base)
        write_android_set(f"{flavor}Debug", add_debug_label(base))
    user = Image.open(mapping["user"]).convert("RGBA")
    write_android_set("main", user)
    IOS_ICON.parent.mkdir(parents=True, exist_ok=True)
    user.save(IOS_ICON, "PNG", optimize=True)
    print("Icons regenerated.")


if __name__ == "__main__":
    main()
