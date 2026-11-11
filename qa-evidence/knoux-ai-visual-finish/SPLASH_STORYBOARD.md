# SPLASH STORYBOARD — cinematic KNOUX AI reveal (fourth pass, final build)

Source of truth: `design/brand/knoux-logo-owner.png` (the owner's raster logo; the K is
traced from it, the presentation circle is not drawn). Implementation:
`app/src/main/java/com/knoux/ai/KnouxSplash.kt` (+ `SplashAtmosphere.kt`, `SplashChime.kt`).
Recording: cold launch on `emulator-5554` (API 26, SwiftShader), screenrecord 540×960,
14.35 s — `splash/cold-launch.mp4`.

## Timing graph (controller clock, ms)

```
0      480     1140      1610        2080    2460 2710  2800    3080   3480
|-------|--------|----------|-----------|--------|---|-----|-------|------|
A bloom  B K       B ribbon   C wordmark  D smile   tip  click D hold  E fade→Home
         wedge+    grows      letters     draws     flare (one)
         flare     K legs     write on    l→r            ~2550-2800
```

Storyboard frames (deterministic `--el knoux.preview.splash.time <ms>` freeze launch,
PNG 1080×1920): `splash-480.png` (bloom), `splash-835.png` (magenta wedge + first flare),
`splash-1140.png` (diagonal ribbon, cyan leg), `splash-1610.png` (wordmark mid-write:
"kno…" visible — progressive left→right reveal), `splash-2280.png` (smile drawing),
`splash-2460.png` (tip near end), `splash-2710.png` (flare/click window),
`splash-2800.png`, `splash-3000.png` (full lockup), `cold-launch-home.png` (landing).
Contact sheet: `splash/sequence.png`. All ten frames are in one image for review.

## Wordmark geometry and the clip fix (this pass)

- The wordmark "knoux ai" is laid out with the app display face (Outfit ExtraBold,
  `app/src/main/res/font/outfit_extrabold.ttf`), target width `775·s` on a `1000·s`
  stage; `knoux` is chalk white, `ai` is the pink→violet gradient.
- Letters reveal left→right, staggered 80 ms, 250 ms per letter, clipped per letter by
  `clipRect` so the pen edge stays crisp.
- **Bug found and fixed this pass:** Compose fakes `FontStyle.Italic` with a horizontal
  skew that pushes ink (the i-dot, stem tops) past the glyph advance. The old 4 px clip
  margin sheared the final `i` at a vertical edge (visible in `splash-3000.png` of the
  previous capture and in the live settle). Fix in `drawWordmark`: while a letter is
  still writing the clip stays tight; once a letter fully lands (`p ≥ 0.999`) the clip
  widens by `0.34 ×` letter height, exactly covering the skew overhang.
  After the fix, `ai` renders complete (measured pink-ink extent x=725..865 on a
  1080-wide frame; well inside the screen).

## Click trigger

- `onSmileCompleted` fires once when the smile path reaches its end (live launches only;
  the reduced-motion path fires after the 750 ms static brand; freeze frames never fire).
- `SplashChime.play()` plays `res/raw/knoux_smile_click.wav` exactly once per reveal
  (`played` guard), only if loaded in time; skipped silently otherwise.
- Audio honors the device: `RINGER_MODE_NORMAL`, system stream not muted, volume > 0 —
  otherwise skipped (`audible()` in `SplashChime.kt`). Usage class
  `USAGE_ASSISTANCE_SONIFICATION`. Released on dispose.
- Proof on the final build: `logcat` `KnouxSplash: Smile click started` — exactly **1**
  occurrence during the cold launch (`splash/click-events.log`,
  `11-11 06:20:15.301 10322 10322 D KnouxSplash: Smile click started`;
  full launch log in `splash/capture-splash.log`).
- Audio-video sync caveat: Android `screenrecord` captures no audio track, and this
  emulator boots with `-no-audio`. The click is verified by the logcat timestamp against
  the recording's smile-completion window, not by audible capture. No 60 fps smoothness
  is claimed on SwiftShader (time-based scenes can step between rendered frames).

## Transition and lifecycle rules

- After the smile completes, the scene holds to `3080 ms`, fades (`fadeStart 3080 →
  total 3480`), then hands off to Home; Home's decorative hero motion stays paused while
  the splash overlay is visible so the splash owns the frame budget.
- `SplashGate` prevents a replay when returning from background; activity recreation
  skips a stuck splash; a watchdog (`total + 2500 ms`) always releases the user.
- Reduced motion (`reduced = true`): 750 ms static brand reveal, no drawn stroke
  animation, no click cue before the static finish, immediate destination access
  (verified by `SplashSequenceTest.reducedMotionShowsStaticBrandAndFinishesQuickly`).
- The full show plays on cold launch only, never on every resume (tests:
  `liveSmileCuesOnceAtCompletionThenFinishes`, `frozenDebugFrameStaysVisibleForCapture`).
- On Android 12+ the platform system splash still runs first; the branded reveal happens
  inside the app afterwards (min SDK is 26, so no compatibility library is in play).

## Reference comparison

`splash/reference-comparison.png`: left = `design/reference-20261111/02_Splash.png`
(design target incl. its phone mockup frame — mockup excluded from comparison per the
brief), right = runtime `splash-3000.png`. Honest deltas:

1. The runtime lockup sits slightly larger and the wordmark slightly wider than the
   reference crop; the reference composition breathes more at the edges.
2. The reference smile is thicker with a stronger magenta glow and a larger tip star;
   the runtime smile is thinner/subtler (glow radii documented in `drawNeonSmile`).
3. The reference background has denser wave detail and a stronger purple ambience.
4. The K silhouette itself matches the traced owner geometry (magenta wedge, crossing
   ribbon, cyan leg) — the logo remains the authority for shape.
