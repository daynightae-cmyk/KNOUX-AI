# Source/Evidence Review (read-only as of November 15, 2026)

## Facts confirmed
- The uploaded `knoux-ai-json.json` is an OpenCode session export with 321 messages. It identifies directory `D:\KNOUX-AI`, agent `build`, model `deepseek-v4.1-flash-free`, session `ses_e23a77d35ffe2vDhN35lOjsXwc`. Its `outcome=succeeded` is an agent-session outcome, not independent feature QA.
- The previous agent's final report claims `:app:assembleDebug` successful, `adb install -r` on Samsung SM-A025F successful, main tabs navigable, and working tree changes uncommitted. Treat this as historical report; no independent E2E proven for all sections.
- The uploaded `shots.rar` had 47 entries, including screenshot versions v2/v3/v4/v7. Two entries (`secure-check.png`, `err.txt`) were zero length and are omitted here. This bundle contains 12 representative existing-runtime screenshots extracted and verified nonzero.
- User-provided 10 PNG references: each 941x1672 pixels. They are **conceptual visual references**, not true device measurements.
- Read-only Windows inspection: D:\KNOUX-AI HEAD `78391aa927b3629fcf856de04b10dae23fc2881c`, live `origin/main` same at the time. Worktree 22 modified/untracked files, including IdentityKit.kt, NeonKit.kt, StorageScreens.kt, DeviceInsights.kt (untracked). Other worktree `K:\KNOUX-AI-NATIVE-UI-20261114` at 6f1d739. Must recheck live.
- A file `D:\KNOUX-AI\app\build\outputs\apk\debug\app-debug.apk` with size 17,014,748 bytes was observed locally; no new build, APK SHA, installation or visual QA was performed during this handoff. Date in observed listing: 2026-11-15 04:54:46 local.

## Critical gap
The historical runtime shots do not match the rich full-screen geometry, card hierarchy, consistent gradients, icon surfaces, and detailed component layouts in the ten supplied reference images. Many screens show honest empty/permission states, while the conceptual references show filled demo data. Restore *real* hierarchy and high-end design without faking filled data.

## Visual proof and risk warnings
- Previous agent temporarily manipulated `privacy_secure_window` through raw byte patch of DataStore and needed a repair. It reported restoration and app launch but this is not a substitute for an independent integrity check; no similar byte-level operations allowed.
- Previous output screenshots include a visible third-party chat-head overlay; do not mistake it for KNOUX design and do not crop it in a way that hides UI differences.
- Prior screenshots are test evidence at their original time, NOT latest production evidence.
- `K:\KNOUX-AI-Pro` is a separate repository and must remain untouched. `Pro` crown within D app is based on authenticated entitlement only.
