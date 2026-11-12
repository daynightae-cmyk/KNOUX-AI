# KNOUX AI — Final Visual Gate (VG00–VG06) Task Ledger

Realm: `D:\KNOUX-AI` (Windows PC workspace). The Linux phone workspace is NOT touched.
Scope lock: `VISUAL_FINISH_ONLY`. No service/Pro/Room/Supabase/E2E implementation in this lane.

---

## VG00 — Baseline & Reference Lock — IN PROGRESS

### Repository truth (measured, not assumed)
| Fact | Value |
|---|---|
| Branch | `main` |
| HEAD at gate start | `08c6e38e62f307fc7a4fb5176f4226257ab17865` |
| `origin/main` (local tracking ref) | `e5b0994555aa181d00d9f75f292e4ead4b602655` |
| Divergence | local `main` ahead by 3 commits, not verified pushed |
| Worktree | DIRTY — tracked edits + untracked work preserved |
| Disk free at gate start | D: 2.76 GB, C: 9.41 GB, K: 5.59 GB |

### Tracked files modified before this gate (pre-existing, preserved)
`AGENTS.md`, `app/src/main/java/com/knoux/ai/MasterScreens.kt`,
`app/src/main/java/com/knoux/ai/NeonArt.kt`, `app/src/main/res/values-v31/styles.xml`.

### Protected untracked work (must never be cleaned/reset)
`.agents/`, `ProSystem.kt`, `docs/pro/*`, `docs/workflows/*`, `prompts/*`,
`database/supabase/drafts/004_pro_entitlements_DRAFT.sql`,
`qa-evidence/**` (all prior evidence including `preview-live/` and `service-worlds-20261111/`),
`scripts/validate-pc-e2e-workflow.ps1`.

### Reference asset reality (honest finding)
- The supplied `REFERENCE_MANIFEST.md` states the 12 originals live in `references/`. **`D:\KNOUX-AI\references\` does not exist.**
- A recursive search of `D:\` for `01_ONBOARDING_AWAKENING*` returned no match.
- The 12 original PNGs are present in the owner's `Downloads` folder with Arabic display
  filenames, and all 12 were also supplied directly in-session at full resolution.
- Decision: design against the in-session originals + `REFERENCE_MANIFEST.md` role table.
  **No duplicate copy of ~20 MB of PNGs was written to D:**, because D: has only 2.76 GB free
  and the contract explicitly forbids bulk asset duplication and ZIP backups.
- Consequence recorded: reference SHA-256 values cannot be re-verified against an on-disk
  `references/` tree in this gate. This is logged as a known gap, not hidden.

### Reference → route authority map (from manifest)
| Ref | Role | Route / state |
|---|---|---|
| 01 | Onboarding ch.1 AR — mandatory composition | Chapter 1 |
| 02 | Onboarding ch.2 AR — ten connected services | Chapter 2 |
| 03 | Onboarding ch.3 AR — bilingual creator card | Chapter 3 |
| 04 | English locale equivalent — **NOT a 4th chapter** | Chapter 1–3 EN copy |
| 05 | Login style variant (security badge) | Login conditional |
| 06 | Login compact alternative | Login conditional |
| 07 | Login biometric/passkey choices | Login conditional |
| 08 | **PRIMARY login authority** | Sign-in |
| 09 | OTP variant, conditional biometric | OTP conditional |
| 10 | OTP filled state (digits are illustrative only) | OTP filled |
| 11 | OTP envelope variant | OTP variant |
| 12 | **PRIMARY empty OTP authority** | OTP empty |

---

## VG01 — Global UI geometry — IN PROGRESS

Defect from independent QA (`qa-evidence/e2e/ONBOARDING_QA_AUDIT_20261112.md` §22):
onboarding top Skip/Back collided with status-bar icons because the root used
`fillMaxSize()` with no inset padding.

Correction applied (structural, not pixel nudging):
- `WindowInsets.safeDrawing` applied **exactly once**, on the onboarding root column.
- Top actions are now 48dp-tall hit targets with small 14sp labels inside them.
- Chapter bodies are adaptively scrollable so short viewports / large font scale do not clip.
- Status bar is never hidden and no arbitrary top pixel offset is used.

---

## VG02 — Onboarding exact visuals — IN PROGRESS

`OnboardingScreen.kt` rewritten to the reference composition while preserving:
- existing `knoux_prefs` / `knoux_onboarding_done` first-run semantics,
- `BackHandler` chapter behaviour,
- the live `Page` service identities (no invented services),
- the official KNOUX K (`Glyph(Mark.K)` draws the brand gradient mark).

Implemented: brand lockup + lit underline, chapter 1 hero K orb with orbital ribbons and
pedestal, outline subtitle capsule, chapter 2 radial ten-service orbit around the
"intelligence nucleus" with energy filaments, chapter 3 bilingual founder glass card with
`knoux.store` link, gradient CTA pill with chevron, three animated progress dots.

---

## VG05 — Runtime capture, iteration 1 (device `1334825549012211`, itel S686LN, API 35)

First capture pass produced 8 portrait PNGs (1080×2436, density 480) in
`qa-evidence/final-visual-gate/runtime/`. Screens inspected at full resolution,
including 2× nearest-neighbour crops of suspicious regions.

### Defects proven by the captures (not guesses)
| # | Screen | Defect | Root cause | Status |
|---|---|---|---|---|
| D1 | Ch.2 | Top service tiles drawn over the "10 خدمات ذكية…" subtitle | `ORBIT_SLOTS` first/last row at `fy` 0.055/0.945 pushed cards ±46dp outside the orbit box, which then overflowed over the subtitle | FIXED — rows moved to `fy` 0.100/0.900 (card containment: `0.10·h − cardH/2 = +2.25dp`, `0.90·h − cardH/2 → bottom 477.75 < 480`) |
| D2 | Ch.2 | The K nucleus rendered in the **top-right**, on top of the right-hand tiles | `Nucleus(modifier)` declared a `modifier` parameter but built `Box(Modifier.size(150.dp))`, silently discarding `Modifier.align(Alignment.Center)`. In RTL the fallback `TopStart` resolves to the top-**right**. Verified arithmetically against the capture: predicted centre (819, 813) px, measured (815, 813) px | FIXED — `Box(modifier.size(150.dp), …)` |
| D3 | OTP | Countdown rendered as `••:0V` instead of `00:59` | `"%02d:%02d".format(…)` is locale-sensitive and the handset locale is Arabic, so `%d` emitted Arabic-Indic numerals `٠٠:٥٩`; **Arabic-Indic zero is a bare dot** | FIXED — `String.format(Locale.ROOT, …)` |
| D4 | OTP | Subtitle asserted "sent to your email" with no mail provider | Reference-12 copy contradicted the honesty rule | FIXED — copy states only the true instruction; layout unchanged. Deliberate, recorded deviation from the reference string |
| D5 | All | Device had rotated to landscape (`cur=2436x1080`) between runs | Auto-rotate | FIXED — capture script forces `accelerometer_rotation 0`, `user_rotation 0` |
| D6 | Ch.1–3, Login, OTP (AR) | Directional glyphs not mirrored: the back control showed `←` and the CTA chevron showed `>` in Arabic, i.e. "next" pointed back toward the text | `Glyph()` has no RTL awareness and only *some* call sites applied their own `.rotate(...)`; the onboarding and auth call sites did not | FIXED — mirrored `Mark.BACK` / `Mark.NEXT` at the five in-scope call sites, matching the convention already used by `IconControl` |

### Verified PASS in iteration 1
- No status-bar collision on any screen; Skip/Back clear of the clock and icons.
- Login shows **no** giant central fingerprint/iris graphic and **no** "Preview Build" badge.
- Login fields are real editable `BasicTextField`s with an eye toggle.
- Google / passkey rows are present and route to truthful "not configured" sheets.

## Checkpoints still open
- VG03 Login primary (ref 08) + conditional 05/06/07
- VG04 OTP (ref 12 primary, 09/10/11 conditional)
- VG05 Runtime capture on the owner-authorized handset `1334825549012211` (debug APK, visual QA only)
- VG06 Safe convergence + owner visual acceptance

## Owner authorization — physical device (2026-11-12)
The owner messaged that the handset was connected, then, when asked to choose the QA target,
selected the option **«أصرّح بتنصيب نسخة الاختبار على هاتفي»** — an explicit, separate
authorization to install the **debug** test APK on `1334825549012211` (itel S686LN) for visual QA.
Scope of that authorization:
- debug/test build only; never a Play production-release claim;
- visual QA only, not end-to-end service verification;
- no owner data is cleared — the onboarding capture route is debug-gated and
  **does not write `knoux_onboarding_done`**.
The `knoux_qa26` AVD was started for the same purpose but exited on its own, so the
authorized handset is the active capture target.

## Standing rules for this gate
- Never force-push, hard reset, `git clean`, or delete untracked work.
- Never claim BUILD PASS as visual acceptance.
- Never hardcode OTP digits, nor imply an email was sent without a real provider.
- Release APK remains debug-signed internal test only.
