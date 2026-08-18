# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit interface
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson model classes
-keep class com.mission.app.data.model.** { *; }
-keep class com.mission.app.data.entity.** { *; }

# kotlinx.coroutines
-dontwarn kotlinx.coroutines.**

# Room entities
-keep class androidx.room.** { *; }