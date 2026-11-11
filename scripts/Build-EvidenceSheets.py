# Builds final contact sheets / comparison images for qa-evidence/knoux-ai-visual-finish.
# Diagnostic layout tooling only - no runtime claims are made by these images.
from PIL import Image, ImageDraw
from pathlib import Path

ROOT = Path(r"D:\KNOUX-AI")
EV = ROOT / "qa-evidence" / "knoux-ai-visual-finish"

LABEL_H = 34
TILE_W = 260
TILE_H = 462  # 1080x1920 runtime captures -> exact aspect 0.5625 at 260px wide
COLS = 5

def load(path):
    im = Image.open(path).convert("RGB")
    scale = min(TILE_W / im.width, TILE_H / im.height)
    w, h = round(im.width * scale), round(im.height * scale)
    im = im.resize((w, h), Image.LANCZOS)
    tile = Image.new("RGB", (TILE_W, TILE_H), (8, 5, 18))
    tile.paste(im, ((TILE_W - w) // 2, (TILE_H - h) // 2))
    return tile

def sheet(tiles, title, out_path):
    rows = (len(tiles) + COLS - 1) // COLS
    pad = 10
    header = 52
    W = COLS * (TILE_W + pad) + pad
    H = header + rows * (TILE_H + LABEL_H + pad) + pad
    canvas = Image.new("RGB", (W, H), (8, 5, 18))
    d = ImageDraw.Draw(canvas)
    d.text((pad + 2, 16), title, fill=(230, 225, 255))
    for i, (label, path) in enumerate(tiles):
        r, c = divmod(i, COLS)
        x = pad + c * (TILE_W + pad)
        y = header + r * (TILE_H + LABEL_H + pad)
        canvas.paste(load(path), (x, y))
        d.rectangle([x, y + TILE_H, x + TILE_W, y + TILE_H + LABEL_H - 6], fill=(20, 14, 44))
        d.text((x + 6, y + TILE_H + 7), label, fill=(200, 190, 235))
    canvas.save(out_path)
    print("saved", out_path, canvas.size)

# 1) Splash sequence storyboard: 9 freeze frames + landing on Home.
frames = [
    ("t=480ms bloom", "splash-480.png"),
    ("t=835ms K wedge", "splash-835.png"),
    ("t=1140ms ribbon", "splash-1140.png"),
    ("t=1610ms wordmark", "splash-1610.png"),
    ("t=2280ms smile grows", "splash-2280.png"),
    ("t=2460ms tip near end", "splash-2460.png"),
    ("t=2710ms click frame", "splash-2710.png"),
    ("t=2800ms settle", "splash-2800.png"),
    ("t=3000ms lockup", "splash-3000.png"),
    ("then Home", "cold-launch-home.png"),
]
tiles = [(lbl, str(EV / "splash" / f)) for lbl, f in frames]
sheet(tiles, "KNOUX AI splash cold launch - recorded sequence (emulator-5554, API 26, 1080x2340 @420dpi)",
      EV / "splash" / "sequence.png")

# 2) Splash reference vs runtime final lockup.
ref = Image.open(ROOT / "design" / "reference-20261111" / "02_Splash.png").convert("RGB")
run = Image.open(EV / "splash" / "splash-3000.png").convert("RGB")
print("ref size", ref.size, "runtime size", run.size)
H = 1100
def fit(im):
    w = int(im.width * H / im.height)
    return im.resize((w, H), Image.LANCZOS)
ref_f, run_f = fit(ref), fit(run)
gap, header = 16, 60
W = ref_f.width + run_f.width + gap * 3
canvas = Image.new("RGB", (W, H + header + LABEL_H + gap), (8, 5, 18))
d = ImageDraw.Draw(canvas)
d.text((gap, 20), "Splash: reference (left, design target incl. mockup frame) vs runtime final lockup t=3000ms (right, emulator API 26)",
       fill=(230, 225, 255))
canvas.paste(ref_f, (gap, header))
canvas.paste(run_f, (ref_f.width + gap * 2, header))
d.text((gap, header + H + 8), "reference-20261111/02_Splash.png", fill=(200, 190, 235))
d.text((ref_f.width + gap * 2, header + H + 8), "splash/splash-3000.png (cold-launch.mp4)", fill=(200, 190, 235))
out = EV / "splash" / "reference-comparison.png"
canvas.save(out)
print("saved", out, canvas.size)

# 3) Outer screens gallery (10 runtime screenshots).
outer = [
    ("01 Home", "01-home-runtime.png"),
    ("02 Privacy Sweep", "02-privacy-runtime.png"),
    ("03 Duplicate Finder", "03-duplicates-runtime.png"),
    ("04 Deep Scan", "04-deep-scan-runtime.png"),
    ("05 Reports", "05-reports-runtime.png"),
    ("06 Residue", "06-residue-runtime.png"),
    ("07 App Manager", "07-app-manager-runtime.png"),
    ("08 Storage", "08-storage-runtime.png"),
    ("09 Battery", "09-battery-runtime.png"),
    ("10 AI Optimization", "10-ai-optimization-runtime.png"),
]
sheet([(lbl, str(EV / "ui" / f)) for lbl, f in outer],
      "Outer screens 01-10 as captured on emulator (ui/ runtime PNGs, SHA-256 in capture-outer.log)",
      EV / "gallery-outer.png")

# 4) Inner screens gallery (10 runtime screenshots).
inner = [
    ("11 Clean Center", "01-clean-center-runtime.png"),
    ("12 Duplicate Group Review", "02-duplicate-group-review-runtime.png"),
    ("13 Privacy Risk Inspector", "03-privacy-risk-inspector-runtime.png"),
    ("14 Deep Scan Summary", "04-deep-scan-summary-runtime.png"),
    ("15 Performance Report", "05-performance-report-detail-runtime.png"),
    ("16 Residue Item Review", "06-residue-item-review-runtime.png"),
    ("17 Installed Apps Review", "07-installed-apps-review-runtime.png"),
    ("18 Storage Breakdown", "08-storage-breakdown-runtime.png"),
    ("19 Battery Diagnostics", "09-battery-diagnostics-runtime.png"),
    ("20 AI Recommendations", "10-ai-recommendations-runtime.png"),
]
sheet([(lbl, str(EV / "interior" / f)) for lbl, f in inner],
      "Inner screens 11-20 as captured on emulator (interior/ runtime PNGs, SHA-256 in capture-interior.log)",
      EV / "gallery-inner.png")
