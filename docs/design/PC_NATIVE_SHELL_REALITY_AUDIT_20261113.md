# KNOUX AI (Windows PC lane) — Native Android Shell & Full-Service UX Repair Gate
Date: 2026-11-13
Canonical source: D:\KNOUX-AI. Do NOT touch the independent /root/workspace/KNOUX-AI mobile source nor KNOUX-AI-Pro.

## Owner problem, reproduced from evidence
This IS a native Android Kotlin/Jetpack Compose application, not a WebView. Its customer UI nevertheless resembles a crowded marketing/dashboard website rather than a spacious Android shell. The owner rejects cramped services, intersecting arrows, oversized headers and decorative UI without real preview/drilldown/actions.

## Primary evidence, including limitations
- Owner-provided screen recording: 720x1624, 113.6s. Launcher visible 0–20s and near end, app capture appears black roughly 20–109s. This does NOT prove that app draws black.
- Actual phone dumpsys window on an authorized Android 15 phone: com.knoux.ai MainActivity has SECURE window flag; this explains screen recorder blackness. DO NOT secretly remove or override a privacy setting for screenshots.
- QA images 2026-11-12: qa-evidence/preview-live/home_screen2.png shows DEV MODE overlapping logo, crowded Design Preview/Pro/settings, huge home hero and clipped Quick Actions; clean_v2.png shows crowded topbar, dense rows and bottom action/nav competition.
- Old QA screenshots are previous-version evidence only; they cannot verify the latest runtime or any new code patch.
- Source reviewed: MainActivity.kt (TopBar, NavHost, bottom nav), HomeScreen.kt (HubCard nested arrow), MasterScreens.kt (SmartHub, Tools/Reports/Privacy/AI previews), DetailPreview.kt (sample-only routes), ServiceWorlds.kt (preview visual art), PrivacyReal.kt (FLAG_SECURE), service controllers.

## P0 / P1 defects
P0-01: 56dp header row contains too many horizontally competing elements: Menu, wordmark, DEV badge, full Design Preview badge, Pro and settings. On standard phone width this overlaps and hides brand. Fix: compact header with single icon controls for Preview/Pro, no DEV in cramped consumer header, still fully accessible with maintained test tags.
P0-02: Home had non-scrollable viewport with six service cards, hero, title and Quick Actions in a weighted Column. Bottom controls could be clipped. Fix: body scrolls separately from persistent native top and bottom nav, no weighted verticalSpacer in unbounded content.
P0-03: Hub service card had an additional clickable arrow inside the clickable card. Bad multiple hit targets; arrow intersects graphics. Fix: one semantic card target, native row layout, compact icon/title/subtitle, no redundant arrow.
P0-04: Buttons, QA/action cards and bottom bar must never obscure one another. Insets and scroll-to-last-action proven at narrow 320/360/412dp widths.
P1-01: Too much art vs usable content. Keep KNOUX high-fidelity globe and animation, shrink home hero for 360dp phone, don't replace art with a blank square.
P1-02: Tools screen height 176dp with satellite decorations, large Pro/banner and sample metric rows: it looks like a site directory. Convert to concise native category hub; honest capability states and actual measured values only.
P1-03: Hardcoded preview numbers in Tools include 128 GB used, 41 apps, 7 trackers, +18% battery, 94 score and 12.6 GB. Reports includes sample time-series. Never remove DESIGN PREVIEW labels before real measured provider-backed data exists.
P1-04: MainActivity mixes PC01–PC05 genuine screens with DetailPreview/ReferenceScreen for unfinished services. Design previews are not production experiences. Keep obvious honest preview status; implement real functionality later, not fake navigation.
P1-05: Bilingual AR RTL and EN LTR, font scaling, TalkBack and Android insets have to be tested on same SHA. Do not clip labels, overlap logo or navigate to wrong service.
P1-06: Native gestures, reduced motion, low-end phone CPU/GPU/memory, off-screen pause, and pause/resume lifecycle need true runtime tests; passing Gradle alone does not establish visual quality.
P1-07: Persistent phone FLAG_SECURE is a real privacy protection, not a rendering error. Collect new runtime screenshots only after authorized privacy-safe capture method, with semantics/window evidence if capture stays blocked.

## Service-specific product interactions mandatory
PC01 Smart Clean/Tide: real category cards with image/video/doc thumbnails, filter/sort, select individual/group/eligible-only, keep/exclude, review exact bytes, explicit user-selected Move/Trash/Delete via Android consent, verification and Room history.
PC02 Duplicates/Mirror: real photo-grid side-by-side, video posters/play, keeper protected in every exact-SHA group, MediaStore/SAF alias protection, separate opt-in similar photos, selected-copy-only OS action, undo if supported.
PC03 App Residue/Strata: expandable folder tree, source/ownership confidence, item inspect and preview, Keep/Archive/Trash with explicit consent, no assumption all remnants are junk.
PC04 Privacy/Vault Radar: real measured nodes and check scopes, other apps shown as UNMEASURABLE, FLAG_SECURE actual toggling, OS settings handoff, journal and retest, no fake antivirus claims.
PC05 Deep Scan/Abyss Sonar: cinematic but lightweight real-state rings/depth/scan modules and actual item browser with image/video/document thumbnails; scanned GB != reclaimable GB. Each item selectable; no whole-result bulk Delete.
PC06 Reports/Prism: real Room history and action evidence charts, filters and share/export; absolutely no fictional trends.
PC07 App Manager/Constellation: installed-app star nodes and native icon/list, real PackageManager / authorized StorageStats, app detail/OS uninstall intent.
PC08 Storage Analyzer/Terrain: real StatFs/MediaStore/SAF views, sunburst/terrain + nested file browser, aliases de-duplicated, selective reviewed actions.
PC09 Battery/Pulse: BatteryManager readings and actual sampled history, no fake claimed battery health/gains/boost.
PC10 AI/Synapse: evidence-backed advice, user confirmation, separately label model advice vs on-device rules, verified after-state.
PC11 Iris home/auth: brand preserving compact native shell, real authentication, no giant fingerprint, proper backstack and permissions.
PC12 Seal Chain: Play-verified subscriptions/entitlements only, honest 50 feature states, no fake Pro.
PC13 Gate: release evidenced by native device screenshots, API tests, translations, Play constraints, CI and install.

## Execution rules
- Keep all unique source, assets, approved animations, logo, QA and other agent work. Original Windows source is D:\KNOUX-AI, separate from phone Linux repo. No hard reset/clean/force-push/deleting branches or worktrees.
- Track current HEAD and origin/main, active agent ownership, status, worktrees, stashes, reflog before Kotlin edits; never collide with another active writer.
- Fix visual bugs IN SOURCE and ACTUALLY RUN: git diff --check; compileDebugKotlin; testDebugUnitTest; assembleDebug; targeted connectedDebugAndroidTest on authorized phone and realistic UI snapshot comparison (when possible). Use mock fixtures, never owner files for destructive tests.
- Preserve approved purple/cyan/gold identity, logos, animations. One service visual instrument tied to measured StateFlow. No heavy browser/webview recreation, no giant generic cards or decorative nested arrows.
- At least one fresh state and result screenshot per service in AR/EN/RTL, narrow/large font, with artifact Git SHA, build exit codes and actual UI evidence. If secure window blocks capture, report BLOCKED_CAPTURE_SECURE_WINDOW and use real semantics bounds; do not fake screenshots.
- Never say PASS, DONE or 100% because an agent said so or a build succeeded.

## Actual repairs begun in this turn
MainActivity.kt: compact top bar for widths <460dp / large fonts, keep preview/pro access, hide DEV badge on compact.
MasterScreens.kt: Home body scrolling under persistent app chrome; hero scaled down; weighted content removed.
HomeScreen.kt: one clickable HubCard with icon/title/description, nested arrow removed.
These repairs are NOT VERIFIED until tests and actual on-device screen/layout evidence. Commit only coherent tested changes to main after review, and verify remote SHA and CI. A compile pass alone is partial evidence.

## Acceptance for this immediate shell correction
1. Header can render on 320/360/412dp in Arabic and English at fontScale 1.0/1.3 without overlapping wordmark/chips; Pro, Preview, Menu, Settings all still work.
2. Home six service cards and Quick Actions scroll entirely into view while top/bottom native navigation persist.
3. Each HubCard is ONE semantic clickable surface, no nested arrow hit target and no hidden clipped labels.
4. No user data modified, no app uninstalled, no unrelated Git/agent artifacts erased.
5. Verify actual signed debug APK, source SHA, test XML, install / OS runtime and screenshots. Mark remaining work honestly.

**Owner instruction: Finish the native UI, not another mock plan. Preserve the unique product and prove actual screen behavior.**
## 2026-11-13 device verification evidence
- Baseline HEAD=origin/main 457b0e66a6ae38275b40c308b834f7e3ab13d504 before this local repair. Preserve all other tracked/untracked/agent work.
- Modified exactly five source/test files: MainActivity.kt, MasterScreens.kt, HomeScreen.kt, DuplicateReal.kt, PreviewNavigationTest.kt.
- Critical newly discovered actual P0: DuplicateReal.kt placed the 320dp MirrorInstrument ABOVE the weight(1f) result list. On 360dp wide Android this consumed the usable viewport, causing the full navigation instrumentation to fail with Semantic Node has no parent layout with a Scroll SemanticsAction. Mirror was moved INTO LazyColumn after the first measured summary so user results remain scrollable, without losing the native art.
- Gradle combined compileDebugKotlin, testDebugUnitTest, assembleDebug and assembleDebugAndroidTest exit 0; BUILD SUCCESSFUL in 4m16s (K:\KNOUX-Phone-Exports\PC_NATIVE_SHELL_DUPLICATE_REPAIR_VERIFY_20261113.log). Actual JUnit XML: 72 tests, 0 failures, 0 errors, 0 skipped.
- Repaired PreviewNavigationTest old PC05 fixture expectations instead of faking sample tags on real Deep Scan. New home cards/quick-actions regression actual Android: OK (1 test). Broader navigation and return from nested services on actual Android: OK (1 test), 26.299s, after the Mirror layout fix. PC05 real screen recreation without triggering personal-data modifications: OK (1 test), 12.885s.
- Fresh debug APK installed non-destructively using adb install -r -t; size 17,193,364 bytes; SHA256 12C9448063E551347EC283B7C69CA07A8010ABDC69BAA28CF2574B1FAF747791. No uninstall or clear-data.
- git diff --check EXIT 0. No raw personal phone serial, private logcat, tokens, source archives or device data should be pushed.
- VISUAL APPROVAL REMAINS OPEN. FLAG_SECURE prevents screenshots, and runtime tests do not replace before/after pixel comparison, EN/AR font-scale tests, full service instruments, real duplicate thumbnail grid, PC06+ full-stack implementation or all service E2E.
- This is a focused native-shell/PC02 viewport remediation, NOT evidence of an app-wide visually finished production release.
