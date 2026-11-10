# SERVICE MISSION: 02 Duplicate Files Finder

**HARD PREREQUISITES**: Owner approved 10-screen UI; previous service evidence accepted; current Git/CI/worktree/Android realities inspected; no concurrent agent changes overwritten.

You are Codex. This is an IMPLEMENTATION task. Preserve approved design references. Work only in canonical `D:\KNOUX-AI` if actually reachable. Use the per-service workflow from `../PHASE_2_MASTER_SERVICES.md` and source links in `docs/policies/ANDROID_PLAY_CONSTRAINTS_2026_11_10.md`.

## Functional scope

Implement exact duplicates by size prefilter + streamed SHA-256 for user-authorized MediaStore/SAF URIs; hash locally, no upload. User chooses retained original, reviews groups and potential space. Trash only via Android OS-mediated request and re-query after successful result. Tests: identical, same-size distinct, inaccessible URI, changed file, revoke, cancel, partial photo grant; no blind deletes or 100% match confidence myths.

## Exit gate

Implement domain repository, integration adapter, view model, actual UI wiring, local Room migration, error/denial/cancel flows, test fixture, instrumentation and documented unsupported states. Run actual build/emulator/screenshots/CI and reconcile final Git SHA. Optional Supabase sync only after user approval and RLS verification. Don't claim PASS without stdout exit code, runtime screenshot and reproducible logs. Only remove proven disposable project-owned build outputs following protection audit. STOP after this service and show full acceptance ledger.
