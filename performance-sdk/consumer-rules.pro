# Rules that apply to consumers of this library.

-keepclassmembers class com.aos.performance.sdk.webview.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.aos.performance.sdk.webview.JsBridge { *; }
