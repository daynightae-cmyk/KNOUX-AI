# KNOUX AI — Phase 1 UI polish, pass 5

## Reality

- Windows workspace `D:\KNOUX-AI`, branch `main`, starting commit `c9cb7f37806f25f71095b86f6c40abd731a50e97` ("Record final Phase 1 polish disk measurements"). No Git remote is configured.
- This pass keeps all modified and untracked work from the earlier polish passes. No commit, branch, worktree, stash, reference image or prior evidence folder was removed.
- Phase 1 is still UI only. Every score, file count, GB and "scan" is a labelled design fixture. No scan, cleanup, privacy change, battery optimization, privileged permission or file deletion was performed. No Room, Supabase or real service code was added.

## What changed since pass 4

- **Palette / depth:** sharpened the violet–magenta–cyan light palette, brighter bloom, saturated glass panels, visible rim highlights, richer atmosphere, globe and pedestal shading in `PremiumArt.kt`; splash glow and ribbons deepened.
- **Motion:** 30 s hero globe phase with bob/squash, 19 s counter-rotating orbit layers, breathing scene/bloom, animated glints; CTA press/breath/shimmer and animated chevrons; animated tab pill/icon, donut sweep with ticks and glint, animated gradient bars, scan pulse, selection pop and gated page transitions.
- **Layout / controls (audited):** all six Home tiles fit at rest; storage category tiles use `selectable`/`selectableGroup`; inert row chevrons removed; duplicate thumbnails open the group preview; tint-aware mint/gold pills; dedicated disabled-CTA surface; both language choices enabled with selected semantics; MSA Arabic copy.
- **Donut captions:** `Device Score`, `Privacy Score`, `Match Confidence`, `Scan Progress`, `Used (128 GB)` and `Battery Health` no longer clip; captions wrap to two auto-fitted, centred lines.
- **Test correction (not a behaviour change):** back navigation intentionally restores the origin scroll position, so `outerReviewOpensAnInnerPageAndBackReturnsToItsOrigin` now scrolls the origin heading into view before asserting. Navigation behaviour is unchanged.
- Full per-control and per-service-block audit: [BUTTON-SERVICE-AUDIT.md](BUTTON-SERVICE-AUDIT.md).

## Verification

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --console=plain --no-daemon` | Exit 0, `BUILD SUCCESSFUL in 4m 27s`. [Log](verify-pass5b.log) |
| `.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain --no-daemon` | Exit 0, `BUILD SUCCESSFUL in 2m 32s`; 10/10 instrumented tests passed, 0 failed, 0 skipped. [Log](connected-test-pass5c.log) · [JUnit XML](connected-test-results.xml) |
| First connected run on the same code | The over-strict origin assertion failed; test was corrected, not the app. Kept for transparency: [log](connected-test-pass5.log), [second run](connected-test-pass5b.log). |
| `adb -s emulator-5554 install -r app-debug.apk` | `Success`. Installed `base.apk` SHA-256 matched the delivered APK exactly. |
| `scripts/Capture-Phase1.ps1 -Destination qa-evidence/ui-polish-20261111-pass5` | Exit 0; all ten outer routes started, `screen-*` hierarchy verified, capture hash logged per screen. [Log](capture-outer-pass5c.log) |
| `scripts/Capture-Phase1.ps1 -Interior -Destination qa-evidence/ui-polish-20261111-pass5` | Exit 0; all ten interior routes started, `screen-*` hierarchy verified, capture hash logged per screen. [Log](capture-inner-pass5c.log) |
| Scrolled Storage Breakdown capture | Exit 0 (`SCROLLED_OK`); uiautomator confirmed the swipe reached "All Categories". [Log](capture-scrolled-pass5c.log) · [Capture](08-storage-breakdown-scrolled-runtime.png) |
| `scripts/Record-FinalSplash.ps1 -Qa qa-evidence/ui-polish-20261111-pass5` | Exit 0; 9 freeze frames + landing frame + cold-launch MP4. `KnouxSplash: Smile click started` = 1, `Cold-launch SoundPool starts: 1`. [Log](splash-pass5c.log) |
| `python scripts/Build-ComparisonSheets.py --qa qa-evidence/ui-polish-20261111-pass5` | Exit 0; 20 screen sheets + splash sheet, mockup handset frame and emulator bars masked. [comparisons/](comparisons/) |
| Final APK hash | `DA24896C97FDC23DCF4F7784944A52D58946DCF8AE3524B8F018E9BF3CB0BC5A`, 11,098,940 bytes. [APK](KNOUX-AI-phase1-ui-polish-pass5-debug.apk) · [SHA file](APK.sha256) |

Device: AVD `kx_preview`, model `Android SDK built for x86_64`, Android 8.0.0 / API 26, x86_64 with SwiftShader. Physical display 1080×1920 @ 420 dpi; captures use a 1080×2340 logical override, which screencap letterboxes to an 886×1920 app area inside 1080×1920. The display was reset to 1080×1920 after capture.

## Screen status

All runtime PNGs are captures of the final APK. "Verified" means the route was foregrounded, its `screen-*` tag was present in the live hierarchy, and the capture hash was logged.

| # | Screen | Capture | Status |
| --- | --- | --- | --- |
| — | Splash | `splash/splash-*.png`, `cold-launch.mp4` | Verified; 9-frame cold launch + click; K/wordmark lockup is a hand-traced approximation, and native stills include system bars |
| 01 | Home | `01-home-runtime.png` | Verified; all six service tiles and the metric panel fit above the tab bar; globe/ribbon art is an approximation |
| 02 | Privacy Sweep | `02-privacy-runtime.png` | Verified; 4 of 5 privacy rows at rest, fifth scroll-reachable; header uses the design-preview chip instead of the reference shield glyph |
| 03 | Duplicate Files | `03-duplicates-runtime.png` | Verified; four groups plus part of the fifth at rest; remainder scroll-reachable |
| 04 | Deep Scan | `04-deep-scan-runtime.png` | Verified; demo progress labelled "no live scan"; Continue is disabled while running, Pause enabled |
| 05 | Reports & Insights | `05-reports-runtime.png` | Verified; five of six report rows at rest; scroll-reachable |
| 06 | App Residue | `06-residue-runtime.png` | Verified; four of five rows at rest; select-all total is live |
| 07 | App Manager | `07-app-manager-runtime.png` | Verified; all five groups visible; `App Health` caption now fits |
| 08 | Storage Analyzer | `08-storage-runtime.png` | Verified; all six categories visible; review CTA reads "View" (shortened from "View Breakdown" for width); `Used (128 GB)` caption fits |
| 09 | Battery Saver | `09-battery-runtime.png` | Verified; four of five action rows at rest; scroll-reachable |
| 10 | AI Optimization | `10-ai-optimization-runtime.png` | Verified; four of five tool rows at rest; scroll-reachable |
| I1 | Smart Clean Center | `01-clean-center-runtime.png` | Verified; selection total corrected to 2.72 GB for the four checked rows (the reference's 2.86 GB was the unfiltered total); recommendation panel needs a scroll |
| I2 | Duplicate Group Review | `02-duplicate-group-review-runtime.png` | Verified; thumbnails open the group preview, Best badge kept, two groups fully visible at rest |
| I3 | Privacy Risk Inspector | `03-privacy-risk-inspector-runtime.png` | Verified; three of five risks at rest; horizontal category chips clip by design; reference Block switches are not reproduced (rows show permission counts + selection) |
| I4 | Deep Scan Summary | `04-deep-scan-summary-runtime.png` | Verified; three of five findings at rest, suggested actions below the fold; fixture shows 5 findings vs the reference's 7 |
| I5 | Performance Report Detail | `05-performance-report-detail-runtime.png` | Verified; four trend cards, timeline; PDF CTA honestly reads "Export PDF Preview" |
| I6 | Residue Item Review | `06-residue-item-review-runtime.png` | Verified; three of five rows at rest; per-item media chips simplified; sticky bar no longer repeats a selection total |
| I7 | Installed Apps Review | `07-installed-apps-review-runtime.png` | Verified; three of five apps at rest; Select All is correctly unchecked with 3 selected; "Manage" shortened from "Manage Selected (3)" |
| I8 | Storage Breakdown | `08-storage-breakdown-runtime.png` + scrolled | Verified at rest and after scroll; Apps Breakdown and All Categories reachable; category tiles expose `selected` semantics; donut caption fits |
| I9 | Battery Diagnostics | `09-battery-diagnostics-runtime.png` | Verified; three of five suggestions at rest; scroll-reachable |
| I10 | AI Recommendations | `10-ai-recommendations-runtime.png` | Verified; four-item plan fits; reference's "Safe Actions Ranking" / "Why It's Safe?" panels are not included |

Reference-vs-runtime sheets for all 20 screens plus splash are in [comparisons/](comparisons/), produced after masking the handset frame. Sheets are diagnostic, not pixel-equality scores.

## Remaining visual gaps (honest)

- The 16:9 emulator viewport is shorter than the 19.5:9 references, so on six list screens the last row sits behind the sticky action bar at rest. Everything is scroll-reachable and covered by the scrolled capture; at-rest parity is not claimed.
- Several reference-only secondary panels are intentionally not reproduced: Privacy Risk Inspector Block switches, Clean Center's three-chip strip, Deep Scan Summary's suggested-action trio, Residue Review's per-item media chips, and AI Recommendations' safe-action ranking panels.
- Splash K face, smile width, orbit arcs and bloom remain approximations of the owner logo, not pixel matches. Native splash stills show Android system bars.
- The emulator is software-rendered (SwiftShader); real-device frame pacing, splash audio and haptics are still unverified.
- Arabic/RTL is covered by the instrumented tab-order/language test and the language-switch state, but this pass did not capture a new full Arabic screenshot set.
- Photographs and app icons are bundled sample assets, not user data.
- Donut captions now wrap to two centred lines where needed, which is a small visual departure from the one-line references but removes clipping.

## Evidence and disk

- Evidence set: **101,469,290 bytes / 71 files**, including the 11,098,940-byte APK, 20 screen captures, 1 scrolled capture, 9 splash frames, landing frame, MP4, 21 comparison sheets, logs and the audit.
- D: free space immediately before cleanup: **6,317,645,824 bytes**. Immediately after cleanup: **6,397,992,960 bytes** (+80,347,136 bytes recovered; the emulator and NTFS keep moving this figure).
- Removed only proven disposable outputs: `app\build` (63,781,839 B / 974 files), `D:\Temp\knoux-scrolled-pass5` (12,308,833 B / 6 files) and three ad-hoc crops (3,033,199 B). `.gradle`, prior evidence folders, reference material and all untracked work from other passes were left in place.
- The delivered APK was copied to evidence and verified byte-identical to the installed `base.apk` before `app\build` was removed.

Phase 1 stops here for owner visual acceptance. Real services, Room/Supabase, privileged permissions and file operations remain unauthorized.
