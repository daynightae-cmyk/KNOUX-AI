# VISUAL_COMPARISONS — index

Device for every runtime PNG: `emulator-5554`, Android 8.0 / API 26, physical
1080×1920, logical override 1080×2340, 420 dpi, SwiftShader. Comparisons were produced
by `scripts/Compare-Preview.py`, which masks the runtime system bars (top 52 px, bottom
104 px) and sides (97 px) and excludes the reference phone-handset mockup frame.

## Side-by-side strips (reference | runtime)

- Outer 01–10: `../ui/comparison-01.png` … `../ui/comparison-10.png`
  (references: `design/reference-20261111/*`, App Manager uses its original
  `design/reference/07_App_Manager.png` baseline)
- Inner 11–20: `../interior/comparison-01.png` … `../interior/comparison-10.png`
  (references: `design/reference-inner-20261111/*`)
- Splash: `../splash/reference-comparison.png`
  (reference `design/reference-20261111/02_Splash.png` vs runtime `splash-3000.png`)

## Contact sheets

- `../gallery-outer.png` — the ten outer runtime screens
- `../gallery-inner.png` — the ten inner runtime screens
- `../splash/sequence.png` — ten-frame splash storyboard incl. landing on Home

## Metric caveat

`compare-outer.log` / `compare-inner.log` report an overlapping mean RGB error
(outer 47.5–56.6 /255, inner 51.3–58.5 /255). This is a coarse alignment diagnostic
only: the reference images carry fictional statistics, different hero renderings and
their own device chrome, so equal pixel values are neither expected nor required.
Manual findings per screen are in `../TEST_RESULTS.md` §4 and the third-pass audit at
`../../ui/visual-audit.md`.
