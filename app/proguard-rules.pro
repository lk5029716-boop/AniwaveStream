-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.aniwavestream.app.**$$serializer { *; }
-keepclassmembers class com.aniwavestream.app.** {
    *** Companion;
}
