# SERVICE MISSION: 05 App Manager

**HARD PREREQUISITES**: Owner approved 10-screen UI; previous service evidence accepted; current Git/CI/worktree/Android realities inspected; no concurrent agent changes overwritten.

You are Codex. This is an IMPLEMENTATION task. Preserve approved design references. Work only in canonical `D:\KNOUX-AI` if actually reachable. Use the per-service workflow from `../PHASE_2_MASTER_SERVICES.md` and source links in `docs/policies/ANDROID_PLAY_CONSTRAINTS_2026_11_10.md`.

## Functional scope

Implement PackageManager scoped package inventory with Android 11+ visibility filtering, optional user-granted UsageStats and system app-settings intents. Design for limited/unknown installed apps state; no QUERY_ALL_PACKAGES without Play approval. Do not clear other-app data, uninstall silently, or show fabricated updates. Test limited visibility and denied usage access.

## Exit gate

Implement domain repository, integration adapter, view model, actual UI wiring, local Room migration, error/denial/cancel flows, test fixture, instrumentation and documented unsupported states. Run actual build/emulator/screenshots/CI and reconcile final Git SHA. Optional Supabase sync only after user approval and RLS verification. Don't claim PASS without stdout exit code, runtime screenshot and reproducible logs. Only remove proven disposable project-owned build outputs following protection audit. STOP after this service and show full acceptance ledger.
