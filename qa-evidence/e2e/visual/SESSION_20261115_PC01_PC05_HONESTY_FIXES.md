# KNOUX AI — Visual honesty pass, 2026-11-15

Scope: PC01–PC05 instruments plus the Home/Clean/Deep Scan presentation defects found in
the real on-device captures. No owner-visible design was replaced and no uncommitted work
from another agent was reverted.

Source state: branch `main`, HEAD `3f69b00dfa232b90c7f9acc8f990fc3122f3521b` (one commit
ahead of `origin/main`). The working tree already contained 16 modified tracked files and
untracked instruments/tests owned by earlier passes; they are all preserved.

Test target: the owner-authorized handset **SM-A025F, API 31, 720×1600 px @ 280 dpi**,
reached through `KNOUX_TEST_DEVICE_SERIAL`. The serial itself is not stored here.

## Build and test evidence

| Command | Result |
| --- | --- |
| `gradlew :app:compileDebugKotlin` | PASS |
| `gradlew :app:testDebugUnitTest :app:assembleDebug` | BUILD SUCCESSFUL (JVM suite green) |
| `app/build/outputs/apk/debug/app-debug.apk` | 21,312,604 bytes, SHA-256 `46C0C820BD1C69036F92E81DDB621921705A17CE908A85D5689DA0673386C6B5` |

A passing build is not a visual claim. Every statement below is backed by a screenshot in
`D:\Temp\opencode\knoux-visual-20261115` (device-side captures, `adb shell screencap`).

## Capture method (a defect found in the method itself)

The first capture loop waited 3 s per route and produced 14 near-black frames. Logcat for
that launch shows why: `Choreographer: Skipped 297 frames`, `Davey! duration=5036ms`, and
the first `reportDrawFinished` at ~7 s after `am start`. The app was not broken — the
capture was early. `tools/qa/capture_visual_states.ps1` now settles **18 s** per cold start
and rejects nothing below 100 KB as "not drawn".

## Fixes in this pass

1. **Vault seal honesty** — `PrivacyReal.kt` now separates the stored preference
   (`secureWindowRequested`) from the flag actually read back off the window
   (`actualSecureWindow`). `VaultRadarInstrument` only closes its seal on the second value.
   The captured Privacy screen shows the seal **open** with "NOT MEASURED YET", which is the
   truth on a fresh install.
2. **Mirror** — null hero renders "not measured" instead of `0`; `FOGGED`/`PARTIAL` draw a
   hollow dashed orb instead of the red failure tick; the per-frame `mutableStateOf` tension
   was replaced with a remembered `animateFloatAsState`.
3. **Deep Scan** — `DeepScanController.frameFor(outcome)` no longer reports `progress = 1f`
   for partial/failed passes; it reports the real measured-module fraction, and `null` where
   nothing was measured. The fog chip in `DeepScanReal` changed from `5 ✕` (reads as
   failure) to `5 ◌` with a real accessibility description.
4. **Terrain** — an unmeasured `StorageSnapshot` now draws a fog field with a dashed horizon
   and no coastline/numerals instead of a measured-looking landscape.
5. **Strata** — an unmeasured pass draws a shorter well with a dashed horizon and fog
   instead of four soil bands that read as "measured, nothing found".
6. **Tide** — the unmeasured sea is drawn in fog tones (bright water only appears once a
   reading exists), and selection lift is now per group (`selectedCounts`) so selecting one
   group never raises another group's buoys.
7. **Routes** — `Page.CLEAN_CENTER` opens the real Smart Clean surface (review queue, Trash,
   Restore) and `Page.REPORT_DETAIL` opens the real measured history; neither falls through
   to `EmptyServiceScreen` any more.
8. **Home** — the gauge no longer prints a clipped "Device" inside the ring (the card already
   says "Device storage"), and service tiles allow three lines, so "App Residue Cleanup" is
   no longer truncated to "App Residue …".
9. **Screenshot protection switch** — the debug capture bypass moved out of production
   resources: `src/main/res` keeps `knoux_debug_no_secure = false`, and only the `debug`
   source set overlays `true`. A release build cannot lose FLAG_SECURE because of a QA flag.

## Screenshot manifest (real captures, English LTR)

| File | Screen | What it proves |
| --- | --- | --- |
| `en-home.png` | Smart Device Hub | measured 89% / 46 GB used / 5.9 GB free from StatFs; gauge face clean; tiles unmeasured (`—`) |
| `en-clean.png` | Smart Clean | unmeasured Tide drawn as fog; no invented bytes |
| `en-clean-center.png` | Clean Center route | opens the real Smart Clean surface, not a placeholder |
| `en-privacy.png` | Privacy Sweep | open seal, "NOT MEASURED YET", no verified claim |
| `en-deep-scan.png` | Deep Scan | 0/7 measured, 5 fog facets, "Ready to dive" |
| `en-storage.png` | Storage breakdown | real partition figures |
| `en-apps.png` | App Manager | real installed-app inventory |
| `en-battery.png` | Battery diagnostics | platform level/temp/voltage |
| `en-large-files.png` | Large files | real media query result |
| `en-ai-recommendations.png` | AI recommendations | Synapse bound to measured evidence |
| `en-reports.png` | Reports & Insights | honest empty state, no trend line invented |
| `en-tools.png` | Device tools | real tool inventory |
| `en-residue.png` | App Residue | unread stratum (fog well), no soil pretence |
| `en-duplicates.png` | Duplicate review | real group scan surface |
| `ar-home.png` / `ar-clean.png` | Arabic RTL | full mirroring: title, chevrons, segments, nav |

## Verified

- Build, JVM tests, install and cold launch of all 14 routes on SM-A025F/API 31.
- Home, Smart Clean, Clean Center, Privacy, Deep Scan, Reports, App Residue, Duplicate review:
  rendered, read correctly, and honest in the unmeasured state.
- Arabic RTL mirroring on Home and Smart Clean.
- Debug capture path works only for the `debug` variant; production resources keep
  FLAG_SECURE.

## Not verified — do not claim

- **No service was run to completion in this pass.** The captures are the pre-measurement
  state; no real cleanup, trash, restore or delete was executed, so no post-operation state
  is proven here.
- Mirror, Terrain, Prism, Synapse and Pulse are compiled and reachable but were not captured
  in a measured/partial state, so their fixed states remain unproven visually.
- Reduced-motion captures were taken with motion enabled; a dedicated reduced-motion set is
  still missing.
- `InstrumentCopyBudgetTest` (EN/AR copy budget) exists as an untracked test from an earlier
  pass; it is not part of a committed, reviewed check yet.
- No device-farm or second-density run; this is one 720×1600 / 280 dpi handset.

## Next

1. Run one real Smart Clean measure and one real Residue measure on synthetic fixtures, then
   capture the measured Tide/Strata states plus the Trash/Restore outcome screens.
2. Capture Mirror (duplicate pass) and Terrain (storage detail) in measured and partial states.
3. Produce the reduced-motion set and a second font-scale (1.3×) set for the RTL proof.
4. Only then move any row of `VISUAL_DOMAIN_MAPPING_20261115.md` to VERIFIED.
