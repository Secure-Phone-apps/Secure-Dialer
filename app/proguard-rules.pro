# Add project specific ProGuard / R8 rules here.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,MethodParameters,ElementValuePairs,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# 1. Keep ALL app code in com.example and subpackages (Models, DAOs, ViewModels, Services, UI)
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# 2. CRITICAL for Room: Package wildcards for generated Database & DAO implementations
-keep class **_Impl { *; }
-keep class **.*_Impl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keep class androidx.room.paging.** { *; }
-keep class androidx.sqlite.db.** { *; }
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# SQLCipher Database Encryption Rules
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-keep class * extends net.sqlcipher.database.SQLiteOpenHelper { *; }
-keep class net.sqlcipher.database.SQLiteDatabase {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn net.sqlcipher.**

# 3. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
-keepclassmembers class * implements coil.request.Request { *; }

# 4. Enums & TypeConverters (CallType, etc.)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public **[] $VALUES;
}
-keep class **.CallType { *; }

# 5. Keep Android Services, BroadcastReceivers, and Telecom Framework Handlers
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.telecom.InCallService
-keep public class * extends android.telecom.CallScreeningService
-keep public class * extends android.telecom.ConnectionService

# 6. Keep Kotlin Coroutines, Flow & Internal Dispatchers
-keepclassmembers class kotlinx.coroutines.android.HandlerDispatcher {
    <init>(...);
}
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 7. Keep Jetpack Compose, Paging & Navigation internal reflection
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}
-keepclassmembers class androidx.compose.runtime.Recomposer { *; }
-keep class androidx.paging.** { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.paging.**
-dontwarn androidx.navigation.**

# 8. Preserve Parcelable & Serializable CREATORs
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 9. Keep ViewModel Constructors
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
