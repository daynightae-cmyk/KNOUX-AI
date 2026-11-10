"""Align two real PNGs for visual inspection, excluding system bars and mock handset.

Requires Pillow. Example for a letterboxed API 26 capture:
python scripts/Compare-Preview.py --runtime-bars 52 104 --runtime-sides 97 97
Run on captures taken at the top of each screen with the same Android display settings.
"""
import argparse
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat

NAMES = ("home", "privacy", "duplicates", "deep-scan", "reports", "residue",
         "app-manager", "storage", "battery", "ai-optimization")
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "qa-evidence" / "ui"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--runtime-bars", nargs=2, type=int, required=True, metavar=("TOP_PX", "BOTTOM_PX"))
parser.add_argument("--runtime-sides", nargs=2, type=int, default=(0, 0), metavar=("LEFT_PX", "RIGHT_PX"))
args = parser.parse_args()
top, bottom = args.runtime_bars
left, right = args.runtime_sides
for num, (reference, name) in enumerate(zip(sorted((ROOT / "design/reference").glob("*.png")), NAMES), 1):
    path = OUT / f"{num:02}-{name}-runtime.png"
    if not path.is_file():
        print(f"{num:02} missing runtime capture")
        continue
    with Image.open(reference) as original, Image.open(path) as runtime:
        if top < 0 or bottom < 0 or top + bottom >= runtime.height:
            parser.error("Invalid runtime bar crop for " + path.name)
        if left < 0 or right < 0 or left + right >= runtime.width:
            parser.error("Invalid runtime side crop for " + path.name)
        ref = original.convert("RGB").crop((78, 76, 786, 1458))
        run = runtime.convert("RGB").crop((left, top, runtime.width - right, runtime.height - bottom))
    # Normalize width and retain each image's actual height to expose geometry shifts.
    width = 540
    def fit(im):
        return im.resize((width, round(im.height * width / im.width)), Image.Resampling.LANCZOS)
    ref, run = fit(ref), fit(run)
    canvas = Image.new("RGB", (width * 2 + 18, max(ref.height, run.height) + 35), "#181529")
    canvas.paste(ref, (0, 35))
    canvas.paste(run, (width + 18, 35))
    label = ImageDraw.Draw(canvas)
    label.text((9, 9), f"{num:02}  Reference / handset excluded", fill="white")
    label.text((width + 28, 9), f"{num:02}  Android / {name}", fill="white")
    canvas.save(OUT / f"comparison-{num:02}.png")
    overlap = min(ref.height, run.height)
    diff = ImageChops.difference(ref.crop((0, 0, width, overlap)), run.crop((0, 0, width, overlap)))
    mean = sum(ImageStat.Stat(diff).mean) / 3
    print(f"{num:02} {name}: normalized height {ref.height} vs {run.height}, overlapping mean RGB error {mean:.1f}/255 (unaligned content; diagnostic only)")
