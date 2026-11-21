# Siyot release (R8) rules.
#
# Most libraries here (CameraX, Compose, ML Kit, Room, DataStore, Coil,
# kotlinx.serialization, OkHttp) ship their own consumer rules, so this file
# only holds what is still needed. Our own code needs no keep rules: it uses no
# reflection and no @Serializable classes (JSON is built and read with the
# JsonElement API).

# Keep readable crash stack traces (upload mapping.txt to your crash reporter).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp optionally uses these TLS providers when present; they are not bundled.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
