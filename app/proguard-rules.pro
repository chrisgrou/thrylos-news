# Room, Hilt and WorkManager already ship consumer proguard rules.
# kotlinx.serialization needs its generated serializers kept.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class gr.thrylos.news.** {
    *** Companion;
}
-keepclasseswithmembers class gr.thrylos.news.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class gr.thrylos.news.**$$serializer { *; }

# Optional/absent-at-runtime references pulled in transitively (readability4j -> slf4j,
# jsoup -> re2j, okhttp's optional TLS providers): not a problem on Android, but R8
# fails the build on a missing class unless told so.
-dontwarn org.slf4j.**
-dontwarn com.google.re2j.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
