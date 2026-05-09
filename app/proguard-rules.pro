# Project specific ProGuard rules
# AdMob / Google Play Services
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}

# For Jetpack Compose and Kotlin Serialization if used
-keepattributes *Annotation*
-keep class androidx.compose.runtime.ParcelableSnapshotState { *; }
