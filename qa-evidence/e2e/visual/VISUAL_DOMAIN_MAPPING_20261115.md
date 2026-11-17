# KNOUX — VISUAL → DOMAIN MAPPING (Observatory pass, 2026-11-15)

Contract: `docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md` §7.4 and §9.
Owner's newest instruction (2026-11-15): the interfaces are authored from the agent's own
creative direction ("KNOUX Observatory / مرصد نوكس"), **not** pixel-matched to the ten PNG
references. The honesty, unique-work, device-safety and non-fake rules of the contract still
apply in full. This table is the binding between every visual attribute and the real domain
field it projects, plus the test/tag that proves it.

Capture status at authoring time: **PENDING device run** on the authorized Samsung Galaxy A02s
(`SM-A025F`, API 31, `armeabi-v7a`; serial supplied only via `KNOUX_TEST_DEVICE_SERIAL`, never
written here). Screenshots are attached only after a real capture; no fabricated states.

Shared atmosphere: `NativeAtmosphere.kt` — one night-sky field (52 seeded stars, aurora ribbons,
light pools) behind every native surface. Phase is fixed at `0.31f` when motion is off, so the
sky is a composed still frame under reduced motion. It carries no domain number.

| Service / route | Instrument (tag) | Visual attribute | Real domain field | Proving test / marker |
| --- | --- | --- | --- | --- |
| PC01 Smart Clean / `clean` | Tide `instrument-tide` | wave travel | `scanning` (only while a pass runs) | `SmartCleanRealTest`; motion off ⇒ still sea |
| | | grain lift height | `selectedBytes / measuredBytes` | fraction coerced 0..1; null before measurement |
| | | grain clusters | real measured group item counts | `grainCounts` from scan candidates |
| | | hollow dashed buoys | groups the pass could not measure | `fogCount`; never drawn as zero |
| | | listening light bar | `scanning && measuredBytes == null` | no percentage when indeterminate |
| PC02 Duplicates / `duplicates` | Mirror `instrument-mirror` | tether tension | `scopesDone / scopesTotal` (null ⇒ slack) | `DuplicateRealScreenTest`; `duplicate-mirror` |
| | | keeper orb sealed | original of each group is protected | keeper never enters a removal plan |
| | | hero numeral | `redundantBytes` only when `measured` | null before a READY pass |
| | | satellite pairs | `groups.size` (one pair per real group) | `groupCount` |
| | | fogged hollow orb | unmeasured / PARTIAL scope | `MirrorPhase.FOGGED/PARTIAL` |
| PC03 Residue / `residue` | Strata `instrument-strata` | root depth | item `lastModifiedMillis` (real age) | `ResidueRealScreenTest` |
| | | bulb colour | owned (green) vs confident (amber) | `ResidueConfidence` |
| | | digging beam | `scopesDone / scopesTotal` | null ⇒ no fabricated depth |
| | | fog band | `DataStatus.PARTIAL` | unmeasured layer stays hollow |
| PC04 Privacy / `privacy-risk-inspector` | Vault Radar `instrument-vault-radar` | 12 node colours | `PrivacyStatus` PASS/ATTENTION/UNMEASURABLE | `PrivacyRealScreenTest` |
| | | sweep wedge | `measuring` only | settles when the pass stops |
| | | centre gold seal | `secureWindow` verified flag | solid when verified, dashed open otherwise |
| | | dashed fog node | UNMEASURABLE check | never coloured as PASS |
| PC05 Deep Scan / `deep-scan` | Abyss Sonar `instrument-abyss-sonar` | echo rings / core density | measured module findings | `AbyssSonarBytesTest` (JVM) |
| PC07 Apps / `app-manager` | Constellation `instrument-constellation` | two orbits | user (inner/cyan) vs system (outer/violet) | `PreviewNavigationTest.appsScreen` |
| | | star position | deterministic package hash | stable across recomposition |
| | | star size | uniform (per-app size not readable) | no invented weight |
| | | focused star name | tap selection | `onFocus` |
| PC08 Storage / `storage` | Terrain `instrument-terrain` | contour radius | `sqrt(bytes / total)` per category | `PreviewNavigationTest.storageScreen` |
| | | category tone | images/video/cache measured bytes | `categoryTone()` |
| | | fog band | unmeasured used remainder | `unmeasuredGreaterThan()` |
| PC09 Battery / `battery-diagnostics` | Pulse `instrument-pulse` | ECG amplitude | `levelPercent / 100` | `PreviewNavigationTest.batteryScreen` |
| | | trace travel | charging status | travels while charging |
| | | flat dashed midline | `levelPercent == null` | never invents a percentage |
| PC06 Reports / `reports` | Prism `instrument-prism` | band thickness | `bytes / maxBytes` per completed cleanup | `completedCleanups()` (SUCCEEDED/PARTIAL, bytes>0) |
| | | one finite reveal | entry animation | settles, no loop |
| | | dim prism | empty history | honest empty state |
| PC10 Recommendations / `ai-recommendations` | Synapse `instrument-synapse` | lit node | a measured pass found something actionable | `PreviewNavigationTest.aiRecommendationsScreen` |
| | | axon solid vs dashed fog | lit vs unlit | unlit never sparks |
| | | accepted pulse | user accept (finite, once) | no idle firing |
| | | core glow | `anyLit` | dim when no evidence |

## Honesty invariants enforced in code

- Every instrument exposes `testTag("instrument-<service>")` and a `contentDescription` built
  from the same frame the visuals use (screen readers get the truth the eye sees).
- `progress == null` renders as a listening sweep / slack tether / flat midline — never a
  fabricated percentage.
- Unmeasured scope is fog: hollow, dashed, never counted, never coloured as measured.
- Motion is gated on `LocalPreviewMotion` (true only when resumed, not reduced, animators
  enabled, not power-save). Reduced motion ⇒ a composed still frame that still tells the state.
- A number appears once per surface; captions are micro-caps `Copy` (EN + AR), never engine prose.
- No preview art inside a real service state; the `DESIGN PREVIEW` pill stays on fixtures only.

## Verification gate status (to be completed on device)

- [x] one signature instrument per service, canvas-drawn, bound to real state (code)
- [x] reduced-motion still frame (code: `rememberMotionPhase` freezes to 0.5f)
- [x] fog/hollow for unmeasured scope (code)
- [x] no fabricated percentage when indeterminate (code)
- [ ] EN + AR copy-budget unit test result
- [ ] three real-state screenshots per service (idle / running / result)
- [ ] RTL result screenshot (instrument mirrored, numerals not mirrored)
- [ ] reduced-motion screenshot
- [ ] instrument perf audit (≤120 particles, no per-frame allocation, paused off-screen)