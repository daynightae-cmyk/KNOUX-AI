# BASELINE — KNOUX AI Phase 1 design closure

**Date:** 2026-11-11 (Asia/Dubai, UTC+04:00)
**Workspace:** `D:\KNOUX-AI` (Windows, local `D:` drive)
**Branch:** `main`
**Starting HEAD:** `7f4ce6402e2a3c45b85fe3c01564edb50f8dbcd7`
`feat(navigation): Refactor page routing to granular detail screens and enhance dashboard reveal animation`
**Remote:** none configured. Nothing was pushed, and no force-push/reset/clean/branch deletion was used.

## 1. Worktree inventory before this pass

The starting worktree was intentionally dirty and contained unique, uncommitted work from the
previous agent. Everything was preserved and inspected before editing:

| Item | State at baseline | Treatment |
|---|---|---|
| `app/src/main/java/com/knoux/ai/ServiceWorlds.kt` | untracked, 29,933 bytes | kept, compacted, retained |
| `app/src/androidTest/.../ServiceWorldsTest.kt` | untracked, 1,731 bytes | kept, fixed import, retained |
| `app/src/main/java/com/knoux/ai/DetailPreview.kt` | modified | preserved, service studio inserted |
| `app/src/main/java/com/knoux/ai/HomeScreen.kt` | modified | preserved, route/layout fixed |
| `app/src/main/java/com/knoux/ai/KnouxComponents.kt` | modified | preserved, button fit fixed |
| `app/src/main/java/com/knoux/ai/MainActivity.kt` | modified | preserved, tab-origin logic and capture hooks |
| `app/src/main/java/com/knoux/ai/NeonArt.kt` | modified | preserved, tall-screen hero height |
| `app/src/androidTest/.../PreviewNavigationTest.kt` | modified | preserved, one test entry path corrected |
| `qa-evidence/` | untracked | preserved; new evidence added under `design-closure-final/` |
| Design references, brand art, prompts, database and `android/integration` | untouched | preserved as references only |

No stash, worktree reflog, or alternate commit was discarded. No Android SDK, Gradle cache,
or design reference was duplicated or deleted.

## 2. Product scope of this baseline

- Phase 1 UI only: Kotlin + Jetpack Compose, single-activity preview app.
- Ten referenced service screens plus ten internal "service studio" detail screens, and the
  cinematic splash — 21 user-visible destinations in total.
- Every figure is labelled sample/design data. No scan, cleanup, privacy change, battery
  change, package action, or file operation is implemented or claimed.
- No Room, Supabase, privileged/restricted permission, Accessibility Service, Shizuku or
  root workaround, and no file deletion exists in this build.
- Phase 2 material under `prompts/phase2`, `database/`, and `android/integration/` remains
  reference-only and was not compiled into the APK.

## 3. Approved visual inputs

| Set | Files | Dimensions |
|---|---|---|
| Main references | `design/reference-20261111/01..10*.png` | 941 × 1672 each |
| Interior references | `design/reference-inner-20261111/01..10*.png` | 941 × 1672 each |
| Master prompt | `MASTER_PROMPT_CODEX_PHASE_1_UI.md` | — |
| Brand mark | `design/brand/knoux-logo-owner.png` (used by traced vector glyphs) | — |

The reference screenshots include an external handset frame. Runtime captures do not, so
comparison strips compare the reference's screen area with the raw device frame. No claim of
pixel identity is made; visual differences are recorded in `VISUAL-PARITY.md`.

## 4. Verification environment

| Item | Value |
|---|---|
| OS / workspace | Windows, `D:\KNOUX-AI` |
| JDK | OpenJDK 21.0.12.1 LTS (Microsoft build) |
| Gradle / AGP / Kotlin | Gradle 8.14.6 / AGP 8.13.2 / Kotlin 2.2.21 |
| App ID / version | `com.knoux.ai` / `0.1.0-preview` (debug) |
| SDK levels | min 26, target 36, compile 36 |
| Device | `emulator-5554`, AVD `knoux_qa26`, Android 8.0.0 / API 26, x86_64 |
| Display | 1080 × 2400 physical, 420 dpi, portrait |
| Resources | 2 vCPU, 2,046,988 kB RAM, software GPU |
| Locale used for captures | English default; Arabic through the in-app language switch |
| Physical phone | disconnected during the official final run; not used as evidence |

The earlier physical-device (`itel S686LN`) run was incidental and had three timing/layout
failures. It is not part of this closure; the official suite below ran only on the emulator.

## 5. Baseline artifact

The pre-pass APK was the previous pass output. Its historical SHA-256 is **not** reused for
this closure. The final APK, size and SHA-256 are measured after the last source change and
recorded in `VERIFICATION.md` and `runtime/MANIFEST.json`.
