# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# === Room Database ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# === Koin DI ===
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}
-dontwarn org.koin.**

# === Jetpack Security (EncryptedSharedPreferences) ===
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# === AndroidJavaMail (SMTP) ===
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**

# === Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# === DataStore ===
-dontwarn androidx.datastore.**

# === WorkManager ===
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
