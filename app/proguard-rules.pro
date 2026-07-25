# Add project specific ProGuard rules here.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,MethodParameters

# Keep all app classes and members
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }
-keep class io.github.securephoneapps.** { *; }
-keepclassmembers class io.github.securephoneapps.** { *; }

# Keep ViewModel constructors for Reflection instantiation
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    public <init>(android.app.Application);
}

# Keep Room database, DAOs, entities, and type converters
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class *_Impl { *; }
-keep class * extends androidx.room.Entity { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-dontwarn androidx.room.**

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep kotlinx.serialization models
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose and Material 3
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

