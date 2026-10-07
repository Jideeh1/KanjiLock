#keep everything, widget n zstd break without it
-keep class com.jideeh.kanjilock.** { *; }
-keep class com.github.luben.zstd.** { *; }
-dontwarn com.github.luben.zstd.**

#ml kit and workmanager make these by name when the app starts, r8 was stripping the constructors so release crashed on open
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * implements com.google.android.datatransport.runtime.backends.BackendFactory { <init>(); }
-keep class * extends androidx.startup.Initializer { <init>(); }
#handwriting (ml kit) talks to native code and reads its model list with gson, keep all of it whole so draw to search doesnt break in release
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_digital_ink.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_digital_ink_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
