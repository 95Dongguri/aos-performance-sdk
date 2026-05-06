package com.aos.performance.sdk.frame

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * [Choreographer]로 연속 디스플레이 프레임 사이 시간을 재어 밀리초 단위로 보고합니다.
 *
 * **스레딩:** [start]는 메인 스레드에서 호출해야 합니다. [stop]은 어떤 스레드에서 호출해도 안전합니다.
 *
 * **수명:** [Lifecycle](예: `FragmentActivity`나 `Fragment`)에 [bindTo]로 묶어 두면
 * 소유자가 파괴될 때 콜백이 정리되므로, [stop]을 잊어도 누수를 막기 쉽습니다.
 */
class FrameTracker(
    private val choreographer: Choreographer = Choreographer.getInstance(),
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var onFrameTimeMs: ((Double) -> Unit)? = null
    private var lastFrameTimeNanos: Long = 0L
    private var running = false

    private var boundLifecycle: Lifecycle? = null

    private val lifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                boundLifecycle = null
                stopInternal()
            }
        }

    private val frameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return

                if (lastFrameTimeNanos != 0L) {
                    val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0
                    onFrameTimeMs?.invoke(deltaMs)
                }
                lastFrameTimeNanos = frameTimeNanos

                if (running) {
                    choreographer.postFrameCallback(this)
                }
            }
        }

    /**
     * [lifecycle]이 [Lifecycle.Event.ON_DESTROY]에 도달하면 [stopInternal]이 실행되도록 합니다.
     * [FrameTracker] 인스턴스당 최대 한 번만 호출하세요.
     */
    fun bindTo(lifecycle: Lifecycle) {
        check(boundLifecycle == null) { }
        boundLifecycle = lifecycle
        lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * 프레임마다 밀리초 단위 델타(이전 프레임 vsync 이후 경과 시간)를 받기 시작합니다.
     * 메인 스레드에서 호출해야 합니다. 이미 실행 중이면 이전 리스너를 교체합니다.
     */
    fun start(onFrameTimeMs: (Double) -> Unit) {
        checkMainThread()
        this.onFrameTimeMs = onFrameTimeMs
        if (running) {
            choreographer.removeFrameCallback(frameCallback)
        }
        running = true
        lastFrameTimeNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    /**
     * 프레임 콜백을 멈추고 리스너를 비웁니다. 어떤 스레드에서 호출해도 안전합니다.
     */
    fun stop() {
        if (Looper.getMainLooper().isCurrentThread) {
            stopInternal()
        } else {
            mainHandler.post { stopInternal() }
        }
    }

    private fun stopInternal() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
        onFrameTimeMs = null
        lastFrameTimeNanos = 0L
    }

    private fun checkMainThread() {
        check(Looper.getMainLooper().isCurrentThread) { }
    }
}
