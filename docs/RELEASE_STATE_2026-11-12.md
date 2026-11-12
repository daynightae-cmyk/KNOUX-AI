# KNOUX AI — GitHub Publication State (2026-11-12)

## Baseline
- Local canonical repository: `D:\KNOUX-AI`.
- At audit start: `main` HEAD `112cbf66fd3f1add321d568be7bac71086480956`.
- Remote `https://github.com/daynightae-cmyk/KNOUX-AI.git` was an empty newly created repository, with no local `origin` configured.
- Canonical repo has 761 tracked files at audit start, plus intentional uncommitted work.
- A nested `D:\KNOUX-AI\KNOUX-AI\.git` existed, **empty** with no commits and must not be mistaken for the original repository. It remains local, ignored.
- No other active KNOUX-AI editing agent process was identified in the targeted process scan at audit time.
- `.avd/`, `.gradle/`, `INBOX/` and build outputs are excluded from source control.

## Latest locally integrated code
- `app/src/main/java/com/knoux/ai/MasterScreens.kt`: dimensional service icon improvements / gradient, bloom, specular highlights.
- `app/src/main/java/com/knoux/ai/ServiceWorlds.kt`: ten distinct Compose service preview studios.
- `app/src/androidTest/java/com/knoux/ai/ServiceWorldsTest.kt`: new UI interaction test source.
- The app is still a truthful **design preview**; no real device scanning, cleanup, file mutation or paid AI service is implemented by this set of changes.
- The README gallery links actual previously captured emulator screenshot assets. The original source screenshot evidence is not to be replaced by generic generated mockups.

## QA limitation during this publish
Executed from the local project with Android SDK available and existing Gradle user home:
`gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --offline --no-daemon --max-workers=2`
Result: **FAIL / exit 1 at :app:checkDebugAarMetadata** — required dependencies missing from offline cache:
- `androidx.biometric:biometric:1.1.0`
- `androidx.appcompat:appcompat:1.7.1`
- `androidx.fragment:fragment:1.8.9`
This does **not** prove a Kotlin compile or test failure, since compilation did not run. Online dependency resolution and complete build/device tests remain required before a production candidate can be called PASS. Trace: `qa-evidence/publish-validation-20261112.log`.

## Screenshot integrity caveat
Newly untracked `qa-evidence/service-worlds-20261111/` contains several 21 KB files with identical SHA-256 despite distinct intended route names, and other duplicate frames. These captures were **not accepted as valid evidence**, were **not deleted**, and should be regenerated from the current APK before inclusion in a final QA report. Historical, genuine emulator captures remain accessible from `qa-evidence/knoux-ai-visual-finish/` and `qa-evidence/final-ten-reference-visual-closure/`.

## Publication contract
- Preserve all Git history and never force-push.
- Commit the canonical project, the newer source and UI tests, and README with linked original screenshots; do not commit the nested empty repository, untrusted placeholder screenshots, secrets, cache or emulator userdata.
- Verify GitHub `main` SHA equals the intended local commit after push; verify README and hero SVG reachable.
- A source publication is NOT a Play Store release, QA PASS or proof that device services work.
