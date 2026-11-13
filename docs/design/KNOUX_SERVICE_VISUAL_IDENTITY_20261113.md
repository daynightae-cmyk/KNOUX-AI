# KNOUX SERVICE VISUAL IDENTITY CONTRACT — 2026-11-13

Owner directive recorded before PC05 continues, in the owner's words:

> «اجعل شرط الخدمات… اجعل كل خدمة أثناء التنفيذ والإخراج أمام المستخدم تكون بـ UI هوية بصرية مبدعة…
> أفضل من الكتابة للنتائج. تكون قليلة الكتابة ولكن الـ UI لها يكون طاغي.»

Translation of intent: **every service must speak through a visual instrument, not through text.**
During execution and in the result state, the screen is carried by a creative, service-specific
visual identity. Text is a caption layer. It is never the content.

This file is the contract. It applies to **PC05 → PC13 in full**. PC01 → PC04 are **reviewed only**
(section 8); their accepted visuals are preserved and are not rebuilt in this pass.

---

## 1. The law (non-negotiable)

1. **The instrument is the interface.** Each service owns exactly one signature instrument — a
   canvas-drawn, live object that embodies how that service actually works. It is the largest
   element on screen and the first thing the eye lands on.
2. **Text is caption, not content.** No paragraphs, no logs, no repeated counts, no explanatory
   sentences in a service surface. Numbers, one verb, one state word.
3. **Every visual attribute is bound to a real domain field.** Colour, fill, motion and glyphs are
   projections of measured state. Nothing decorative may imply work that did not happen.
4. **Unknown is drawn as unknown.** Unmeasured scope = fog / hatch / hollow. It is never coloured
   as measured, never counted, never called clean.
5. **Motion means work.** Movement exists only while the service is actually working. When work
   stops, the instrument settles and becomes still. No perpetual "scanning" theatre.
6. **The instrument never replaces control.** Buttons, confirmation and undo stay reachable and
   accessible. Minimal text must not become minimal agency.
7. **Both languages are first-class.** EN and AR must each satisfy the text budget independently.
   Instruments mirror under RTL where direction carries meaning; numerals never mirror.
8. **Reduced motion is a first-class state.** With motion off, shape + colour + number must still
   tell the whole truth. Animation is an enhancement, never the only carrier of meaning.
9. **A design fixture is labelled.** Any preview/sample value renders under the existing
   `DESIGN PREVIEW` pill. A real service state never borrows preview art.
10. **PC05 is the first service born under this law.** It cannot be marked VERIFIED without the
    gate in section 9.

---

## 2. Text budget

Counted per screen, both languages, excluding OS dialogs and including chip labels.

| State | Max words on screen | Allowed text |
| --- | --- | --- |
| Idle | 10 | service name, one status word, one primary action |
| Armed (permission / scope) | 12 | what is needed, why, one action |
| Running | 8 | one live metric, one phase word, cancel |
| Paused / cancelled | 10 | state word, resume, discard |
| Result | 16 | one hero number, ≤ 3 stat chips, one primary action |
| Partial | 16 | hero number + the word "partial" + what is missing (≤ 5 words) |
| Failed | 12 | one plain cause (≤ 8 words), retry |
| Undo window | 10 | what can be undone, undo, dismiss |

Rules that follow from the budget:

- A number appears **once**. If the summary card and the status strip both say "12 files", one of
  them is deleted.
- Lists show **one label per item at rest** (name or size, not both). The second value lives in the
  detail sheet.
- No sentence longer than 8 words anywhere in a service surface.
- Counts are tabular numerals in `DisplayFace`; labels are micro-caps at 10–11 sp, letter-spaced.
- Arabic copy is written natively, not translated word-for-word, and must fit the same budget.

---

## 3. State choreography (shared by all instruments)

| Phase | Visual behaviour | Honesty rule |
| --- | --- | --- |
| `IDLE` | slow breath (0.08–0.15 Hz), instrument dim but present | no numbers except last real result, labelled with its age |
| `ARMED` | instrument aims at the missing source; one beam / one open ring | no progress before the first measured unit |
| `RUNNING` | cadence tied to real throughput; facets appear per real finding | `progress` is nullable; if unknown, draw a listening sweep, **never** a percentage |
| `PAUSED` | freeze frame; the last facet stays lit | frozen frame is the truth at pause time |
| `RESULT` | one settle animation (≤ 450 ms), then still; hero numeral | only measured values |
| `PARTIAL` | one facet stays fogged/hollow, never filled | missing scope is named, not hidden |
| `FAILED` | single crack / desaturation pulse, then still | plain cause, no euphemism |
| `UNDO` | reverse motion of the original action | reverse only what the provider confirms |

Cadence rule: the instrument may breathe while idle, but **only real completions may advance it**.
If a provider call hangs, the instrument holds its last true frame; it never creeps forward.

---

## 4. Shared visual system

Base (already in the app): ink `#07051A`, panel `#0D0935`, glass `#1A1642`,
cyan `#00E8FF`, magenta `#FF54FF`, violet `#8B5CF6`, gold `#FFC24B`, mint `#3DFFB0`.
Each service takes **one dominant hue** from this spectrum; the rest of the UI stays quiet so the
instrument owns the screen.

- Geometry: instruments are circular, radial or columnar — never rectangles. Cards remain only for
  controls and sheets.
- Light: one local light source per instrument; glow is earned by state, not decoration.
- Depth: glass blur is allowed behind instruments; no drop shadows on canvas.
- Type: `DisplayFace` for numerals only. Arabic uses the system family with heavier weight.
- Sound/haptics: one tick on phase change, off by default; no loops.
- Particles: ≤ 120 live particles per instrument; zero allocations inside `DrawScope`; animation
  paused when the screen is not resumed.

---

## 5. The fourteen identities

Each identity is a *mechanism made visible* — the metaphor is the service's actual physics, not a
random skin.

| Task | Identity | Arabic | Metaphor = real mechanism | Dominant |
| --- | --- | --- | --- | --- |
| PC00 | **Bedrock** | الأساس | a calm grid assembling into a foundation | violet `#8B5CF6` |
| PC01 | **Tide** | المدّ | debris lifted by a tide, grains dissolve when the provider confirms removal | cyan → cobalt |
| PC02 | **Mirror** | المرآة | twin orbs on a tether; choosing one collapses the twin into the keeper | magenta / violet |
| PC03 | **Strata** | الطبقات | soil cross-section; roots of uninstalled apps pulled out of their layers | amber / rust |
| PC04 | **Vault Radar** | رادار الخزنة | radar sweep over 12 real check nodes; a gold seal clicks when the window flag is verified | cyan + gold |
| PC05 | **Abyss Sonar** | سونار العمق | descending sonar into storage depth; each echo is a real finding | abyss teal |
| PC06 | **Prism** | المنشور | history split into spectral bands; band width is a measured metric | spectral |
| PC07 | **Constellation** | الكوكبة | apps as stars; orbit = recency, radius = size, gravity = usage | starfield |
| PC08 | **Terrain** | التضاريس | topographic map of storage; contour density = measured bytes | contour mint |
| PC09 | **Pulse** | النبض | ECG trace; amplitude = real drain, interventions flatten the line | ember → cyan |
| PC10 | **Synapse** | المشبك | a neural mesh where each recommendation is a synapse that lights only when accepted | violet / magenta |
| PC11 | **Iris Gate** | بوابة القزحية | iris aperture for auth; home is an orbital hub of service satellites | gold |
| PC12 | **Seal Chain** | سلسلة الأختام | entitlement seals on a chain; a seal is solid only after Play verification | gold / emerald |
| PC13 | **Gate** | البوابة | fourteen arches; each lights from a real evidence row; the last gate opens only when all are lit | white-gold |

### PC05 — Abyss Sonar (the first full implementation)

- **Instrument:** a vertical depth column with a radial sonar head. Depth is real scan progress;
  each completed module emits one echo ring that condenses into a luminous facet on a core orb.
- **Modules:** sonar sectors, one per selected scan module (media, documents, archives, app residue,
  duplicates…). Selecting a sector aims the beam; deselecting clears its echo history.
- **Running:** beam descends; echoes appear only when a module returns measured items; the core orb
  grows by measured bytes, never by time.
- **Result:** one core orb whose internal density = findings by module. The hero numeral is bytes
  measured. Tap a facet → sheet with item details. Export evidence = "echo print" (real file/hash).
- **Partial:** unmeasured modules stay fogged at the bottom of the column and are named once.
- **Forbidden:** the words "malware", "virus", "threat" for unflagged files; any percentage when
  progress is indeterminate; any finding without provenance.
- **Text at rest:** depth numeral, findings numeral, one verdict word, one action.

### PC06 — Prism

Prism at the leading edge, spectral bands fanning across the screen. Band width = measured metric
per period. Scrub the timeline to move the light. A report is a band, not a card. Detail opens in a
sheet. No tables in the main surface.

### PC07 — Constellation

Apps as star nodes. Radius = installed size, orbit = last-used recency, glow = user attention.
Focus one star to read its name; never a wall of labels. Moving a star to the trash orbit is the
uninstall/disable action, and it dims only after the OS confirms.

### PC08 — Terrain

Contour topography; each ring is a measured directory depth. Pinch to descend, tap a ridge for the
sheet. Unmeasured directories render as fog — the map is honest about its own blank regions.

### PC09 — Pulse

A single ECG line. Amplitude is real drain rate; the target line is the user's chosen profile. An
applied optimisation is a notch that visibly flattens the trace afterwards. Never invent a battery
percentage.

### PC10 — Synapse

A mesh that grows one synapse per recommendation. Accepting lights it; rejecting dims it. The
screen says plainly whether a rule is on-device or model-backed. No "AI magic" claims.

### PC11 — Iris Gate / Home hub

Auth is an iris aperture that opens on real credential success. Home is an orbital hub: service
satellites sized by real pending findings, not fake badges.

### PC12 — Seal Chain

Each entitlement is a seal. Hollow = unverified, solid gold = Play-verified, broken = revoked.
A purchase mints a seal only after the Play response and server receipt agree.

### PC13 — Gate

Fourteen arches, one per PC task. Each lights from its evidence row in the manifest/ledger. The
final arch opens only when every prior arch is lit. No manual override.

---

## 6. Instrument contract (implementation)

New package: `com.knoux.ai.visual`.

```kotlin
enum class InstrumentPhase { IDLE, ARMED, RUNNING, PAUSED, RESULT, PARTIAL, FAILED, UNDO }

data class Facet(
    val id: String,          // real domain id, used by the detail sheet
    val weight: Float,       // measured share, never guessed
    val state: FacetState,   // MEASURED, FOGGED, BLOCKED, REMOVED
    val label: Copy?         // optional, max 2 words
)

data class InstrumentFrame(
    val phase: InstrumentPhase,
    val progress: Float?,    // null = indeterminate -> listening sweep, not a percentage
    val heroValue: Long?,    // measured bytes / items / score
    val facets: List<Facet>, // max 12 visible
    val motion: Boolean,     // reduced-motion flag
    val caption: Copy        // ≤ 4 words
)
```

Rules:

- The ViewModel exposes `InstrumentFrame` derived from the same `StateFlow` the tests assert on.
  There is no second source of visual truth.
- Instruments are `Canvas` composables with `testTag("instrument-<service>")` and a state
  `contentDescription` generated from the frame (so screen readers get the truth the visuals show).
- `progress == null` must render as a sweep with no arc fill and no numeral.
- A `FacetState.FOGGED` facet is drawn hollow with a dotted rim and can never be tapped as if real.

---

## 7. Visual acceptance per service (evidence)

For every task from PC05 on, attach to the evidence folder:

1. `visual/idle.png`, `visual/running.png`, `visual/result.png` — real device screenshots, masked
   frame only, no fabricated states.
2. `visual/rtl-result.png` — Arabic locale, result state.
3. `visual/reduced-motion.png` — motion off, running or result.
4. A one-line mapping table: `visual attribute → domain field → test that proves it`.
5. A copy-budget unit test result for EN and AR.

Screenshots come from the authorized device only, sanitized of serial/transport data.

---

## 8. Review of PC01 → PC04 (review only, no rebuild)

Per the owner's wording, existing services are **reviewed, not rebuilt**. This is a code-level
review; no visual acceptance is claimed because no new screenshots were taken in this pass.

| Service | Current real surface | Gap against this contract | Decision |
| --- | --- | --- | --- |
| PC01 Smart Clean | summary card + donut + status strip + group rows + review sheet | Tide instrument exists only as preview art; execution is a bar, not a tide; repeated counts across card and strip | keep; pay down at next touch |
| PC02 Duplicates | summary card + group cards + review sheet | no Mirror tether/filmstrip; group cards are text-first | keep; pay down at next touch |
| PC03 App Residue | summary + age card + audit card + item cards | no Strata core; age is a chip row rather than depth | keep; pay down at next touch |
| PC04 Privacy Sweep | score donut + 12 rows + secure card + audit card | radar is absent; rows carry the 12 checks that should be nodes | keep; pay down at next touch |

What is already right and must survive: the `DESIGN PREVIEW` pill on fixtures, the explicit
"real permission sweep, not a preview" statement, measured-only numbers, and the bottom-sheet
detail pattern.

**Identity debt** is tracked here; it does not block PC05, and it is not a licence to delete
accepted visuals. Any future change to these four screens must move them toward the contract,
never away from it.

---

## 9. Verification gate (added to the per-service pipeline)

A service cannot move to VERIFIED unless all of these are true:

- [ ] one signature instrument, canvas-drawn, bound to the real `StateFlow`
- [ ] text budget test passes for EN and AR (unit test on the copy model)
- [ ] `progress == null` renders without a fabricated percentage (test)
- [ ] fogged/hollow state exists and is used for unmeasured scope (test)
- [ ] reduced-motion frame still communicates the state (screenshot + test)
- [ ] RTL result screenshot (instrument mirrored, numerals not mirrored)
- [ ] three real-state screenshots attached to evidence
- [ ] visual → domain mapping table attached
- [ ] no preview art inside a real service state (grep + test)
- [ ] instrument perf: ≤ 120 particles, no per-frame allocation, paused off-screen

PC05 is the first task judged by this gate.
