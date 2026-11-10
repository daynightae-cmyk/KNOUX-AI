# SERVICE MISSION: 06 Battery Saver

**HARD PREREQUISITES**: Owner approved 10-screen UI; previous service evidence accepted; current Git/CI/worktree/Android realities inspected; no concurrent agent changes overwritten.

You are Codex. This is an IMPLEMENTATION task. Preserve approved design references. Work only in canonical `D:\KNOUX-AI` if actually reachable. Use the per-service workflow from `../PHASE_2_MASTER_SERVICES.md` and source links in `docs/policies/ANDROID_PLAY_CONSTRAINTS_2026_11_10.md`.

## Functional scope

Implement BatteryManager battery level/current/state and PowerManager thermal/discharge-prediction when supported. Show unsupported when health/cycles unavailable. Recommend user settings intents; never fabricate +18%, 6h42m, 287 cycles or force hibernation. Tests OEM null/unavailable readings, charging, low power, API 26 and 36.

## Exit gate

Implement domain repository, integration adapter, view model, actual UI wiring, local Room migration, error/denial/cancel flows, test fixture, instrumentation and documented unsupported states. Run actual build/emulator/screenshots/CI and reconcile final Git SHA. Optional Supabase sync only after user approval and RLS verification. Don't claim PASS without stdout exit code, runtime screenshot and reproducible logs. Only remove proven disposable project-owned build outputs following protection audit. STOP after this service and show full acceptance ledger.
