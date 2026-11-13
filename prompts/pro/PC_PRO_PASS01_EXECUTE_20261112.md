# OWNER P0 FULL-FUNCTIONALITY OVERRIDE: Read docs/pro/PC_PRO_OWNER_FULL_FUNCTIONALITY_OVERRIDE_20261112.md FIRST. It supersedes any preview-only / read-only limitation that would prevent actual user-controlled operations available through authorized Android APIs, user permissions, or permitted optional advanced modes. Build working user journeys (including user-approved trash and permanent delete where supported), not display shells. Do not fake unsupported powers, completion or entitlement.

# KIRO / WINDOWS — EXECUTE KNOUX AI PRO PHASE 00 + FIRST SECTION
Mission: full-stack implementation, not planning or cosmetic Pro cards.
Only repo: D:\KNOUX-AI (PC independent project). Do NOT read, merge, or modify phone OpenCode /root/workspace/KNOUX-AI.

READ FIRST, COMPLETELY:
1. docs/pro/PC_PRO_FULLSTACK_MASTER_20261112.md
2. docs/pro/PC_PRO_FEATURE_CATALOG_20261112.json
3. database/supabase/drafts/004_pro_entitlements_DRAFT.sql
4. Actual current ProSystem.kt, DevAccess.kt, MasterScreens.kt, MainActivity.kt and existing service/domain/database/test files.

BASELINE (HISTORICAL AT AUTHORING; REVERIFY):
main HEAD e5b0994555aa181d00d9f75f292e4ead4b602655 tracking origin/main.
ProSystem.kt and DevAccess.kt are unique untracked work. There are concurrent working-tree changes; preserve them and avoid overlapping an active file writer.
No hard reset, clean, force push, indiscriminate restore/checkout, mass overwrite, new clone, branch proliferation or project rewrite.

BEGIN BY RECONCILING REALITY:
- Record HEAD, git status, origin/main, stash, reflog, worktrees, owners of active Kotlin edits and free disk space.
- Determine what Pro UI and activation paths actually do; identify 10 ProSection IDs vs only 6 mapped feature lists.
- Build a complete section/feature matrix from the 50-entry JSON. Unsupported Android promises must be revised or disabled, never marketed as real.
- Determine actual Room/DataStore/WorkManager/Play Billing/Supabase installation and migrations/tests; do not assume any are deployed.
- Note: UI prices 29 AED/month and 249 AED/year are NOT real Play ProductDetails. WhatsApp is support only. Client-generated KX codes are not activation. The temporary owner password/debug override MUST NEVER ship in release.

PASS 00 — REAL PRO FOUNDATION
Implement:
- Stable typed Pro catalog; 10-section mapping; 50 unique features; truthful statuses.
- ProAccessDecision/FeatureCapability/EntitlementRepository, fail-closed paid gate and read-only local cache with expiry.
- Release guard removing hardcoded developer override and random trial-code grant. Local debug test mode may remain clearly marked.
- Billing adapter and server interface can report CONFIGURATION_REQUIRED until Play Console and approved backend are real; NEVER fabricate grant or price.
- Reuse approved KNOUX premium design, global compact inset-safe header and all unique UI assets.
- Unit tests cover catalog completeness, ID stability, missing mappings, unknown feature, offline/expired/cancelled/revoked access, fake-client code rejection and release bypass guard.

PASS 01 — SMART CLEAN PRO (FIRST REAL VERTICAL SLICE)
Feature IDs:
- smart_clean_unlimited: repeatable permission-scoped read-only inventory/reviews; no unlimited privileges.
- smart_clean_scheduled: user-opted WorkManager read-only recurring review; no silent deletion.
- smart_clean_deep_analysis: explainable, deterministic recommendations on actual evidence.
- smart_clean_priority: prioritization within KNOUX's own tasks only; not global CPU/GPU boosts.
- smart_clean_rollback: operation journal and OS-possible restore only; not universal undo.

Implement and connect in THIS current repository:
1. Real Android adapter: only app-owned cache and user-granted MediaStore/SAF storage; use minimum shared read-only primitives rather than rebuilding unrelated service sections.
2. Domain/repository: scoped candidate bytes, source/time/provenance, permissions, explicit errors, idempotency.
3. Durable local Room state: scan snapshots, candidate counts, selected plan, OS approval result, per-URI actual outcome, scheduled work and recoverability.
4. Existing SmartCleanScreen: actual evidence for metrics, controls, selection, review, consent, safe action and report; preserve premium GLASS design/RTL and fixed viewport; never show fixtures as measured data.
5. Real Pro authorization gates based on verified entitlement/OS permissions, not a client boolean or generated code. If Play/Backend unavailable, mark purchase CONFIGURATION_REQUIRED and implement/test the safe non-commercial feature slice independently.
6. Safe user-mediated OS Trash or scoped SAF operations on SYNTHETIC emulator data only, with re-query. NO physical phone install/test/change; verify ADB serial is emulator before any deployment.
7. WorkManager scheduling with correct constraints/cancellation; scheduling cannot silently delete.
8. Actual tests (permission denied/partial grant, keeper protection, changed URI, duplicate execution, canceled OS prompt, partial operation, process death, malformed/expired entitlement, release bypass, RTL/a11y) + real Gradle build/lint/unit tests/emulator instrumentation and screenshots when supported.
9. Document exact commands, exit codes, test counts, APK SHA, screenshots, files changed, current HEAD and remaining owner-only external setup.

DO NOT label the phase COMPLETED unless all independently executable criteria have evidence. Record external blockers distinctly. Do not block other independently executable services because Play products/backend credentials aren't yet configured.

AFTER FIRST SECTION:
Mark each verified Smart Clean feature and its limits in qa-evidence/pro-end-to-end/PRO_EXECUTION_LEDGER.md, preserve unique work, then automatically execute next service (Duplicates), then Residue, Privacy, Deep Scan, Reports, App Manager, Storage, Battery, AI, and finally the authorized commercial/Play release gate.

NO A/B/C QUESTIONS, NO PLAN-ONLY REPLY, NO GENERIC SHELL, NO FAKE PAID STATUS.
BEGIN NOW BY READING THE ACTUAL FILES, THEN IMPLEMENT PASS 00 AND PASS 01.
