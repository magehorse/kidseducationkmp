# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.kidsplay.kidseducation.**$$serializer { *; }
-keepclassmembers class com.kidsplay.kidseducation.** { *** Companion; }
-keepclasseswithmembers class com.kidsplay.kidseducation.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn org.slf4j.**

# YouTube in-app player + views created by reflection (from the old app)
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-dontwarn com.pierfrancescosoffritti.androidyoutubeplayer.**
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keepclassmembers class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep class com.harvitech.kidsdrawing.** { *; }
-keep class com.warkiz.widget.** { *; }
-dontwarn com.warkiz.widget.**
