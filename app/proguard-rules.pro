# ═══════════════════════════════════════════
# GsGit ProGuard / R8 Rules
# ═══════════════════════════════════════════

-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'g'
-overloadaggressively
-flattenpackagehierarchy 'g'

# Remove logs
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }

# Coil
-keep class coil.** { *; }

# JNA / Lazysodium
-keep class com.goterl.lazysodium.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# WorkManager
-keep class androidx.work.** { *; }

# Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Enums
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

# Parcelable
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }
