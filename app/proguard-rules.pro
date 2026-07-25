# Add project specific ProGuard rules here.

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
-dontwarn androidx.room.paging.**

# Keep app data models, enums, and database DAOs
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keepclassmembers enum com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Keep Android system components (Services, Receivers, MainActivity)
-keep class com.example.MyInCallService { *; }
-keep class com.example.CallBlockerService { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.CallManager { *; }

# Keep kotlinx.serialization models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose and Material 3
-dontwarn androidx.compose.**
