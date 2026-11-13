# KNOUX AI PC — DEEPSEEK / OPENCODE — REAL DATA + FULL FUNCTIONALITY EXECUTION START
## OWNER AUTHORIZATION TO TRANSITION FROM FINAL VISUAL PHASE TO END-TO-END ENGINEERING
**Date:** 2026-11-12 (Asia/Dubai)  
**Scope:** Windows-hosted Android repository `D:\KNOUX-AI` ONLY.  
**Execution mode:** Implement → test → repair → verify → safely integrate → automatically advance. NOT planning-only.

### CRITICAL BASELINE FROM INDEPENDENT LIVE INSPECTION
At 2026-11-12 21:54 +04:
- Branch: `main`, HEAD **`c0530b61872841dacb52b7d8602ff6a479b1bde1`**.
- Local `origin/main` equals that SHA. Both were synchronized at this observation, but **recheck live remote with `git ls-remote` before claiming that the current remote is synchronized**.
- Latest visual commits: `3adc636` final UI implementation, `c0530b6` final visual evidence ledger.
- Visual QA: `qa-evidence/final-visual-gate/TASK_LEDGER.md`; eight runtime captures in `qa-evidence/final-visual-gate/runtime/`.
- Owner now explicitly authorizes beginning REAL SERVICE ENGINEERING after the visual handoff. Do **not** repeat or discard Kiro/DeepSeek visual work.
- IMPORTANT: QA ledger declared gate `AWAITING OWNER VISUAL ACCEPTANCE`; the latest owner message authorizes progression, **not a fabricated claim that each unverified reference variant has been independently tested**. Preserve reported visual exceptions and regression tests.
- Existing unique work is still present: modified `AGENTS.md`, `MasterScreens.kt`, `NeonArt.kt`, `values-v31/styles.xml`; untracked `ProSystem.kt`, `.agents/`, `docs/pro/`, `docs/workflows/`, `prompts/e2e/`, `prompts/pro/`, `database/supabase/drafts/`, QA artifacts and other work. NO LOSS, NO BLIND MASS STAGING.
- `D:` had **~2.62 GB free**. This is a severe build-space risk. Do not create duplicate repositories, archive copies, downloaded SDK duplicates, videos or unnecessary full rebuilds. No destructive cache purge before inventory and preservation.
- `app/build.gradle.kts` had minSdk 26 / targetSdk 36; no installed Room, WorkManager, or DataStore dependency was found during preflight. `AndroidManifest.xml` did not yet declare media/file access. These are gaps, not proof of an implemented data layer.
- The observed apk-debug is a DEBUG artifact, not a signed Play production release.
- OpenCode GUI/service processes were running; native task ownership and concurrent writers need a fresh check. Do not run two code writers on the same shared files.

## 0 — READ THESE SOURCES FIRST, THEN EXECUTE
1. `AGENTS.md` — interpret the newer owner's E2E override as taking precedence over historical "UI only" / stop instructions, while preserving all current design/identity directives.
2. `docs/workflows/KNOUX_PC_AUTONOMOUS_E2E_WORKFLOW_20261112.md`
3. `docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json`
4. `qa-evidence/e2e/EXECUTION_LEDGER.md` and `qa-evidence/e2e/ACCEPTANCE_MATRIX.md`
5. `docs/pro/PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md` — owner P0: customer controls, **no preview-only service**.
6. `docs/pro/PC_PRO_FULLSTACK_MASTER_20261112.md` and `docs/pro/PC_PRO_FEATURE_CATALOG_20261112.json` — 10 sections, 50 PRO feature contracts, NOT 50 implemented features.
7. `prompts/pro/PC_PRO_PASS01_EXECUTE_20261112.md`
8. `database/supabase/migrations/001_core.sql`, `002_rls.sql`, `003_account_erasure.sql` plus `database/supabase/drafts/004_pro_entitlements_DRAFT.sql` (not deployed).
9. `qa-evidence/final-visual-gate/TASK_LEDGER.md` and actual relevant Kotlin/service files.

The independent Android phone Linux repository `/root/workspace/KNOUX-AI` is **OFF LIMITS**: do not open, sync, merge, port or copy code from it. Only this Windows-hosted repo exists for this task.

## 1 — UNAMBIGUOUS PRODUCT GOAL: REAL DATA AND REAL ACTIONS
STOP presenting fictitious data as "live".
STOP treating a scan preview, fake percentage, or paywall as a completed customer service.
For EACH real service, implement the entire customer journey:
**actual source discovery → permissions → scoped scan → evidence/provenance → inspect/filter/search → choose/exclude → action preview → explicit customer confirmation + system approval → real operation → re-query → actual measured result → durable history → cancel/retry/restore where supported**.
User is fully in control. Support **Trash AND permanent delete** where permitted and user-confirmed. Never perform undeclared background deletion or destroy personal device files during tests.
If platform genuinely restricts a path, inspect supported normal OS APIs, appropriate special grants with current Play policy, SAF, and clearly opted-in authorized advanced modes before limiting function. No fake privilege escalation, no sales promises for impossible features.

## 2 — START WITH PC00: THE MINIMUM COMPLETE REAL DATA FOUNDATION
Do **not** spend another session redesigning login/onboarding. Only fix regressions proven by tests.

1. Inventory real code and data sources. Create a capabilities matrix for API 26–36, actual declared permissions, supported provider types and current installed integrations; check active agent file ownership.
2. Add minimal appropriate dependencies for **Room**, **WorkManager**, **DataStore** and necessary Android platform libraries. Verify version compatibility with existing Kotlin/AGP/Gradle versions before editing. Do not indiscriminately upgrade the whole toolchain.
3. Implement typed local domain contracts:
   - `AccessScope`, `PermissionGrant`, `DataProvenance`, `SourceSnapshot`, `FileCandidate`, `ObservedBytes`, `ScanSession`, `OperationPlan`, `OperationResult`, `CleanupHistory`, `FeatureCapability`, `ProAccessDecision`.
   - Distinguish `MEASURED`, `ESTIMATED`, `PREVIEW`, `UNAVAILABLE`, `PARTIAL`, `VERIFIED` statuses.
   - Include timestamps, included/excluded scopes, granted capabilities, file counts and bytes using `Long`, operation IDs, user-confirmation states, and errors.
   - Avoid eager reads of entire file contents; use cancellable/paginated streams and bounded memory.
4. Add a Room database with versioned, **non-destructive** migrations for scans, candidates, per-item operations, progress, exclusions, schedules, recovery metadata and evidence references. Preserve existing user data. If existing Room/database already exists, REUSE rather than create a duplicate.
5. Use DataStore for settings, user consent and UI preferences only. Do **not** use local booleans to forge paid subscriptions. Pro rights require real verified entitlement or explicit marked debug-only TEST entitlement.
6. Add a permission-aware `StorageSourceRepository` and `MediaRepository` based on `ContentResolver`, `MediaStore`, `Storage Access Framework`, and `StatFs` as appropriate. Handle revocable/partial grants. Never infer permission from previously stored state.
7. Correct manifests / runtime permission flows by API and use case:
   - API ≤32 read permission where appropriate, bounded by `maxSdkVersion`; special handling for API 29.
   - API 33+ granular `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` **only where justified**.
   - API 34+ selected photos access `READ_MEDIA_VISUAL_USER_SELECTED` and explicit reselection support when required.
   - SAF `ACTION_OPEN_DOCUMENT_TREE` for customer-granted compatible folders; persist URI permissions only if truly persistable; never assume access to restricted app-private data.
   - Android 11+ `MediaStore.createTrashRequest` and `createDeleteRequest` with real Android OS confirmation; use provider-compatible paths and SDK-specific alternatives on older versions.
   - Examine `MANAGE_EXTERNAL_STORAGE` only if it genuinely qualifies for the declared core file management purpose and applicable distribution rules; implement SAF/scoped fallback and do not assume Play approval.
   - Always show the customer what access is available and how to grant/revoke it.
8. Use dependency injection consistent with the existing codebase (avoid adding a new framework just for this task if not necessary). Expose real data via `Flow`/`StateFlow`; app lifecycle and cancellation must work.

### PC00 VALIDATION
Actual DB migration tests, source permission tests, capability classification tests, duplicate ID/entitlement tests, and Kotlin compilation. Log exact command and outcome. Then **immediately move into PC01 in the same workflow**.

## 3 — PC01 FIRST COMPLETE SERVICE: SMART CLEAN (FREE + PRO)
### CUSTOMER'S REAL OUTCOME
The customer chooses eligible storage and cache sources, discovers real candidates, inspects why they're candidates, selects them, removes them by actual authorized OS/provider operations, confirms what happened and can see history and supported recovery.

### REQUIRED FEATURES — NO EMPTY BUTTONS
**Scanning**
- Real inventories for app-owned cache + selected shared media + SAF folders, without accessing other apps' private cache.
- Accurate counts/bytes, metadata, category, human-readable file info, last modified, source/permission state.
- Cancellable scans, progress based on observed work; duplicates and stale items handled; no sample file totals displayed as real.
- Exclusions: favorites/keep items, user-protected folders, file-type/date/size filters and remembered scoped rules.
- Full accessible list, search, category filters, selection, select-all-eligible, deselect and item preview/open where allowed.

**Real actions**
- Separate **Move to Trash** and **Delete Permanently** where the provider supports them. Both must have an honest user review/confirmation. Use OS permission dialogs when needed. Display irreversible deletion warning for permanent removal.
- App-owned cache: real selective deletion of eligible app-owned cache entries; never delete unrelated configuration/user files.
- SAF: actual `DocumentsContract.deleteDocument` only on legitimately granted writable document URIs and where provider flags support it.
- MediaStore: `createTrashRequest` / `createDeleteRequest` (API30+) with OS-mediated result handling, and SDK-appropriate alternatives for API26–29 when authorized.
- Support cancellation before irreversible OS action, errors and partial approvals without pretending to undo OS-completed work.
- Re-query provider URI state after system result; separately record before/after `StatFs` deltas as *observations*, not guaranteed exact freed bytes (filesystem accounting may be delayed).
- Store per-item actual outcomes and operation journal; avoid double-running same action with idempotency keys.

**Premium advanced functionality**
- `smart_clean_unlimited`: repeatable REAL scoped review operations, no imaginary system-wide access.
- `smart_clean_scheduled`: WorkManager user-configured, cancellable **scan/notification** schedule; OS-owned delete consent must not be silently bypassed.
- `smart_clean_deep_analysis`: transparent on-device evidence-based heuristics over actual measurements; no cloud AI claim without a real integration.
- `smart_clean_priority`: prioritize KNOUX-owned jobs, do not claim to increase device CPU/GPU privileges.
- `smart_clean_rollback`: actual operation ledger and Android/provider supported Trash restore. Do not promise universal undo after permanent delete.
- Test/implement feature gating but never treat a locally generated `KX-...` code or owner debug mode as a real subscription.
- Do **not** make basic real cleaning unusable merely because Play Billing / Supabase credentials are not configured. Core FREE capability works; optional paid gates report `CONFIGURATION_REQUIRED` until authoritative backend exists.

**UI integration**
- Reuse existing `MasterScreens.kt`, `NeonArt.kt`, `ServiceWorlds.kt`, `ProSystem.kt` and their approved KNOUX glass/neon design.
- Do NOT redesign onboarding, login, header, hero, icons, colors or animation. UI changes are limited to integrating real data/controls and fixing evidence-proven viewport issues.
- No root-level infinite page scroll that hides primary actions; use bounded internal lists. RTL/LTR, API35 safe insets, 48dp minimum touch targets and full state UI for DENIED/EMPTY/LOADING/PARTIAL/ERROR/COMPLETED.

### PC01 HARD TESTS
Set up synthetic test data **only on verified emulator or isolated app-owned test fixture**; no personal file removal from the real phone:
- Permission denied / partially granted / revoked after session.
- Empty library, duplicate URI, enormous file metadata, missing/corrupt provider, cancelled scan.
- Actual review + successful trash with re-query.
- Actual review + permanent deletion of a synthetic test file with explicit OS approval.
- Cancelled OS confirmation does nothing.
- Partial success, stale URI/moved file, retry, double-tap idempotency, process death / restart, rejected/expired subscription, offline.
- Persistence, scheduled scan/cancel, no background deletion.
- Actual UI interactions, button mapping, owner-selected scope and accessible selection, Arabic RTL/English LTR, different phone widths and safe system insets.

Record exact Gradle commands and exit codes, test counts, emulator name/serial/API, screenshots, operation evidence, APK hash, permissions and limitations. **Do not label PASS if any required test was not actually run.**

## 4 — AUTO-CONTINUE ALL OTHER REAL SERVICES
After PC01 is actually verified, proceed through existing tasks **PC02–PC13**, using the manifest rather than inventing a new backlog:
1. Duplicate Finder: exact streamed hash; similar images separate; keeper protection; actual user-authorized trash/delete/requery.
2. App Residue: user-accessible residue discovery, selection, actual authorized actions/history.
3. Privacy Sweep: real permission audit/OS actions; working cryptographic vault where advertised; tracker block claims only with tested authorized network engine.
4. Deep Scan: actual accessible sources, worker orchestration, evidence, real authorized remediation.
5. Reports: real persistent measurements and CSV/PDF exports.
6. App Manager: PackageManager/UsageStats with appropriate grants; real system launch/settings/uninstall; advanced modes only where legitimately supported.
7. Storage Analyzer: real volume + scoped inventory, open/share/rename/move/delete.
8. Battery Saver: actual battery/thermal data and settings actions/alerts; no invented saved hours.
9. AI Optimization: provenance-backed recommendations + supported customer-controlled applied actions; no fake boosts.
10. Home/Auth/Settings and Pro integrity: connect real results, maintain visual regression.
11. Play Billing/Supabase stage: only to **verified owner-approved target** and after real product/keys/test accounts are available. Draft SQL alone is NOT deployed.
12. Final build/UI/security/release validation.

If a genuine external prerequisite blocks only one branch, log `BLOCKED_EXTERNAL` with exact evidence and continue independent tasks. **Never substitute a mocked service and mark complete.**

## 5 — WORKFLOW / ONE WRITER / GIT CONVERGENCE
- Recheck HEAD, `git fetch` only if safe, `git status`, `origin/main`, untracked and agents before any code changes; do not alter phone Linux project.
- Use existing native OpenCode workflow tooling when actually available. Otherwise reuse the file-backed `docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json`, `qa-evidence/e2e/EXECUTION_LEDGER.md`, `ACCEPTANCE_MATRIX.md`.
- One writer for overlapping source files. Controller can audit, implementer writes, independent verifier reviews. Never spawn a second agent that overwrites live unique work.
- Reconcile earlier `AGENTS.md` UI-only instructions with the newer owner authorization. Do not delete historical rules or accepted screens.
- Check for unique changes and stage ONLY coherent verified edits, never `git add -A` without inventory. Existing unrelated dirty work must not be silently included, stashed, discarded, or overwritten.
- Commit tested batches on existing `main` (no branch proliferation). Push fast-forward to `origin/main` when safe; verify remote SHA with `git ls-remote origin refs/heads/main`. Do not force-push, reset --hard, clean, rewrite Git history or delete unique files. On conflicts stop risky integration, preserve changes, and report.
- Disk is constrained: avoid redundant repos, archives/ZIP snapshots, duplicate SDKs and massive intermediate artifacts. Inspect size and keep unique work. Only remove proven regenerateable caches after checking no agents use them and after user approval for destructive cleanup when required. Do not demand backups that consume D:.
- A successful Gradle build is not UI acceptance, backend deployment, or production readiness. Debug signing is not a Play Store release.
- No uploads of device personal paths, media names/content, hashes or app inventory to Supabase by default; opt-in cloud sync and tested RLS are required.

## 6 — FIRST MESSAGE MUST CONTAIN EVIDENCE OF EXECUTION, NOT A PLAN
Do this NOW in the current session:
1. Inspect current repo and active agents, and print current HEAD/remote/status.
2. Identify actual real-data service gaps from source.
3. Implement the smallest complete `PC00` real data backbone (dependencies + Room entity/DAO + adapter + permission state) with at least one RED→GREEN unit test.
4. Wire an actual permission-scoped scan into existing Smart Clean UI, and make the first real tested customer-action path work on synthetic emulator data.
5. Re-run tests/build and capture actual evidence.
6. Persist a ledger checkpoint and safely integrate verified work on `main`.
7. Continue PC01 and then next independent service automatically.

Your eventual acceptance report must state:
`REALITY | RISKS | UNIQUE WORK | GAPS | ACTUAL CHANGED FILES | PERMISSION MATRIX | CUSTOMER ACTION EXECUTED | TEST COMMANDS/EXIT CODES | RUNTIME EVIDENCE | APK SHA | LOCAL HEAD | REMOTE SHA | BLOCKERS | NEXT ACTION`.

**NO MORE PLANNING-ONLY. NO PREVIEW-ONLY. NO FAKE CLEANUP. NO UNAUTHORIZED PERSONAL DATA DELETION. NO VISUAL REBUILD. NO UNIQUE WORK LOST. BUILD REAL KNOUX AI SERVICES, STARTING RIGHT NOW.**
