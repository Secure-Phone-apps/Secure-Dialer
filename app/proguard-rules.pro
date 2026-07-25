# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,MethodParameters,ElementValuePairs

# Keep all App Data Models & Entities (Prevents Room DB & JSON reflection crashes)
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.ui.** { *; }

# Keep Room Database implementation classes and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class *_Impl { *; }
-dontwarn androidx.room.**

# Keep Android Services & Main Activity
-keep class com.example.CallBlockerService { *; }
-keep class com.example.MyInCallService { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.DialerRepository { *; }

# Keep Kotlin Coroutines internals
-keepclassmembers class kotlinx.coroutines.** {
    public *** *;
}
-dontwarn kotlinx.coroutines.**

# Keep Jetpack Compose & Navigation internal reflection
-dontwarn androidx.compose.**
