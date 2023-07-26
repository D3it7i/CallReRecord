-repackageclasses
-allowaccessmodification

-keep class io.github.d3it7i.callrecording.xposed.Init {
    <init>();
}

-keepclasseswithmembers class io.github.d3it7i.callrecording.xposed.DexHelper {
    native <methods>;
    long token;
    java.lang.ClassLoader classLoader;
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
