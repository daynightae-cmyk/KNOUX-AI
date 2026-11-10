# Source ledger — last checked 2026-11-10

| Claim | Official source | How used |
|---|---|---|
| Play phone new apps/updates target API 36 since 2026-08-31 | https://developer.android.com/google/play/requirements/target-sdk | Target SDK guidance |
| `MANAGE_EXTERNAL_STORAGE` is restricted | https://support.google.com/googleplay/android-developer/answer/10467955 | No broad storage by default |
| `QUERY_ALL_PACKAGES` restricted | https://support.google.com/googleplay/android-developer/answer/10158779 | Filtered package inventory |
| MediaStore delete/trash request | https://developer.android.com/training/data-storage/shared/media | User-approved deletion |
| SAF restrictions Android 11+ | https://developer.android.com/training/data-storage/shared/documents-files | Residue/document limits |
| UsageStats special grant | https://developer.android.com/reference/android/app/usage/UsageStatsManager | App use history |
| Clipboard access changes | https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling | No blanket watcher claims |
| Battery properties | https://developer.android.com/reference/android/os/BatteryManager | State vs health |
| Thermal status | https://developer.android.com/reference/android/os/PowerManager | Observability only |
| Room local database | https://developer.android.com/training/data-storage/room | On-device state |
| Supabase Kotlin client modules and minSdk 26 | https://supabase.com/docs/reference/kotlin/installing | Optional cloud adapter |
| Supabase Kotlin initialization | https://supabase.com/docs/reference/kotlin/initializing | Client instantiation |
| Supabase RLS and auth.uid | https://supabase.com/docs/guides/database/postgres/row-level-security | Multi-tenant policies |
| Supabase authenticated Edge Functions | https://supabase.com/docs/guides/functions/auth | Authentication route |
| Codex platform | https://developers.openai.com/learn/codex | Tool orchestration |
| Gemini 4 Carbon announced 2026-11-10 | https://blog.google/innovation-and-ai/technology/developers-tools/gemini-4-carbon-developers/ | Candidate ranking |
| Claude Fable/Mythos 5.2 announced 2026-11-09 | https://www.anthropic.com/claude-fable-and-mythos-5-2 | Candidate ranking |

**Verification nuance:** Web pages can change after this review. User-facing UI mockup totals are NOT sources for service metrics. A citation to docs never proves that supplied sample Kotlin compiles in user's project, Supabase SQL runs, or Google Play will approve a package.
