# RoadSaathi ProGuard Rules

# Keep Room entities
-keep class com.roadsaathi.data.local.entity.** { *; }

# Keep DTOs for Gson serialization
-keep class com.roadsaathi.data.remote.dto.** { *; }
-keep class com.roadsaathi.domain.model.** { *; }

# Keep TFLite model classes
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Firebase messaging
-keep class com.google.firebase.messaging.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
