# BASELINE — knoux-ai-visual-finish (fourth visual pass)

Recorded: 2026-11-11, Asia/Dubai (UTC+04:00). This pass is Phase 1 UI only.

## Project identity

| Item | Value |
|---|---|
| Repo / workspace | `D:\KNOUX-AI` (the only worktree: `git worktree list` → `D:/KNOUX-AI 68670c3 [main]`) |
| Framework | Android native, Kotlin 2.2.21 + Jetpack Compose (AGP 8.13.2, Gradle 8.14.6, JDK 21.0.12) |
| App ID / version | `com.knoux.ai` / `0.1.0-preview` (debug) |
| SDK levels | min 26, target 36, compile 36 |
| Branch | `main`, local commits only; **no remote configured, nothing pushed** |
| Pre-commit HEAD | `68670c3dfadb8e9761e65eef730c88208f1552cf` — `Add Phase 1 owner-review handoff` |
| Stash | empty |
| Final APK | `qa-evidence/knoux-ai-visual-finish/KNOUX-AI-phase1-visual-finish-debug.apk` (13,269,818 bytes) |
| Final APK SHA-256 | `343FC50947A5396E7AD3BE00DF00106ED1C5994724207A0E3BE33F7B8274FEF2` (byte-identical to `app/build/outputs/apk/debug/app-debug.apk` at capture time) |

## Runtime target used for every capture

| Item | Value |
|---|---|
| Device | `emulator-5554`, AVD `kx_preview` |
| OS | Android 8.0 / API 26, x86_64 |
| Renderer | SwiftShader software GPU, headless boot `-no-snapshot -no-snapshot-save -no-audio` |
| Display | physical 1080×1920; logical override `1080x2340` active during all captures; 420 dpi |
| Screenshot size | PNGs are 1080×1920 (the physical framebuffer; the framework lays out for 1080×2340 and scales) |
| Language | English (Arabic RTL verified by instrumentation, see TEST_RESULTS.md) |

## What this pass preserved

No destructive git operation was performed in any pass. Preserved and committed in this pass:

1. The interrupted fourth-pass source work found uncommitted at `68670c3`:
   `DetailPreview.kt`, `DetailPreviewData.kt`, `SplashAtmosphere.kt`, `SplashChime.kt`,
   sample drawables (`app/src/main/res/drawable-nodpi/`), the splash click sample
   (`app/src/main/res/raw/knoux_smile_click.wav`), and edits to `HomeScreen.kt`,
   `KnouxComponents.kt`, `KnouxSplash.kt`, `MainActivity.kt`, `NeonArt.kt`,
   `PreviewData.kt`, `ReferenceScreen.kt`, test files and scripts.
2. Twenty committed-reference candidates found untracked and now committed:
   `design/reference-20261111/` (10 outer incl. `02_Splash.png`) and
   `design/reference-inner-20261111/` (10 inner). All 20 files remain independent PNGs,
   used strictly as visual targets (never as runtime assets or hit-box backdrops).
3. All prior evidence folders: `qa-evidence/ui/` (third pass, refreshed runtime PNGs),
   `qa-evidence/phase1-polish/`, `qa-evidence/splash/` (incl. `refine/`),
   `qa-evidence/build/`, `qa-evidence/apk/`.
4. The owner brand source of truth `design/brand/knoux-logo-owner.png` — unchanged.

## New work in this pass (summary)

- Home fit compaction (all six service cards above the bottom navigation at the capture size; 48 dp targets kept).
- Stronger dashboard colors (violet `#843BFF`/`#B047FF`, cyan `#00DCFF`/`#21B9FF`, pink `#E34CFF` family) in `KnouxComponents.kt` / `NeonArt.kt` so panels do not read washed out.
- Splash wordmark fix: per-letter reveal clip no longer shears the faux-italic overhang (i-dot, stem tops) once a letter lands — see SPLASH_STORYBOARD.md.
- Full re-verification with the final APK: build, unit, lint, 10/10 instrumentation,
  20 screen captures + comparisons, splash re-record with click-once proof, walkthrough video.
- Complete evidence set in this folder (`SCREEN_MAP.md` indexes everything).

## Constraints honored (unchanged from the master prompt)

Phase 1 is UI only: no real scanning/cleanup/deletion, no Room/Supabase/services,
no privileged permissions (root / Accessibility / Shizuku / `MANAGE_EXTERNAL_STORAGE`).
All numbers shown in the UI are labeled sample/estimated design data. This baseline
does not claim visual approval; owner review is still pending.
