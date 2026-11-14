# KNOUX AI — native shell + Home, executed in an isolated checkout

Date 2026-11-14 · Branch `knoux-native-ui-20261114` · Base `78391aa927b3629fcf856de04b10dae23fc2881c`
Authority: `docs/design/KNOUX_FIGMA_SHELL_PRESERVATION_EXECUTION_20261114.md` (owner's Figma-first shell contract)
and the owner directive `KNOUX_AI_AGENT_DIRECT_GITHUB_FIGMA_NATIVE_UI_EXECUTE_20261114`.

## 1. Reality before any edit

| Fact | Value |
| --- | --- |
| Canonical repo | `https://github.com/daynightae-cmyk/KNOUX-AI.git`, `main` at `78391aa` |
| `D:\KNOUX-AI` | dirty — 5 tracked files owned by a **concurrent writer** (HomeScreen, KnouxComponents, KnouxScaffold, MainActivity, MasterScreens, last writes 09:07–09:14) |
| Free space on `D:` | ~2.1 GB, tracked tree 1166.6 MB → building there was not viable |
| Working tree used | `K:\KNOUX-AI-NATIVE-UI-20261114` — sparse cone worktree (`app`, `gradle`, later `docs`), 7.2 MB, shares the object store |
| Writer's work protected | archived as `CONCURRENT_WRITER_UI_PASS_20261114.patch` (924 lines) before any edit |
| Device | none connected → all on-device proof recorded NOT_RUN, not skipped silently |

Per shell-contract §REALITY FIRST and directive §1C, the writer's files were not touched. Their
identifiers (`NativePalette`, `LocalNativeApp`, same hex values, same insertion points) were adopted
deliberately so the two passes merge mostly additively.

## 2. Token and shell layer

`NativePalette` (deep navy base `#090A1E`, card `#0F1734`, raised `#152343`, border `#263558`,
blue `#1287F7`, cyan `#2AB9FF`, violet `#A579EC`) and `NativeSpace` (edge 20, row 12, block 16,
gutter 12, tile 92) are the only colour and rhythm sources the utility surfaces may use.

Everything is switched by one composition local, `LocalNativeApp`, provided `true` by the real app
shell only. Splash, onboarding and the identity gateway keep the approved cinematic treatment and
the exact logo — they are outside the native flag.

Banned decoration → where it was stopped (chokepoints, not per-screen rewrites):

| Banned | Mechanism now native-gated |
| --- | --- |
| Giant hero / 3D globe / ribbon field / pulsing rings | `NeonHero` returns a 34 dp service plate and nothing else |
| Gradient headline + signature flourish | `MasterTitle` renders 20 sp plain title + 12 sp muted subtitle |
| Glass on every card | `GlassPanel` → flat raised→card gradient with 1 px border |
| Neon borders, bloom, double arrows on buttons | `AccentButton`, `PrimaryAction`, `SecondaryAction` → flat blue primary, card secondary, cyan selected border |
| Light pools, particles, fog on all pages | app root draws `drawNativeAmbientField()`: three thin static arcs, max alpha .20 |
| Shimmer/scan animation abuse | `GradientBar` single fill; `Shimmer` static under reduced motion |
| Decorative donut chrome | `ProgressDonut` → track + measured sweep, blue→cyan→violet only |

## 3. Home — Figma 2:2 / 2010:2 geometry mapping

| Measured in Figma | Implemented | Source |
| --- | --- | --- |
| toolbar y24..80 (56 dp) | flush 56 dp bar, ink background, 1 px hairline; `menu`/`back`/`pro`/`settings` kept | `MainActivity.KnouxTopBar` |
| content x20, width 320 | `NativeSpace.edge` = 20 dp horizontal padding | `NativeHubScreen` |
| storage overview 320x108 | `NativeStorageOverview` — ring + used/free | `HomeScreen.kt` |
| Start Deep Scan 320x48 | `AccentButton` `heightIn(min = 48.dp)`, tag `start-scan` | `NativeHubScreen` |
| segmented tabs 320x36 | `NativeHubSegments` — 36 dp pill inside a 48 dp touch band | `HomeScreen.kt` |
| grid 2 columns, 154x92, 12 dp gutter | `NativeServiceTile` at `NativeSpace.tile` with `NativeSpace.gutter` | `HomeScreen.kt` |
| recent activity y628 | `NativeRecentActivity` — real Room journal, max 3 rows | `HomeScreen.kt` |
| bottom nav 80 dp, 4 tabs | full-width card dock, 80 dp tabs, M3 selection plate | `KnouxBottomNavigation` |

Two deliberate departures, both recorded rather than hidden:

1. **The segment does not filter.** Selecting Clean / Manage / Protect promotes that group to the top
   and keeps every other service composed and scroll-reachable, because instrumentation and customers
   must both still find all eight services. Figma's static frame cannot express this; the app can.
2. **Eight tiles, not six.** The Figma grid shows six services; the shipped app has eight real ones.
   Dropping two would disconnect working services, which the directive forbids.

## 4. Honesty of the numbers

- Device capacity comes from `StatFs(Environment.getDataDirectory().path)` on `Dispatchers.IO`, no
  permission, wrapped in `runCatching`. If Android reports nothing, the ring becomes an empty track
  and both figures print `—` with the line "Android did not report a capacity."
- Every tile value still comes from a finished pass (`KnouxData.smartClean / residue / duplicates /
  privacy .latest()`); unmeasured stays `—`.
- Recent activity reads `KnouxData.smartClean.observeHistory()` — the same real Flow the Reports
  chart uses — filtered to SUCCEEDED / PARTIAL with `bytes > 0`. Empty means the card says
  "Nothing measured yet".
- No design-fixture percentage is shown anywhere on the new surfaces.

## 5. Verification actually run (Windows PC, no device)

| Command | Result |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL, exit 0 (after fixing 14 real errors from the first shell pass) |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL, exit 0, 1 m 29 s, 59 tasks |
| unit tests | 6 result files — **74 tests, 0 skipped, 0 failures, 0 errors** |
| instrumentation compile | `compileDebugAndroidTestKotlin` succeeds → all 17 androidTest classes still build against the new tags |
| APK | `app-debug.apk` 19,272,116 bytes, sha256 `d2fb1bf554db5a255411f34a…` |
| on-device UI / screenshots / permissions / Room runtime | **DEVICE_TEST_NOT_RUN at the time this table was written** — no device was connected then. Real captures from the API 26 `kx_preview` AVD exist from the brand pass; see §7 |

Transcripts: `K:\KNOUX-NATIVE-UI-EVIDENCE\COMPILE_SHELL_CHECKPOINT_20261114.txt`,
`COMPILE_HOME_20261114.txt`, `COMPILE_HERO_GATE_20261114.txt`, `VERIFY_NATIVE_UI_20261114.txt`.

Note on the two Gradle `--no-daemon` failures at 10:33: they died with `BindException` while forking a
single-use daemon (port contention with the writer's concurrent builds) and produced **no** verdict on
the code. Re-running with a shared daemon is what produced the real compiler output. A build that
never started is not a passing build.

APK size reads 19,272,116 B against the 16,811,060 B baseline figure recorded earlier. Only eight
text files changed and no asset, font or dependency was added, so the delta cannot come from this
pass; the baseline figure needs re-measuring before any size claim is trusted.

## 6. Contract preserved

Test tags asserted by instrumentation and kept present: `screen-home`, `start-scan`,
`open-clean-center`, `hub-duplicates`, `hub-residue`, `hub-storage-analyzer`, `hub-privacy-sweep`,
`hub-ai-optimization`, `hub-app-manager`, `hub-reports`, `quick-clean`, `quick-battery`,
`quick-privacy`, `tab-*`, `menu`, `pro`, `settings`, `state-*`, `technical-details`.
`design-preview` remains absent from the shell. No route, controller, permission or persistence call
was rewired: Home only changed presentation, grouping and its data bindings.

## 7. Brand fingerprint pass (2026-11-14 → 15)

The owner's finding was precise: the shell was correct but had **no fingerprint** — icons, inner cards,
section headers and motion were missing, and every service icon was the same flat blue square. This is
the pass that answered it.

### 7.1 The one rule that shaped everything

Four logo tones and nothing else: cobalt `#2F80FF → #0B63E8`, cyan, violet, magenta. Amber, red and
green stay **reserved for a measured warning or success** and are never used as decoration. Before this
pass `NativePalette.amber` was the Privacy icon tone; it is now cyan. A service tone appears in exactly
three places per card — the icon plate, one short top hairline, and the measured figure — so the grid
reads as one product instead of eight tinted cards.

### 7.2 Shared layer (`NativeBrand.kt`, new)

`BrandTone` (stable tone per service tag), `ServiceIconPlate` (gradient plate + lit top edge + glyph),
`BrandHairline` (one violet→cyan line, separator only, never a card border), `NativeScreenTitle`
(3dp logo-gradient bar + name + one muted sentence), `Modifier.brandClickable` (1.5 % press scale,
90 ms, keeps the platform ripple), `Modifier.enterRise` (10 dp rise + fade, 300 ms, staggered ≤240 ms,
skipped entirely when motion is off).

### 7.3 What each screen got

- **Home** — `NativeScreenTitle`; storage card on a cyan accent with a measured used/free bar that
  fills from the right in RTL; animated segment fill and stroke; a staggered entrance; 36dp icon
  plates, tone chevrons, 18→15sp measured values on a tinted action pill; 104dp tile minimum.
- **Smart Clean** — violet measured card with the honest `—` donut; group rows restyled on
  violet/cyan with the measured size moved onto the title line and a single checkbox at the end
  (the previous layout stacked a lone dash above the checkbox, which read as a stray mark); the two
  fixed actions now share one row instead of stacking two 48dp bars, and the status line is bare text
  instead of a third card; a hairline separates the pinned pair from the scrollable list.
- **Tools** — `ServiceRow` on `SurfaceCard(accent = tone)` with a 40dp plate and chevron.
- **Reports** — summary on cyan, empty panel on violet with a 52dp plate, `BrandHairline` between the
  copy and the single honest CTA; chart bars are a cyan→blue→violet gradient revealed once (520 ms)
  and never animated again.
- **Shell** — top bar gradient hairline; bottom nav gradient indicator plate plus a cyan top light;
  two wide, very quiet radial washes for depth.

### 7.4 Verification actually run

| Command | Result |
| --- | --- |
| `gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL, exit 0, 1 m 55 s — **7 suites / 80 tests / 0 failures / 0 skipped** |
| `adb -s emulator-5556 install -r -t app-debug.apk` | `Success`, exit 0 |
| launch + capture, API 26 `kx_preview`, 1080x1920 | Home AR, Home EN, Tools EN, Clean AR captured |

APK `app-debug.apk` **19,329,036 bytes**, sha256
`a80635f64b800bb37033f31a773e9d636544adb927ecb7c3df5db03bfcf0da7b`.

**Motion is proven by measurement, not by assertion.** `screenrecord` on a `-no-window` emulator writes
a single decodable frame, so eight `screencap` frames were pulled across one launch and diffed. Frame
`m7` carried the toolbar, section title and bottom nav but **no body**; `m8`, 0.7 s later, carried the
full body: **165,874 differing samples, intensity delta 18,939,733, max per-sample delta 697**. That is
the staggered fade/rise running. The gated APK was re-installed and re-captured and is identical to the
captures reviewed here (**177 differing samples of 640,000**, all of them the two live byte figures),
so the evidence applies to the build that passed the gate.

Motion is **not** proven for reduced-motion-off transitions between tabs — no recording exists, and the
press-scale/segment-colour animations were verified in code only.

### 7.5 What was checked and found NOT to be defects

- The back arrow already points right in Arabic; the logo has no dark plate (background pixel is
  exactly `#090A1E`); the storage bar fills from the right in RTL; the header places the menu and the
  logo at the start edge in both directions. All four were suspected from stills and were disproved by
  pixel inspection or by reading the code.
- The "last row is cut under the navigation bar" symptom was the honest scroll fold, not a layout bug:
  the `NavHost` is `weight(1f)` above the dock, and the final `Spacer(24.dp)` clears the last card.

## 8. Open work (not claimed as done)

- Gates 3/4 for the other services: each still needs its own signature instrument per
  `KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md` — the Deep Scan ring from `12:45` and the Reports
  summary+chart from `12:147`, the Battery gauge, the Duplicate media grid. **The shared shell makes
  them consistent; it does not make them designed.** Battery, App Manager and Storage still render the
  honest `EmptyServiceScreen` placeholder in the captured build.
- Visual parity cannot be signed off until the owner approves a real capture. The screenshots above are
  evidence of what was built, not approval of how it looks.
- The 3 skipped device cases are inapplicable on API 26, and API 35 / physical-device visual parity was
  not re-proven in this pass.
- The scratch worktree `K:\KNOUX-AI-NATIVE-UI-20261114` still has to be merged into canonical `main`
  and retired.
- Auth lane (Supabase + Google Credential Manager) untouched — deliberate, the owner chose UI first.
