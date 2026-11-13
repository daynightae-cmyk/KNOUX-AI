plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    // KSP is required by Room's compiler. Pinned to the exact Kotlin version in
    // use (2.2.21) so the processor and the compiler never drift apart.
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" apply false
}
