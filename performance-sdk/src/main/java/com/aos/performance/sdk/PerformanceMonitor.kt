package com.aos.performance.sdk

import android.content.Context
import android.os.Looper
import android.webkit.WebView
import com.aos.performance.sdk.config.PerformanceConfig
import com.aos.performance.sdk.metrics.PerformanceAggregator
import com.aos.performance.sdk.webview.WebPerformanceBridge
import com.aos.performance.sdk.webview.WebPerformanceData
import java.util.concurrent.ConcurrentHashMap

/**
 * 성능 SDK의 공개 진입점입니다.
 *
 * 일반적인 사용 순서: [init] → [start] → … 작업 … → [stop].
 * [WebView]를 관찰하려면 [init] 이후, 모니터링 중이거나 시작 전에 [attachWebView]를 호출하세요.
 *
 * 네이티브 프레임 간격은 [recordFrameTimeMs]로 [aggregator]에 넘기면 됩니다
 * (예: [com.aos.performance.sdk.frame.FrameTracker] 콜백).
 */
object PerformanceMonitor {

    /** 공유 메트릭 수집기. [attachWebView]로 들어온 Web 샘플은 여기에 자동으로 기록됩니다. */
    val aggregator = PerformanceAggregator()

    private var appContext: Context? = null
    private var config: PerformanceConfig? = null
    private var running: Boolean = false

    private val webBridges = ConcurrentHashMap<WebView, WebPerformanceBridge>()

    /**
     * 애플리케이션 컨텍스트와 옵션으로 SDK를 초기화합니다.
     * 프로세스당 한 번 호출해도 안전하며, 이후 호출은 저장된 [config]를 덮어씁니다.
     */
    fun init(context: Context, config: PerformanceConfig) {
        appContext = context.applicationContext
        this.config = config
    }

    /** 모니터링을 시작합니다. 먼저 [init]이 필요합니다. */
    fun start() {
        requireInitialized()
        if (running) return
        running = true
    }

    /** 모니터링을 중지합니다. 시작하지 않았다면 아무 동작도 하지 않습니다. */
    fun stop() {
        if (!running) return
        running = false
        detachAllWebBridges()
    }

    /**
     * [WebPerformanceBridge]를 통해 JS/rAF 성능 샘플링을 하도록 [WebView]를 등록합니다.
     * 메인 스레드에서 호출해야 합니다. 같은 [webView]에 대해 기존 브리지가 있으면 교체됩니다.
     */
    fun attachWebView(
        webView: WebView,
        onWebPerformanceData: (WebPerformanceData) -> Unit = { _ -> },
    ) {
        requireInitialized()
        checkMainThread()
        webBridges.remove(webView)?.detach()
        val bridge =
            WebPerformanceBridge(webView) { data ->
                aggregator.recordWebRenderTimeMs(data.renderTimeMs)
                onWebPerformanceData(data)
            }
        bridge.attach()
        webBridges[webView] = bridge
    }

    /**
     * [aggregator]에 네이티브 프레임 간격(밀리초)을 한 번 기록합니다.
     * [com.aos.performance.sdk.frame.FrameTracker] 콜백(또는 이에 상응하는 경로)에서 호출하세요.
     */
    fun recordFrameTimeMs(frameTimeMs: Double) {
        requireInitialized()
        aggregator.recordFrameTimeMs(frameTimeMs)
    }

    /** 해당 인스턴스에 대한 WebView 추적을 중지합니다. 어떤 스레드에서 호출해도 안전합니다. */
    fun detachWebView(webView: WebView) {
        webBridges.remove(webView)?.detach()
    }

    private fun detachAllWebBridges() {
        val bridges = webBridges.values.toList()
        webBridges.clear()
        bridges.forEach { it.detach() }
    }

    private fun checkMainThread() {
        check(Looper.getMainLooper().isCurrentThread) { }
    }

    private fun requireInitialized() {
        check(appContext != null && config != null) { }
    }
}
