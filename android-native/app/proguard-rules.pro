# Keep app entry points and app-owned classes stable for reflection and runtime callbacks.
-keep class com.hurry.voicechat.** { *; }

# Preserve runtime metadata used by Android and dependency reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
