# KNOUX AI cinematic splash — evidence

Source of truth: `design/brand/knoux-logo-owner.png` (owner-supplied, 1125×1125).
Implementation: `app/src/main/java/com/knoux/ai/KnouxSplash.kt`, wired from
`MainActivity.kt`; tests in `app/src/androidTest/java/com/knoux/ai/SplashSequenceTest.kt`.

The mark, wordmark and smile are vector paths, not the raster logo. No badge,
rectangular background or image halo is drawn.

## Choreography implemented

| Scene | Window (ms) | What is drawn |
|---|---|---|
| A — Silence | 0–300 | Midnight-violet background, soft radial illumination, empty centre |
| B — K forms | 300–1260 | Upper violet plane, purple→blue diagonal blade, cyan lower leg, then a restrained light sweep |
| C — Wordmark | 1260–2070 | `knoux ai`, letter by letter, 80 ms stagger, white `knoux`, pink/violet `ai` |
| D — Neon smile | 2080–2800 | Bézier curve drawn left→right, violet → purple → blue → cyan, soft bloom |
| E — Hold / entry | 2800–3480 | Completed signature holds, then fades into the existing Home screen |

Reduced motion: 750 ms static brand (fade begins at 450 ms). A 2.5 s watchdog
guarantees the splash always releases even if the animation stalls. Returning
from background does not replay it (`SplashGate` + `savedInstanceState`).
The Home scaffold's decorative motion is paused while the opaque splash covers
it, so the brand animation owns the frame budget.

## Evidence

| File | What it shows |
|---|---|
| `splash-logo-fidelity.png` | Owner logo (left) vs Android final frame (right) |
| `splash-sequence-contact.png` | Captured scenes at 150 / 500 / 850 / 1150 / 1700 / 2400 / 3000 ms |
| `splash-final-frame.png`, `splash-t3000.png` | Final composition |
| `splash-motion-burst.png` | Progressive scenes plus the Home transition |
| `splash-cold-launch.mp4` | 14 s cold-launch recording (full sequence + Home) |
| `splash-cold-launch-strip.png` | 2 fps contact sheet of that recording |
| `splash-clean-boot.mp4` | 12 s recording after a clean emulator boot of the final build |
| `splash-clean-boot-detail.png` | 4 fps contact sheet around the transition |
| `reduced-motion-static.png`, `reduced-motion-after.png` | Reduced-motion brand and its prompt finish |
| `resume-no-replay.png` | App resumed from background without replaying the splash |

## Runtime caveat (honest)

Device: `emulator-5554`, AVD `kx_preview`, Android 8.0 / API 26, x86_64,
1080×1920 at 420 dpi, 2 vCPU, 2 GB RAM, **software renderer (SwiftShader)**.

`am start -W` measured 3481 ms to first displayed frame. Choreographer reported
skipped frames during launch, and the time-based splash scenes can jump between
rendered frames on this renderer (≈2 fps in the contact sheets). The sequence,
ordering, final composition, reduced-motion path, watchdog and Home transition
are functionally verified; 60 fps motion smoothness is **not** claimed on this
software-rendered emulator. Freeze frames were used for deterministic scene
capture. Real-device motion review is still recommended.

## Known visual differences from the owner logo

- The Android stage scales the K slightly larger relative to the wordmark.
- The background halo is softer and wider than the logo's tighter glow.
- The neon smile sits a little lower and spans slightly wider.
- The wordmark uses Outfit ExtraBold rather than the logo's custom outlines.

Geometry, gradient directions, lowercase spelling, and the violet→cyan smile
transition follow the supplied logo. Owner visual approval remains pending.
