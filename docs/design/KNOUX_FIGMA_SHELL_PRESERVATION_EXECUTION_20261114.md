# KNOUX AI — OWNER'S FIGMA-FIRST NATIVE ANDROID SHELL CONTRACT
Date: 2026-11-14 | Canonical PC repo: D:\KNOUX-AI
Figma file: https://www.figma.com/design/MLUbH2QkR1tOlj0qzk2O4e/Design-a-futuristic-smart-home-app-UI-with-Figma--Community-?node-id=2010-6
Authenticated owner account: knouxops@gmail.com (verified via Figma connector).
Original KNOUX AI artwork supplied: violet/magenta/cyan curved streaks and KNOUX AI logo. Preserve proportions/colors, not an all-screen wallpaper.

## PRIMARY DIRECTIVE — DESIGN'S NATIVE SKELETON IS IMMUTABLE
User rejects prior "futuristic glow / 3D / giant hero" prompts and neon UI. Faithfully reuse the FIGMA COMPOSITION (screen structure, native header, padding, list density, tactile controls, scroll geometry, bottom nav and material surfaces), replacing smart-home content with KNOUX AI's real device utility actions. Apply only disciplined, minimal violet/cyan highlights and slim decorative brand waves on designated edges. NO sci-fi particle rain, no laser graphics, no full-page glow, no high-saturation card outlines, no glass explosions, no random arrows. No website, landing page, iOS cutout, poster or static screenshot in place of Android Compose.

## REALITY FIRST — NO BLIND EDITS
The repo was directly checked. main HEAD and origin/main matched fbfc1e3dcc4e985087e16f8fb11383e2a29e8159 at check time. At least 19 modified tracked source/test files exist NOW and multiple agent/QA artifacts are untracked. OpenCode process is running. NEVER overwrite these unique modifications. Re-run git status, diff, branch/worktree/stash/reflog, review active agents and files BEFORE touching code. If another writer owns HomeScreen/MainActivity/MasterScreens/KnouxScaffold/AuthScreen, write BLOCKED_CONCURRENT_WRITER and defer conflicting modifications. Do NOT reset --hard, clean, force-push, delete, rewrite history or merge the independent mobile recovery project.

## READ THE ACTUAL FIGMA — NOT SPARSE METADATA OR GENERIC PROMPTS
The connected Figma file currently contains 10 KNOUX AI sections, all 360x800dp:
01 Home 2010:2; 02 Deep Scan 2010:5; 03 Smart Clean 2010:7; 04 Duplicate Files 2010:8; 05 App Residue 2010:9; 06 Privacy Sweep 2010:10; 07 Battery 2010:11; 08 AI Recommendations 2010:3; 09 Reports 2010:4; 10 App Manager 2010:6.
For each screen use Figma screenshot PLUS high-fidelity actual child-frame design context. Figma source read-only. Preserve original master Figma and approved historical visuals. Do not silently replace a creative reference with a crude generated frame.

Measurements verified from actual Home Figma:
- 360x800dp native screen, Android status bar 24dp, top app toolbar y24..80 = 56dp, fixed bottom navigation y696..776 = 80dp, system gesture inset y776..800 = 24dp.
- Main content scroll area y80..696, horizontal padding 20dp, user actions touch target 48dp.
- Compact Storage overview x20,y88,320x108; Start Deep Scan x20,y208,320x48; compact tabs x20,y268,320x36; service grid x20,y316,320x300 with six real cards in two columns at 154x92 and 12dp gutter; small Recent Activity at x20,y628.
- Bottom customer navigation: Home, Clean, Tools, Reports, each occupies 90dp, Android native selected indicator. No overlapping brand/dev badge/extra actions.
- All percentages and metrics in Figma are design fixtures, NOT live results. Android production must connect REAL StateFlow/repositories and label demo status when still sample.

App Manager Figma shows 56dp back/search toolbar, summary x20 y92 320x94, filter chips x20 y198 320x36, rows 68dp collapsed / 152dp expanded, OS uninstall consent. Do not ship fictional Figma demo app names or stats.

## STYLE TOKENS AND BRAND TREATMENT
- Deep graphite/navy near-black (clean solid base), tactile panels one elevation step lighter, subtle controlled stroke or shadow, clear typography. Purple from original logo for ACTIVE selection; cyan for one critical data accent. Warm amber reserved for caution/occasional micro-accent. Do not turn the whole app blue/purple neon.
- Curved wave echoes from supplied KNOUX AI image: a THIN low-opacity magenta-to-violet arc in splash/onboarding and one modest background strip or corner per screen. A restrained cyan parallel arc only where it does not compete with content. Preserve exact original logo; do not redraw or stretch it.
- App toolbar 56dp, status/gesture insets, 16-20dp horizontal spacing, 8/12/16/20/24dp design rhythm, 48dp hit boxes, real body scrolling, one primary CTA per viewport. Compact typography, accessible contrast, 320dp/360dp/412dp widths, 1.0/1.3/1.5 font scale, EN/AR and RTL/LTR layouts.
- Compose Material 3 native Scaffold and genuine state/navigation. Never build a WebView or pretend a static screenshot is a functioning UI. No shader that repaints huge frames continuously. Honor Reduce Motion.

## TEN SCREEN ADAPTATIONS — SHELL STAYS, PRODUCT DOMAIN CHANGES
01 HOME: exact compact Figma toolbar + real Storage overview donut + one Start Deep Scan + segmented Clean/Manage/Protect + 2x3 real-service grid Smart Clean, Duplicate Media, App Residue, Storage, Privacy, Apps + recent history and fixed bottom tabs. No full-screen hero artwork.
02 DEEP SCAN: from tactile circular control reference to a data-bound, measured scan ring, real scope modules/progress, expandable real results, selectable item rows. Scanned bytes ARE NOT reclaimable bytes. Avoid 320dp fixed instruments hiding results.
03 SMART CLEAN: data-bound storage category summaries, safe/eligible markers, individual and grouped review, selections with exact bytes, explicit Android Trash/Delete consent, no one-click undisclosed deletion.
04 DUPLICATES: native photo/video grid with actual thumbnails, exact hash groups, protected keeper, file preview, select redundant copies, exclusions, user-authorized Move/Copy/Trash. Do not replace media grid with decorative Mirror art.
05 RESIDUE: real folder explorer and provenance confidence, per-file keep/ignore/review, OS consent, no automatic dangerous cleanup.
06 PRIVACY: precise checked permissions and evidence, Android Settings intents, accessible vs inaccessible states. No fictional security score.
07 BATTERY: tactile Figma circular gauge reused for actual BatteryManager level/charging/temp; recorded trends if they exist, honest unknown health/estimates, no fake improvements.
08 AI: evidence-backed recommendation list with provenance, confidence, review/apply user authorization, truthful actions and verification.
09 REPORTS: readable charts driven by Room journals, source dates, drilldown, period filters; honest empty states if data missing.
10 APP MANAGER: preserve actual Figma compact expandable installed app rows and filter chips; real installed apps via PackageManager and OS-owned app info/uninstall prompts.

Each of ten screens must share native header typography, spacing, panel radii, 48dp touch control, selected states and correct back navigation, but never duplicate giant decorative hero cards. Show actual product functions. Keep approved Splash smile, onboarding, Google Sign-In, passkeys, biometric, Supabase config, original assets and every working service unchanged unless scope and evidence require local adaptation.

## SAFE EXECUTION GATES
A. Check reality and active writer ownership; preserve uncommitted unique code. If conflicted, produce diff-specific handoff and STOP modifying those files.
B. Capture Figma screenshots, read high-fidelity nodes and inspect current Android runtime screenshots. Map exact geometry, control routes and states.
C. Implement HOME ONLY after safe code ownership is proven; inspect current Kotlin and reuse components, never rewrite from zero.
D. Run compileDebugKotlin, testDebugUnitTest, assembleDebug, relevant Android instrumentation on physical verified phone (never implicit emulator), compare screenshots at 320/360/412dp, AR/EN and font scales, inspect function/navigation. If FLAG_SECURE blocks capture, report the real blocker instead of disabling privacy.
E. Present a meaningful Home screenshot to the owner; only after approved, incrementally apply approved shell to remaining nine screens, preserving all existing functional services and unique work.
F. Every change record: source HEAD, source paths, Figma node ID, before/after visual references, actual test commands/results, installed APK hash, phone evidence, Git diff/CI and remaining defects.
G. Do NOT falsely claim PASS from only a build or a chat agent report. No destructive Git or user file operations; no forced push.

**ACCEPTANCE: Looks like a native Android application following the actual Figma geometry and the user's reference smart-home tactile discipline, not a flashy neon website. Real KNOUX AI services, real data, safe controls and only minimal brand wave polish.**
