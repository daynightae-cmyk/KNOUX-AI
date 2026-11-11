"""Build reference-vs-runtime comparison sheets for the Phase 1 UI evidence.

Masks the mock handset frame in the design references and the emulator system
bars/letterboxing in the runtime captures. Diagnostic images only: the fixture
metrics shown are not real scan results.

Usage: python scripts/Build-ComparisonSheets.py --qa qa-evidence/ui-polish-20261111-pass5
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUTER_REF = ROOT / "design" / "reference-20261111"
INNER_REF = ROOT / "design" / "reference-inner-20261111"
# The new outer set uses slot 2 for the splash, so App Manager keeps its original reference.
ORIGINAL_REF = ROOT / "design" / "reference"

# Reference mockups are 941x1672 with the handset shell around a 765x1500 screen.
REF_BOX = (88, 76, 853, 1576)
# Runtime captures are 1080x1920 letterboxing a 1080x2340 override: the app
# occupies 886x1920 at x=97, with 52 px status bar and 104 px navigation bar.
RUN_BOX = (97, 52, 983, 1816)

PAIRS = [
    ("outer", "01-home", "01_AI_Smart_Clean_Home.png", "01-home-runtime.png"),
    ("outer", "02-privacy", "03_Privacy_Sweep.png", "02-privacy-runtime.png"),
    ("outer", "03-duplicates", "04_Duplicate_Files_Finder.png", "03-duplicates-runtime.png"),
    ("outer", "04-deep-scan", "05_Deep_Scan.png", "04-deep-scan-runtime.png"),
    ("outer", "05-reports", "06_Reports_Insights.png", "05-reports-runtime.png"),
    ("outer", "06-residue", "07_App_Residue_Cleanup.png", "06-residue-runtime.png"),
    ("legacy", "07-app-manager", "07_App_Manager.png", "07-app-manager-runtime.png"),
    ("outer", "08-storage", "08_Storage_Analyzer.png", "08-storage-runtime.png"),
    ("outer", "09-battery", "09_Battery_Saver.png", "09-battery-runtime.png"),
    ("outer", "10-ai-optimization", "10_AI_Optimization.png", "10-ai-optimization-runtime.png"),
    ("inner", "01-clean-center", "01_Smart_Clean_Center.png", "01-clean-center-runtime.png"),
    ("inner", "02-duplicate-review", "02_Duplicate_Group_Review.png", "02-duplicate-group-review-runtime.png"),
    ("inner", "03-privacy-inspector", "03_Privacy_Risk_Inspector.png", "03-privacy-risk-inspector-runtime.png"),
    ("inner", "04-deep-summary", "04_Deep_Scan_Summary.png", "04-deep-scan-summary-runtime.png"),
    ("inner", "05-perf-detail", "05_Performance_Report_Detail.png", "05-performance-report-detail-runtime.png"),
    ("inner", "06-residue-review", "06_Residue_Item_Review.png", "06-residue-item-review-runtime.png"),
    ("inner", "07-apps-review", "07_Installed_Apps_Review.png", "07-installed-apps-review-runtime.png"),
    ("inner", "08-storage-breakdown", "08_Storage_Breakdown.png", "08-storage-breakdown-runtime.png"),
    ("inner", "09-battery-diag", "09_Battery_Diagnostics.png", "09-battery-diagnostics-runtime.png"),
    ("inner", "10-ai-recs", "10_AI_Recommendations.png", "10-ai-recommendations-runtime.png"),
]

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--qa", type=Path, default=ROOT / "qa-evidence" / "ui-polish-20261111-pass5")
parser.add_argument("--width", type=int, default=430)
args = parser.parse_args()

out = args.qa / "comparisons"
out.mkdir(parents=True, exist_ok=True)

try:
    font = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 19)
    small = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 15)
except OSError:
    font = small = ImageFont.load_default()


def fit(image, width):
    return image.resize((width, round(image.height * width / image.width)), Image.Resampling.LANCZOS)


def sheet(name, ref_path, run_path, ref_box, run_box, runtime_width):
    with Image.open(ref_path) as ref_im, Image.open(run_path) as run_im:
        ref = fit(ref_im.convert("RGB").crop(ref_box), args.width)
        run = fit(run_im.convert("RGB").crop(run_box), runtime_width)
    height = max(ref.height, run.height)
    canvas = Image.new("RGB", (args.width * 2 + 30, height + 70), "#140e27")
    draw = ImageDraw.Draw(canvas)
    draw.text((10, 8), "REFERENCE (handset frame masked)", font=font, fill="#faf5ff")
    draw.text((args.width + 20, 8), "RUNTIME (final APK, API 26 emulator)", font=font, fill="#faf5ff")
    draw.text((10, 34), ref_path.name, font=small, fill="#b9a9e0")
    draw.text((args.width + 20, 34), run_path.name, font=small, fill="#b9a9e0")
    canvas.paste(ref, (10, 60))
    canvas.paste(run, (args.width + 20, 60))
    draw.text((10, 60 + height + 4), "Sample/fixture metrics are visibly labelled in-app; nothing was scanned, cleaned or optimized.",
              font=small, fill="#8f7fbd")
    canvas.save(out / f"{name}.png")
    print("wrote", out / f"{name}.png", canvas.size)


for group, label, ref_name, run_name in PAIRS:
    ref_dir = {"outer": OUTER_REF, "inner": INNER_REF, "legacy": ORIGINAL_REF}[group]
    sheet(label, ref_dir / ref_name,
          args.qa / run_name, REF_BOX, RUN_BOX, args.width)

# Splash stills are captured at native 1080x1920 (no size override), so only the
# mockup frame is masked from the reference side.
sheet("splash", OUTER_REF / "02_Splash.png", args.qa / "splash" / "splash-3000.png",
      REF_BOX, (0, 0, 1080, 1920), args.width)
