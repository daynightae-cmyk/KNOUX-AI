# KNOUX AI PC — Mandatory autonomous per-service WORKFLOW contract
Owner directive updated 2026-11-13. Scope D:\KNOUX-AI ONLY. The Linux-phone repo is independent and off-limits.

## Why this exists
The Kiro sess_76ae4392 audit proved workflowsEnabled=false, agentMode=vibe and no native Kiro workflow run. A workflow JSON that validates is NOT an executed workflow. Therefore the NEXT OpenCode agent must choose its OWN actually supported orchestration (native workflow if available, controller + implementer + reviewer otherwise, sequential gated checkpoints if no subagents), EXECUTE it rather than repeatedly research it, and preserve state so any next agent can resume.

## Persistent source of truth
- docs/workflows/PC_E2E_TASK_MANIFEST_20261112.json — existing PC00..PC13 task IDs, dependencies, status, current stage
- qa-evidence/e2e/EXECUTION_LEDGER.md — append-only checkpoints
- qa-evidence/e2e/ACCEPTANCE_MATRIX.md — functionality acceptance
- qa-evidence/e2e/KIRO_SESSION_76AE_DEEP_AUDIT_20261113.md — exact latest Kiro evidence and gaps
- AGENTS.md — newest owner directives
Do not create a second conflicting task manager or falsify the manifest to make a loop exit.

## Exact pipeline — EVERY service, no exception
1. REALITY & UNIQUE WORK GATE: current pwd, HEAD, status including untracked, remote SHA (git ls-remote), worktrees/stash/reflog, concurrent agents, source ownership, disk/SDK, actual code; preserve all unique work and approved visuals. One writer per overlapping source.
2. SCOPED ACCEPTANCE: enumerate every customer action, actual Android provider/API, permission, input/output, state, failure and UI. A feature catalogue or pretty card is NOT a working service.
3. IMPLEMENTATION: reuse real existing Kotlin/Compose/Room/WorkManager; build actual data flows, selection/filter/exclusion, confirmation, actions, provider requery, persistent history, retry and available recovery. No dummy counters/data/entitlements.
3b. VISUAL IDENTITY GATE (owner directive 2026-11-13): every service is presented through one signature canvas instrument bound to measured state, with minimal text and the UI dominant. Follow `docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md`; prove EN+AR text budget, indeterminate-progress honesty, fog for unmeasured scope, reduced-motion and RTL frames, and three real-state screenshots before VERIFIED. PC01-PC04 are review-only for this rule.
4. CURRENT UNIT/BUILD PROOF: exact Gradle command, process exit, JUnit XML counts, current SHA or dirty-state fingerprint; do not rely on stale tests. Repair actual failures in a bounded loop (three iterations), then diagnose/record a real blocker rather than mask it.
5. DEVICE INSTRUMENTATION: owner-authorized itel S686LN, ADB serial REDACTED_USE_KNOUX_TEST_DEVICE_SERIAL, API35. Recheck before run. Only newly created, UUID-namespaced synthetic test-owned file/MediaStore URIs may undergo Trash/Delete/Restore. Inspect @Before/@After and OS dialogs BEFORE destructive tests. Never wipe/reset or touch existing personal data/apps. Test full interactive customer journey; capture timeouts, skipped tests and provider outcomes honestly. No emulator/system-image download when not required.
6. INDEPENDENT REVIEW: if genuine reviewer/subagent exists, task it to examine source and actual logs/JUnit/screenshots; otherwise perform explicitly labelled separate self-audit. Fail if required user action is only preview, if tests skip an acceptance-critical path, or if permission/MediaStore result isn't really verified.
7. INTEGRATION GATE: ONLY after an acceptance-worthy coherent batch, inspect exact tracked/untracked paths, stage exact intended files, review git diff --cached, commit on existing main, fast-forward push (never force), read git ls-remote and CI status. If remote changed unexpectedly or a writer is active, preserve work and document BLOCKED_INTEGRATION; do not blindly merge or force push. Do not stage unrelated unique incomplete work. No claims of RELEASED solely from build/push.
8. SPACE RECOVERY GATE — AFTER successful push and evidence retention: record free bytes before/after; audit sizes and reclaim ONLY redundant, reproducible, untracked GENERATED artifacts (stale Gradle intermediary/test outputs, obsolete duplicate APKs no longer needed, abandoned download fragments) once no process uses them. NEVER delete Kotlin/source/tests, Room schema/migrations, assets, authored QA, notes, .git, unique untracked, user data, model session work, active APK or unverified Build output. Preserve newest test XML and screenshots/evidence. Dry-run inventory first; mark DELETE_CANDIDATE with exact bytes and reason; if uniqueness/ownership unclear, SKIP. Never run reset --hard, git clean, force-push, repository-wide deletes or cache erasure before completed verification. A validated Git commit is NOT proof a file is disposable.
9. LEDGER & AUTO-NEXT: record status transitions, test and runtime outcomes, local+remote SHA, CI, blockers, actual reclaimed bytes and next dependency-ready PC task. Automatically advance within current live agent session. At credits exhaustion persist exact restart command and file ownership. Do not claim unattended background execution.

## First section resumes at exact current evidence
- PC00: Data foundation partially implemented, 16/16 JVM pass as of 02:05:36 on 2026-11-13; migrations/integration still need verification. Keep NOT VERIFIED until all critical proofs pass.
- PC01: 8/8 non-media SmartCleanStorageTest instrumentation passed on API35; MediaStore OS Trash→Restore→Delete FAILED/UNVERIFIED and last SmartCleanRealTest was launched without a final result. Current task RUNTIME_TESTING, NOT VERIFIED.
- A 20-second MediaStore timeout was changed to assumeTrue; a SKIP does not count as full service success. Diagnose real device/lifecycle/USB/provider behavior.
- PC02–PC13 remain dependency-ordered and QUEUED; do not restart completed visual work. When a task cannot proceed due to a genuine external block, record it and choose another independent dependency-ready task.
- Free disk is low (~2.38GB D at audit). Prioritize compact logs and avoid creating duplicates or large emulator images.

## Mandatory per-service record
Service ID | Baseline HEAD | Live remote SHA | Unique work & file ownership | status | changed paths | accepted user journeys | Gradle+exit+JUnit pass/fail/skip | device API/serial | actual synthetic-only executed action | provider state before/after | Room history | screenshots/logcat | visual identity (instrument, idle/running/result, RTL, reduced-motion, copy budget) | QA verdict | local commit | remote verified SHA/CI | cleanup candidate/reclaimed bytes | NEXT.

## Explicit quality promises
No fake closure. No preview-only paid services. No claiming user data removal or Storage freed bytes without requery. No silent deletion of personal content. No assumed server entitlements. No loss of unique work. Existing main is the integrated product; everything useful gets integrated after true proof, not abandoned on unmerged branches. No text-only service results: execution and outcome must be carried by the service's visual instrument, with text as a minimal caption layer.