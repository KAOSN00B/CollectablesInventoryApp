# Keep source file names and line numbers so crash stack traces are readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Gson / JSON models ---
# Gson uses reflection to read field names from data classes.
# Without this, R8 renames the fields and JSON parsing silently returns nulls.
-keep class com.lasallecollegevancouver.gameinventoryapp.network.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# --- Kotlin Parcelize ---
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# --- Navigation Component safe args ---
-keep class * extends androidx.navigation.NavArgs { *; }
