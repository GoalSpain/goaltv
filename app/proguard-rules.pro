# Keep model data classes and Moshi generated adapters
-keep class com.goaltv.model.** { *; }
-keep class com.goaltv.data.network.** { *; }

# Moshi
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @retrofit2.http.** *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ZXing
-keep class com.google.zxing.** { *; }
