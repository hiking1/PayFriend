# kotlinx.serialization: keep generated serializers for @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.payfriend.**$$serializer { *; }
-keepclassmembers class com.payfriend.** { *** Companion; }
-keepclasseswithmembers class com.payfriend.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit service interfaces (generic return types are read reflectively)
-keepattributes Signature, Exceptions
-keep,allowobfuscation interface com.payfriend.core.network.PayFriendApi
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
