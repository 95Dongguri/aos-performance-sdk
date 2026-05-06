package com.aos.performance.sdk.metrics

import com.aos.performance.sdk.frame.FrameTracker
import com.aos.performance.sdk.webview.WebPerformanceData

/**
 * 네이티브 프레임 간격(예: [FrameTracker])과 WebView 렌더 간격([WebPerformanceData])을 모은 뒤
 * [getMetrics]로 요약 지표를 제공합니다.
 *
 * 스레드 안전하며, 어떤 스레드에서 호출해도 됩니다.
 */
class PerformanceAggregator {

    private val lock = Any()

    private var frameSampleCount = 0
    private var frameTimeSumMs = 0.0
    private var jankCount = 0
    private val recentJankWindow = BooleanArray(RECENT_FRAME_WINDOW_SIZE)
    private var recentIndex = 0
    private var recentCount = 0
    private var recentJankCount = 0

    private var webSampleCount = 0
    private var webRenderTimeSumMs = 0.0

    /**
     * 네이티브 프레임 간격(밀리초)을 한 번 기록합니다. Choreographer / [FrameTracker] 델타에 해당합니다.
     */
    fun recordFrameTimeMs(frameTimeMs: Double) {
        synchronized(lock) {
            frameSampleCount++
            frameTimeSumMs += frameTimeMs
            val isJank = frameTimeMs > JANK_THRESHOLD_MS
            if (isJank) {
                jankCount++
            }

            if (recentCount == RECENT_FRAME_WINDOW_SIZE) {
                if (recentJankWindow[recentIndex]) {
                    recentJankCount--
                }
            } else {
                recentCount++
            }
            recentJankWindow[recentIndex] = isJank
            if (isJank) {
                recentJankCount++
            }
            recentIndex = (recentIndex + 1) % RECENT_FRAME_WINDOW_SIZE
        }
    }

    /**
     * WebView rAF 렌더 간격(밀리초)을 한 번 기록합니다. [WebPerformanceData.renderTimeMs]와 동일한 의미입니다.
     */
    fun recordWebRenderTimeMs(renderTimeMs: Double) {
        synchronized(lock) {
            webSampleCount++
            webRenderTimeSumMs += renderTimeMs
        }
    }

    /**
     * 집계 메트릭의 특정 시점 스냅샷을 반환합니다.
     */
    fun getMetrics(): PerformanceMetrics {
        synchronized(lock) {
            val averageFrameTimeMs =
                if (frameSampleCount > 0) {
                    frameTimeSumMs / frameSampleCount
                } else {
                    0.0
                }
            val fps =
                if (averageFrameTimeMs > 0.0) {
                    1000.0 / averageFrameTimeMs
                } else {
                    0.0
                }
            val jankRate =
                if (recentCount > 0) {
                    (recentJankCount.toFloat() / recentCount.toFloat())
                } else {
                    0f
                }
            val state =
                when {
                    fps >= 55.0 && jankRate < 0.05f -> PerformanceState.GOOD
                    fps >= 45.0 && jankRate < 0.15f -> PerformanceState.WARNING
                    else -> PerformanceState.BAD
                }
            val averageWebRenderTimeMs =
                if (webSampleCount > 0) {
                    webRenderTimeSumMs / webSampleCount
                } else {
                    0.0
                }
            return PerformanceMetrics(
                state = state,
                fps = fps,
                averageFrameTimeMs = averageFrameTimeMs,
                jankCount = jankCount,
                jankRate = jankRate,
                averageWebRenderTimeMs = averageWebRenderTimeMs,
            )
        }
    }

    private companion object {
        private const val JANK_THRESHOLD_MS = 17.0
        private const val RECENT_FRAME_WINDOW_SIZE = 60
    }
}
