# SCREEN MAP — routes vs references (01–20 + App Manager overview)

All captures: `com.knoux.ai/.MainActivity` on `emulator-5554` (API 26), debug build,
English, portrait, logical 1080×2340 override / physical 1080×1920, 420 dpi.
Every route was verified via its Compose tag (`screen-<route>`) in the UI Automator
hierarchy **before** the frame was taken (see `capture-outer.log`, `capture-interior.log`).
PNGs contain the real system bars; comparisons exclude reference handset mockup bars.

Debug-only deterministic entry points (no production behavior depends on them):
`--es knoux.preview.route <route>`, `--ez knoux.preview.still true` (freezes decorative
motion), `--el knoux.preview.splash.time <ms>` (freeze-frame splash).

## Outer screens (evidence: `ui/`, contact sheet `gallery-outer.png`)

| Ref | Screen | Route | Runtime evidence | Comparison | Status | Data honesty |
|---|---|---|---|---|---|---|
| 02 | Home / AI Smart Clean | `home` | `ui/01-home-runtime.png` | `ui/comparison-01.png` | Implemented & verified (runtime capture + navigation tests) | All counters labeled sample/estimated; hero CTA opens preview flow only |
| 03 | Privacy Sweep | `privacy` | `ui/02-privacy-runtime.png` | `ui/comparison-02.png` | Implemented & verified | Sample risk items; opens inner 13 |
| 04 | Duplicate Files Finder | `duplicates` | `ui/03-duplicates-runtime.png` | `ui/comparison-03.png` | Implemented & verified | Sample groups; opens inner 12 |
| 05 | Deep Scan | `deep-scan` | `ui/04-deep-scan-runtime.png` | `ui/comparison-04.png` | Implemented & verified | Pause/resume toggles preview state only; "No live scan" disclosure shown; opens inner 14 |
| 06 | Reports & Insights | `reports` | `ui/05-reports-runtime.png` | `ui/comparison-05.png` | Implemented & verified | Sample charts; opens inner 15 |
| 07 | App Residue Cleanup | `residue` | `ui/06-residue-runtime.png` | `ui/comparison-06.png` | Implemented & verified | Sample candidates; opens inner 16 |
| —  | App Manager overview (predecessor baseline, polished) | `app-manager` | `ui/07-app-manager-runtime.png` | `ui/comparison-07.png` | Implemented & verified | Sample inventory; opens inner 17 |
| 09 | Storage Analyzer | `storage` | `ui/08-storage-runtime.png` | `ui/comparison-08.png` | Implemented & verified | Sample categories; opens inner 18 |
| 10 | Battery Saver | `battery` | `ui/09-battery-runtime.png` | `ui/comparison-09.png` | Implemented & verified | OS-limited signals labeled; opens inner 19 |
| 11 | AI Optimization | `ai-optimization` | `ui/10-ai-optimization-runtime.png` | `ui/comparison-10.png` | Implemented & verified | Estimated outcomes labeled; opens inner 20 |

Scrolled proof from the third pass (same layout code, retained): `qa-evidence/ui/02-privacy-scrolled-runtime.png` … `10-ai-optimization-scrolled-runtime.png`.

## Inner screens (evidence: `interior/`, contact sheet `gallery-inner.png`)

| Ref | Screen | Route | Runtime evidence | Comparison | Status |
|---|---|---|---|---|---|
| 12 | Smart Clean Center | `clean-center` | `interior/01-clean-center-runtime.png` | `interior/comparison-01.png` | Implemented & verified; preview sheet + confirm flow recorded in the walkthrough video |
| 13 | Duplicate Group Review | `duplicate-group-review` | `interior/02-duplicate-group-review-runtime.png` | `interior/comparison-02.png` | Implemented & verified; keep-one invariant enforced in the preview (one item stays selected) |
| 14 | Privacy Risk Inspector | `privacy-risk-inspector` | `interior/03-privacy-risk-inspector-runtime.png` (+ scrolled) | `interior/comparison-03.png` | Implemented & verified; rows are sample items, no fake "tracker blocked" claims |
| 15 | Deep Scan Summary | `deep-scan-summary` | `interior/04-deep-scan-summary-runtime.png` | `interior/comparison-04.png` | Implemented & verified; sample timeline |
| 16 | Performance Report Detail | `performance-report-detail` | `interior/05-performance-report-detail-runtime.png` | `interior/comparison-05.png` | Implemented & verified; sample history |
| 17 | Residue Item Review | `residue-item-review` | `interior/06-residue-item-review-runtime.png` (+ scrolled) | `interior/comparison-06.png` | Implemented & verified; selection total updates; confirm performs nothing |
| 18 | Installed Apps Review | `installed-apps-review` | `interior/07-installed-apps-review-runtime.png` (+ scrolled) | `interior/comparison-07.png` | Implemented & verified; sample app list |
| 19 | Storage Breakdown | `storage-breakdown` | `interior/08-storage-breakdown-runtime.png` | `interior/comparison-08.png` | Implemented & verified; category drill-down |
| 20 | Battery Diagnostics | `battery-diagnostics` | `interior/09-battery-diagnostics-runtime.png` | `interior/comparison-09.png` | Implemented & verified; unavailable values labeled |
| 21 | AI Recommendations | `ai-recommendations` | `interior/10-ai-recommendations-runtime.png` | `interior/comparison-10.png` | Implemented & verified; estimated deltas labeled |

Scrolled proof for tall inner lists: `interior/privacy-risk-inspector-scrolled-runtime.png`,
`interior/residue-item-review-scrolled-runtime.png`,
`interior/installed-apps-review-scrolled-runtime.png` (captured on the fourth-pass build
before the splash clip fix; these screens do not draw the splash, so the captures remain valid).

## Reference 01 — Splash (cinematic reveal)

Not a static screen; implemented as a time-based Canvas animation. Evidence lives in
`splash/` (storyboard frames, cold-launch video, click log) and is indexed in
`SPLASH_STORYBOARD.md`. Status: **Implemented & verified** (sequence frames + one-click
log proof + instrumentation splash tests).

## Changed components (this pass + carried fourth-pass work)

| File | Change |
|---|---|
| `app/src/main/java/com/knoux/ai/HomeScreen.kt` | Home compaction so all six service cards clear the bottom nav at the capture size; 48 dp targets kept |
| `app/src/main/java/com/knoux/ai/KnouxComponents.kt` | Stronger glass/panel colors, vivid donut/CTA gradients (anti-washout pass) |
| `app/src/main/java/com/knoux/ai/NeonArt.kt` | Brand K, hero orb and `NeonHero` sizing/color tuning |
| `app/src/main/java/com/knoux/ai/KnouxSplash.kt` | Wordmark clip fix (no sheared faux-italic ink); timing/geometry as storyboarded |
| `app/src/main/java/com/knoux/ai/MainActivity.kt` | Splash gating, Home preparation, debug route extras, `InfoSheet` |
| `app/src/main/java/com/knoux/ai/DetailPreview.kt` / `DetailPreviewData.kt` | All ten inner reference screens (carried uncommitted work, now committed) |
| `app/src/main/java/com/knoux/ai/SplashAtmosphere.kt` / `SplashChime.kt` | Splash ambience + one-shot click honoring silent mode |
| `app/src/main/res/raw/knoux_smile_click.wav`, `res/drawable-nodpi/*` | Splash audio sample + sample image assets |

No API, database, service, permission or device-operation code exists in the app.
