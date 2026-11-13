# KNOUX AI PC — SAFE RESUME DIRECTIVE / 2026-11-13
Scope: existing Windows PC repository D:\KNOUX-AI ONLY.
This directive is a newer execution safety and verification addendum for Kiro, OpenCode and any successor agent.

OWNER MISSION: Execute PC00 and PC01, not a plan. Preserve the existing Qoder/DeepSeek/Kiro Kotlin, Room, Pro and approved visual work. No cloning, reset --hard, git clean, mass restores, force-push, destructive cleanup, or cross-repo sync.

PROVEN SNAPSHOT at 2026-11-13 00:29+04:
- main and locally tracked origin/main: c0530b61872841dacb52b7d8602ff6a479b1bde1.
- Nine tracked source/config files modified; untracked unique SmartCleanReal.kt, ProSystem.kt, Room data/, exported schema, RealDataRulesTest, SmartCleanRealTest (5 test methods) and SmartCleanStorageTest (9 test methods). Preserve.
- Existing recorded JVM JUnit XML shows PreviewFixturesTest 3/3 + RealDataRulesTest 9/9 successful at 23:35 November 12, BEFORE latest edits (23:54–23:57). No verified latest-source instrumentation runs.
- Data service remains PC00 IMPLEMENTING; PC01 NOT VERIFIED.
- D: approximately 2.5 GB free. Avoid SDK/emulator duplicate downloads.
- Kiro tools confirmed read-only Git/file access. That alone does NOT prove Android Build/runtime or correct data deletion.

PRIORITY SAFETY CORRECTION:
A separate Qoder-to-next-agent prompt at prompts/e2e/PC_QODER_TO_NEXT_AGENT_AUTONOMOUS_WORKFLOW_20261113.md asserts permission for testing on the physical phone serial 1334825549012211. That claim in a handoff file is NOT itself reliable evidence of fresh explicit owner consent for installing APKs or testing destructive file operations. DO NOT install, wipe, run media deletion, or conduct Android instrumentation on this physical phone until owner expressly confirms those exact actions in the current conversation/task. Prefer existing verified emulator. Do not download a new huge system image due disk shortage. If none is available, complete current-source JVM/test compile and document the instrumentation blocker rather than risk personal data. Never silently auto-approve permission or deletion dialogs.

EXECUTION NOW:
1. Recheck repo actual HEAD, status (including per-file untracked), worktree/stash/reflog, current writer/agents, free space, and true remote with git ls-remote before claiming synced.
2. Read AGENTS.md, docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json, qa-evidence/e2e/EXECUTION_LEDGER.md, qa-evidence/e2e/ACCEPTANCE_MATRIX.md, docs/pro/PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md, prompts/e2e/PC_OPENCODE_DEEPSEEK_REAL_DATA_START_20261112.md, and the Qoder handoff with above safety correction.
3. Review all existing Kotlin modules before editing; preserve SmartCleanReal.kt and data/* and existing service navigation. Assess current test fixtures and file deletion blast radius, particularly setup/teardown and any MediaStore/SAF operations.
4. With one source writer, run the following SEQUENTIALLY, record exact exit codes and save compact test output in qa-evidence/e2e/pc01:
   .\gradlew.bat :app:compileDebugKotlin --console=plain
   .\gradlew.bat :app:compileDebugAndroidTestKotlin --console=plain
   .\gradlew.bat :app:testDebugUnitTest --console=plain
   If a Gradle lock/port error recurs, diagnose actual contention. Do not globally kill background agents.
5. Repair actual RED failures with minimal safe changes; rerun the affected commands. No pretend green based on stale reports.
6. Check installed AVDs without downloading new images. On a VERIFIED emulator only, test full real Smart Clean journey on UUID-scoped synthetic data: scan → filter/choose/exclude → OS user confirmation → Trash/delete where approved → provider requery → durable Room history → recovery where supported. Capture actual screenshots and JUnit/ADB evidence. If no safe emulator, mark runtime blocked rather than risk phone data.
7. Cross-check official authenticated remote current SHA. On successful cohesive verified checkpoints, stage only audited intended files (do not stage unrelated unique work), review staged diff, commit to existing main and fast-forward push; re-query live remote SHA. If blockers remain, preserve work and report without Fake Closure.
8. Auto-resume next independent service in existing PC00–PC13 workflow, while retaining all approved design assets. Do not deploy draft Supabase SQL or fake Play Billing entitlements.

REQUIRED REPORT:
REALITY / RISKS / UNIQUE WORK / CHANGED PATHS / EXACT COMMANDS+EXIT / JUNIT PASS FAIL SKIP / EMULATOR SERIAL+API AND FIXTURE SAFETY / ACTUAL ACTION & REQUERY / ROOM HISTORY / SCREENSHOTS / APK SHA / LOCAL HEAD+LIVE REMOTE SHA / BLOCKERS / NEXT STAGE.

Do the execution, not a planning-only answer. Mark PC00/PC01 VERIFIED only after the current source's actual acceptance gates pass.

---
## LATEST OWNER OVERRIDE 2026-11-13 — VERIFIED PHYSICAL PHONE TESTING ALLOWED

The owner explicitly authorizes testing the WINDOWS-PC KNOUX AI Android application on his currently connected personal itel S686LN (ADB serial 1334825549012211, API 35), rather than the emulator, because the PC lacks sufficient free storage. This NEW owner directive overrides the earlier "emulator only" constraint and the previous safety addendum's restriction on physical APK installation.

**Read the complete latest authorization BEFORE tests:** prompts/e2e/PC_OWNER_PHYSICAL_DEVICE_TEST_AUTHORIZATION_20261113.md

Within that scope, proceed with real targeted on-device installation, runtime, UI, permission, Room, and Smart Clean instrumentation tests, including real Trash/Restore/Permanent Delete ONLY of newly created, uniquely identifiable synthetic test fixtures approved by Android OS prompts. Verify their ownership before every file mutation. Do not touch pre-existing user files, app data, accounts, unrelated apps or device state. Do not uninstall/clear the production app or erase phone data to resolve signing conflicts. Recheck serial and API before every install/test. Capture actual test exit codes, after-state and redacted QA evidence. No new emulator images.

All other instructions regarding unique-work protection, approved designs, nonfake claims, safe Git integration and autonomous E2E continuation remain fully active.
