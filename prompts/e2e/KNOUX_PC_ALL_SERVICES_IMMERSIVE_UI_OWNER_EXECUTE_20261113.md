# KNOUX AI — ONE-SHOT SUPREME ALL-SERVICE IMMERSIVE UI + E2E OWNER CONTRACT
**Date 2026-11-13 | This file is an EXECUTION COMMAND, not a proposal.**
**CANONICAL PC PROJECT: D:\KNOUX-AI — Do not touch the separate Linux/phone /root/workspace/KNOUX-AI.**

## OWNER OBJECTIVE
The app must never present customers with primitive text-only output or one generic delete button. For EVERY category and each tool, create an original premium KNOUX customer experience: signature immersive 2.5D / 3D-feel instrument, lively state-driven animations, rich searchable and selectable RESULT CARDS with genuine photo/video/file previews, granular user-controlled actions, clear OS consent, real post-action verification, history and recovery. Fully preserve approved KNOUX identities and animations. The customer must SEE, REVIEW, MARK, FILTER, EXCLUDE, OPEN, MOVE/COPY/RESTORE (only where supported), or send explicitly selected items to system Trash before any permanent deletion. A deep scan reporting 70 GB means 70 GB MEASURED, not 70 GB available for deletion. Every action must be honest, safe, reversible where supported, and functional on the actual Android phone.

## OBSERVED BASELINE — RECHECK BEFORE EDITING
At 2026-11-13 17:xx +04, PC repo main HEAD was 1b69344b594d503f063f99b7aa614127f2fab98e (PC05 Deep Scan + Abyss Sonar), local main AHEAD of origin/main by 1. Commit is LOCAL / NOT VERIFIED PUSHED. Worktree has modified qa-evidence/e2e/EXECUTION_LEDGER.md and unique untracked .agents, .kiro, QA logs/images/databases. NEVER erase, overwrite, reset, clean or force-push this work. Other agents may be writing: identify active work before touching the same files.

Actual project and manifest:
PC00 Foundation — VERIFIED functionally; PC01 Smart Clean VERIFIED within existing scope; PC02 Duplicates VERIFIED; PC03 App Residue VERIFIED; PC04 Privacy Sweep VERIFIED. Their accepted real services have VISUAL IDENTITY DEBT: improve UI without replacing proven backends.
PC05 Deep Scan — implemented/committed LOCALLY at 1b69344. Ledger Entry 0014 claims a real API35 phone scan measuring ~3,400 files and ~4 GB, results persisted in Room v5, live UI screenshots, but no complete androidTest suite and no synthetic Trash/Restore operation through the PC05 screen. PC05 must remain PARTIAL/IMPLEMENTED_UNVERIFIED pending its acceptance gate. Confirm all claims against actual files and test evidence.
PC06 Reports — next QUEUED; PC07 App Manager; PC08 Storage Analyzer; PC09 Battery Saver; PC10 AI Optimization; PC11 Home/Auth/Settings/Navigation; PC12 Play Billing/Supabase staging; PC13 Release. Current manifest may be older than ledger; reconcile source, new commits, XML, screenshots and owner notes.
Physical phone owner-authorized for PC QA: itel S686LN / Android 15 API35; serial and wireless port dynamically discovered and kept PRIVATE. DO NOT place real device serial, personal IP, unredacted screenshots/logcat or secrets into public source/QA.
Current Android sources include SmartCleanReal.kt, DuplicateReal.kt, ResidueReal.kt, PrivacyReal.kt, DeepScanReal.kt, corresponding controllers/repositories, KnouxDatabase and com.knoux.ai.visual. Reuse in-place, no restart.

## FIRST: READ THE WHOLE PROJECT CONTRACT AND VERIFY UNIQUE WORK
Read fully:
AGENTS.md
docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md
docs/workflows/KNOUX_PC_PER_SERVICE_AUTONOMOUS_EXECUTION_20261113.md
docs/workflows/KNOUX_PC_AUTONOMOUS_E2E_WORKFLOW_20261112.md
docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json
qa-evidence/e2e/EXECUTION_LEDGER.md including entries 0013/0014 and later updates
qa-evidence/e2e/ACCEPTANCE_MATRIX.md
docs/pro/PC_PRO_FEATURE_CATALOG_20261112.json
prompts/e2e/PC_OWNER_PHYSICAL_DEVICE_TEST_AUTHORIZATION_20261113.md
docs/visual/reference-matrix.md, docs/visual/component-inventory.md, all approved assets, service visual QA and code.

Before writing: actual git root, HEAD, origin/main, branch -avv, status -uall, worktrees, reflog, stash, unique uncommitted files, pending agents, Gradle/build activity, recent APK sha. If another writer owns a file, coordinate or select non-overlapping work; never race. Preserve main and push only safe fast-forward commits after verified gates. Never use reset --hard, git clean, force push, branch/file/worktree deletion, or install duplicate SDK/emulators. No destructive phone data changes.

## UNIVERSAL CUSTOMER UI SYSTEM — BUILD ONCE, REUSE
Create/extend small native Jetpack Compose building blocks in existing KNOUX components:
1. ImmersiveServiceScaffold: hero instrument on top + collapsible animated context, visible scope and measured metrics, independent scrolling results, bottom accessible action controls that NEVER cover content.
2. Typed ServiceInstrument StateFlow: IDLE / NEEDS_PERMISSION / ARMED / RUNNING / PAUSED / PARTIAL / RESULT / CANCELLED / FAILED / UNDO. Each visual action maps to real repository phase and provenance. Unknown progress => indeterminate listening animation, NOT fake moving percent. Glow/rings respond to actual findings and bytes, not elapsed time.
3. Actual media browsing: thumbnail galleries for Photos with zoom/compare; Videos with real poster frames, duration if known, tap-to-play; Documents with filename, extension, source, size and preview/open via content URI; Audio with known duration. Use efficient decoding, lazy grids and stable keys, no unlimited bitmap cache or fake icons as media.
4. Faceted results: tabs/categories/filter search, date/size/type/source sort, group expansion, item details sheet, protected/selected/excluded badges, long-press select plus explicit accessible checkbox, selection count and byte sum, eligible-only Select all, Clear, Exclude, Keep, Move, Copy, OS Trash and advanced Permanent Delete (only where supported).
5. Consented action pipeline: actual selected items -> preview exact item IDs and size -> capability check -> Android OS request / SAF destination -> user approval/denial -> real result from provider -> independently re-query affected items -> append-only Room journal -> actual undo/restore ONLY if supported. Never treat a click or PendingIntent launch as completion. No irreversible "Clean all" on true user data.
6. Details per service must be service-specific, NOT the same generic card with changed icon. Text hierarchy concise but fully understandable; no giant JSON reports. Accessible alternate list view for interactive 3D scenes, no gesture-only actions.
7. Empty/blocked/partial/offline/denied/revoked/missing-grant/loading/cancelled/test-fixture failure states must have truthful cards and recovery CTA. No fake malware, unavailable app-private access, storage saved or AI boost.
8. 48dp touch targets, TalkBack semantics, Arabic RTL and English LTR with localized strings, no text collisions at high font scale, proper Android insets, 360dp+ layouts, reduced-motion, pause animation off-screen, color contrast, dynamic device sizes. Do not leak file paths/device metadata via screenshot publication.
9. Light-weight 2.5D: Compose Canvas, graphicsLayer rotationX/Y where appropriate, radial lights, particles and depth. Defer frame-state reads to Draw/graphicsLayer, avoid composing entire screen at frame rate. No new heavyweight 3D game engine and no unnecessary SDK. One accent color per existing visual identity. Preserve palette base ink #07051A, panel #0D0935, glass #1A1642, cyan #00E8FF, violet #8B5CF6, magenta #FF54FF, gold #FFC24B and mint #3DFFB0. Preserve logos, existing cinematic headers, service art, Splash, Onboarding, Founder.

## PER SERVICE — ONE SIGNATURE EXPERIENCE, REAL CONTROLS

### PC00 FOUNDATION — BEDROCK
Keep the verified Room schemas, migrations, access scopes, evidence labels and controllers. Violet luminous grid only as a truthful diagnostics UI, not a fake customer scan. Show measured capability status, real permissions and data-source availability.

### PC01 SMART CLEAN — TIDE (retrofit without losing verified features)
Hero: premium translucent tide ring with grains/islands per measured category. Flow occurs ONLY after provider reports confirmed action. Results: grouped eligible app-owned temporary cache, images/screenshots, videos, old and large files as allowed. Each category opens actual grid or details: thumbnails, filters, age, size, source, confidence, exclusions. Keep/Move to chosen folder/Copy/Trash/Remove only per scope. Separate measured size from eligible savings, explain inaccessible other-app caches. Build item-level review + persistent action timeline, with proof of actual user-authorized OS outcome. Keep prior SmartCleanReal/controller and Room tests.

### PC02 DUPLICATE FILES — MIRROR (HIGHEST PRIORITY FOR VISUAL RETROFIT)
Hero: luminous paired glass orbs joined by a tether; a keeper badge seals the original; each real group creates a real pair. Not just textual group rows.
Main customer UI MUST show *square thumbnail grids* for exact duplicate PHOTOS right in front of user; each verified group has side-by-side original and duplicates with clearly marked "Keep original". Tap to inspect full-screen images/zoom/compare. VIDEOS have real thumbnails, length and explicit preview/play. Documents and audio get specialized real cards. Tabs All / Photos / Videos / Documents / Audio, grid/list view, group carousel/filmstrip, hash status, sort by size/date/source. Never use file size+timestamp as a duplicate hash; use proven existing full-stream SHA-256 and physicalKey alias collapse. One protected keeper per group; "Select all copies" never includes keeper. Provide select/unselect, choose a DIFFERENT keeper safely, exclusion rules, review exact filenames and bytes, user-confirmed Trash/restore/delete and optional Move/Copy to user-authorized destination. Similar photos are explicitly separate opt-in heuristics, NOT exact duplicate results and never selected by default. Preserve PC02 existing tested logic and regression for MediaStore/SAF aliases.

### PC03 APP RESIDUE — STRATA
Hero: animated semi-3D earth/strata column, real orphan-folder roots visible only for measured candidates; uncertain ownership stays foggy. Results grouped by former app/source, folder hierarchy and timestamps; app icons when actual owner known, confidence/risk "Certain/Possible/Protected". Expand to tree/folder explorer and photo/video/document previews with real sizes. Filter by risk, age, source, keep/exclude, one-click safe-review. Candidate names alone do not prove junk. No automatic deletion of user notes/photos. Optional archive/Move with SAF destination and verification; Trash/Delete only when user explicitly approves and OS permits. Persist decisions and outcomes using existing Room.

### PC04 PRIVACY SWEEP — VAULT RADAR
Hero: 12 real radar nodes in a glass vault. Each node opens a dedicated permission/evidence Inspector card with exact observed API, timestamp, scope, Android constraints, OS Settings handoff and verified retest. Green/gold only when actually measured/verified. Separate own-app security measurements from other-app data which is UNMEASURABLE. Screenshot protection toggle backed by actual FLAG_SECURE, readback and Room audit; show real limited-scope posture score without pretending to run antivirus. Filter warnings/info/unknowns. Fix any overlapping cards/readability. Preserve verified PC04 3->4 migration/tests.

### PC05 DEEP SCAN — ABYSS SONAR (MOST CINEMATIC SCREEN)
Preserve committed local SHA 1b69344 and actual DeepScanRepository/Controller/Room v5. Build a floating rotating 3D-feel sonar sphere with synchronized real rotating rings/scan laser and vertical depth bar. Real selected modules appear as orbiting satellites/sector facets: Photos, Videos, Documents/SAF, Cache, Residue, Exact Duplicates, Large Files, Old Files when available. Tap satellite to focus and drill down; unmeasured scopes remain fogged, no fake percentage. Running=real item/byte progress plus indeterminate option. Cancel/pause/retry and persisted last result. Stop animation after result, preserve accessible list fallback.

MOST IMPORTANT: a 70 GB "found" result is *inventory scanned* not deletable storage! Show total measured, exact candidate count and eligible/protected/unavailable subsets separately. Tap "Files" -> category cards -> actual photo/video/doc previews -> selected item detail and preview, search/filter/sort -> select / safe-select-all / unselect / exclude -> review EXACT items and sum -> user-selected Move/Copy/Trash/secondary Permanent Delete if supported -> OS consent -> verify individual provider result -> Room journal. Show 0/partial/fogged modules accurately. Never default to "delete 70 GB". A beautiful orb without selection/review is still not an accepted customer tool.
Missing gate from latest ledger: execute synthetic-fixture Deep Scan UI -> select precise fixture -> OS Trash/Restore/Delete where available with before/after evidence; full relevant androidTest; never bulk-operate on thousands of owner files. If OEM MediaProvider fails, mark BLOCKED_EXTERNAL with logs, not VERIFIED.

### PC06 REPORTS — PRISM (NEXT NEW SECTION)
Hero: a premium refractive Prism dispersing measured history into spectral bands; width/brightness from real counts and bytes, user scrubs timeline to see history. Results as interactive charts, customer timeline stories, before/after comparisons only where comparable baseline exists, service/source/date filters, cards for real actions with actual status and per-item details. Powered by existing Room PC01-PC05 journals, not fake chart arrays. Allow user-authorized export to real redacted PDF/CSV/JSON if supported, actual share/Save result and proof of readable generated artifact. Empty history -> honest empty state. Verified savings only when OS post-state proves it; no fake "you saved 22 GB".

### PC07 APP MANAGER — CONSTELLATION
A 3D-feel orbit of installed app stars with measured icon, app size via StorageStats if available and last-use via UsageStats only with permission; otherwise unknown/grey, never guessed. Accessible searchable list/grid with app icon, actual package/name/installed state, user/system badge, filters, category and size. Tap star -> real app detail sheet, Open, App Info, permission/settings/OS uninstall intent. Verify uninstall only on synthetic test app; no silent force stop/uninstall or app-private data access.

### PC08 STORAGE ANALYZER — TERRAIN
A zoomable layered topographic storage terrain / sunburst / treemap tied to measured StatFs/MediaStore/SAF, with unknown/protected fog and explicit storage device scopes. Navigate into actual directory/category tree and rich media grids, largest/oldest files, sort/filter, detail/Move/Copy/Trash controls. Avoid double-counting physical alias/same file exposed through multiple providers. Tap chart to REVIEW items, never straight delete.

### PC09 BATTERY SAVER — PULSE
Beautiful glass battery core and pulsing oscilloscope whose line changes with REAL Android power/thermal/battery readings. Data chips for % charge, status, voltage, temp, available OS mode; real time-series from persisted samples with interval disclosure. Action cards for OS Battery Saver/optimization settings, not fake kill/boost. Never claim battery health, estimated drain or "20% saved" without valid measurement model; unknown states shown clearly. Inspiring reference: AccuBattery's transparent, history-first approach.

### PC10 AI OPTIMIZATION — SYNAPSE
A neural mesh with one evidence-labelled recommendation per node. Tap to open explanation, source and scope, risk, estimated vs measured improvement, user-selectable preview and OS-authorized action; post-verify and rollback when real. Suggestions grouped Files / Privacy / Apps / Battery / Storage; deterministic offline engine works without model key. Distinguish provider model advice from deterministic on-device rules, avoid fabricated free space/CPU figures, never send photos or private data to a model without consent.

### PC11 HOME/AUTH/SETTINGS/NAVIGATION — IRIS GATE
Preserve approved login/onboarding, no massive unwanted fingerprint graphic. Iris aperture opens only on actual successful identity. Home should be cinematic orbital hub WITH an equally capable accessible grid/list; each service preview card reflects real last verified session, permissions and date, no fake badges. All routing from MainActivity opens real service with correct backstack/state; debug bypass only in debug builds. Settings: language AR/EN, sound/motion, permissions/SAF, exclusions, history, privacy, notification, account and actual sign-out.

### PC12 PRO/BILLING/SUPABASE — SEAL CHAIN
Real 50 Pro feature catalogue mapped to actual capabilities. Hollow seals for unverified, solid only after real Play Billing and authorized backend receipt/entitlement verify, revoked clearly shown. Real restore purchase, grace/refund/cancel where supported; no fabricated purchases, no deploy billing/SQL without authorized account credentials. Preserve privacy and secret handling.

### PC13 RELEASE/QA — GATE
An evidence-based final quality gate with an arch for each PC stage that lights only when actual source/test/device/visual/permission/security proofs exist. Verify cold/warm start, permission revocation, orientation/font scale RTL, accessibility, actual result thumbnails, selection, OS consents, crashes/ANR, Room migrations, signed build and store policy. Build SUCCESSFUL != production.

## ACTUAL DEVICE E2E GATE — AFTER EACH CHANGED SERVICE
Run real compile, targeted JVM tests, current androidTest, use full logs and XML (not grepped success alone). Query ADB device list and dynamic mdns endpoints: do not hardcode USB serial/wireless port. Set the verified serial through local KNOUX_TEST_DEVICE_SERIAL; prefer USB when stable, fallback to already-paired Wireless ADB discovered dynamically, re-prove model/API. Use adb install -r -t only after package/signature checks, NEVER uninstall/clear app or erase owner data.
On actual authorized itel API35: launch real screen, capture before/during/result/selection/review/consent/post-action screenshots or allowed private video. Test buttons, image previews, video playback, search/sort, select keeper and safe copies, grant/deny/revoke, system consent and Room journal on clearly isolated UUID synthetic fixtures ONLY. Check file count/hash and post-state before/after. Do NOT delete owner's photos/videos/documents. A dropped connection -> safe bounded reconnect via mDNS, no false PASS; avoid disrupting other ADB consumers or Shizuku.
Compare screenshots with approved KNOUX references; no overlaps, clipped text, empty placeholder content, giant fake buttons, broken Arabic, tiny hit targets or light/dark regressions. Reduced-motion must retain information. Check actual device size/density and accessible 360dp+ width.

## AUTONOMOUS ORDER AND SAFE INTEGRATION
1) Inspect current unique work, make per-service actual control+visual inventory, preserve all verified tests.
2) Create minimal reusable rich-result primitives. FIRST implement PC02 real photo/video thumbnail group UI with protected original, selection, and genuine E2E on device. This is the owner’s defining example.
3) Upgrade PC01 Smart Clean, PC03 Residue, PC04 Privacy visuals + full real control affordances. Keep their verified backends; run regressions after EACH change. Do not change unrelated code needlessly.
4) Upgrade PC05 Abyss Sonar and results review/selection, close outstanding synthetic operations and full device acceptance. Never fake close an OEM blocker.
5) Immediately begin PC06 Prism Reports next, then PC07–PC13 in manifest dependency order, each complete from design to real action to tests.
6) Integrate incremental proven fixes to existing main with review, Git status/reflog/unique work preservation and safe fast-forward push only after successful proof; verify origin/main SHA and CI. Do not create a new branch unless necessary and owner-approved. Never discard unique untracked QA/assets. Workspace D only.
7) Deliver latest actually built APK, SHA256, real Android screenshots, test XML/logs, changed source list, HEAD, origin/main and outstanding blockers per stage. Transfer to phone only if it succeeds and receipt hash is verified. No fake delivery.

### PER-SERVICE ACCEPTANCE RECORD
service ID; unique hero instrument; real source implementation; before/after screenshot evidence; AR/EN RTL/a11y; real API scope/provenance; actual data fields; visible control matrix; checked synthetic fixtures; permission/OS consent result; verified after-state; Room audit; measured results vs eligible bytes; JVM + device test XML; Gradle exit code; APK sha; Git HEAD / origin/main; CI status; truthful unresolved issues. Use VERIFIED_ON_DEVICE / IMPLEMENTED_UNVERIFIED / PARTIAL / BLOCKED_PERMISSION / BLOCKED_EXTERNAL / FAILED_VERIFICATION / NOT_IMPLEMENTED. No blanket COMPLETE.

## EXTERNAL UX BENCHMARKS (IDEAS ONLY, DO NOT COPY ASSETS/CODE)
- Files by Google: original-labelled duplicates, select-file previews, Trash and Restore. https://support.google.com/files/answer/9764075 and https://support.google.com/files/answer/10607740
- SD Maid SE: deduplicator, CorpseFinder, AppCleaner, folder filters and safety risk. https://sdmse.darken.eu/ and https://sdmse.darken.eu/corpsefinder and https://sdmse.darken.eu/systemcleaner
- Samsung My Files: categorized media, Copy/Move with chosen destination. https://www.samsung.com/us/support/answer/ANS10002531/
- AccuBattery: honest measurement and history before user action. https://play.google.com/store/apps/details?id=com.digibites.accubattery
- Android MediaStore: system consent and verified Trash/Restore/Delete. https://developer.android.com/training/data-storage/shared/media
- Compose motion performance: https://developer.android.com/develop/ui/compose/animation/quick-guide and https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer

## NOW — START EXECUTION, NOT ANOTHER PLAN
Inspect active writer ownership, actual HEAD and the current source. Produce a short factual baseline, then **immediately implement** the PC02 customer photo/video gallery and group review controls, preserve PC02 keeper logic and run genuine device E2E; continue PC01/PC03/PC04, PC05 final gate, and PC06. Work through the complete goal. Report real source and device evidence, not an unsupported DONE.