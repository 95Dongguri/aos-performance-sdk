package com.aos.performance.sample

import android.app.Application
import com.aos.performance.sample.debug.DebugOverlayManager
import com.aos.performance.sdk.PerformanceMonitor
import com.aos.performance.sdk.config.PerformanceConfig

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PerformanceMonitor.init(
            this,
            PerformanceConfig(debugLogging = BuildConfig.DEBUG),
        )
        if (BuildConfig.DEBUG) {
            registerActivityLifecycleCallbacks(DebugOverlayManager())
        }
    }
}
