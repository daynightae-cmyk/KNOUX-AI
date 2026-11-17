# KNOUX AI — Visual Continuation Evidence

Date: 2026-11-15 (Asia/Dubai)

## Lane and preservation

- Canonical source: `D:\KNOUX-AI`
- Branch: `main`
- Local HEAD: `78391aa927b3629fcf856de04b10dae23fc2881c`
- Live `origin/main`: `78391aa927b3629fcf856de04b10dae23fc2881c`
- Parallel worktree preserved and untouched: `K:\KNOUX-AI-NATIVE-UI-20261114`
- Existing 18 modified tracked paths and 4 unique untracked Kotlin/data files were preserved. No source file was edited in this validation pass.
- Separate `K:\KNOUX-AI-Pro` and the independent Linux repository were not accessed.

## Reference package inspection

The attached package was extracted only into `.work/continuation-review-20261115/`. All 10 approved reference PNGs were opened by the local file reader and verified as 941x1672 PNGs. All 12 historical runtime shots were also opened and verified as non-empty 720x1600 PNGs. Historical shots remain comparison evidence only and were not treated as acceptance evidence.

## Build and test evidence

Process-local SDK configuration used the existing `D:\Android\sdk`; no SDK or emulator was downloaded.

| Command | Exit | Result |
|---|---:|---|
| `$env:ANDROID_HOME='D:\\Android\\sdk'; .\\gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` | 0 | BUILD SUCCESSFUL |
| same SDK env; `.\\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain --no-daemon` | 0 | BUILD SUCCESSFUL |
| same SDK env; `.\\gradlew.bat :app:testDebugUnitTest --console=plain --no-daemon` | 0 | BUILD SUCCESSFUL |

JVM XML totals: **74/74 PASS**, 0 failures, 0 errors, 0 skipped (DeepScanRulesTest 11, DuplicateRulesTest 13, PageRoutesTest 5, PrivacyRulesTest 16, RealDataRulesTest 13, ResidueRulesTest 16).

The first unit-test attempt hit a transient Gradle `java.net.BindException: Address already in use: bind` and was retried once; the bounded retry passed. This is not counted as a test failure.

APK: `D:\KNOUX-AI\app\build\outputs\apk\debug\app-debug.apk`  
SHA-256: `B2B4AAA0BABA2F3E4386B57FAEFF17D260D7C2A3DDBC24937D76E4AA65300483`

## Samsung runtime evidence

- Target: verified Samsung Galaxy A02s, model `SM-A025F`
- Android: 12 / API 31
- APK installation: `adb -s <verified-local-target> install -r -t app-debug.apk` → **exit 0, Success**
- Activity launch: `com.knoux.ai/.MainActivity` returned `Status: ok` for each requested route.
- No app crash was found in the inspected KNOUX/AndroidRuntime logcat tail.
- No DataStore byte patch, app-data clear, uninstall, device wipe, or privacy bypass was performed.

## Ten-screen route evidence

The debug route names below are existing app routes, not newly invented screens. UI semantics confirmed the native Compose shell and route content when the Samsung accessibility bridge returned a root. `BLOCKED_VISUAL_CAPTURE` means Android/device capture returned an empty frame; it is not a visual PASS.

| Ref | Approved screen | Existing route | Runtime evidence | Screenshot evidence | Status |
|---:|---|---|---|---|---|
| 01 | Home / Device Hub | `home` | Compose shell route launched; semantics collected | `01-home.png` was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 02 | Smart Clean — Review Files | `clean-center` | Compose shell route launched; semantics collected | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 03 | Privacy Risk Details | `privacy-risk-inspector` | route launched; UiAutomator intermittently returned null root | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 04 | AI Recommendation Details | `ai-recommendations` | Compose shell route launched; semantics collected | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 05 | App Details | `installed-apps-review` | Compose shell route launched; semantics collected | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 06 | Battery Plan | `battery-diagnostics` | Compose shell route launched; semantics collected | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 07 | Action History | `performance-report-detail` | Compose shell route launched; semantics collected | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |
| 08 | Large Files | `large-files` | route launched | valid pulled PNG, 720x1600, 719,757 bytes; SHA-256 `DF9B031736481F236C41A3A20B250D7BF9DFAA784592021E6BC6083B3551D2CF` | Captured; not owner-approved parity |
| 09 | Final Review Before Execution | `clean-center` (current shell has no separate final-review route) | route launched; same existing Smart Clean review surface | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE / route-gap |
| 10 | Deep Scan Results | `deep-scan-summary` | semantics explicitly showed `Deep Scan`, `Nothing measured yet`, `Abyss Sonar, idle, 0 measured module(s), 5 unmeasured`, and test tag `instrument-abyss-sonar` | pulled capture was 0 bytes | BLOCKED_VISUAL_CAPTURE |

## Visual comparison verdict

No pixel-parity or visual PASS is claimed. The approved references are conceptual 941x1672 targets; the implementation runs at the Samsung's 720x1600 viewport. One valid screenshot was obtained for Large Files. The remaining captures were rejected as invalid rather than substituted with historical shots or fabricated states.

The app's supported `SecureWindowEffect` honors the persisted `privacySecureWindow` setting through Android `FLAG_SECURE`. The capture behavior is consistent with Android refusing screenshots of a protected window, and the task explicitly forbids raw DataStore edits or silent security weakening. A future visual-capture pass needs an owner-authorized, supported way to turn that setting off in-app, or an approved test capture path; it must then recapture all ten routes and perform side-by-side comparison.

## Safe next action

Coordinate the active visual writer and obtain an owner-authorized supported capture state for `privacySecureWindow`, then recapture the ten routes and review concrete differences in header/safe-area placement, hero-card geometry, tile hierarchy, typography, bottom navigation, RTL, and truthful empty-state treatment. Do not mark the task visually complete from this build/test result alone.
