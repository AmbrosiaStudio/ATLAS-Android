# PageNest Release ProGuard / R8 Keep Rules

-keepattributes Annotation,Signature,InnerClasses,EnclosingMethod

# Preserve Room entities, DAOs, and database schema
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Preserve Reading models & enums serialized/persisted by name
-keep class com.example.model.** { *; }

# ML Kit On-Device Translation & Language ID
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Jetpack Glance AppWidget & WorkManager
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }
-keep class * extends androidx.work.CoroutineWorker { public <init>(...); }
