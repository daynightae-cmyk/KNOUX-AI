# Motion QA

Scope: every animated element in the KNOUX AI preview, the reduced-motion
behaviour, and the deterministic capture mode.

## 1. Motion sources

| Element | Implementation | Driver |
|---------|----------------|--------|
| Hero artwork (particle globe, orbital arcs, pedestal) | `NeonHero` in `NeonArt.kt` | Three independent infinite transitions — phase 30 s, spin 19 s, pulse — all linear |
| Fingerprint orb (ring spin, breath, verification sweep) | `FingerprintOrb` in `AuthScreen.kt` | Ring spin 14 s linear; breath 3.6 s reverse; light sweep 2.2 s reverse while verifying |
| Screen transition | `KnouxAppScaffold` in `MainActivity.kt` | Fade + horizontal slide, 260 ms, `FastOutSlowInEasing` |
| Dashboard reveal | `MainActivity.kt` | Scale 0.95 → 1.00 plus fade, 520 ms, `FastOutSlowInEasing` |
| Bottom-navigation selection | `KnouxBottomNavigation` | Disc fill 260 ms, icon scale 230 ms, tone cross-fade 200 ms |
| Progress donut sweep | `ProgressDonut` in `KnouxComponents.kt` | 1 000 ms sweep, plus a 4.2 s glint when live |
| Bar-chart and sparkline reveal | `ActivityChart`, `TrendSparkline` | 950 ms and 900 ms |
| Risk-donut reveal | `RiskDonut` | 900 ms segment-by-segment frontier |
| Splash sequence | `KnouxSplash.kt` | Its own timeline, 520 ms dashboard hand-off |

Every one of these reads `LocalPreviewMotion.current`, which is the single
switch that turns the whole decorative layer on or off.

## 2. Motion gates

The motion flag is only true when **all** of the following hold:

| Gate | Source |
|------|--------|
| The activity is at least `RESUMED` | `Lifecycle.State.RESUMED` |
| The user has not enabled "Reduce motion" | `PreviewPreferences.reduceMotion` |
| The capture flag is not set | `MainActivity.captureStill` |
| The platform animator scale is on | `ValueAnimator.areAnimatorsEnabled()` |
| The device is not in power-save mode | `PowerManager.isPowerSaveMode` |

This means the decorative layer stops automatically when the app is
backgrounded, when the system animator scale is 0, and when the handset is
low on battery.

## 3. Reduced motion

`knoux.preview.reduceMotion` (debug only) and the in-app **Reduce motion**
setting both set `PreviewPreferences.reduceMotion`. With it enabled:

* every infinite transition is skipped rather than merely shortened — the
  animations are started conditionally, so a static frame is rendered instead
  of a fast loop;
* the splash shows a static brand frame and finishes immediately;
* the dashboard reveal jumps straight to its final scale and alpha;
* the donut, chart and sparkline reveals render their final values.

`SplashSequenceTest.reducedMotionShowsStaticBrandAndFinishesQuickly` asserts
this behaviour on the device.

## 4. Deterministic capture mode

`knoux.preview.still` (debug only, `BuildConfig.DEBUG` guarded) freezes the
decorative layer so a screenshot is byte-stable. It is implemented by holding
the flag in an **activity field** (`captureStill`) rather than reading it from
the collected visit state.

This was a real defect found during this mission: the flag was previously
derived from `visits.collectAsStateWithLifecycle()`, which delivers its first
value *after* the initial composition. The motion flag therefore flipped from
`true` to `false` in the middle of a screen transition, which froze the
outgoing screen on top of the incoming one and produced a visible ghost of
the previous screen's title. Moving the flag to an activity field removed the
ghosting — confirmed on the Reports capture, which previously showed a faded
"Smart Device Hub" title behind the "Reports & Insights" title.

## 5. Verification performed

| Check | Method | Result |
|-------|--------|--------|
| Splash timeline completes and cues once | `SplashSequenceTest.liveSmileCuesOnceAtCompletionThenFinishes` | PASS |
| Frozen splash frame stays visible for capture | `SplashSequenceTest.frozenDebugFrameStaysVisibleForCapture` | PASS |
| Reduced motion is static and fast | `SplashSequenceTest.reducedMotionShowsStaticBrandAndFinishesQuickly` | PASS |
| No ghosting between screens | Visual diff of the Reports capture before and after the fix | PASS |
| Ten captures render a single static frame | Byte-size check plus visual inspection of all ten runtime PNGs | PASS |
| Motion is not fabricated as a device result | Every animated figure is accompanied by a DESIGN PREVIEW label | PASS |

## 6. Instrumentation result

```
adb -s emulator-5554 shell am instrument -w -r \
    com.knoux.ai.test/androidx.test.runner.AndroidJUnitRunner
Time: 87.805
OK (16 tests)
```
