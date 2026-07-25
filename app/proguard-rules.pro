# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Disable aggressive bytecode optimization (inlining/argument reordering) to prevent R8 runtime crashes
-dontoptimize

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,MethodParameters,ElementValuePairs

# 1. CRITICAL: Preserve Enums so Room TypeConverters (e.g. CallType.valueOf) don't crash on startup
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public **[] $VALUES;
}

# 2. Keep all App Classes, Models, Services, Repositories, ViewModels, and Data Entities
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# 3. Keep Room Database implementation classes and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class *_Impl { *; }
-dontwarn androidx.room.**

# 4. Keep Android Services, BroadcastReceivers, and Telecom Framework Handlers
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.telecom.InCallService
-keep public class * extends android.telecom.CallScreeningService

# 5. Keep Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 6. Keep Jetpack Compose & Navigation internal reflection
-dontwarn androidx.compose.**
-dontwarn androidx.paging.**
-keep class androidx.paging.** { *; }
