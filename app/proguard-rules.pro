# ==============================================================================
# JNI: keep the native bridge and every external (native) method on it.
# Without these rules, R8 may rename methods that the Rust side looks up.
# ==============================================================================
-keep class com.fxsound.mobile.audio.NativeDspBridge { *; }
-keepclasseswithmembernames class com.fxsound.mobile.audio.NativeDspBridge {
    native <methods>;
}

# ==============================================================================
# JNI: any class that declares an `external fun` (i.e. loads a native lib)
# must keep both the class and its native members.
# ==============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==============================================================================
# R8: keep generated R$* inner classes (resource id lookups by reflection).
# ==============================================================================
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ==============================================================================
# Oboe / AAudio / OpenSL ES: keep the audio engine entry points and any
# classes that JNI references by name. Oboe ships its own consumer rules
# but the version we pin is small enough that a blanket keep is safe.
# ==============================================================================
-keep class com.google.oboe.** { *; }
-keep interface com.google.oboe.** { *; }
-dontwarn com.google.oboe.**

# ==============================================================================
# kotlinx.coroutines: keep the volatile fields used for cancellation /
# completion state, and silence warnings about the internals package which
# R8 cannot rewrite (the compiler emits calls into it from generated code).
# ==============================================================================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ==============================================================================
# Compose runtime: keep the synthetic classes the compiler emits.
# ==============================================================================
-dontwarn androidx.compose.**

# ==============================================================================
# JSON models used by PresetPack and DspParameters: keep field names so the
# reflective readers (org.json) keep working.
# ==============================================================================
-keepclassmembers class com.fxsound.mobile.core.model.** { *; }

# ==============================================================================
# Strip line numbers and debug info in release, but keep SourceFile +
# LineNumberTable attributes so crash stacktraces can still be symbolicated
# using the uploaded R8 mapping file.
# ==============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
