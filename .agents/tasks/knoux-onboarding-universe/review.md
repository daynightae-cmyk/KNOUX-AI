# Cinematic Onboarding and KNOUX Universe Corporate Section

Three new Kotlin/Compose files and four modified ones implement a first-launch cinematic onboarding (3 chapters) and a KNOUX Universe corporate portfolio screen. The onboarding gates via a `SharedPreferences` boolean; the Universe screen is wired into the existing nav scaffold as `Page.KNOUX_UNIVERSE`. All 16 portfolio projects are present as hardcoded data with no runtime API calls. This is review pass 2; it addresses three blocking findings that were raised in the prior pass.

**Watch for:**
- The `scope.launch { chapter-- }` pattern in `BackHandler` and `goBack` wraps a simple integer increment in a coroutine with no suspend call; possible lint warning.
- `OnboardingChapter3` independently writes the preference before calling `onComplete`, while `OnboardingScreen.goNext` also writes it on chapter 2. Idempotent, but `Chapter3` is coupled to the preference key name rather than delegating fully to its callback.

**Verdict**: APPROVED

---

## High-level view

All three blocking findings from the prior review are resolved cleanly. `onboardingDone` is now `var … by remember { mutableStateOf(…) }`, so completing onboarding within a session sets it to `true` immediately in-memory and the splash's `onEnter` path won't retrigger the flow. The `onComplete` lambda now also sets `onboardingDone = true` in MainActivity, closing the stale-read gap. `BackHandler` is added to `OnboardingScreen` with correct chapter-aware behavior: back on chapter > 0 returns to the previous chapter; back on chapter 0 persists the preference and completes, treating it as a skip — matching the skip button's behavior.

The `Page.KNOUX_UNIVERSE` enum position is moved before `CLEAN_CENTER`, which keeps `references = entries.take(10)` stable at exactly 10 (HOME through OPTIMIZE) and `detailRoutes = entries.dropWhile { it != CLEAN_CENTER }` at exactly 10 (CLEAN_CENTER through AI_RECOMMENDATIONS). `KNOUX_UNIVERSE` itself falls between the two ranges and routes to `KnouxUniverseScreen()` through the existing `when (target)` branch in the NavHost.

The portfolio data in `KnouxUniverse.kt` contains exactly 16 projects, all three approved URLs (`knoux.store`, the two Edge Add-ons search URLs), no tokens or private endpoints, and no runtime network calls. All URL launches go through `Intent.ACTION_VIEW` with `runCatching` wrapping, so a missing browser won't crash. The KNOUX logo is loaded via `painterResource(R.drawable.knoux_logo)` from the copied asset; byte sizes match the source (`1,189,663` bytes each). RTL is handled through the project-wide `KnouxTheme` → `LocalLayoutDirection` / `LocalArabic` pipeline; `Copy.localized()` delegates to `text(LocalArabic.current)`. The wordmark row in Chapter I is explicitly pinned to `LayoutDirection.Ltr` via `CompositionLocalProvider`, which is the correct pattern for a brand name that must never mirror.

<details>
<summary>Issues (1)</summary>

1. **Redundant preference write in Chapter 3 CTA** — `OnboardingChapter3`'s Enter button writes `knoux_onboarding_done = true` directly before calling `onComplete()`, but `onComplete` in `OnboardingScreen.goNext` already writes the same key. The double write is idempotent; consider removing the write from `OnboardingChapter3` and relying solely on `onComplete`. Low severity, no blocking risk.

</details>

<details>
<summary>Details</summary>

### Prior findings resolved: `onboardingDone` stale read and `BackHandler` gap

The original `val onboardingDone = remember { … }` captured the SharedPreferences value once at composition time and never updated it. If the user completed onboarding and the splash was replayed in the same process (debug reset, or any path that left `showSplash = true` again), `onboardingDone` would still read `false` and re-trigger onboarding. The fix promotes it to `var onboardingDone by remember { mutableStateOf(…) }` and adds `onboardingDone = true` inside the `onComplete` lambda. Now all paths that set the pref (Chapter 3 CTA, Skip button, BackHandler on chapter 0) also update the in-memory state.

The `BackHandler` is wired with `enabled = true` at the `OnboardingScreen` level, intercepting the system back gesture across all three chapters. Chapter > 0 decrements; chapter == 0 persists and calls `onComplete`. The behavior on chapter 0 — treat back-press as skip — aligns with the skip button's behavior and is the correct UX for a first-launch flow: users who press back to escape should not be re-shown onboarding.

### Double-write path in `OnboardingChapter3`

`OnboardingChapter3` receives `onComplete` and its CTA writes the preference before calling it:

```kotlin
// In OnboardingChapter3's CTA button
context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
    .edit().putBoolean("knoux_onboarding_done", true).apply()
onComplete()
```

But `onComplete` is `OnboardingScreen.goNext`, which also writes the preference when `chapter == 2`. The double-write is safe — SharedPreferences `apply()` is async and idempotent — but `OnboardingChapter3` is tightly coupled to the preference key name instead of delegating fully to its `onComplete` callback. Not blocking.

</details>

---

<details>
<summary>File map</summary>

| File | Change |
|---|---|
| `app/src/main/java/com/knoux/ai/OnboardingScreen.kt` | New file: 3-chapter cinematic onboarding with persistence and `BackHandler` |
| `app/src/main/java/com/knoux/ai/KnouxUniverse.kt` | New file: corporate universe screen with 16-project portfolio |
| `app/src/main/java/com/knoux/ai/PreviewData.kt` | `KNOUX_UNIVERSE` enum entry added at correct position (ordinal 12) |
| `app/src/main/java/com/knoux/ai/MainActivity.kt` | `onboardingDone` promoted to `mutableStateOf`; `onComplete` now sets it; `KNOUX_UNIVERSE` route added |
| `app/src/main/res/drawable/knoux_logo.png` | Logo asset copied from `design/brand/knoux-logo-owner.png` (1,189,663 bytes, unchanged) |

Full diff: `git -C d:\KNOUX-AI diff HEAD~2..HEAD`

</details>
