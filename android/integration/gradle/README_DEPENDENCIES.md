# Kotlin integration snippets — dependencies and limits

These are **integration-ready source examples, not a built complete Android app**. Merge with the actual project namespace, DI, minimum API, service interfaces and its tested dependency catalog in Phase 2. Code has **not** been compiled with Gradle in this handoff environment.

- Kotlin coroutines: `org.jetbrains.kotlinx:kotlinx-coroutines-android`.
- Room 2.8.5 stable: `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` via KSP; verify Android Gradle Plugin/Kotlin/KSP compatibility first. Do not combine Room 2 and Room 3 imports.
- Supabase Kotlin: `io.github.jan-tennert.supabase:bom:<PIN_TESTED_VERSION>`, `auth-kt`, `postgrest-kt` and compatible Ktor Android engine; **community-maintained library**. Pin actual tested version after `./gradlew dependencies`, don't leave placeholder in committed Gradle.
- Kotlin serialization plugin and `kotlinx-serialization-json` for cloud DTO.
- Android platform compileSdk/targetSdk **36** currently required for new Play phone apps/updates from August 31 2026; check current Play Console target before release.
- Minimum SDK recommendation **26** if the business supports API 26+; use runtime gates for API 29/30/33/34/36.
- App permission minimality; do not paste unrestricted `MANAGE_EXTERNAL_STORAGE` or `QUERY_ALL_PACKAGES` into production manifest.

Sources:
- https://developer.android.com/jetpack/androidx/releases/room
- https://supabase.com/docs/reference/kotlin/installing
- https://supabase.com/docs/reference/kotlin/initializing
- https://developer.android.com/google/play/requirements/target-sdk
