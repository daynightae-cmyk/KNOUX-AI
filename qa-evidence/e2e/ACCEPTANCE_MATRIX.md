# KNOUX AI PC — Customer Functionality Acceptance Matrix

A commercial service is accepted ONLY if real data + actual customer controls + legitimate execution + post-action requery + persistence + permission/failure handling + actual tests + runtime UI evidence are VERIFIED.

| Task | Customer's real outcome to prove | Required proof | Current outcome |
|---|---|---|---|
| PC00 Foundation | 10 mapped Pro sections; 50 unique IDs; trustworthy entitlement/capability and debug/release gates | catalog assertions, real OS capabilities, build/test | VERIFIED 2026-11-13 @722af86 |
| PC01 Smart Clean | Actual selected file review, Trash/permanent delete where supported, verified freed bytes and recovery | scoped MediaStore/SAF, Room, OS confirmation, synthetic emulator E2E | VERIFIED 2026-11-13 @722af86 |
| PC02 Duplicate Finder | SHA matches, protect keeper, select, user-confirmed actual delete/trash | byte-hash tests, keeper invariant, requery | VERIFIED 2026-11-13 @59806ca |
| PC03 App Residue | Real user-scoped residue finding and approved action | SAF/app cache scopes + journal | VERIFIED 2026-11-13 @c1b1c2e |
| PC04 Privacy Sweep | Permission assessment + real OS controls, optional real vault/blocker if advertised | device permissions, cryptographic/network tests | QUEUED |
| PC05 Deep Scan | Run real modules, inspect/remediate with actual evidence | worker progress/abort, result provenance | QUEUED |
| PC06 Reports | Real stored history and actual PDF/CSV export | Room → export file + tests | QUEUED |
| PC07 App Manager | Discover, search, launch, OS uninstall and authorized settings | PackageManager/UsageStats, return-state tests | QUEUED |
| PC08 Storage Analyzer | Actual scoped files, sorting and authorized file operations | StatFs + MediaStore/SAF, before/after | QUEUED |
| PC09 Battery Saver | Live battery/thermal, alarms and real OS settings | BatteryManager/PowerManager, requery | QUEUED |
| PC10 AI Optimization | Measured recommendation with customer-applied actual supported action | confidence/evidence, state verification | QUEUED |
| PC11 Integration UX | Real routes, authentication boundaries, settings, RTL and no viewport clipping | UI instrumentation + snapshots | QUEUED |
| PC12 Pro Backend | Server-verified real Play purchase, lifecycle and owner-isolated DB | test purchase, RTDN, RLS tests | QUEUED — EXTERNAL CONFIG LIKELY |
| PC13 Release | Combined tests, signatures, CI, runtime and owner release decision | full verified report and APK SHA | QUEUED |

**NEVER** label a screen COMPLETE on build alone, or a code catalog as a working feature. Each row must be updated with exact test and run refs before verdict VERIFIED.


## Current evidence checkpoint — 2026-11-13, Qoder stopped

This checkpoint overrides stale QUEUED/PREFLIGHT interpretations above; individual table rows cannot be marked VERIFIED on earlier test results.

- **PC00 — IMPLEMENTING (uncommitted, partially tested):** Room, DomainModels, KnouxDatabase, PermissionState, SettingsStore and ScanWorker exist. The earlier Kotlin compile and 12/12 JVM tests passed, but the code was edited after those results; rerun exact current tree and investigate migration/runtime coverage.
- **PC01 — UNIT_TESTING (uncommitted, current code not verified):** SmartCleanReal.kt, SmartCleanController.kt, SmartCleanRepository.kt exist with substantial real data/action code; SmartCleanRealTest.kt (5 Android tests) and SmartCleanStorageTest.kt (9 Android tests) were written/updated by 23:57 on Nov 12. No current-source androidTest compile / targeted physical phone instrumentation proof, no verified Trash/Restore/Delete or customer UI path. **VERIFIED=NO**.
- **PC02–PC13 — QUEUED**, except previously recorded visual QA debt for PC11. User-approved physical phone only for strictly isolated synthetic test fixtures. App visuals are preserved.
- **Git:** local main c0530b6; local origin/main ref same; Qoder work remains modified/untracked; no new commit/push seen. LIVE remote SHA not independently fetched.
- **Device:** ADB serial REDACTED_USE_KNOUX_TEST_DEVICE_SERIAL, itel S686LN API 35 observed, but NO latest service runtime tests proved. C ~9.44GB, D ~2.50GB, K ~5.54GB free at audit.
- **Primary evidence:** qa-evidence/e2e/pc01/logs/01-compileDebugKotlin.log, 02-compileDebugAndroidTestKotlin.log, 03-testDebugUnitTest.log and generated app/build/test-results/testDebugUnitTest/*.xml.
- **Next checkpoint:** compile current source; code-review destructive operations in 14 new Android tests; execute only audited tests on exact physical device; verify actual result; update ledger; selective commit/push when fully supported.


## Latest Kiro session audit override — 2026-11-13
This is a factual checkpoint; do not overwrite historical acceptance criteria:
- PC00: UNIT_TESTING. JVM 16/16 pass (PreviewFixtures 3, RealDataRules 13). Room migrations, actual permission/capability and WorkManager customer scheduling acceptance not fully demonstrated; NOT VERIFIED.
- PC01: RUNTIME_TESTING. Eight non-media SmartCleanStorageTest cases PASS on itel S686LN API35; one MediaStore synthetic Trash→Restore→Delete test FAILED/UNVERIFIED due exact-state timeout and correlated ADB disconnect; a later assumeTrue could SKIP rather than pass. Five SmartCleanRealTest UI cases without final report. The complete customer operation remains NOT VERIFIED.
- PC02..PC13: existing task dependencies and previous visual evidence preserved; do not confuse UI existence with full-stack verification.
- Audit: qa-evidence/e2e/KIRO_SESSION_76AE_DEEP_AUDIT_20261113.md.
- Mandatory workflow: docs/workflows/KNOUX_PC_PER_SERVICE_AUTONOMOUS_EXECUTION_20261113.md.
- Post-verified acceptance for EACH service: evidence-backed local main selective commit, fast-forward remote push, remote SHA and CI verification, ONLY THEN inventoried safe generated-output disk reclamation, then next section.


## Verified checkpoint - 2026-11-13 06:12+04 (overrides the Qoder/Kiro checkpoints above for PC00 and PC01)
- **PC00 - VERIFIED @ `722af86ec8a8dbb7463e67d6413cc1dbfaa65909`:** Room schema 1 exported under `app/schemas`, DataStore settings, permission state, `ScanWorker` and repository/controller all compile from the current tree; JVM 16/16 (PreviewFixturesTest 3 + RealDataRulesTest 13), 0 failures/errors/skips.
- **PC01 - VERIFIED @ the same SHA:** `SmartCleanRealTest` 5/5 on the owner-authorized itel S686LN (Android 15 / API 35) covering real scan -> review -> permanent delete of exact UUID cache fixtures with Room history; `SmartCleanStorageTest` 9/9 including OS Trash -> Restore -> Permanent delete of newly inserted synthetic PNGs (media case needs the temporary `READ_MEDIA_IMAGES` grant documented below; it was revoked).
- **What was actually wrong before:** the UI case failed because the first-run splash handed the app over to `OnboardingScreen` mid-test and disposed the Smart Clean screen; the harness now consumes `SplashGate` before the activity launches. Checkbox rows are driven through `SemanticsActions.OnClick` because the checkbox is visually covered by the row overlay on this device.
- **Permissions:** no standing media grant is left on the device (`appops get READ_MEDIA_IMAGES` = `ignore`). Only synthetic fixtures were touched; 27 `Pictures/KNOUX_QA_*` directories and 27 stale synthetic MediaStore `files` rows were removed afterwards.
- **Still not claimed:** release signing, Play Billing, Supabase staging, PC02-PC13, or owner visual re-acceptance. PC02 (Duplicates) is the next task because its dependencies PC00 and PC01 are now satisfied.

## Verified checkpoint - 2026-11-13 13:50+04 (overrides the checkpoints above for PC02 and PC03)
- **PC02 - VERIFIED @ `59806cad7c33f41deb39a78513c23c883b76a8a1`:** 29/29 JVM and 36/36 on-device tests; real duplicate grouping by full-content SHA-256, protected original, physical-file alias collapse, customer-confirmed Trash/Restore/Delete on the owner-authorized itel S686LN API 35. Evidence: `qa-evidence/e2e/pc02/PC02_EVIDENCE_20261113.md`.
- **PC03 - VERIFIED @ `c1b1c2e57cfc8ed665e616fca1132a9fbd9d9278`:** 45/45 JVM and 45/45 on-device tests; measured cache/MediaStore/SAF residue taxonomy, no pre-selection outside KNOUX's own cache, irreversible acknowledgement, provider-verified removal, append-only audit and Room v3 migration. The real UI case deleted one selected 8,192-byte fixture, preserved the unselected 4,096-byte keeper and persisted `REVIEWED -> DRY_RUN -> ATTEMPTED -> VERIFIED`; the DB copy reports `user_version=3`, `integrity_check=ok`. Evidence: `qa-evidence/e2e/pc03/PC03_EVIDENCE_20261113.md`.
- **Device hygiene:** the temporary `READ_MEDIA_IMAGES` grant is revoked (`granted=false`, uid mode `ignore`); stale synthetic fixtures from an aborted media run were removed after their owner package and KNOUX_QA path were verified; no owner file was touched.
- **Still not claimed:** release signing, Play Billing, Supabase deployment, PC04+ or owner visual re-acceptance. PC04 (Privacy Sweep) is the next dependency-satisfied task.
