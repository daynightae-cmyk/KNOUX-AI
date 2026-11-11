# KNOUX AI — Phase 1 UI polish, pass 4

## Reality

- Windows workspace `D:\KNOUX-AI`, branch `main`, starting commit `5f9a14d` ("Refine KNOUX AI cinematic splash and shared visual materials"). No Git remote is configured.
- This pass builds on the previous agent's uncommitted polish. The seven modified Kotlin files were kept and extended; no reference image, earlier evidence folder, branch, or commit was removed.
- Phase 1 remains UI only. Every metric, file count, and "scan" on screen is a labelled design fixture. No scan, cleanup, privacy change, or battery optimization was executed.

## Implemented in this pass

- **Splash:** thicker magenta-to-cyan smile ribbon with a tighter glow; open orbit arcs plus stardust; more vivid "ai" gradient, K rim and bevel; denser ribbon spill into the flare.
- **Home:** dark-glass K inside the globe, extra globe strands and particles, richer ribbons, and a single-line donut label.
- **Shared components:** glass rim pinpoints instead of a continuous rim light; brighter active-tab treatment; primary CTA gains a darker weighted share (`.82 / 1.18`) so long labels such as "Clean Selected (2.72 GB)" stay on one line; action labels over 20 characters drop to 11sp and are capped at one line.
- **Clean Center / detail screens:** corrected selection count, sample totals, and item notes so the preview copy stays consistent with the selected rows.

## Verification

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug --offline --console=plain` | Exit 0, `BUILD SUCCESSFUL in 2m 49s`. [Log](build-pass4.log) |
| `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --offline --console=plain` | Exit 0, `BUILD SUCCESSFUL in 3m 45s`; 10/10 instrumented tests passed (0 skipped, 0 failed). [Log](verify-pass4.log) |
| `adb -s emulator-5554 install -r app-debug.apk` | `Success`. Device `kx_preview`, Android 8.0.0 / API 26, x86_64 with SwiftShader. Physical 1080×1920, capture viewport 1080×2340, 420 dpi. |
| `scripts/Capture-Phase1.ps1` (outer) | Exit 0; all ten entry screens started and their `screen-*` hierarchy was verified before capture. [Log](capture-outer-pass4.log) |
| `scripts/Capture-Phase1.ps1 -Interior` | Exit 0; all ten interior screens started and their `screen-*` hierarchy was verified before capture. [Log](capture-inner-pass4.log) |
| `adb shell am start -W -S -n com.knoux.ai/.MainActivity` | Cold launch `Status: ok`, 2,085 ms to Home. [Home after cold launch](cold-launch-home.png) |
| `adb logcat -d --pid=<pid> -s KnouxSplash:D` | Exactly one `Smile click started` event on the final APK. [Log](splash-pass4.log) |
| Splash freeze frames | 2,460 / 2,850 / 3,300 ms re-captured from the final APK. [2460](splash-2460.png) · [2850](splash-2850.png) · [3300](splash-3300.png) |
| Screen recording | [knoux-splash-final.mp4](knoux-splash-final.mp4), cold launch through Home. The emulator is software-rendered (SwiftShader), so frame rate is low; this is not a motion-quality measurement. |

APK: [KNOUX-AI-phase1-ui-polish-pass4-debug.apk](KNOUX-AI-phase1-ui-polish-pass4-debug.apk), 13,269,818 bytes.
SHA-256: `CC02DD1D610219399F0B082CD8E7CAF36211595F084D4E4DA80BA9234CD10CEA` ([file](APK.sha256)).

The hash above was re-checked after the verification build and matched the installed APK pulled from `/data/app/.../base.apk` before the final reinstall.

## Screen status

All captures are 1080×2340 runtime PNGs from the final APK. "Verified" means the route was foregrounded, its `screen-*` test tag was present in the live UI hierarchy, and the UI was not stale when captured.

| # | Screen | Capture | Status |
| --- | --- | --- | --- |
| — | Splash | `splash-2460/2850/3300.png` | Verified; close, orbits and bloom still simpler than the reference |
| 01 | Home | `01-home-runtime.png` | Verified; close, bottom card row sits behind the tab bar on this viewport |
| 02 | Privacy Sweep | `02-privacy-runtime.png` | Verified; close, list is taller so the fifth row needs a scroll |
| 03 | Duplicate Files | `03-duplicates-runtime.png` | Verified; close |
| 04 | Deep Scan | `04-deep-scan-runtime.png` | Verified; close |
| 05 | Reports & Insights | `05-reports-runtime.png` | Verified; close |
| 06 | App Residue | `06-residue-runtime.png` | Verified; close |
| 07 | App Manager | `07-app-manager-runtime.png` | Verified; close |
| 08 | Storage Analyzer | `08-storage-runtime.png` | Verified; close |
| 09 | Battery Saver | `09-battery-runtime.png` | Verified; close |
| 10 | AI Optimization | `10-ai-optimization-runtime.png` | Verified; close |
| I1 | Smart Clean Center | `01-clean-center-runtime.png` | Verified; close, CTA now one line, lower panels require a scroll |
| I2 | Duplicate Group Review | `02-duplicate-group-review-runtime.png` | Verified; close |
| I3 | Privacy Risk Inspector | `03-privacy-risk-inspector-runtime.png` | Verified; close, final row requires a scroll |
| I4 | Deep Scan Summary | `04-deep-scan-summary-runtime.png` | Verified; close |
| I5 | Performance Report Detail | `05-performance-report-detail-runtime.png` | Verified; close |
| I6 | Residue Item Review | `06-residue-item-review-runtime.png` | Verified; close |
| I7 | Installed Apps Review | `07-installed-apps-review-runtime.png` | Verified; close |
| I8 | Storage Breakdown | `08-storage-breakdown-runtime.png` | Verified; close |
| I9 | Battery Diagnostics | `09-battery-diagnostics-runtime.png` | Verified; close |
| I10 | AI Recommendations | `10-ai-recommendations-runtime.png` | Verified; close |

Reference-vs-runtime sheets for all 20 screens plus splash are in [comparisons/](comparisons/), produced by `D:\Temp\knoux-polish-sheets.py` after masking the handset frame.

## Remaining visual gaps

- Overall vertical density is still higher than the owner references: Home's last card row is partly behind the tab bar, and the interior screens need a scroll to reach the recommendation/gain panels that fit in the reference.
- Splash orbits, stardust distribution, and bloom shape are approximations of the reference, not pixel matches.
- Home ribbons are closer but still thinner than the reference artwork.
- Motion smoothness and the splash click cannot be judged on the software-rendered emulator; a physical-phone motion and audio check is still pending.
- Arabic/RTL is covered by the instrumented tab-order test and the language switch, but this pass did not capture a new full Arabic screenshot set.

## Evidence and disk

- New evidence set: 95,777,111 bytes total, including the APK, 20 comparison sheets, 20+3 captures, logs, and the recording.
- D: free space at handoff: 4,761,968,640 bytes. No cleanup was performed; earlier build outputs and prior evidence folders were left in place.
- The two prior evidence folders (`knoux-ai-visual-polish`, `knoux-ai-refinement-20261111`) and the imported references remain untouched and untracked by this pass.

Phase 1 stops here for owner visual approval.
