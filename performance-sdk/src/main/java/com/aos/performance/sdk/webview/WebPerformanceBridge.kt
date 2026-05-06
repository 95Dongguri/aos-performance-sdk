package com.aos.performance.sdk.webview

import android.os.Looper
import android.webkit.WebView

/**
 * [JsBridge]를 붙이고, [WebView.evaluateJavascript]로 [requestAnimationFrame](https://developer.mozilla.org/docs/Web/API/window/requestAnimationFrame)
 * 루프를 주입한 뒤 측정값을 [onPerformanceData]로 넘깁니다.
 *
 * [WebView]가 준비된 뒤 메인 스레드에서 [attach]를 호출하세요.
 * 뷰가 파괴되거나 추적을 멈출 때는 [detach]로 인터페이스를 제거하고 rAF 체인을 끕니다.
 */
class WebPerformanceBridge(
    private val webView: WebView,
    private val onPerformanceData: (WebPerformanceData) -> Unit,
) {
    private val jsBridge = JsBridge(onPerformanceData)

    /**
     * 필요 시 JavaScript를 켜고, [JsBridge]를 [INTERFACE_NAME]으로 등록한 뒤 부트스트랩 스크립트를 실행합니다.
     */
    fun attach() {
        checkMainThread()
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(jsBridge, INTERFACE_NAME)
        webView.evaluateJavascript(bootstrapScript(), null)
    }

    /**
     * rAF 루프를 멈추고 Javascript 인터페이스를 제거합니다. 어떤 스레드에서 호출해도 안전합니다.
     */
    fun detach() {
        val stopScript = "window.$ACTIVE_FLAG=false"
        if (Looper.getMainLooper().isCurrentThread) {
            webView.evaluateJavascript(stopScript, null)
            webView.removeJavascriptInterface(INTERFACE_NAME)
        } else {
            webView.post {
                webView.evaluateJavascript(stopScript, null)
                webView.removeJavascriptInterface(INTERFACE_NAME)
            }
        }
    }

    private fun checkMainThread() {
        check(Looper.getMainLooper().isCurrentThread) { }
    }

    private companion object {
        const val INTERFACE_NAME = "AosPerformanceSdk"
        private const val ACTIVE_FLAG = "__aosWebPerfActive"
        private const val SCHEDULED_FLAG = "__aosWebPerfLoopScheduled"

        /**
         * 연속 rAF 타임스탬프 사이 델타로 프레임 길이를 재고, JSON을 [INTERFACE_NAME].onWebPerformanceData로 보냅니다.
         */
        private fun bootstrapScript(): String =
            """
            (function(){
              window.$ACTIVE_FLAG=true;
              var last=0;
              function tick(now){
                var active=window.$ACTIVE_FLAG===true;
                if(active&&last>0){
                  var dt=now-last;
                  $INTERFACE_NAME.onWebPerformanceData(JSON.stringify({renderTimeMs:dt}));
                }
                if(active){last=now;}
                if(active){
                  requestAnimationFrame(tick);
                }else{
                  window.$SCHEDULED_FLAG=false;
                }
              }
              if(!window.$SCHEDULED_FLAG){
                window.$SCHEDULED_FLAG=true;
                requestAnimationFrame(tick);
              }
            })();
            """.trimIndent().replace("\n", "")
    }
}
