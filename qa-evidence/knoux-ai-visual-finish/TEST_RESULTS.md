# TEST RESULTS — fourth visual pass (final verification on the shipped APK)

Final commit containing this pass: `ff9da8e454d04ad41a7a5df415d35674007f9df6` on local
`main` (parent `68670c3`, no remote, nothing pushed).

Device: `emulator-5554`, AVD `kx_preview`, Android 8.0 / API 26, x86_64,
SwiftShader software GPU (headless boot), physical 1080×1920, logical override
1080×2340, 420 dpi. App `com.knoux.ai` 0.1.0-preview debug, min 26 / target 36.

## 1. Build — exact commands and exit codes

| # | Command | Result |
|---|---|---|
| 1 | `Set-Location 'D:\KNOUX-AI'; .\gradlew.bat --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain` | **exit 0** — `BUILD SUCCESSFUL in 3m 37s`, 84 tasks (14 executed / 70 up-to-date) |
| 2 | `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest --console=plain` | **exit 0** — `Starting 10 tests on kx_preview(AVD) - 8.0.0` … `Finished 10 tests`, `BUILD SUCCESSFUL in 2m 24s` |

- Unit tests: 3/3 passed (`PreviewFixturesTest`), 0 failures.
- Instrumentation: **10/10 passed**, 0 failures —
  `PreviewNavigationTest` (7): tabsReachAllTenScreensAndDetailBackReturnsToTheIndex,
  scanPauseAndResumeOnlyChangePreviewStateAndSurviveRecreation,
  residueSelectionUpdatesTotalAndConfirmationDoesNotClean,
  arabicSwitchMirrorsTabOrderAndPreservesNavigation,
  everyRemainingPrimaryActionOpensAReversiblePreview,
  cleanCenterSelectionChangesThePreviewTotalAndDoesNotClean,
  outerReviewOpensAnInnerPageAndBackReturnsToItsOrigin;
  `SplashSequenceTest` (3): liveSmileCuesOnceAtCompletionThenFinishes,
  reducedMotionShowsStaticBrandAndFinishesQuickly,
  frozenDebugFrameStaysVisibleForCapture.
  Note: `connectedDebugAndroidTest` uninstalls both APKs when it finishes; the app was
  reinstalled with `adb install -r` before the capture pass (recorded exit 0, `Success`).
- Lint: **0 errors, 11 warnings** (8 dependency-update notices, 2 Compose Modifier
  parameter-order notices, 1 `allowBackup`/`dataExtractionRules` notice) —
  `app/build/reports/lint-results-debug.txt`.
- These runs happened **after** the splash wordmark clip fix, i.e. they verify the exact
  source that produced the shipped APK below.

## 2. Artifact

| Item | Value |
|---|---|
| APK | `qa-evidence/knoux-ai-visual-finish/KNOUX-AI-phase1-visual-finish-debug.apk` |
| Bytes | 13,269,818 |
| SHA-256 | `343FC50947A5396E7AD3BE00DF00106ED1C5994724207A0E3BE33F7B8274FEF2` |
| Equality check | `Get-FileHash` of `app/build/outputs/apk/debug/app-debug.apk` vs the evidence copy → **identical** |
| Install | `adb -s emulator-5554 install -r <apk>` → `Success` (exit 0) |

## 3. Runtime capture QA (all on the APK above)

| Step | Command | Result |
|---|---|---|
| Viewport | `adb shell wm size 1080x2340` | override active; `wm density` = 420 |
| Outer screens | `scripts\Capture-Phase1.ps1 -Serial emulator-5554 -Destination qa-evidence\knoux-ai-visual-finish\ui` | **exit 0**, 10/10; each route verified via `resource-id="screen-<route>"` before capture; per-file SHA-256 in `capture-outer.log` |
| Inner screens | `scripts\Capture-Phase1.ps1 -Interior -Serial emulator-5554 -Destination ...\interior` | **exit 0**, 10/10, same verification; SHA-256 in `capture-interior.log` |
| Outer comparisons | `python scripts\Compare-Preview.py --references outer --captures-dir qa-evidence\knoux-ai-visual-finish\ui --runtime-bars 52 104 --runtime-sides 97 97` | exit 0, `comparison-01..10.png` + metrics in `compare-outer.log` |
| Inner comparisons | same with `--references inner --captures-dir ...\interior` | exit 0, `comparison-01..10.png` + metrics in `compare-inner.log` |
| Splash re-record | `scripts\Record-FinalSplash.ps1 -Qa <folder>` | exit 0; cold launch recorded, `Smile click started` count = **1**; 9 freeze frames + landing PNG; log `splash\capture-splash.log` |
| Walkthrough | `scripts\Record-Walkthrough.ps1` | exit 0; `videos\walkthrough-clean-preview.mp4` (49.9 s) — see videos/README.md |
| Contact sheets | `python scripts\Build-EvidenceSheets.py` | exit 0; `gallery-outer.png`, `gallery-inner.png`, `splash\sequence.png`, `splash\reference-comparison.png` |

Comparison metric (diagnostic only, **not** an acceptance score): overlapping mean RGB
error after mock-bar masking — outer 47.5–56.6 /255, inner 51.3–58.5 /255. The values
match the third pass within noise because screen content did not change in this pass;
the references keep their fictional stats and the mock handset frame is excluded.

## 4. Known visual differences (honest list)

1. Inner detail rows are simplified and taller than the reference mockups (readability +
   48 dp targets), so the last row of several inner screens needs a short scroll
   (scrolled proof provided for the three tallest).
2. Privacy Risk Inspector shows ~3 of 5 sample rows before scrolling.
3. Hero artwork (orb, radar, shield, cylinder, chip) is a vector approximation — less
   filament detail than the reference renders; brand K follows the owner logo.
4. Reference screens carry fictional numbers; the app shows the same values but labels
   them as sample/estimated design data (e.g. "Sample scan complete", "No action
   performed — UI preview only.").
5. Splash lockup slightly larger than the reference crop; smile thinner/subtler; see
   SPLASH_STORYBOARD.md deltas.
6. Home fits all six service cards at the capture size now; at other font scales /
   screen sizes content scrolls (by design, nothing is clipped).
7. SwiftShader: animation smoothness not claimed; frames of time-based scenes can step.

## 5. Verification gaps (stated, not hidden)

- No physical-device run: smoothness, audio latency and haptics remain emulator-observed.
- The walkthrough navigates by UI Automator tags/visible text; sheet buttons are located
  by text because Material3's `ModalBottomSheet` popup window does not expose Compose
  debug resource-ids (main-window tags do work).
- Reduced-motion and resume-no-replay behavior is covered by instrumentation and by the
  third-pass splash evidence (`qa-evidence/splash/`), not by a new video in this folder.
- Arabic RTL is covered by `arabicSwitchMirrorsTabOrderAndPreservesNavigation`; the
  captures in this folder are English-only.
