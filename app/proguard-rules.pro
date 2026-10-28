# ===========================================================================
# NightOff Production Anti-Decompilation & ProGuard Obfuscation Rules
# ===========================================================================

# 1. Anti-Decompilation: Strip debug tables, line numbers, and original filenames
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable

# 2. Obfuscate package hierarchies by flattening classes into the root package
-repackageclasses ""
-allowaccessmodification

# 3. Maximum optimization passes
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/allocation/variable

# 4. Remove all logging invocations in production builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# 5. Keep required Android entry points declared in AndroidManifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.admin.DeviceAdminReceiver
-keep public class * extends android.accessibilityservice.AccessibilityService

# 6. Keep Room database entities and DAOs intact
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Update <methods>;
}

# 7. Keep Jetpack Compose runtime methods
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 8. Keep Parcelable / Serializable creators
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
