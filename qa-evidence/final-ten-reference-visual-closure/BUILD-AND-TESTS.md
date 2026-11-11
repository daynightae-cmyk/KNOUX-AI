# Build & Tests

All commands were run from the repository root `D:\KNOUX-AI` on Windows, with
the Gradle wrapper. Every exit code below is the real exit code of the
command, not an estimate.

## 1. Toolchain

| Component | Version |
|-----------|---------|
| JDK | Microsoft OpenJDK 21.0.12.1 (LTS) |
| Gradle | 8.14.6 (wrapper) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 (Gradle's embedded Kotlin is 2.0.21) |
| Compose BOM | 2025.04.01 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |

New dependencies added by this mission:

| Artifact | Version | Why |
|----------|---------|-----|
| `androidx.biometric:biometric` | 1.1.0 | The real `BiometricPrompt` used by the identity gateway |
| `androidx.appcompat:appcompat` | 1.7.1 | Required by the biometric compatibility dialog on Android 6–8 |
| `androidx.fragment:fragment` | 1.8.9 | `FragmentActivity` host required by `BiometricPrompt` |

## 2. Clean build

```
.\gradlew.bat clean
```
Exit code: **0** — `BUILD SUCCESSFUL in 45s`

## 3. Required build and test tasks

Each task was executed individually so that its exit code is attributable.

| Command | Exit code | Result | Duration |
|---------|-----------|--------|----------|
| `.\gradlew.bat :app:assembleDebug` | **0** | `BUILD SUCCESSFUL in 2m 11s` | 131.6 s |
| `.\gradlew.bat :app:testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 13s` | 14.0 s |
| `.\gradlew.bat :app:lintDebug` | **0** | `BUILD SUCCESSFUL in 2m 18s` (after a retry, see §7) | 138 s |
| `.\gradlew.bat :app:assembleDebugAndroidTest` | **0** | `BUILD SUCCESSFUL in 29s` (after a retry, see §7) | 29 s |

### Unit tests

| Suite | Tests | Failures | Errors | Skipped |
|-------|-------|----------|--------|---------|
| `com.knoux.ai.PreviewFixturesTest` | 3 | 0 | 0 | 0 |
| **Total** | **3** | **0** | **0** | **0** |

### Lint

`app/build/reports/lint-results-debug.xml`:

| Severity | Count |
|----------|-------|
| Error | **0** |
| Warning | 16 |
| Hint | 1 |

`lintDebug` therefore succeeds. The one error that lint raised during this
mission (`StateFlow.value should not be called within composition`) was a
genuine defect — the capture "still" flag was being read from a `StateFlow`
inside composition — and was fixed by moving the flag into an activity field.
See `MOTION-QA.md` §4.

## 4. Instrumentation on a real device

```
adb -s emulator-5554 install -r -t app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r \
    com.knoux.ai.test/androidx.test.runner.AndroidJUnitRunner
```

Result:

```
Time: 98.307
OK (16 tests)
INSTRUMENTATION_CODE: -1
```

| Test class | Tests | Result |
|------------|-------|--------|
| `com.knoux.ai.AuthGatewayTest` | 5 | PASS |
| `com.knoux.ai.PreviewNavigationTest` | 7 | PASS |
| `com.knoux.ai.ServiceWorldsTest` | 1 | PASS |
| `com.knoux.ai.SplashSequenceTest` | 3 | PASS |
| **Total** | **16** | **PASS** |

### Verification device

| Property | Value |
|----------|-------|
| Serial | `emulator-5554` |
| Device | Android Emulator, AVD `knoux_qa26` |
| Android API | 26 |
| Model string | `Android SDK built for x86_64` |
| Display | 1080 × 2400, 420 dpi |
| Biometric hardware | `android.hardware.fingerprint` present |
| Enrolled fingerprints | none — `dumpsys fingerprint` → `prints: []` |

## 5. Runtime capture

Each of the ten surfaces was launched on the device and photographed with the
deterministic capture flag:

```
adb -s emulator-5554 shell am force-stop com.knoux.ai
adb -s emulator-5554 shell am start -n com.knoux.ai/.MainActivity \
    --es knoux.preview.route <home|clean|tools|reports|privacy|ai-optimization> \
    --ez knoux.preview.still true
adb -s emulator-5554 exec-out screencap -p > <file>.png
```

The four authentication states use the same activity with
`--es knoux.preview.auth <welcome|verify|verified|unavailable>`.

The Arabic / RTL pass adds `--ez knoux.preview.arabic true`.

## 6. Deliverable artifacts

| Artifact | Path | Size | SHA-256 |
|----------|------|------|---------|
| Application (debug) | `app\build\outputs\apk\debug\app-debug.apk` | 12 591 019 bytes (12.01 MiB) | `e4b523c7cbc07c219b5318df41b57c224b66b82109df548dbb2f64efeac523bd` |
| Instrumentation (debug) | `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1 097 523 bytes (1.05 MiB) | `ae5b915335cc995fbe0423ac930f320b99dc07c79b60bd70f0538a6e986f25d2` |

These hashes are of the **clean-built** artifacts that produced the captures
in `runtime/` and the instrumentation result in §4.

## 7. One transient infrastructure failure, retried and resolved

During the clean-build sequence, `:app:lintDebug` and
`:app:assembleDebugAndroidTest` failed to start with:

```
Could not create service of type FileLockContentionHandler ...
java.net.BindException: Address already in use: bind
```

This is a Gradle **daemon port collision** on the build host, not a source
defect: the previous daemon still held its control socket. After
`.\gradlew.bat --stop` and a short wait, both tasks were re-run and both
reported `BUILD SUCCESSFUL` (2m 18s and 29s) with exit code 0. The same
failure had been observed once earlier in the session and cleared by the same
`--stop`. No source change was made in response to it.

## 8. What was **not** run

| Item | Reason |
|------|--------|
| Instrumentation on a physical handset | The connected handset (`1334825549012211`, itel S686LN, API 35, 1080 × 2436, fingerprint hardware present) disconnected from USB partway through the session and did not reappear. The emulator carried the full verification. |
| Instrumentation on a device with an **enrolled** fingerprint | Neither available device has an enrolled fingerprint, so the biometric **success** path could not be exercised end to end. The success callback is wired to the real `BiometricPrompt` and is asserted to be the only route into the VERIFIED state. See `AUTH-AND-HEADER-QA.md`. |
| Release build / Play Store packaging | Phase 1 is a UI-only preview; signing, shrinking and release variants belong to the pre-production stage and are not authorised yet. |
