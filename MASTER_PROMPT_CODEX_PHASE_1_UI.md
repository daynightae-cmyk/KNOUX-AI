# KNOUX AI — SUPREME VISUAL RECONSTRUCTION COMMAND
## CODEX EXECUTION MISSION — PHASE 1 ONLY / ANDROID NATIVE / TEN EXACT REFERENCES

**Mission date:** 2026-11-10  
**Requested local project root:** `D:\KNOUX-AI`  
**App display name:** `KNOUX AI`  
**Workflow:** ONE comprehensive instruction, ONE executable UI-focused mission, then a HARD STOP for owner visual approval before any real service implementation.  
**Input references:** `design/reference/01_AI_Smart_Clean_Home.png` through `10_AI_Optimization.png`, supplied in the companion ZIP.

---

## 0 — ABSOLUTE DIRECTIVE / START EXECUTING

You are Codex acting as Principal Android Engineer, Senior Jetpack Compose Architect, Pixel-Accuracy UI Director, Motion Designer, QA Automation Engineer, Git Forensics Engineer, and Windows Build Environment Specialist. This is **an implementation mission**, not a theoretical proposal. Inspect the real machine and repository, then **write code, build, run, inspect screenshots, correct discrepancies, and deliver evidence**. Do not stop after a plan; do not ask routine questions. You may make technically sound decisions that preserve the supplied references. **STOP immediately after the ten-screen UI milestone and safe in-scope cleanup; do NOT implement any scanner, cleaner, backend, database, cloud calls, subscriptions, privileged operations, or destructive data actions in this run.** Those will be individually specified and executed end-to-end by the owner in later sessions.

Priority order when requirements conflict:
1. Data safety, existing unique work, and concurrent agent protection.
2. Native Android runtime truth and accessibility.
3. Fidelity to the TEN actual reference images, not generic aesthetic preferences.
4. Complete and correctly navigable visual UI, including honest preview interactions.
5. Clean and economical implementation with evidence.
6. Reclaim disk space **only from proven disposable material and only after preservation checks**.

This phase has **zero authorization** to clean the owner's media or phone, request privileged Android access, connect to Supabase, or pretend any metrics are real. The reference figures are design fixtures, not device measurements.

## 1 — MACHINE / DISK / REPOSITORY FORENSICS BEFORE FIRST WRITE

1. Identify whether this Codex session has genuine access to the owner's **Windows PC**, PowerShell, local `D:` drive, Android SDK, emulator, and project files. If running in a remote/cloud workspace with no Windows D: drive, **do not claim you created `D:\KNOUX-AI`**. Work only in the actually accessible workspace, produce a portable Android project, report precisely where it was created and how to put it onto D:. Prefer a connected authorized PC execution environment when available. Do not invent shell output.
2. In PowerShell check `Get-PSDrive D`, `Get-Volume -DriveLetter D`, free-space/size and file-system health as available. Verify `D:\KNOUX-AI` does not conflict with another application. Inspect `D:\KNOUX-AI`, `D:\Android\sdk`, `D:\.gradle`, and `D:\Temp` **read-only** if they exist; discover actual `ANDROID_HOME`, `ANDROID_SDK_ROOT`, JDK/Gradle paths, Windows Android Studio/SDK installation. Reuse validated caches/SDKs; **never duplicate an entire Android SDK just to make a build pass**. A low-free-space situation is a blocker for heavyweight installs, not permission to delete unrelated projects.
3. If there is an existing `D:\KNOUX-AI` project, treat it as an irreplaceable possible product. Inspect before edits:
   - `git rev-parse --show-toplevel`, `git status --porcelain=v1 -uall`, `git branch --show-current`, `git rev-parse HEAD`;
   - `git remote -v`, `git ls-remote origin refs/heads/main` when authorized and reachable, `git branch -avv`, `git worktree list --porcelain`, `git stash list`, `git reflog -n 30`, recent commits and PRs when connector/auth permits;
   - ignored/untracked files, staged changes, parallel agents, live Gradle processes, and files authored by other tools;
   - actual implementation, app IDs, visual artifacts, tests, CI, readmes, and any existing approved components.
4. If the target is absent and no existing same-app project is discovered, create exactly **one canonical project** at `D:\KNOUX-AI`, initialize Git on `main` if appropriate, and build from there. Do not clone or recreate unrelated KNOuX projects (KX Private VPN, KNOuX Crypt, SmartOrganizer, or other active work). If an earlier same-app repository exists elsewhere, do not abandon or overwrite unique work: report and reconcile safely within available permissions.
5. If the target exists and is not clean, first inventory ownership and unique work; preserve it by selective integration. Do not overwrite uncommitted edits, rebase or switch another agent's active branch, or assume a stale-looking branch is disposable. If a safe merge of a proven unique commit is possible, use a reviewable non-destructive merge/cherry-pick; check build and identity before remote sync. Avoid broad bulk merge and avoid making concurrent agents race on `main`.
6. **NEVER** use `git reset --hard`, `git clean` (including `-fdx`), forced pushes, history rewriting, destructive branch/worktree deletion, or deletion of an unknown folder. Do not run `Remove-Item -Recurse` on a parent directory on guesswork. Do not alter Git repositories other than the confirmed KNOUX AI repository.
7. Before implementing, report briefly: actual workspace, free D: space, SDK/JDK, Git HEAD/origin/main if available, dirty/unique work status, and the next safe action. Then proceed with implementation—do not stop at the report.
8. For disk space, store the source and project-owned evidence on D:, but do not replicate huge dependencies. Use the validated installed SDK wherever it currently exists; if the free space is insufficient for build/emulator, optimize only approved project-local disposable outputs first and explain any unresolved limitation.

## 2 — EXACT INPUTS, VISUAL IDENTITY & ASSET RULES

First locate the companion `knoux_ai_codex_kit` folder or `KNOUX_AI_CODEX_UI_PHASE1_KIT.zip` in the authorized working directory (or user Downloads if the owner placed it there). If zipped, unpack safely to a project-owned temporary staging directory on D: **without overwriting any existing app files**; copy the ten original PNGs into the canonical project `D:\KNOUX-AI\design\reference\` only after target checks. If the images are absent or the agent cannot read their actual pixels, **do not invent that the reference images were inspected**: report the precise missing-input blocker. Do not substitute online lookalikes or discard unique assets.

Read and view **all ten** individual PNGs before writing components. They are 864×1536 rendered marketing mockups with a device shell. **The physical bezel, phone side buttons, hole-punch camera, marketing backdrop, and external phone frame are NOT the app UI**. The actual Android app comprises the interior content plus Android-managed system bars. Do not draw a fake handset, camera hole, status bar clocks, or gesture navigation handle over the OS. Establish the effective display viewport by cropping/alignment for screenshot comparisons. Do not simply place the whole PNG, a sliced PNG, or a flattened screenshot as the background of a functional screen. Every card, label, button, list item, tab, value, progress indicator and hit target must be genuine responsive Compose elements.

Asset preparation:
- Reference filenames and UI sequence must remain stable and unchanged.
- Build a visual inventory and geometry notes at `docs/visual/reference-matrix.md` for every image: usable screen crop bounds, aspect ratio, header, hero, each information panel, list geometry, CTAs, bottom tab, fonts, colors, glow layers, icon sizes, spacing. Note uncertainty rather than invent pixel values.
- Source a permitted font family close to the references, bundle locally with proper license, and implement weights/italic styles. Never take licensed fonts from private system locations without permission.
- Create a faithful custom vector recreation of the branded gradient K mark if an official source asset is unavailable. Retain the precise `knoux ai` wordmark silhouette, relative kerning, gradient identity, and placement. Prefer an existing approved official logo over an approximate redesign. Do not introduce unrelated mascots, KX shield logos, or other KNOuX sub-brand identities.
- Hero graphics must use editable layers where reasonable: Compose Canvas, vector paths, shaders/gradients, radial glows, subtle animated orbital rings, and custom icons. You MAY use separately authored original hero illustration assets for rich 3D artwork if needed, but do not embed an entire reference screen as an image. If a hero is raster art, keep the actual information/UI live around it; use proper image licenses and source documentation.
- Design language: deep midnight navy/purple nearly black background, electric purple/violet and neon cyan/blue with delicate magenta/rose highlights, layered purple wave fields, soft bloom, luminous angular and circular borders, indigo-tinted glass cards, restrained outer glows, high contrast white labels, lavender emphasized metrics, cyan primary action pills, magenta–cyan two-tone score arcs, hairline separators, premium cinematic lighting.
- Define semantic tokens (do not blindly hardcode these until sampled from the screenshots). Suggested starting **candidate** palette: backdrop `#09051B`/`#100A2A`; elevated indigo `#1C1545`; primary violet `#8F30FF`; magenta `#E040FB`; cyan `#03DAFF`; text `#F6F3FF`; soft text `#C7BDE5`. Refine with sampled reference pixels. Keep contrast accessible.
- Prefer smooth fine outlines over thick cartoon strokes. Preserve reference cards' heights, glow softness, typography scaling, white-space balance, hero proportions, horizontal splits and primary bottom CTA placement.
- Keep a distinctive hero visual for each tool; do not use the same generic Material symbol as every hero. Screen-specific 3D-like neon marks are detailed below.

## 3 — IMPLEMENTATION STACK & APP ARCHITECTURE

**Native Android Kotlin + Jetpack Compose**, Gradle Kotlin DSL, current compatible stable versions selected from the actual installed toolchain, AndroidX Navigation Compose, Material 3 used only underneath bespoke styling, lifecycle-aware ViewModels, Coroutines/Flow for local UI state, accessible semantics, and an explicit preview fixture data repository. No Flutter, React Native, WebView imitation, JavaScript front-end, or website as a substitute. No compulsory Hilt/Room/Supabase/WorkManager in the visual-only phase unless already present and proven needed; avoid unnecessary dependencies on low disk space.

Suggested structure, adapt sensibly to existing source:

```
D:\KNOUX-AI\
  app\src\main\...
  app\src\test\...
  app\src\androidTest\...
  design\reference\01_...png  ... 10_...png
  docs\visual\reference-matrix.md
  docs\visual\component-inventory.md
  docs\architecture\phase-2-capability-contracts.md
  qa-evidence\ui\[all-10-runtime-screenshots]
  qa-evidence\ui\[reference-crops-and-comparisons]
  qa-evidence\build\[command-results]
  AGENTS.md
  README.md
  PHASE_1_HANDOFF.md
```

Required reusable Compose primitives:
`KnouxTheme`, `KnouxAppScaffold`, `KnouxTopBar`, `KnouxBrandMark`, `NeonHero`, `NeonOrbRings`, `ParticleWaveBackdrop`, `GlassPanel`, `GlowEdge`, `GradientActionButton`, `OutlinedGhostButton`, `MetricPairPanel`, `MiniMetricStrip`, `ProgressDonut`, `GradientProgressBar`, `StatusSummaryPanel`, `CategoryCard`, `FeatureRow`, `SelectableFindingRow`, `KnouxBottomNavigation`, `PreviewModeDisclosure`, `EmptyState`, `CapabilityUnavailableState`, `ConfirmDialog`, `InfoSheet`, `Success/Failure visual state templates`.

One data source type represents immutable **design fixture content**; one type represents future **unavailable real capability**, both clearly separate. Example conceptual contract (do not overengineer):

```
ScreenDataSource = ReferencePreviewFixtures | LiveDataUnavailable
UiDataProvenance = DESIGN_PREVIEW | DEVICE_OBSERVED | COMPUTED | ESTIMATED | UNAVAILABLE
```

For this stage, the application shows design fixture figures only in an **explicit, conspicuous Visual Preview mode**, using an always-identifiable preview chip or notice that doesn't silently masquerade as device data. In any future live-mode placeholder, display `Not scanned yet`/`—`/`Requires device permission`/`Coming in next phase`, NOT fake scores. Screenshot comparisons may optionally use a `referenceCapture` debug variant to minimize the preview disclaimer overlay; this variant MUST remain debug-only and never be shipped or claimed as real scan results. The screenshot preview dataset MUST NOT write fabricated operation history, count imaginary saved bytes, or persist fake cleanup actions.

### Navigation hierarchy

Persistent 4-tab navigation exactly as visual references: **Home / Clean / Tools / Reports**; selected tab is a larger purple luminous capsule. Home opens the Home UI. Clean opens a visually coherent tool index offering Deep Scan, App Residue and Duplicate Files. Tools opens a coherent index offering Storage Analyzer, App Manager, Privacy Sweep, Battery Saver, AI Optimization. Reports opens Reports & Insights. A tile on Home goes to its corresponding screen. Back arrow returns to prior screen; hardware back navigates correctly; bottom tab changes selected state correctly; scroll positions survive short navigation where practical; no dead tap targets. Additional index screens not pictured must be minimal, on-brand and clearly secondary—not a replacement for any of the 10 references. Settings button opens a properly styled settings **UI shell only** without actual system-changing actions. Pro badge opens a clearly labeled informational preview only; NO real purchasing or entitlement claim.

Use a scalable portrait layout: prioritize 360×800dp and a second taller phone (for example 412×915dp); ensure clipping/keyboard/safe insets, natural vertical scrolling for low-height screens; support RTL/LTR and Arabic/English without text mirroring bugs; maintain dark mode visual priority while applying readable accessibility behavior. Compose preview tests and emulator screenshots should catch overflow. Preserve user font scaling where possible and set minimum touch areas around 48dp. Avoid forced fixed pixel layout; use proportional spacing plus controlled dp limits. A screen's 864×1536 marketing mockup is NOT automatically a 864dp Android window.

## 4 — TEN REFERENCE SCREENS: EXACT PER-SCREEN CONTRACT

**These numbers are VISUAL REFERENCE FIXTURES ONLY**, not verified real-device outputs. Recreate reference typography, positions, labels, icons, color behavior, controls, list contents and hierarchy. Do not silently alter wording or substitute generic placeholder cards. Do not claim contradictory values are real; preserve them only under Reference Preview.

### 01 — `AI Smart Clean` Home (file `01_AI_Smart_Clean_Home.png`)

- Header: left hamburger menu, brand gradient K + `knoux ai`; right narrow gold-edged crown **Pro** capsule and a white settings gear.
- Large glowing violet/cyan 3D **K logo inside luminous crystal plasma orb**, segmented orbital ring and delicate dotted aurora/wave field behind. This is the hero centerpiece; should feel premium, slightly alive, NOT a standard flat icon.
- Bold italic headline with white `AI Smart` and magenta/purple `Clean`; subtitle `Intelligent Cleaning & Device Optimization`.
- Thin four-benefit row separated by rules: `Clean`, `Optimize`, `Protect`, `More Space`, each with neon mini symbol.
- Dominant luminous rounded CTA: search icon + `Start Deep Scan` + chevrons. It navigates to the Deep Scan **preview**, but must NOT launch a real device scan in phase 1.
- Three KPI slots in one glass panel: reference values `93% Optimization Score / Excellent`, `12.6 GB Storage Saved`, `72% Scan Progress / Analyzing device...`.
- Six *two-column* home service cards, each with unique icon, title, brief description, visible metric or review/clean action pill and chevron as pictured:
  1. Temporary Files — `2.4 GB removable`, `Clean`.
  2. Duplicate Files — `1,248 duplicates found`, `Review`.
  3. Storage Analyzer — `128 GB / 256 GB used`, `50%` bar.
  4. App Residue — `836 MB`, leftover uninstalled apps.
  5. Privacy Sweep — `7 trackers found`, `Review`.
  6. AI Optimization — `Boost device health`, `Optimize`.
- Rounded bottom navigation Home/Clean/Tools/Reports with Home highlighted. Temporary Files may route to an explicitly labeled **UI-only** preview detail within Clean; don't invent a finished backend service.

### 02 — `Privacy Sweep` (file `02_Privacy_Sweep.png`)

- Back/brand/shield/settings top bar; huge holographic neon security **shield enclosing padlock** and orbital rings.
- Italic `Privacy Sweep` with purple gradient on Sweep; subtitle `Review trackers, risky permissions, and privacy exposure indicators.`
- Large top split metrics glass: `7 trackers found`; cyan-edged shield capsule `86 /100 Privacy Score`.
- Four compact horizontally aligned indicators: `Camera Access 2 apps`, `Microphone Access 1 app`, `Background Trackers 7 trackers`, `Clipboard Watchers 2 apps`.
- Ring `86% Privacy Score`; right summary `Protected apps: 24`, `Needs review: 5`, `Last sweep: Today, 10:21 AM`.
- Section `Privacy Items (5)` plus `Select All (7 Trackers)` checkbox pill. Five interactive rows with icons, descriptions, badges and `Review`:
  `Ad SDK Trackers — 7 found`; `Background Location — 1 app`; `Clipboard Access — 2 apps`; `Overlay Permission — 1 app`; `Notification Listeners — 2 apps`.
- Bottom two actions `Review Risks` / `Improve Privacy`. These open review/decision **preview** bottom sheets only, never revoke actual permissions or claim live tracker detection.

### 03 — `Duplicate Files Finder` (`03_Duplicate_Files_Finder.png`)

- Neon stacked photo/landscape picture pair hero, tall orbital rings; heading `Duplicate Files Finder`, precise subtitle.
- Two primary numbers `1,248 duplicates found` and `8.7 GB recoverable` with file + database icons.
- Four compact stats: `612 Similar Photos`, `238 Repeated Videos`, `316 Copy Documents`, `82 Duplicate Audio`.
- Ring `96% Match Confidence`; right summary `Auto-grouped items: 218 groups`, `Needs review: 37 items`, `Last scan: Today, 10:21 AM`.
- Title `Duplicate Groups (5)`; list: `Similar Selfies — 189 items / 1.6 GB`, `Burst Photos — 423 / 3.1 GB`, `Duplicate Videos — 238 / 2.4 GB`, `Repeated Documents — 316 / 1.1 GB`, `Shared Media Copies — 82 / 521 MB`. Each with picture/video/document/share icon, 1-line helper, chevron, proper click state.
- Two bottom actions `Preview Groups` / `Review & Remove`. Tapping opens purely visual selection/review/confirmation flows; **never request deletion or claim media scans** in this phase.

### 04 — `Deep Scan` (`04_Deep_Scan.png`)

- Hero holographic purple-blue **radar sphere** with radar lines, orbit rings, glowing points and sweeping beam; title `Deep Scan`.
- Two-line subtitle matching reference: junk, duplicates, privacy issues, system health.
- Main card `72% Scan Progress` radial ring with right `Analyzing device state` and horizontal gradient progress.
- Four tiny metrics: `2.4 GB Temporary Files`, `1,248 Duplicate Media`, `7 Privacy Alerts`, `95% App Health`.
- Secondary ring `72% Overall Scan`; right `Modules scanned: 5 of 5`, `Findings so far: 1,656 items`, `Estimated time: ~ 1 min 24 sec`. Values are fixture only; `5 of 5` alongside 72% is an apparent reference inconsistency—do not use as a real scan state.
- `Scan Modules (5)` interactive rows with progress bars: `Scan Storage 84%`, `Review Media 67%`, `Inspect Privacy 73%`, `Analyze Apps 91%`, `Battery Diagnosis 58%`.
- Bottom `Pause Scan` / `Continue Scan` buttons. In visual fixture capture match reference geometry; for actual UI state machine, **only an enabled action that makes sense** (paused/running/idle), and never pretend that the device was scanned. Run/pause controls change local DEMO animation state only, with a clear preview indicator. No timed randomized scan simulation presented as actual measurement.

### 05 — `Reports & Insights` (`05_Reports_Insights.png`)

- Glowing four rising bars/arrow with orbit rings hero; title `Reports & Insights`; subtitle cleanup history and device trends.
- Top two metrics `12.6 GB total saved`, `31 cleanups completed`.
- Four short metrics `4.8 GB Weekly Savings`, `7 Privacy Fixes`, `+18% Battery Gains`, `1,248 Duplicate Reduction`.
- Donut `92% Device Score` + `+6%` mini tag. Beside `Best improvement: Storage Cleanup (6.4 GB saved)`, `Current device score: 92% ▲ +6%`, `Last report: Today, 10:21 AM`.
- Five roomy list rows, each with matching neon icon, mini graph where pictured, metric and chevron: `Cleanup History (31 sessions)`, `Storage Trend (12.6 GB total saved)`, `Privacy Improvements (7 fixes applied)`, `Battery Sessions (+18% battery gain)`, `Smart Recommendations (3 new)`.
- Bottom `View History` / `Generate Report` visually interact with preview history and preview report sheet, **not a real PDF or measurement until later phase**.
- Bottom Reports tab glows selected.

### 06 — `App Residue Cleanup` (`06_App_Residue_Cleanup.png`)

- Three luminous stacked layers **3D cache icon** hero; headline `App Residue Cleanup`.
- Dual main metrics `836 MB residual data`, `42 orphan folders`.
- Four micro-stats `8 Uninstalled Apps`, `42 Empty Directories`, `516 MB Old Cache`, `17 Broken Shortcuts`.
- Donut `91% Cleanup Safe`; right `Safe to clean: 836 MB`, `Needs review: 28 MB`, `Last scan: Today, 10:21 AM`. Note apparent overlap/inconsistency; render under Preview only.
- Section `Items Found (5)`, `Select All (836 MB)` right checkbox.
- Five selectable rows with brand-like pictograms, consistent checked/unchecked states and individual sizes:
  `WhatsApp Leftovers — 268 MB`, `Old Game Cache — 184 MB`, `Empty Media Folders — 96 MB`, `Broken Temp Data — 172 MB`, `Archived Installer Remnants — 116 MB`.
- Bottom `Preview Residue` / `Clean Remnants` buttons. For UI-only stage these show detailed preview + mandatory confirm/cancel modal with **"No cleanup performed — UI preview only"**, not actual filesystem changes.

### 07 — `App Manager` (`07_App_Manager.png`)

- Neon 2×2 grid app icon hero with circular aura; title `App Manager`.
- Main metrics `143 installed apps`, `18.4 GB app storage`.
- Four mini-counters: `27 Rarely Used`, `16 Large Apps (≥ 500 MB)`, `9 Startup Apps`, `12 Updates Available`.
- Donut `87% App Health`; right `Apps to review: 27`, `Safe suggestions: 18`, `Last checked: Today, 10:21 AM`.
- Five category rows with left icons, category names, descriptions, visible size + last used, right cyan pill:
  `Streaming Apps — 5.86 GB — Optimize`;
  `Social Apps — 3.12 GB — Review`;
  `Game Collection — 6.27 GB — Manage`;
  `Utility Tools — 2.43 GB — Clean`;
  `Messaging Apps — 744 MB — Open`.
- Bottom `Review Apps` / `Manage Apps`. In this phase, simulate panel opening only; do not query installed apps, uninstall, clear cache, open real other-app settings, or suggest actual optimizations completed.

### 08 — `Storage Analyzer` (`08_Storage_Analyzer.png`)

- Bright neon stack-of-disks 3D hero; title `Storage Analyzer`, subtitle about usage and distribution.
- Large capacity bar `128 GB / 256 GB used`, `50%`, separated left disk icon.
- Five short labeled-category numbers: `Apps 48.6 GB`, `Media 36.8 GB`, `Documents 8.4 GB`, `System 22.2 GB`, `Free Space 128 GB`.
- Donut `50% Used (128 GB)` with cyan/magenta/yellow arcs. Right `Largest category: Apps & Games — 48.6 GB (38% of used space)`, `Unused space trend: +12.6 GB`, `Last updated: Today, 10:21 AM`.
- `Storage Categories` **two-column grid of six cards**, each with neon icon, category, size, description, chevron: `Apps & Games 48.6 GB`, `Photos & Videos 36.8 GB`, `Downloads 6.2 GB`, `Documents 8.4 GB`, `WhatsApp Media 4.1 GB`, `System Files 22.2 GB`.
- Bottom `View Breakdown` / `Manage Space` opens clear preview detail and management-intent UI, no actual file access in phase 1. These categories can overlap; they MUST NOT be treated as an additive accounting ledger without a validated data model.

### 09 — `Battery Saver` (`09_Battery_Saver.png`)

- Large neon violet/cyan 3D battery with lightning symbol hero; title `Battery Saver`.
- Main metrics `6h 42m estimated gain` and cyan heart graphic `91% Battery health`.
- Mini-stats `5 Drain Sources`, `18 Background Apps`, `−22% Brightness Impact`, `287 Charging Cycles`.
- Donut `91% Battery Health`; right `Critical drain apps: 3`, `Safe actions: 5 available`, `Last analysis: Today, 10:21 AM`.
- Five neon action rows (with smaller glass buttons/chevrons): `Screen Brightness −22% impact / Auto Optimize`, `Background Sync −14% / Restrict Sync`, `App Wakeups −18% / Limit Wakeups`, `GPS Usage −11% / Use While Active`, `Charging Health +7% health / Smart Charging`.
- Bottom `View Details` / `Save Battery`. Clicks show preview recommendations only. Avoid claims that a regular third-party Android app can directly enforce these controls, obtain real cycle count/health percentage on every phone, or guarantee hours gained.

### 10 — `AI Optimization` (`10_AI_Optimization.png`)

- Purple neon AI semiconductor hero with chunky 3D chip plus glowing `AI` lettering and orbital rings; italic heading `AI Optimization`.
- Main metric `Performance Score 94` and cyan `Battery gain +18%`.
- Four mini-values `CPU Load 24% (−36%)`, `Memory Freed 1.6 GB`, `App Hibernation 12 apps`, `Thermal Status Normal`.
- Donut `94% Optimized`; right `Recommended actions: 5`, `Applied safely: 5`, `Last tune-up: Today, 10:21 AM`.
- `Optimization Tools (5)` with `Enable All` checkbox and five checked rows: `Memory Boost +1.6 GB`, `Battery Balancing +18% battery life`, `Smart Hibernation 12 apps`, `Startup Control −4.2 s boot time`, `Thermal Guard −6°C peak temp`.
- Bottom `See Recommendations` / `Optimize Now`. No real RAM reclaim, background app kill, thermal control, privilege escalation, or fabricated successful optimization. Only a clearly labeled preview selection/detail/confirm flow.

## 5 — GLOBAL VISUAL EXPERIENCE / MOTION

- Produce faithful moving UI rather than a lifeless template. Gentle orb rotation with layered blue/purple parallax; breathing neon bloom; tiny radar sweep on Deep Scan; activity shimmer; selected-tab glow; tactile button press; score arcs animate on entering the page **only when suitable for Preview mode**; cards elevate subtly on tap. Avoid expensive uncontrolled animation loops, overheating, or heavy GPU overdraw.
- Respect reduced-motion preferences and power/visibility lifecycle; pause decorative loops when off-screen/background and disable when motion reduction is requested. Do not use flashy seizure-inducing strobe effects.
- Top and bottom insets must be accounted for on Android edge-to-edge. Do not draw over status/nav content incorrectly. Keep menu, logos, icons and text legible and balanced across common phone densities.
- Visual reading order should match the screenshot: header → hero → heading/subtitle → metric group → secondary metrics → ring/status → section + rows → CTA group → fixed navigation. On shorter devices scroll the main content while the bottom navigation remains accessible; avoid compressing everything into unreadable microscopic text just to force one screenshot height.
- Provide selectable row states with real Compose controls/semantics. `Select All` and `Enable All` update checkboxes and count/selected-size derived from **fixture data**, not static unchanging text. Review buttons open the corresponding preview sheets. Every tappable component has a visible response or is genuinely disabled with an explanation.
- All diagrams/graphs: actual Compose drawn marks (sparklines/bars/arcs), not fake screenshots. No placeholder emoji in production UI. SVG/vector/drawable icons should be consistent with reference language.
- Provide an in-app optional comparison/developer-only UI inspector for navigation to any of the 10 screens. This is a developer tool only and must not displace the main UI.

## 6 — NON-FUNCTIONALITY BOUNDARY / FUTURE-PROOF INTERFACES

**PHASE 1 IS UI ONLY.** You may define well-named lightweight UI contracts and document later Android platform work, but must not actually implement these services now. In `docs/architecture/phase-2-capability-contracts.md`, document intended module interfaces and Android constraints, in this future sequence:

1. Deep Scan orchestration and honest progress from actual module states;
2. Storage Analyzer: available Android storage APIs / scoped storage / MediaStore / SAF;
3. Duplicate Files Finder: size bucketing, streaming hashes, content and perceptual similarity, user-confirmed deletion;
4. App Manager: package visibility, UsageStats when authorized, system intents;
5. Privacy Sweep: observed vs inferred vs unavailable data; no claims of omniscient tracker detection;
6. App Residue / Temporary Files Cleanup: accessible files only, selected explicit user operations, system confirmation where needed;
7. Battery Saver: supported BatteryManager, settings handoff, evidence-backed estimates rather than guaranteed boosts;
8. AI Optimization: local feature extraction, safe recommendations, explicit user control, never invented capabilities;
9. Reports/History: real persisted outcomes and provenance, no fictional histories;
10. Local-first Room and optional Supabase/Auth/Edge Functions **only where justified**, with strong RLS and no sensitive raw paths/uploads by default.

Do not add speculative database migrations, request `MANAGE_EXTERNAL_STORAGE`, request `QUERY_ALL_PACKAGES`, automate Settings with `AccessibilityService`, connect provider APIs, or modify phone data in Phase 1. These are owner-controlled later implementation gates. Document any future API/Play-policy limitations, but do not implement destructive or privileged behavior.

## 7 — IMPLEMENTATION ORDER (ONE RUN; NO EARLY STOP)

A. Machine and Git reality audit, unique work identification, disk-space check.  
B. Confirm all 10 reference files, generate reference image matrix, agree in-code display metrics with actual screenshots.  
C. Create/repair a valid native Android/Gradle project **on the verified D: path**; avoid duplicating existing unique work.  
D. Build theme, brand mark, glass/neon primitives, orbital heroes, typography, navigation, honest preview fixture layer.  
E. Implement **all ten actual screens**; preserve exact per-screen layout order, text, components, and gestures.  
F. Implement navigation, selected-tab behavior, Settings/Pro UI-only shells, tool indexes, preview modals, selection states and CTA flows without pretending to perform services.  
G. Compile early, run unit/static tests, repair errors.  
H. Launch the real Android app in a real emulator attached to the session. Capture a full screenshot **for each of 10 screens** at the same stable portrait viewport. Do not substitute Compose IDE previews for emulator evidence.  
I. For each screenshot, crop the reference app display, align interior UI regions, compare side-by-side and (where practical) with pixel difference/SSIM/perceptual indicators. Quantitative indicators are diagnostic only, not automatic proof of "pixel perfect". Document matching and deviations by region: header, hero, typography, cards, metric hierarchy, lists, CTA, bottom nav. Iterate to correct the largest gaps, then re-capture. If no emulator is accessible, do not assert fidelity verified: mark **NOT RUNTIME VERIFIED** and include precise blocker.  
J. Test clickable UI, back stack, scrolling, RTL/LTR, landscape handling where appropriate, dark theme, accessibility font scaling, screen reader semantics, 360dp and ~412dp widths, tablet/large UI overflow behavior. Prioritize reference portrait fidelity while making the rest functional.  
K. Final in-scope unique-work integration (only within KNOUX AI project), build verification, and selective housekeeping with disk measurements.  
L. Commit only verified own work on the correct safe branch; sync `main` only if an actual authorized origin exists, fast-forward/normal integration is safe, CI state is known, and no concurrent agent work would be overwritten. If there is no remote, say exactly "local main only; origin not configured". Then write handoff and **STOP**. Do not continue into Phase 2.

## 8 — TESTS / SCREENSHOTS / EVIDENCE: NON-NEGOTIABLE

Run commands that actually exist in this project and record `stdout`, `stderr`, and exit code, e.g. from PowerShell:

```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` is only a pass claim if an emulator/device is actually connected and ran the tests. Verify `adb devices -l` and the package launch status; do not silently skip instrumentation. Implement Compose UI checks for the 10 routes, clickable primary buttons and demo selection states, and checks that production/non-preview mode cannot display fixture stats as observed data.

Required evidence outputs **kept** in repo (not deleted as "junk"):

```
qa-evidence/ui/01-home-runtime.png
qa-evidence/ui/02-privacy-runtime.png
qa-evidence/ui/03-duplicates-runtime.png
qa-evidence/ui/04-deep-scan-runtime.png
qa-evidence/ui/05-reports-runtime.png
qa-evidence/ui/06-residue-runtime.png
qa-evidence/ui/07-app-manager-runtime.png
qa-evidence/ui/08-storage-runtime.png
qa-evidence/ui/09-battery-runtime.png
qa-evidence/ui/10-ai-optimization-runtime.png
qa-evidence/ui/comparison-01..10.png [only if actually generated]
qa-evidence/ui/visual-audit.md
qa-evidence/build/build-test-lint-results.md
```

Keep a readable table with all 10 screens: `implemented`, `runnable`, `captured`, `visually compared`, `major differences`, `fixed`, `remaining`, source file path. Screenshots must be captured from the actual app process. Record emulator model/OS, dimensions/density, device rotation, app variant. Don't label static screenshots as live evidence.

**Acceptance criteria for completing Phase 1:**
- 10/10 reference images present and actually inspected.
- 10/10 real Compose screens implemented and reachable.
- Brand, original screen composition, graphics and copy reproduced to high fidelity; owner can visually inspect comparisons. **Owner approval remains pending until supplied.**
- No screenshot-as-whole-screen fake UI, no navigation dead ends, no clipped critical buttons, no illicit OS status-bar clones.
- Review toggles/selection state/clicks respond correctly as UI previews without running privileged services.
- Debug APK builds and installed runtime launch verified when toolchain/emulator permit; tests and lint executed with actual results.
- Explicit provenance: preview metrics are never asserted to represent actual device findings.
- No private external project/data modifications, no loss of unique Git work.
- Git state, cleanup inventory, and disk free space evidenced.

## 9 — SAFE DISK RECLAMATION AFTER UNIQUE WORK HAS BEEN INTEGRATED

The owner has critically low free disk space and authorizes **safe cleanup of positively proven disposable/duplicate/old KNOUX AI build material**, but does NOT authorize wiping uninspected unique work, modifying other projects, or deleting source references. Make cleanup **an actual final phase** rather than an excuse to discard work. Requirements:

1. Recheck repository **after** coding: `git status`, tracked files, untracked/ignored, SHA, branches/worktrees/stashes/reflog, known agents and active processes. Identify what is genuinely unique and protect/integrate it. Ensure committed work is recoverable from the canonical repo, and if remote configured verify pushed SHA before treating a redundant copy as removable. Do not conflate 'committed locally' with 'on remote'.
2. Inspect disk candidates by **exact path, size, purpose, owner, SHA/content parity, and current process use**. Classify each `KEEP: unique`, `KEEP: essential toolchain`, `KEEP: QA evidence`, `SAFE_DISPOSABLE`, `DUPLICATE_PROVEN`, `UNKNOWN / OWNER APPROVAL REQUIRED`.
3. You MAY remove unneeded **project-owned temporary files produced during this run** (stale temp files, abandoned scratch snapshots, throwaway intermediate generated assets whose final retained version is verified) and positively proven redundant **project-local generated outputs** that can be regenerated, **only if not used by another process** and after test/evidence capture. Avoid deleting the final APK, current build needed for inspection, reference PNGs, golden screenshots, untracked source, design assets, user data, AGENTS.md, CI records, or the only copy of a file. For Gradle build directories, identify exact project-local generated directories first and prefer Gradle cache management mechanisms over brute-force deletion.
4. Never delete any **global caches**, Windows/Android SDK directories, `D:\Temp` wholesale, `D:\.gradle` globally, other people's worktrees, other KNOUX projects, device data, or external folders without a separate explicit approval identifying exact candidate paths and expected reclaimed bytes.
5. If a candidate is an older same-app branch/worktree or duplicate source tree, inspect actual unique commits, patches, untracked files and byte-level differences. Integrate genuinely unique content and **verify it on main**. Do not automatically delete the branch/worktree; report it as a deletion candidate with exact path/SHA and seek explicit approval because loss might be irreversible. No automatic `git branch -D`, `git worktree remove --force`, or reflog pruning.
6. Avoid creating backup folders/second clones/dated duplicate source trees as normal process. When temporary scratch files are needed, use one controlled `.tmp` directory and remove only verified own scratch. Do not overwrite source content in place as a way to avoid legitimate preservation steps.
7. Capture `D:` free bytes **before and after each deletion batch**; report bytes actually recovered, not an estimate presented as a result. If cleanup has zero verified safe candidates, report `0 bytes reclaimed` rather than fabricate success.

**Important scope boundary:** New KNOUX AI project may have **no branches or old garbage**. In that case there is nothing to merge and nothing to delete. Do not wander into unrelated C:/D:/K: repositories to manufacture cleanup statistics.

## 10 — COMMIT / HANDOFF / MANDATORY HARD STOP

Produce `PHASE_1_HANDOFF.md` containing exactly distinguishable status groups:

- `IMPLEMENTED + EMULATOR VERIFIED`
- `IMPLEMENTED + TESTED BUT NOT EMULATOR VERIFIED`
- `IMPLEMENTED / PREVIEW-ONLY BY DESIGN`
- `GAPS / VISUAL DIFFERENCES / BLOCKERS`
- `NOT STARTED: ALL PHASE 2 LIVE SERVICES & DATABASE`.

Include:
1. Actual canonical directory on Windows `D:\KNOUX-AI` or exact inaccessible/alternate location.
2. Whether starting from scratch or reusing existing repo, starting HEAD/origin SHA and final HEAD/origin SHA.
3. Evidence of all 10 source reference files and deployed UI screenshots.
4. Exact project tree / app ID / Android minSdk, targetSdk, compileSdk, Kotlin/AGP/Compose versions.
5. Exact build/test/lint/instrumentation commands and real exit codes; emulator launch evidence and package name.
6. Runtime screenshot comparisons with mismatches not concealed.
7. Responsive/RTL/accessibility testing results.
8. Verified navigation/interactive previews and what is **not** functional yet.
9. Any unique Git work integrated and any remaining unique unmerged work with paths/SHAs; whether remote was actually updated and CI status.
10. Disk `D:` free bytes pre/post, actual paths/size of safe cleanup, items deliberately retained, candidates requiring approval.
11. 10 later end-to-end service modules in implementation order and any Android platform constraints, without implementing them yet.
12. Exact owner's next decision: approve UI, or list concrete visual corrections first.

**Final reply template (facts only):**

```
KNOUX AI — PHASE 1 UI GATE
Local path: [verified]
Git HEAD / origin/main: [real SHA or unavailable]
References inspected: [x/10]
UI screens implemented: [x/10]
Screens captured in emulator: [x/10]
Visual comparison: [x/10] — [remaining mismatches]
UI navigation tests: [actual]
Gradle build / unit tests / lint / instrumentation: [exact PASS/FAIL/SKIPPED and reason]
Phase 2 services/database: NOT STARTED AS REQUESTED
D: free before → after: [measured]
Disposable bytes actually reclaimed: [measured]
Unmerged unique work: [paths/SHAs or none if proven]
Owner action: Review ten screenshots and approve UI for module-by-module implementation.
STOP.
```

**CRITICAL FINAL COMMAND:** Once visual phase and safe in-scope cleanup have been performed to the maximum verifiable extent, **do not proceed** to any scanner, optimizer, Room schema, Supabase schema, Play permission request, login, subscription, cleanup engine, or deletion feature. **STOP and await owner approval.** Never label the phase Production-ready. Do not assert 100% fidelity without real comparison and user sign-off.

**EXECUTE NOW. NOT A PLAN. NOT A MOCK CLAIM. NO FAKE CLOSURE.**
