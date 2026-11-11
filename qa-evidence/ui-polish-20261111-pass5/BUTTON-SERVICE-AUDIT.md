# KNOUX AI — Phase 1 button & service-block audit (pass 5)

Date: 2026-11-11
Scope: `D:\KNOUX-AI` debug build, Android-native Compose UI, design-preview fixtures only.
Device: `emulator-5554` (`kx_preview`) · Android 8.0.0 / API 26 · x86_64 · 1080×2340 @ 420 dpi.

This audit covers every visible control and every content/service block in the ten outer screens,
the ten interior screens, the global chrome and all sheets. It records what each control does,
its accessibility role/state, and whether the surrounding block presents fixture data honestly.

## Method

1. Read every Compose source that renders chrome, screens, rows, sheets, bars and donuts.
2. Walked the uiautomator hierarchy of all 20 screens (`resource-id="screen-*"` verified before each
   capture) and checked clickable/selected/enabled state on tags.
3. Ran the connected suite: 8 navigation/state/RTL tests + 2 splash-sequence tests (10/10 passing).
4. Compared captured frames against the supplied references; reported density/geometry gaps below.

## Global controls

| Control | Tag | Role / state | Action | Verdict |
|---|---|---|---|---|
| Menu (Home only) | `menu` | Button, 48 dp, content-desc "Menu" | Opens "Explore KNOUX AI" sheet listing every page | PASS |
| Back (all non-Home) | `back` | Button, 48 dp, mirrored in Arabic | Pops the nav stack; falls back to Home | PASS |
| Design preview chip | `design-preview` | Button, persistent, pulsing dot when motion on | Opens the disclosure sheet: fixtures, no scan/cleanup | PASS |
| Pro badge | `pro` | Button, content-desc "KNOUX AI Pro", gold outline | Opens Pro sheet stating purchases/subscriptions are unavailable | PASS |
| Settings | `settings` | Button, 48 dp, content-desc "Settings" | Opens preview settings (language + motion) | PASS |
| Brand wordmark | — | Static, forced LTR | — | PASS (does not mirror in RTL) |

## Bottom navigation

| Tab | Tag | Role / state | Action | Verdict |
|---|---|---|---|---|
| Home | `tab-home` | Tab, selected pill + animated icon | Home | PASS |
| Clean | `tab-clean` | Tab | Clean index (Clean Center, Deep Scan, Duplicates, Residue) | PASS |
| Tools | `tab-tools` | Tab | Tools index (Storage, App Manager, Privacy, Battery, AI) | PASS |
| Reports | `tab-reports` | Tab | Reports | PASS |

Selected tab is a lit pill with a 260 ms fill/scale/color animation, and the pill is animated rather
than toggled. In RTL the row mirrors (verified by test) while Home remains the start destination.

## Sheets

| Sheet | Opened by | Controls | Verdict |
|---|---|---|---|
| Explore KNOUX AI | Menu | One full-width button per page + Pro; tapping closes the sheet and navigates | PASS |
| KNOUX AI Pro | Pro badge / Explore | Copy only + Close | PASS (explicitly unavailable) |
| Preview settings | Settings | English + العربية + motion toggle + Close | PASS |
| Design preview disclosure | Preview chip | Disclosure copy + Close | PASS |
| Item detail | Any row / category tile | Title, detail, value, note; Privacy adds a `toggleable` checkbox | PASS |
| Review / Proceed | Review + primary actions | Fixture list, optional per-item `PreviewChoice` checkboxes, Confirm preview | PASS |
| Preview finished | Confirm preview | "No action performed" / "No cleanup performed — UI preview only" | PASS |

Language sheet: both choices stay enabled; the active one is a `selectable` radio with a lit border
and check, the inactive one is an unselected ghost. Previously the inactive choice looked disabled,
which implied the language could not be changed. Motion toggle rewrites its own label.

## Home

| Control / block | Behaviour | Verdict |
|---|---|---|
| Hero globe + "AI Smart Clean" | 30 s globe phase (tilt/bob/squash), 19 s counter-rotating orbit layers, breathing bloom, glints; static when motion is off | PASS |
| Capability strip (Clean/Optimize/Protect/Manage) | Static labels, brighter glyphs | PASS |
| Start Deep Scan | Primary CTA, breathing glow + travelling shimmer + double chevron, opens Deep Scan | PASS |
| Device score panel | 92 % donut, animated sweep/ticks/glint, +6 % mint pill, 3 animated metrics | PASS (fixture, chip present) |
| Six service tiles | Clean → Clean Center; Duplicate Files → Duplicates; Storage → Storage; Privacy → Privacy; Battery → Battery; AI Optimization → AI Optimization | PASS |
| Tile icons | 9 s per-tile float offset; light pool + rim | PASS |
| Storage tile progress | Animated bar with travelling gloss; 128 GB / 256 GB / 50 % | PASS |

Home was re-densified in this pass so all six tiles and the metric panel fit inside the 1080×2340
viewport without the last row being clipped.

## Outer screens (10)

Common chrome on every screen: back, wordmark, preview chip, settings; Pro appears on Home and
detail routes. Hero + title + subtitle; primary panel; compact metric strip; summary panel; list
rows; sticky review/primary actions.

| Screen | Controls audited | Verdict |
|---|---|---|
| Privacy Sweep | 5 tracker rows with `Mark.PERSON` pills, select-all chip, review/proceed sheets | PASS |
| Duplicate Files Finder | 5 group rows with sizes, sparkline-free value column, review → Duplicate Group Review | PASS |
| Deep Scan | Pause/Continue state machine, 72 % donut + bar with live gloss, 5 module rows with progress, "Demo animation · no live scan", shortcut to Scan Summary | PASS |
| Reports & Insights | 5 report rows with animated sparklines, review → Performance Report Detail | PASS |
| App Residue Cleanup | 5 residue rows, select-all chip with live MB total, confirmation states "no cleanup performed" | PASS |
| App Manager | 5 app-group rows, 3.12 GB total, review → Installed Apps Review | PASS |
| Storage Analyzer | Capacity panel (128/256 GB), 6 category tiles with per-tile values, review → Storage Breakdown | PASS |
| Battery Saver | 5 rows, mint estimated-gain pill (tinted fill, dark text), review → Battery Diagnostics | PASS |
| AI Optimization | 5 opportunity rows, row toggle updates total, review → AI Recommendations | PASS |
| Clean index / Tools index | Full-width ghost buttons to every sub-screen | PASS |

## Interior screens (10)

| Screen | Controls / blocks audited | Verdict |
|---|---|---|
| Smart Clean Center | 2.86 GB donut, inline 3-metric strip, 5 selectable rows, live "n of 5 selected" + total, AI recommendation, estimated gain, review/clean sheets | PASS |
| Duplicate Group Review | 3 sample groups, Best badge, 4 thumbnails per group (tap opens the group review), group checkboxes, sort/filter/grid chips, keep-best/remove sheets | PASS |
| Privacy Risk Inspector | risk filters, reverse-risk chip, 5 rows with severity pills, per-row detail checkbox | PASS |
| Deep Scan Summary | 4 modules, timeline, suggested-next-actions, review/proceed | PASS |
| Performance Report Detail | 4 trend rows with sparklines, range strip, review/proceed | PASS |
| Residue Item Review | filter chips, 5 selectable items, dynamic total, "no cleanup performed" result | PASS |
| Installed Apps Review | 5 app rows with brand icons, Select All chip, storage/battery/permission metrics, manage/optimize sheets | PASS |
| Storage Breakdown | Donut + legend, 6 selectable category tiles (`Role.Tab`, `selectableGroup`), category detail, app breakdown bars, largest-files/free-space sheets | PASS |
| Battery Diagnostics | 5 app rows, drain ranking, time estimates, review/proceed | PASS |
| AI Recommendations | 94→98 panel, AI insight, 4 toggleable plan rows, notes/order, customize/apply sheets | PASS |

Storage row chevrons were removed because those rows do not navigate; the category tiles now expose
`selected=true` for the active category (confirmed in the uiautomator hierarchy). Duplicate photo
tiles are now clickable and open the same group preview as the row.

## Service blocks — disclosure check

| Block | Fixture language present | Verdict |
|---|---|---|
| Global preview chip | "Design preview" on every screen | PASS |
| Home metrics | Persistent chip + "vs last scan"; no claim of real cleanup | PASS (chip) |
| Scan progress | "Demo animation · no live scan"; paused copy "Sample progress stays at 72 %" | PASS |
| Clean Center | "Sample scan complete", "example files", "Safe preview — nothing is deleted" | PASS |
| Lists/totals | "sample", "example", "preview" in summaries and empty states | PASS |
| Sheets | Confirm → "No action performed" / "No cleanup performed — UI preview only" | PASS |
| Pro sheet | "Purchases and subscriptions are unavailable" | PASS |
| Settings | Language + motion only; no device permissions | PASS |

No block invokes a real scan, cleanup, privacy fix, battery optimization, file deletion, root,
Accessibility service, or Shizuku. No storage or system permission is requested.

## Defects found and fixed in this pass

1. Inactive language choice rendered like a disabled button and was actually disabled → both choices
   enabled, active one selected (`Role.RadioButton`), inactive one ghost.
2. Storage rows displayed a next chevron with no action → chevron removed.
3. Storage category tiles had button semantics but no selected state → `selectable` + `selectableGroup`.
4. Mint/gold pills rendered as purple → tint-aware fills with dark text for light tints.
5. Disabled CTAs collapsed into the same ghost glass as secondary buttons → dedicated disabled
   surface with muted border and text.
6. Duplicate photo thumbnails looked tappable but were inert → they open the group preview.
7. Home's last tile row clipped at rest → hero/tile/spacing density retuned; all six tiles now fit.
8. Arabic copy mixed Egyptian colloquial ("مفيش", "دي", "لحد") → MSA throughout ("لا توجد", "هذه", "حتى").
9. Motion was a single slow globe drift → layered 30 s/19 s/4.6 s clocks, counter-rotation, glints,
   CTA breath/shimmer, tab pill, donut/bar sweep, scan pulse, selection pop, page transitions.
10. CTA halo washed over adjacent content → outer bloom reduced while keeping the lit body/rim.
11. Long donut captions ("Device Score", "Privacy Score", "Match Confidence", "Scan Progress",
    "Used (128 GB)", "Battery Health") were clipped to one line → captions now wrap to two
    auto-fitted, centre-aligned lines.

## Remaining gaps (honest)

- Interior screens are denser than the reference mockups: the sticky action bar leaves 3–4 list rows
  visible at rest; the remainder is reachable by scrolling. No content is unreachable.
- Splash K face shading and smile width remain hand-traced approximations of the owner logo.
- The emulator uses SwiftShader; frame pacing on a physical GPU, audio and haptics are unverified.
- Photographs and app icons are bundled sample assets, not user data.
