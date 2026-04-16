# Keep source/debug metadata useful for crash diagnostics.
-keepattributes SourceFile,LineNumberTable

# Preserve Kotlin metadata required by some reflection-backed tooling.
-keep class kotlin.Metadata { *; }

# Keep Parcelable CREATOR fields (safe across Android framework parceling).
-keepclassmembers class * implements android.os.Parcelable {
	public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Google Maps SDK and Maps Compose public API surfaces used by app code.
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.compose.** { *; }

# Keep app entry points and navigation/viewmodel classes that are instantiated by framework code.
-keep class com.pillion.MainActivity { *; }
-keep class com.pillion.PillionApplication { *; }
-keep class com.pillion.navigation.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
