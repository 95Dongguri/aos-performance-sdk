package com.aos.performance.sdk.metrics

/**
 * [PerformanceAggregator.getMetrics]에서 얻는 집계 성능 지표의 스냅샷입니다.
 *
 * @property state FPS와 Jank 비율을 바탕으로 한 종합 성능 상태.
 * @property fps 네이티브 프레임 간격 평균으로부터 추정한 초당 프레임 수
 *   (샘플이 있으면 `1000 / averageFrameTimeMs`).
 * @property averageFrameTimeMs 네이티브 프레임 사이 평균 시간(밀리초). 예: [com.aos.performance.sdk.frame.FrameTracker].
 *   샘플이 없으면 `0`.
 * @property jankCount Jank 임계값을 **초과한** 네이티브 프레임 샘플 개수.
 * @property jankRate 최근 구간에서 젠크 프레임 비율. 프레임 샘플이 없으면 `0`.
 * @property averageWebRenderTimeMs WebView 브리지에서 온 rAF 간격 평균(밀리초). 샘플이 없으면 `0`.
 */
data class PerformanceMetrics(
    val state: PerformanceState,
    val fps: Double,
    val averageFrameTimeMs: Double,
    val jankCount: Int,
    val jankRate: Float,
    val averageWebRenderTimeMs: Double,
)
