# KNOUX AI PC — QODER STOPPED / AUTONOMOUS WORKFLOW TAKEOVER
Date: 2026-11-13 00:09+04
Workspace: D:\KNOUX-AI
IMPORTANT: This is the WINDOWS PC Android repo. The independent Linux-phone repo /root/workspace/KNOUX-AI is off limits.

## 0. AUTHORITY AND EXECUTION RULES
Owner requests an immediate takeover from Qoder with explicit priority on the durable End-to-End workflow. Mission: REAL execution, not planning, rebuilding or fake closure.
Owner has EXPLICITLY AUTHORIZED use of his connected PHYSICAL Android phone for testing, superseding the historical emulator-only restriction for this PC project. But deletion in tests is restricted to fresh uniquely identifiable synthetic test fixtures created by the current test. Preserve personal photos, existing files, apps, app data, identities and customer content.
Preserve accepted KNOUX visual design (onboarding, login, OTP, logo, animations, Arabic and English). Modify visuals only for proven integration regressions.
Never git reset --hard, git clean, force-push, blind restore, mass-stage unknown files, delete untracked unique work, or create duplicate repositories, backups, SDKs or emulators. One writer per shared file. Do not switch to the independent Linux project.

## 1. INDEPENDENTLY OBSERVED BASELINE AT AUDIT
- Local main HEAD: c0530b61872841dacb52b7d8602ff6a479b1bde1.
- Local origin/main tracking ref: same SHA. Actual LIVE remote SHA was not independently queried; check git ls-remote before claiming remote synchronization.
- Qoder.exe and its command shells were NOT running during audit. Exact reason Qoder stopped is NOT established; do not invent a credits failure or build failure.
- PC00/PC01 are UNIQUE UNCOMMITTED WORK, not merged/published. Modified tracked: AGENTS.md, app/build.gradle.kts, AndroidManifest.xml, KnouxComponents.kt, MainActivity.kt, MasterScreens.kt, NeonArt.kt, values-v31/styles.xml, root build.gradle.kts. Untracked: SmartCleanReal.kt, app/src/main/java/com/knoux/ai/data/, ProSystem.kt, Room schema JSON, two Android test files, JVM tests, docs, prompts, QA, and Supabase DRAFT.
- Real data code: DomainModels.kt, KnouxData.kt, KnouxDatabase.kt, PermissionState.kt, ScanWorker.kt, SettingsStore.kt, SmartCleanController.kt, SmartCleanRepository.kt.
- New Qoder Kotlin edits were observed at 2026-11-12 23:54–23:57. Earlier build/test output from 23:32–23:35 is STALE relative to the exact current tree.
- Earlier evidence qa-evidence/e2e/pc01/logs:
  * 01-compileDebugKotlin.log: a previous compiler PASS.
  * 02-compileDebugAndroidTestKotlin.log: BUILD SUCCESSFUL in 17s, before last edits.
  * 03-testDebugUnitTest.log: BUILD SUCCESSFUL in 22s, real XML suites PreviewFixturesTest 3/3 and RealDataRulesTest 9/9, total 12/12. Before last edits.
  * Initial attempt failed at Gradle FileLockContentionHandler BindException; retry passed. Not proof that new source is verified.
- NEW UNVERIFIED instrumentation classes: SmartCleanStorageTest.kt (9 @Test methods, 33265 bytes, modified 23:57) and SmartCleanRealTest.kt (5 @Test methods, 13554 bytes, modified 23:54). Neither has a final successful current-source compile or on-device test report.
- SmartCleanReal.kt 64958 bytes; SmartCleanController.kt 35592 bytes; SmartCleanRepository.kt 38543 bytes. KEEP THEM; no rewrite-from-zero.
- ZERO confirmed successful connectedDebugAndroidTest executions on the latest source; no witnessed real trash/restore/permanent-delete workflow or current APK hash after 23:57.
- Physical ADB device currently connected: serial REDACTED_USE_KNOUX_TEST_DEVICE_SERIAL, itel S686LN, API 35. Reconfirm exact serial immediately before any installation/test. Existing com.knoux.ai and com.knoux.ai.debug packages were seen. A separate user-999 access error was observed, so never assume work-profile access or wipe profiles.
- Disk free near audit: C 9.44 GB / D 2.50 GB / K 5.54 GB. Qoder task output showed an approximately 720MB emulator image download attempt; DO NOT download or create another emulator image.
- Root git has one worktree, no stash shown. No local commit from Qoder was observed. No Kiro/Qoder source writer confirmed active during this audit.

## 2. MANDATORY WORKFLOW FILES — READ FIRST
1. AGENTS.md (latest owner update supersedes historical UI-only and emulator-only rules).
2. docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json.
3. docs/workflows/KNOUX_PC_AUTONOMOUS_E2E_WORKFLOW_20261112.md.
4. qa-evidence/e2e/EXECUTION_LEDGER.md and ACCEPTANCE_MATRIX.md.
5. docs/pro/PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md.
6. prompts/e2e/PC_OPENCODE_DEEPSEEK_REAL_DATA_START_20261112.md.
7. Actual SmartCleanReal.kt and every file in app/src/main/java/com/knoux/ai/data/.
8. Actual SmartCleanRealTest.kt and SmartCleanStorageTest.kt.
9. Supabase draft for comparison ONLY, not deployment.

Use EXISTING task IDs PC00–PC13. Do not create a conflicting second backlog. At every checkpoint append evidence to the ledger and update manifest status, timestamp, SHA, tested device, exact command, actual result, blocker, next action. Controller + one code writer + independent reviewer + safe integration gate (logical roles unless supported). Persistent progress must survive model changes, even if agent execution cannot continue unattended.

## 3. NEXT ACTIONS — IMPLEMENT, REPAIR, TEST
A. BEFORE CODING: check current pwd, HEAD, git status --porcelain -uall, worktree, stash, reflog, git ls-remote origin refs/heads/main; active writers; free space; Gradle cache; exact phone with adb devices -l and getprop ro.build.version.sdk. Do NOT change unrelated files.
B. CURRENT SOURCE BUILD — sequentially run and capture exit codes and compact logs:
   .\gradlew.bat :app:compileDebugKotlin --console=plain
   .\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain
   .\gradlew.bat :app:testDebugUnitTest --console=plain
If bind exception recurs, inspect Gradle contention and use bounded retry. No global service kill, no speculative source edits. Fix actual errors preserving implementation and rerun after edits. Count exact JUnit pass/fail/skip.
C. PHYSICAL DEVICE SAFETY AUDIT, BEFORE running instrumentation:
- Inspect ALL @Before/@After and every delete / trash / restore / MediaStore transaction in BOTH new Android tests, plus controller and repository. Their comments might still say emulator-only; newest owner authorization allows phone testing ONLY for strictly isolated synthetic fixtures.
- Operations must target precisely new UUID fixture files in app-owned cache/files or MediaStore items created by KNOUX during the test. For each MediaStore URI, immediately validate owner package, relative path, name, URI and expected size before OS Trash/Delete.
- Never blindly approve a system dialog of unknown origin. Never reset phone, delete app data, uninstall unrelated apps, revoke personal permissions, clear account/profile storage, or delete preexisting photos/docs.
- Do not run full unknown Android instrumentation suite on a personal device. Only run targeted audited SmartCleanRealTest and SmartCleanStorageTest classes, with correct runner and exact serial. If method could affect preexisting data, SKIP that specific test and document why.
D. ON-DEVICE VERIFICATION after safe compile:
- Verify install target serial REDACTED_USE_KNOUX_TEST_DEVICE_SERIAL and API 35. Run targeted instrumented UI and repository tests on real device.
- Verify scoped scan; filters/search/selection; exclusions; confirmation/cancel; local cache deletion; MediaStore Trash, restore and permanent delete where supported; stale URI handling; operation idempotency; persisted Room history; requery of exact fixtures; denied and partial permission handling. An Assume skip is NOT a PASS.
- Save JUnit XML, compact redacted logs/screenshots, permissions scope, exact fixture IDs, exit codes, APK SHA and observed after-state. Do not upload personal files to cloud. Do not claim a commercial Pro license exists because ProSystem displays one.
E. INTEGRATION:
- PC00 foundation = Room schema and real migrations, capability/permission/WorkManager/DataStore invariants + current compilation and tests.
- PC01 Smart Clean = actual customer-controlled scan → select → confirm → perform user-authorized action → re-query → persistent operation history + failure/restore, verified on phone.
- No DONE, VERIFIED or RELEASED until evidence covers latest tree. Protect any remaining unrelated unique untracked work.
- Stage only intended inspected files, diff --cached review, commit coherent tested batches on existing main; push fast-forward without force, verify git ls-remote matches. Distinguish local commits from published, deployed or release-ready.
- Supabase 004_pro_entitlements_DRAFT.sql not deployed; Google Play Billing and subscriptions not verified.
F. AUTONOMOUS CONTINUATION:
PC02 Duplicates; PC03 App Residue; PC04 Privacy; PC05 Deep Scan; PC06 Reports; PC07 App Manager; PC08 Storage; PC09 Battery; PC10 AI Optimization; PC11 Home/Auth/Settings integration; PC12 verified billing+approved staging backend; PC13 full release gate. After independently VERIFIED checkpoint automatically advance in same active session. Log external blockers and proceed to next independent section; no preview-only closure, no fake pass. Persist resume action before agent limits interrupt.

## 4. EVIDENCE DELIVERY CONTRACT
REALITY / RISKS / UNIQUE WORK / CURRENT & LIVE REMOTE SHA / TASK ID+STATE / CHANGED PATHS / TEST COMMANDS AND EXIT CODES / JUNIT PASS-FAIL-SKIP / ADB SERIAL+API / EXACT SYNTHETIC SCOPE / OBSERVED ACTION+PROVIDER REQUERY+ROOM JOURNAL / VISUAL SCREENSHOTS & REDACTED LOGS / APK HASH / COMMIT & PUSH SHA / BLOCKERS / NEXT SAFE WORKFLOW ACTION.

START by reading current manifest and reconciling it with actual working tree, then run CURRENT builds and targeted safe phone verification. Do not respond with a plan-only message. Never touch independent Linux mobile repo.