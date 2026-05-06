package com.aos.performance.sdk.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import androidx.annotation.Keep

/**
 * 주입된 JavaScript에서 오는 JSON을 받는 [JavascriptInterface]입니다.
 *
 * [JavascriptInterface]가 붙은 메서드는 백그라운드 스레드에서 호출되므로,
 * [onData]는 메인 스레드에서 실행되도록 Handler로 포스팅합니다.
 */
@Keep
class JsBridge(
    private val onData: (WebPerformanceData) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onWebPerformanceData(jsonPayload: String) {
        val data =
            try {
                WebPerformanceData.fromJson(jsonPayload)
            } catch (_: Exception) {
                return
            }
        mainHandler.post { onData(data) }
    }
}
