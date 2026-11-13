import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release signing material never lives in source. Read a local, ignored
// keystore.properties first, then the documented environment variables.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

fun releaseSecret(property: String, environment: String): String? =
    keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environment)?.takeIf { it.isNotBlank() }

val releaseStorePath = releaseSecret("storeFile", "KNOUX_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("storePassword", "KNOUX_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("keyAlias", "KNOUX_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("keyPassword", "KNOUX_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.knoux.ai"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.knoux.ai"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-preview"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }

    // A release APK must never be signed with the debug key.
    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            // Contact details are build configuration, never source. Pass
            // -PknouxProContact=... locally; the repository stays free of a
            // personal number, and the UI states honestly when it is absent.
            buildConfigField(
                "String", "PRO_CONTACT_WHATSAPP",
                "\"${providers.gradleProperty("knouxProContact").getOrElse("")}\"",
            )
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField(
                "String", "PRO_CONTACT_WHATSAPP",
                "\"${providers.gradleProperty("knouxProContact").getOrElse("")}\"",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Exported Room schemas are the input of the on-device migration test; without
    // this they live only on the build machine and the test cannot validate 2 → 3.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

// Release hygiene is enforced only when a release task is requested; a debug
// build must never be blocked by the absence of production signing material.
val releaseRequested = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
if (releaseRequested && !hasReleaseSigning &&
    project.findProperty("knouxAllowUnsignedRelease")?.toString()?.toBoolean() != true
) {
    error(
        "Release builds need a production signing config. Create " +
            "keystore.properties (storeFile, storePassword, keyAlias, keyPassword) " +
            "at the repository root, set KNOUX_RELEASE_* environment variables, or pass " +
            "-PknouxAllowUnsignedRelease=true to build an unsigned release. " +
            "KNOUX never signs a release with the debug key."
    )
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// Room schema export keeps every migration reviewable and makes destructive
// migration impossible to slip in unnoticed.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // ── PC00 real data foundation ────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
