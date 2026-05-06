package com.aos.performance.sample.debug

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.aos.performance.sample.BuildConfig
import com.aos.performance.sdk.overlay.DebugOverlay
import java.util.WeakHashMap

/**
 * 디버그 빌드에서 모든 액티비티에 SDK [DebugOverlay]를 띄웁니다.
 *
 * [WeakHashMap]을 써서 액티비티가 파괴된 뒤에는 강하게 붙잡지 않습니다.
 */
class DebugOverlayManager : Application.ActivityLifecycleCallbacks {

    private val overlays = WeakHashMap<Activity, DebugOverlay>()

    override fun onActivityStarted(activity: Activity) {
        if (!BuildConfig.DEBUG) return
        val overlay = overlays.getOrPut(activity) { DebugOverlay(activity) }
        overlay.show()
    }

    override fun onActivityStopped(activity: Activity) {
        overlays[activity]?.hide()
    }

    override fun onActivityDestroyed(activity: Activity) {
        overlays.remove(activity)?.hide()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
