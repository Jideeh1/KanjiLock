#keep everything, widget n zstd break without it
-keep class com.jideeh.kanjilock.** { *; }
-keep class com.github.luben.zstd.** { *; }
-dontwarn com.github.luben.zstd.**
