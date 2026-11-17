# KNOUX AI — Identity gateway (login) redesign — 2026-11-15

Scope: the post-splash **login gate only** (`AuthScreen.kt` + a new seal instrument).
No other route, instrument, service or previously accepted visual was replaced. The
16 modified files and 15 untracked instruments/tests from earlier passes are preserved
verbatim; nothing was reverted, stashed, cleaned or committed.

Source state: branch `main`, HEAD `3f69b00` (one commit ahead of `origin/main`).
Everything described here is **uncommitted working-tree work**.

Test target: the owner-authorized handset **SM-A025F, API 31, 720x1600 px @ 280 dpi**,
reached through `KNOUX_TEST_DEVICE_SERIAL`. The serial is not stored in this repository.

## What was wrong before

| Defect | Evidence in the old screen |
| --- | --- |
| Headline read "WelcomeBack" | lead + accent concatenated with no space |
| The identity orb filled the screen | seal sized at 258 dp with no relation to the content |
| A `PRO` pill that could not be tapped | dead affordance implying a paid state |
| "Remember me" checkbox with no stored preference | dead control |
| "Sign up" opened the same non-existent flow | dead control |
| Email + password + "Sign in" implied a working account path | no provider, no session, no error path |
| A hex-grid backdrop unrelated to the rest of the app | the shared observatory sky was already available |

## What the gate does now

| Control | Real behaviour |
| --- | --- |
| Fingerprint tile | opens the **real** `BiometricPrompt`; only its success callback advances the seal |
| Local mode | opens the device tools with no account at all (a real entry) |
| Google / Passkey | open an explicit "not connected in this build" sheet; no session, no token |
| Continue (credentials) | opens the same honest sheet; the fields stay editable but never fake a sign-in |
| Forgot password | states that recovery needs the same identity provider and is unavailable |
| Settings | real language switch (EN/AR) and reduce-motion switch |
| Code step (OTP) | digits are never pre-filled; pressing Verify states that no OTP provider is configured |

Honesty rules enforced in code:

- `AuthStage.UNAVAILABLE` is drawn in **fog**, not in an alert colour — a handset without
  an enrolled print has not failed, it simply has nothing to read.
- The seal is animated only while a real prompt is open, and reaches `VERIFIED` only from
  `onAuthenticationSucceeded`.
- No provider, credential or code path can create a session in this build.

## The seal instrument

`app/src/main/java/com/knoux/ai/visual/PortalSeal.kt` (new, 263 lines) — the one visual on
the gate: a shield carrying the brand K, two tilted orbits, and a standing pedestal.
States: `IDLE`, `SCANNING`, `VERIFIED`, `UNAVAILABLE`; the K sits at 27 % of the seal so
the mark never becomes a wallpaper. Sizes: welcome 150 dp, code 150 dp, verifying 176 dp,
verified 196 dp, unavailable 132 dp.

## Files touched

| File | Lines | SHA-256 (12) | Note |
| --- | --- | --- | --- |
| `app/src/main/java/com/knoux/ai/AuthScreen.kt` | 960 | `E62051CC7B60` | rewritten gate (was 1551 lines) |
| `app/src/main/java/com/knoux/ai/visual/PortalSeal.kt` | 263 | `F4ABBE048199` | new instrument |
| `app/src/main/java/com/knoux/ai/PreviewData.kt` | 89 | `E4B2031A9456` | `Mark.FINGERPRINT` |
| `app/src/main/java/com/knoux/ai/NeonArt.kt` | 486 | `D983E284BD50` | fingerprint path |
| `app/src/main/res/values/strings.xml` | 1112 | `4426F3359D6B` | +58 lines |
| `app/src/main/res/values-ar/strings.xml` | 1103 | `10CDC92F4445` | +50 lines |
| `tools/qa/capture_auth_states.ps1` | 65 | `E96E5EB299ED` | TalkBack-safe capture harness |

Line endings: `strings.xml` and `values-ar/strings.xml` keep their original CRLF so the
diff stays at the real 58/50 added lines instead of the whole file.

## Build and test evidence

| Command | Result |
| --- | --- |
| `gradlew :app:compileDebugKotlin` | EXIT 0 |
| `gradlew :app:assembleDebug :app:testDebugUnitTest` | BUILD SUCCESSFUL, EXIT 0 |
| `app-debug.apk` | 19,732,152 bytes, SHA-256 `6450AB3A535C84856F116CE7E767E5971C4E9FC76E7AC2A132085D7608852300` |

A passing build is not a visual claim. The states below were captured from the device.

## Capture evidence

Method: `tools/qa/capture_auth_states.ps1` — one cold start per state, 18 s settle
(the first Compose frame needs 7–12 s on this handset), `adb shell screencap`. The script
parks the accessibility service for the run and restores the exact previous value in
`finally`, because TalkBack's focus ring would otherwise be read as an app defect.

Folder: `D:\Temp\opencode\knoux-auth-20261115` (10 PNG, EN + AR x 5 states).

| State | What the frame proves |
| --- | --- |
| `welcome` | compact seal, single headline with correct spacing, three equal provider tiles, honest note card, local-mode entry |
| `verify` | seal in `SCANNING`, no fake percentage, Cancel reachable |
| `verified` | seal in `VERIFIED` + green check, "Opening your dashboard" |
| `unavailable` | seal in fog + amber badge, explicit body text, local mode offered first |
| `otp` | six empty slots, real timer, Verify states the provider is not connected |

| File | Bytes | SHA-256 (12) |
| --- | --- | --- |
| `en-auth-welcome.png` | 751,247 | `FCFC244BF3DA` |
| `en-auth-verify.png` | 656,571 | `F17034F033AC` |
| `en-auth-verified.png` | 652,223 | `5DD0C82F9DAA` |
| `en-auth-unavailable.png` | 692,403 | `C2AD9010EAA9` |
| `en-auth-otp.png` | 674,660 | `C5980C62A554` |
| `ar-auth-welcome.png` | 745,584 | `60DE36746ECD` |
| `ar-auth-verify.png` | 654,238 | `6C08B138C902` |
| `ar-auth-verified.png` | 651,016 | `AB034354AB88` |
| `ar-auth-unavailable.png` | 685,581 | `39629BCA6F43` |
| `ar-auth-otp.png` | 683,262 | `1DA7C039AAA2` |

Arabic frames confirm RTL mirroring (gear left, wordmark LTR, arrow flipped, tile order
mirrored) and that no Arabic label is clipped.

## Defects the harness itself found

| Finding | Cause | Resolution |
| --- | --- | --- |
| A whole run produced 7,904-byte PNGs | the handset slept between cold starts; `screencap` on a sleeping screen writes pure black | the harness now sends `KEYCODE_WAKEUP` and `wm dismiss-keyguard` before each launch and before each capture |
| `Resend code in00:47` | the label and the clock were emitted as two adjacent `Label`s with no gap | 5 dp spacer added between them (both locales re-captured) |
| A run wrote `en-auth-welcome,verify.png` | PowerShell `-File` passes `a,b` as one string, so the state filter became a single unknown state | `-States` is now a comma string split inside the script |

## Known, recorded limitations (not defects hidden)

- The reference handset has **no enrolled fingerprint**, so the real biometric prompt
  cannot be exercised here; the gate correctly reports it as unavailable and now says so
  in a caption under the tiles.
- The code step is reachable only through the debug preview hook; no live flow routes to
  it because no OTP provider exists.
- Account sign-in, passkey and Google remain unconnected; every one of them says so.
