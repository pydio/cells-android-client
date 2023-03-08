# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html


# More info for debugging
-verbose

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class javax.net.ssl.**
-keep class com.pydio.android.cells.db.**

-addconfigurationdebugging

# Gson specific classes
-dontwarn sun.misc.**

# Application classes that will be serialized/deserialized over Gson
-keep class com.pydio.cells.api.ServerURL { <fields>; }
-keep class com.pydio.cells.transport.ServerURLImpl { <fields>; }
-keep class com.pydio.cells.transport.StateID { <fields>; }
# double check this
-keep class com.pydio.cells.transport.auth.jwt.Header  { <fields>; }
-keep class com.pydio.cells.transport.auth.jwt.Claims  { <fields>; }

# to manage legacy migration
-keep class com.pydio.android.legacy.v2.AccountRecord { <fields>; }
-keep class com.pydio.android.legacy.v2.LegacyAccountRecord { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# added by bs
-keep class * implements com.google.gson.reflect.TypeToken

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

##---------------End: proguard configuration for Gson  ----------
