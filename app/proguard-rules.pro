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

# ML Kit Translation
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class com.google.android.libraries.language.** { *; }
-keepnames class * implements com.google.mlkit.common.model.Model$ModelFactory
-keep class * extends com.google.mlkit.nl.translate.Translator
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontwarn com.google.mlkit.**

# Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Enums
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

# Parcelable
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }
