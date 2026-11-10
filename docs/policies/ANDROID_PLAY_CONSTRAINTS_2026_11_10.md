# Android / Google Play feasibility matrix (verified 2026-11-10)

This is engineering guidance, **not a guarantee of Play approval**. Re-check official policies at release time.

| Module / claim | Public Android route | Hard limitation / safe product language |
|---|---|---|
| Exact duplicate media | MediaStore + Photo Picker/partial media grants + streaming SHA-256 | Only authorized visible items. No cross-app private folder access. Similarity uses separate perceptual algorithm and review, NEVER auto-delete. |
| Duplicate documents | SAF `ACTION_OPEN_DOCUMENT` / chosen directory via `ACTION_OPEN_DOCUMENT_TREE` | On Android 11+ tree access cannot select storage roots, Download root or `Android/data`/`obb`; individual files can be selected as permitted. |
| File deletion | `MediaStore.createTrashRequest()` API 30+ with OS approval; SAF `DocumentsContract.deleteDocument()` for granted documents | Must distinguish trash vs permanent delete; no delete without explicit review + OS/user consent. |
| App residue | scan only own cache plus user-granted accessible residual files | Cannot read/write `other_app/data` or purge other apps' private caches with ordinary Play app rights. Never label guessed files safe. |
| Installed app management | PackageManager filtered visibility, targeted `<queries>`, app details settings intent, user-granted UsageStats | Broad `QUERY_ALL_PACKAGES` restricted and subject to Play declaration/core use; App Manager cannot reliably inspect all apps with default permissions. |
| App usage / rarely-used | `UsageStatsManager` after special Usage Access granted in Settings | User may deny. No fabricated `last used` dates. OEM differences apply. |
| Privacy sweep | User-visible permission/settings referrals, app's own permissions, visible SDK inventories in user-selected APKs | All-other-app permission access histories or background clipboard watcher inventory are not generally exposed. `AppOps` cross-UID tracking requires system privilege. Trackers cannot be "detected" simply by package name with 100% certainty. |
| Clipboard surveillance | Restricted since Android 10 | No general ability to audit all clipboard-reading apps. Don't request notification-access to bypass user boundaries. |
| Battery saver | `BatteryManager`, `ACTION_BATTERY_CHANGED`, user settings intents, discharge prediction when supported | Current level != battery health. OEM battery health % and charge cycle count may not be available; hours gained or +18% benefit require controlled measurement. |
| Thermal guard | `PowerManager.getCurrentThermalStatus()` API 29+ | Observe thermal status; app cannot guarantee -6°C or hardware thermal control. |
| AI Optimization | Local deterministic recommendations based on measured state; optional ML with controlled offline eval | Third-party apps can't reliably kill/hibernate other apps, alter system scheduler, disable arbitrary autostart or clear their private caches; don't display guaranteed memory/boot gains. |
| Deep Scan | Cooperative cancelable WorkManager / foreground-appropriate scan; per-module measured completion | The selected accessible scope, cancellation, storage constraints and permissions limit what can be scanned. 5/5 and 72% simultaneously in mockup is fixture inconsistency. |
| Reports | Room action journal and measured before/after | Savings must be reconciled with actual successful actions; cannot equate candidate bytes with reclaimed bytes. |
| Cloud history | Supabase optional Auth + Postgres RLS with explicit opt-in | Not a prerequisite to make device services real. No raw paths/media/packages uploaded by default. |

## Permission decision tree

- **Default Phase 1 UI:** no sensitive runtime permissions at all.
- **Choose photos or videos:** Photo Picker for limited manual selections; granular `READ_MEDIA_IMAGES`/`VIDEO` when feature truly needs library-wide access, subject to Google Play Photo and Video policy.
- **Choose documents:** SAF with persistable grants where supported and appropriate; respect Android 11 restricted directories.
- **Broad storage:** `MANAGE_EXTERNAL_STORAGE` should **not** be assumed. Only after core-use-case fit, alternatives analysis, Play declaration and owner approval. May be rejected.
- **Installed packages:** targeted visibility first; `QUERY_ALL_PACKAGES` only if truly indispensable and approved under policy.
- **Battery / thermal:** public read APIs, no extra secret privileges. UsageStats access is user enabled in Settings.

## Official sources

1. Android shared media: https://developer.android.com/training/data-storage/shared/media
2. Android SAF and directory restrictions: https://developer.android.com/training/data-storage/shared/documents-files
3. Android package visibility: https://developer.android.com/training/package-visibility
4. Android usage stats: https://developer.android.com/reference/android/app/usage/UsageStatsManager
5. Android BatteryManager: https://developer.android.com/reference/android/os/BatteryManager
6. Android PowerManager: https://developer.android.com/reference/android/os/PowerManager
7. Android AppOpsManager scope: https://developer.android.com/reference/android/app/AppOpsManager
8. Google Play all files policy: https://support.google.com/googleplay/android-developer/answer/10467955
9. Google Play query-all packages policy: https://support.google.com/googleplay/android-developer/answer/10158779
10. Google Play target SDK: https://developer.android.com/google/play/requirements/target-sdk
11. Android Photo Picker: https://developer.android.com/training/data-storage/shared/photo-picker
12. Android Room persistence: https://developer.android.com/training/data-storage/room
13. Android background scheduling: https://developer.android.com/develop/background-work/background-tasks/persistent

### Target SDK change verified

From **August 31, 2026**, new standard Android phone apps and app updates targeting Google Play need **API level 36 or higher**, per the official page. This is subject to future change, so verify at submission. Min SDK 26 is an architectural recommendation, not a Play mandate.
