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
INNER_NAMES = ("clean-center", "duplicate-group-review", "privacy-risk-inspector", "deep-scan-summary",
               "performance-report-detail", "residue-item-review", "installed-apps-review",
               "storage-breakdown", "battery-diagnostics", "ai-recommendations")
ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--captures-dir", type=Path, default=ROOT / "qa-evidence" / "ui")
parser.add_argument("--references", choices=("original", "outer", "inner"), default="original")
parser.add_argument("--runtime-bars", nargs=2, type=int, required=True, metavar=("TOP_PX", "BOTTOM_PX"))
parser.add_argument("--runtime-sides", nargs=2, type=int, default=(0, 0), metavar=("LEFT_PX", "RIGHT_PX"))
args = parser.parse_args()
OUT = args.captures_dir
top, bottom = args.runtime_bars
left, right = args.runtime_sides
original = sorted((ROOT / "design/reference").glob("*.png"))
if args.references == "outer":
    outer = sorted((ROOT / "design/reference-20261111").glob("*.png"))
    if len(outer) != 10 or len(original) != 10:
        parser.error("Expected ten outer and ten original references.")
    # The second outer PNG is the splash; App Manager still uses its original reference.
    references = [outer[i] if i is not None else original[6] for i in (0, 2, 3, 4, 5, 6, None, 7, 8, 9)]
    names = NAMES
elif args.references == "inner":
    references = sorted((ROOT / "design/reference-inner-20261111").glob("*.png"))
    names = INNER_NAMES
else:
    references, names = original, NAMES
if len(references) != len(names):
    parser.error(f"Expected {len(names)} references, found {len(references)}.")
for num, (reference, name) in enumerate(zip(references, names), 1):
    path = OUT / f"{num:02}-{name}-runtime.png"
    if not path.is_file():
        print(f"{num:02} missing runtime capture")
        continue
    with Image.open(reference) as original, Image.open(path) as runtime:
        if top < 0 or bottom < 0 or top + bottom >= runtime.height:
            parser.error("Invalid runtime bar crop for " + path.name)
        if left < 0 or right < 0 or left + right >= runtime.width:
            parser.error("Invalid runtime side crop for " + path.name)
        bounds = (88, 76, 853, 1576) if original.width == 941 else (78, 76, 786, 1458)
        ref = original.convert("RGB").crop(bounds)
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
