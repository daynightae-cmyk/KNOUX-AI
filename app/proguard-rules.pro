# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Biometric
-keep class androidx.biometric.** { *; }

# Keep app entry points
-keep class com.knoux.ai.MainActivity { *; }
-keep class com.knoux.ai.** extends androidx.lifecycle.ViewModel { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin serialization (if used)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# General Android
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
