# HANDOFF TO PERSONALIZATION — what is genuinely left after Phase 1 UI

Status: Phase 1 UI only. Owner visual approval **not yet given**. Everything below is
the honest remaining delta toward a real product; nothing here is implemented, and no
service code has been dropped into the app.

## 1. Real data services (Phase 2)

| Gap | What the UI pretends today | Needed for reality |
|---|---|---|
| Scan engine | "Sample scan complete · 128,430 example files" | A real storage/index scan honoring scoped storage; progress + cancel semantics already stubbed in state |
| Cleanup engine | "Clean Selected → Confirm preview → No action performed — UI preview only." | Real deletion only after owner approval, per-item review, audit log; protected paths (`/Android/data` of other apps) stay untouched |
| Duplicate engine | Sample groups with keep-one preview invariant | Real hashing/matching, thumbnails, canonical-original guarantee |
| Privacy risk | Sample permission categories, "7 trackers found" | Real permission inventory + package analysis; distinguish measured vs inferred risk; OS-settings handoff (no fake blocking) |
| Residue | Sample candidates | Real accessible-URI inventory; exclusion list; scoped deletion |
| App Manager | Sample installed-app list | Real `PackageManager` inventory + usage where permitted |
| Storage / Battery | Sample categories, estimated battery gain | `StorageStats`, `BatteryManager`; label unavailable values honestly |
| Reports / AI recommendations | Sample history and estimated deltas | Persisted history, evidence-backed recommendations with source/impact/risk |

Suggested backends already referenced by the owner's materials (NOT wired): Room for
history/audit, Supabase for sync. Schemas in `prompts/phase2`, `database/`,
`android/integration` remain reference material only.

## 2. Platform-correct capabilities

- No `MANAGE_EXTERNAL_STORAGE`, root, Accessibility or Shizuku shortcuts were used or are
  proposed. Everything must pass Play policy and scoped-storage rules.
- Deep links: the debug route extras (`knoux.preview.route/still/splash.time`) must never
  ship in release builds (they are `BuildConfig.DEBUG`-gated; verify in the release task).
- Sound: the click already honors silent/DND/muted-volume. Real-device latency and
  audio-video sync still need one recorded check on hardware.

## 3. Design decisions the owner must make

1. Approve/adjust the splash lockup scale vs the reference (runtime is slightly larger;
   smile thinner — see SPLASH_STORYBOARD.md deltas).
2. Approve Home hero scale/detail vs the reference (vector orb is simpler than the mock).
3. Decide which inner rows stay simplified/taller (touch-first) vs matched tighter to
   the reference density.
4. Approve the four-tab bottom navigation mapping (Home / Clean / Tools / Reports).
5. Confirm Arabic copy for every new label (RTL verified structurally; copy review pending).

## 4. Safe next actions (prioritized, no destructive steps)

1. Owner reviews `gallery-outer.png`, `gallery-inner.png`, all 20 `comparison-*.png`
   strips, `splash/sequence.png`, `splash/reference-comparison.png` and the two videos.
2. One physical-device session: record splash + walkthrough, confirm smoothness/audio.
3. After approval: Phase 2 begins with the scan/index engine and Room history schema —
   services first, still no deletion without explicit per-feature approval.
4. Keep every `qa-evidence/*` folder; they are the regression baseline for later passes.

## 5. What must not regress

- The brand K / wordmark geometry (owner logo is the authority).
- The keep-one duplicate invariant and the confirm-before-any-real-action pattern.
- Honest labeling of sample/estimated/unavailable values.
- Reduced-motion and silent-mode behavior; splash never replays on resume.
