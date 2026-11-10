"""Build a splash sequence contact sheet and a logo-vs-render fidelity comparison."""
from PIL import Image, ImageDraw, ImageFont
import os

BASE = r"D:\KNOUX-AI\qa-evidence\splash"
LOGO = r"D:\KNOUX-AI\design\brand\knoux-logo-owner.png"

def load(name):
    return Image.open(os.path.join(BASE, name)).convert("RGB")

# --- contact sheet of the choreography ---
times = [150, 500, 850, 1150, 1700, 2400, 3000]
labels = ["A 150ms\nsilence", "B 500ms\nupper K", "B 850ms\ndiagonal", "B 1150ms\ncyan joins",
          "C 1700ms\nwordmark", "D 2400ms\nsmile draws", "E 3000ms\nhold"]
thumb_w = 300
cols = 4
rows = 2
pad = 12
label_h = 44
cell_h = int(thumb_w * 1920 / 1080) + label_h
sheet = Image.new("RGB", (cols * (thumb_w + pad) + pad, rows * (cell_h + pad) + pad), (10, 8, 24))
d = ImageDraw.Draw(sheet)
for i, (t, lab) in enumerate(zip(times, labels)):
    im = load(f"splash-t{t}.png") if t != 3000 else load("splash-final-frame.png")
    im = im.resize((thumb_w, int(thumb_w * 1920 / 1080)))
    x = pad + (i % cols) * (thumb_w + pad)
    y = pad + (i // cols) * (cell_h + pad)
    sheet.paste(im, (x, y))
    d.text((x + 4, y + im.height + 6), lab, fill=(220, 215, 245))
sheet.save(os.path.join(BASE, "splash-sequence-contact.png"))
print("contact sheet saved")

# --- fidelity comparison: logo content vs rendered content ---
logo = Image.open(LOGO).convert("RGB")
# logo content bbox (K+wordmark+smile) approx x150..950, y265..965 in 1125
logo_crop = logo.crop((120, 250, 980, 980))
render = load("splash-final-frame.png")
# render content: scale s = 1080*0.92/1000 = 0.9936; design block centered.
# content x ~ (135..843)*s + ox ; ox=(1080-993.6)/2=43.2 ; y centered in 1920
s = 1080 * 0.92 / 1000
ox = (1080 - 1000 * s) / 2
oy = (1920 - 1000 * s) / 2
x0 = int(120 * s + ox); x1 = int(880 * s + ox)
y0 = int(235 * s + oy); y1 = int(875 * s + oy)
render_crop = render.crop((x0, y0, x1, y1))
# normalize both to same width
W = 520
logo_n = logo_crop.resize((W, int(logo_crop.height * W / logo_crop.width)))
render_n = render_crop.resize((W, int(render_crop.height * W / render_crop.width)))
H = max(logo_n.height, render_n.height)
comp = Image.new("RGB", (W * 2 + 30, H + 50), (10, 8, 24))
comp.paste(logo_n, (10, 40))
comp.paste(render_n, (W + 20, 40))
dc = ImageDraw.Draw(comp)
dc.text((10, 12), "OWNER LOGO (source of truth)", fill=(230, 225, 255))
dc.text((W + 20, 12), "ANDROID SPLASH final frame", fill=(230, 225, 255))
comp.save(os.path.join(BASE, "splash-logo-fidelity.png"))
print("fidelity comparison saved", comp.size)