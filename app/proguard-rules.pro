# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontusemixedcaseclassnames

# Keep the BuildConfig
-keep class **.BuildConfig { *; }

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Room database
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey <fields>;
}

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }
