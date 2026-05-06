package com.aos.performance.sdk.webview

import org.json.JSONObject

/**
 * WebView 안에서 만들어져([requestAnimationFrame] 경로) Android로 전달되는 페이로드입니다.
 *
 * @property renderTimeMs 연속 애니메이션 프레임 사이 시간(밀리초, rAF 델타).
 */
data class WebPerformanceData(
    val renderTimeMs: Double,
) {
    companion object {
        fun fromJson(json: String): WebPerformanceData {
            val o = JSONObject(json)
            return WebPerformanceData(
                renderTimeMs = o.getDouble("renderTimeMs"),
            )
        }
    }
}
