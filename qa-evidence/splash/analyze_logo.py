"""Analyze the owner-supplied KNOUX logo to extract animation geometry.

Segments the three K strokes by hue, locates the wordmark band and traces
the neon smile curve. Outputs normalized coordinates (0..1000 viewBox).
Read-only analysis: never modifies the source asset.
"""
import sys
from PIL import Image
import colorsys

SRC = r"D:\KNOUX-AI\design\brand\knoux-logo-owner.png"
img = Image.open(SRC).convert("RGB")
W, H = img.size
print(f"size={W}x{H}")
px = img.load()

def hue_sat(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return h * 360, s, v

# --- K stroke segmentation (upper area only, y < 0.58H) ---
# stroke 1: magenta/violet  (hue 270-320, high sat)
# stroke 2: violet->blue    (hue 225-275)
# stroke 3: blue->cyan      (hue 180-225, high sat, bright)
boxes = {1: [W, H, 0, 0], 2: [W, H, 0, 0], 3: [W, H, 0, 0]}
counts = {1: 0, 2: 0, 3: 0}
samples = {1: [], 2: [], 3: []}
for y in range(int(H * 0.20), int(H * 0.58)):
    for x in range(int(W * 0.25), int(W * 0.75)):
        r, g, b = px[x, y]
        h, s, v = hue_sat(r, g, b)
        if s < 0.45 or v < 0.45:
            continue
        stroke = None
        if 268 <= h <= 330:
            stroke = 1
        elif 225 <= h < 268:
            stroke = 2
        elif 175 <= h < 225:
            stroke = 3
        if stroke:
            bx = boxes[stroke]
            bx[0] = min(bx[0], x); bx[1] = min(bx[1], y)
            bx[2] = max(bx[2], x); bx[3] = max(bx[3], y)
            counts[stroke] += 1
            if len(samples[stroke]) < 4000:
                samples[stroke].append((x, y, r, g, b))

for k in (1, 2, 3):
    x0, y0, x1, y1 = boxes[k]
    print(f"stroke{k}: px=({x0},{y0})-({x1},{y1}) count={counts[k]} "
          f"norm=({x0/W*1000:.0f},{y0/H*1000:.0f})-({x1/W*1000:.0f},{y1/H*1000:.0f})")
    # extreme corner points of the mask
    pts = [(x, y) for x, y, *_ in samples[k]]
    if pts:
        top = min(pts, key=lambda p: p[1]); bot = max(pts, key=lambda p: p[1])
        left = min(pts, key=lambda p: p[0]); right = max(pts, key=lambda p: p[0])
        print(f"  extremes: top={top} bottom={bot} left={left} right={right}")

# --- wordmark band: bright white pixels below K ---
wy0, wy1 = H, 0
wx0, wx1 = W, 0
for y in range(int(H * 0.55), int(H * 0.75)):
    for x in range(int(W * 0.10), int(W * 0.90)):
        r, g, b = px[x, y]
        if r > 220 and g > 220 and b > 220:
            wy0 = min(wy0, y); wy1 = max(wy1, y)
            wx0 = min(wx0, x); wx1 = max(wx1, x)
print(f"wordmark-white: px=({wx0},{wy0})-({wx1},{wy1}) norm=({wx0/W*1000:.0f},{wy0/H*1000:.0f})-({wx1/W*1000:.0f},{wy1/H*1000:.0f})")

# --- smile curve: bright saturated pixels in lower band ---
# For each x column, find the brightest saturated pixel y in [0.74H, 0.92H]
curve = []
for x in range(int(W * 0.10), int(W * 0.92), 8):
    best = None
    for y in range(int(H * 0.74), int(H * 0.93)):
        r, g, b = px[x, y]
        h, s, v = hue_sat(r, g, b)
        if s > 0.45 and v > 0.55:
            score = v * s
            if best is None or score > best[0]:
                best = (score, y, h, r, g, b)
    if best:
        curve.append((x, best[1], best[2], best[3], best[4], best[5]))
print(f"smile samples: {len(curve)}")
for x, y, h, r, g, b in curve[::6]:
    print(f"  x={x/W*1000:.0f} y={y/H*1000:.0f} hue={h:.0f} rgb=({r},{g},{b})")
if curve:
    xs = [c[0] for c in curve]; ys = [c[1] for c in curve]
    print(f"smile extent: x {min(xs)/W*1000:.0f}..{max(xs)/W*1000:.0f}  y {min(ys)/H*1000:.0f}..{max(ys)/H*1000:.0f}")
    mid = curve[len(curve)//2]
    print(f"smile center: x={mid[0]/W*1000:.0f} y={mid[1]/H*1000:.0f}")
    print(f"smile left end: x={curve[0][0]/W*1000:.0f} y={curve[0][1]/H*1000:.0f}")
    print(f"smile right end: x={curve[-1][0]/W*1000:.0f} y={curve[-1][1]/H*1000:.0f}")

# --- background colors ---
for label, (x, y) in {"center": (W//2, H//2), "top": (W//2, int(H*0.08)),
                       "edge-left": (int(W*0.06), H//2), "bottom": (W//2, int(H*0.94))}.items():
    print(f"bg {label}: {px[x, y]}")