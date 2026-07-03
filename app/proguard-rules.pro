# ============================================================
#  Margin App — ProGuard / R8 Rules
#  These rules are applied during release builds only.
# ============================================================

# ── Kotlin ──────────────────────────────────────────────────
# Preserve Kotlin metadata so reflection-based libraries work.
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }

# Kotlin coroutines — keep internal machinery needed at runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
# Coroutine debug info (safe to strip in release)
-dontwarn kotlinx.coroutines.debug.*

# ── Room ────────────────────────────────────────────────────
# Keep all Room-generated implementations (AppDatabase_Impl, etc.)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
# Keep Room query result data classes (used for projections)
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}
# Suppress warnings from Room's optional annotation processors
-dontwarn androidx.room.**

# ── DataStore ───────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Jetpack Compose ─────────────────────────────────────────
# Compose itself is well-handled by R8; no special rules needed
# for Compose UI. Only keep the internal stability annotations
# used by the Compose compiler plugin.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ── WorkManager ─────────────────────────────────────────────
# Keep Worker subclasses so WorkManager can instantiate them
# via reflection.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ── BroadcastReceivers ──────────────────────────────────────
# Keep receivers so AlarmManager PendingIntents can resolve them
# after R8 renames classes.
-keep class com.Margin.app.receivers.** { *; }

# ── App Entities & Data Classes ─────────────────────────────
# Keep all entity and DAO classes in the data package.
# R8 would normally shrink these correctly via Room's keep rules,
# but this is an explicit safety net.
-keep class com.Margin.app.data.** { *; }

# ── General Android rules ────────────────────────────────────
# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in stack traces for release.
-renamesourcefileattribute SourceFile

# Keep custom Application class
-keep class com.Margin.app.MarginApplication { *; }

# ── org.json ────────────────────────────────────────────────────────────────
# Used by TimetableExportManager for JSON serialization (no external dependency).
# org.json is part of the Android SDK but keep it explicit for R8 safety.
-keep class org.json.** { *; }
