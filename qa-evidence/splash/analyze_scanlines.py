"""Scanline analysis of the K strokes + smile for precise corner extraction."""
from PIL import Image
import colorsys

SRC = r"D:\KNOUX-AI\design\brand\knoux-logo-owner.png"
img = Image.open(SRC).convert("RGB")
W, H = img.size
px = img.load()

def bright(x, y):
    r, g, b = px[x, y]
    h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
    return s > 0.45 and v > 0.5

def runs(y, x0, x1):
    out = []
    start = None
    for x in range(x0, x1):
        if bright(x, y):
            if start is None: start = x
        else:
            if start is not None:
                out.append((start, x - 1)); start = None
    if start is not None: out.append((start, x1 - 1))
    return out

print("=== K scanlines (x-runs of bright pixels) ===")
for y in range(265, 630, 15):
    rr = runs(y, 300, 800)
    if rr: print(f"y={y}: {rr}")

print("=== upper stroke vertical extent at x columns ===")
for x in [420, 450, 480, 500, 520, 540, 560, 580, 600, 620, 630]:
    ys = [y for y in range(260, 530) if bright(x, y)]
    if ys: print(f"x={x}: y {min(ys)}..{max(ys)}")

print("=== cyan stroke (hue<225) scanlines ===")
def cyan(x, y):
    r, g, b = px[x, y]
    h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
    return 175 <= h*360 < 222 and s > 0.5 and v > 0.55
for y in range(460, 625, 10):
    rr = []
    start = None
    for x in range(430, 760):
        if cyan(x, y):
            if start is None: start = x
        else:
            if start is not None: rr.append((start, x-1)); start = None
    if start is not None: rr.append((start, 759))
    if rr: print(f"y={y}: {rr}")

print("=== wordmark full extent (white + pink ai) ===")
# ai is pink: hue 280-320, bright, in wordmark band
for y in [660, 700, 740, 780]:
    rr = runs(y, 150, 950)
    print(f"y={y}: {rr}")
# pink ai region
xs = []
for y in range(650, 790):
    for x in range(700, 950):
        r, g, b = px[x, y]
        h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
        if 270 <= h*360 <= 330 and s > 0.4 and v > 0.6:
            xs.append(x)
if xs: print(f"ai pink x-range: {min(xs)}..{max(xs)}")

print("=== smile fine trace (peak brightness per column) ===")
curve = []
for x in range(120, 1010, 4):
    best = None
    for y in range(820, 1000):
        r, g, b = px[x, y]
        h, s, v = colorsys.rgb_to_hsv(r/255, g/255, b/255)
        if s > 0.4 and v > 0.5:
            score = v * s
            if best is None or score > best[0]: best = (score, y, h*360)
    if best: curve.append((x, best[1], best[2]))
for x, y, h in curve[::10]:
    print(f"x={x} ({x/W*1000:.0f}) y={y} ({y/H*1000:.0f}) hue={h:.0f}")
print(f"ends: {curve[0]} .. {curve[-1]}")
ys = [c[1] for c in curve]
print(f"y min={min(ys)} max={max(ys)} (max at x={curve[ys.index(max(ys))][0]})")