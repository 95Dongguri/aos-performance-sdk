package com.aos.performance.sdk.config

/**
 * 성능 모니터를 시작할 때 넘기는 설정입니다.
 */
data class PerformanceConfig(
    /** true이면 SDK가 추가 진단 출력(예: Logcat)을 낼 수 있습니다. */
    val debugLogging: Boolean = false,
)
