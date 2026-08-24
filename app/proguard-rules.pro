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
