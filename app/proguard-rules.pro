# Keep JNI bridge methods
-keepclasseswithmembernames class com.fxsound.mobile.audio.NativeDspBridge {
    native <methods>;
}

# Keep Compose models and JSON models
-keepclassmembers class com.fxsound.mobile.core.model.** { *; }

# Strip line numbers and debug info in release
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
