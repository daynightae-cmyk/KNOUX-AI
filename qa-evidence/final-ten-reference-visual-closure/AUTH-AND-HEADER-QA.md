# Authentication & Header QA

Scope: the four authentication states (references 07–10) and the single shared
application header (references 01–06).

## 1. The identity gateway

Implementation: `app/src/main/java/com/knoux/ai/AuthScreen.kt` — one screen, four
states driven by `AuthStage`.

| State | Trigger | What the user sees |
|-------|---------|---------------------|
| `WELCOME` | Launch (splash completes) | Wordmark with gold PREVIEW badge, "Welcome to KNOUX AI", holographic fingerprint, "Unlock with fingerprint", three provider rows, privacy link |
| `VERIFYING` | Fingerprint pressed **and** the device reports a usable biometric | "Verify your identity", scanning fingerprint with travelling light sweep, Cancel |
| `VERIFIED` | **Only** from `BiometricPrompt.AuthenticationCallback.onAuthenticationSucceeded` | Cyan check mark over the print, "Identity verified", "Opening your dashboard…" |
| `UNAVAILABLE` | `BiometricManager.canAuthenticate()` reports no usable biometric, or the prompt fails with a hardware/enrolment error | Warning badge on the print, "Biometric unavailable", three provider rows |

### Genuine biometric behaviour

* The prompt is the platform `androidx.biometric.BiometricPrompt`, constructed
  with the real `FragmentActivity` host and the main-thread executor.
* `BIOMETRIC_WEAK` is requested; `canAuthenticate()` is consulted **before**
  the prompt is launched, so a device without an enrolled biometric never shows
  a broken dialog — it shows the honest UNAVAILABLE state instead.
* The `VERIFIED` state is reached from exactly one place: the authentication
  success callback. No animation, timer, or decorative event can grant access.
* `ERROR_HW_UNAVAILABLE`, `ERROR_HW_NOT_PRESENT`, `ERROR_NO_BIOMETRICS` and
  `ERROR_NO_DEVICE_CREDENTIAL` all route to UNAVAILABLE; a user cancel returns
  to WELCOME.
* The biometric prompt is a **system** UI surface and is not redrawn by the app,
  so the app cannot fake its own success.

### Provider buttons — honest by construction

`Use a passkey` and `Continue with Google` open a sheet that states the provider
is not configured in this preview build and that no credential provider, key,
OAuth client, client secret or account session is created. They never produce a
session and never advance into the application.

`Continue as Guest` is the only path into the application from the gateway, and
it grants a clearly-labelled guest session with no account.

### Verification device

| Property | Value |
|----------|-------|
| Device | `emulator-5554` — Android Emulator `knoux_qa26` (AVD) |
| Android API | 26 |
| Display | 1080 × 2400, 420 dpi |
| Biometric hardware | `android.hardware.fingerprint` reported **present** |
| Enrolled fingerprints | none (`dumpsys fingerprint` → `prints: []`) |

Because no fingerprint is enrolled on the verification emulator, the honest
runtime outcome of pressing the fingerprint is the **UNAVAILABLE** state — which
is exactly reference 07. The VERIFYING and VERIFIED states were captured with
the debug-only `knoux.preview.auth` extra, which renders a *specific* state for
deterministic photography; it does not unlock anything and is compiled out of
release behaviour by the `BuildConfig.DEBUG` guard.

## 2. The shared application header

One header composable (`KnouxTopBar`) is used by every application screen, so
there is exactly one logo lock-up, one **Pro** action and one settings action
per screen — no duplicated logos, back arrows or badges.

| Property | Value |
|----------|-------|
| Height | 56 dp content, inside a rounded glass capsule |
| Logo | 31 dp brand K, LTR-locked so it never mirrors in Arabic |
| Wordmark | "knoux" + magenta " ai"; scales down for Arabic and for large font scales |
| Pro action | 48 dp touch target wrapping a compact gold-outlined crown pill |
| Settings | 48 dp gear, always last |
| Navigation | Back arrow on every screen except Home (which shows the menu) |

Verification on the emulator screenshots: one logo lock-up, one Pro pill and one
gear per screen, with no clipping at 420 dpi or in Arabic.

## 3. Known limitations

* The emulator used for verification has **no enrolled fingerprint**, so a
  genuine end-to-end biometric success could not be exercised on the device. The
  success path is wired to the real callback and is covered by the assertion
  that the VERIFIED state is unreachable without it
  (`AuthGatewayTest.theGatewayNeverShowsAVerifiedStateWithoutABiometricCallback`).
* Passkey and Google remain provider-configuration placeholders by design; real
  provider provisioning belongs to the pre-production stage.
