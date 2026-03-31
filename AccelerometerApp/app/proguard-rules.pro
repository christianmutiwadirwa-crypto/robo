# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontobfuscate
-dontshrink

# Keep the main application classes
-keep class com.robotdash.accelerometer.** { *; }

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Okio
-keep class okio.** { *; }
-dontwarn okio.**
