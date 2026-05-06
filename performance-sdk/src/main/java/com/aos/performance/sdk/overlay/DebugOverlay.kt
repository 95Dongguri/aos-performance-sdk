package com.aos.performance.sdk.overlay

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aos.performance.sdk.PerformanceMonitor
import com.aos.performance.sdk.metrics.PerformanceMetrics
import com.aos.performance.sdk.metrics.PerformanceState
import java.util.Locale

/**
 * [PerformanceMetrics]를 화면에 짧게 보여 줍니다. 액티비티 윈도우 데코의 자식 뷰로 붙으며
 * `SYSTEM_ALERT_WINDOW` 권한은 필요 없습니다. [show]는 메인 스레드에서 호출하는 것이 좋고,
 * [Activity.onDestroy](또는 액티비티가 종료 중일 때)에는 반드시 [hide]를 호출하세요.
 */
class DebugOverlay(
    private val activity: Activity,
    private val metricsProvider: () -> PerformanceMetrics = { PerformanceMonitor.aggregator.getMetrics() },
) {
    private val handler = Handler(Looper.getMainLooper())
    private var host: FrameLayout? = null
    private var stateBadge: TextView? = null
    private var label: TextView? = null
    private var visible = false
    private var statusBarTopInsetPx: Int = 0
    private var hasUserPosition: Boolean = false
    private var dragOffsetX: Float = 0f
    private var dragOffsetY: Float = 0f

    private val tick =
        object : Runnable {
            override fun run() {
                if (!visible) return
                val act = host?.context as? Activity ?: activity
                if (act.isFinishing || act.isDestroyed) {
                    hideInternal()
                    return
                }
                val m = metricsProvider()
                stateBadge?.applyState(m.state)
                label?.text = formatMetrics(m)
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }

    /** 오버레이를 표시하고 주기적으로 갱신합니다. 메인 스레드 전용입니다. */
    fun show() {
        checkMainThread()
        if (visible) return
        if (activity.isFinishing || activity.isDestroyed) return

        val decor = activity.window.decorView as? FrameLayout ?: return

        val container = FrameLayout(activity).apply {
            isClickable = false
            isFocusable = false
        }

        val stack =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
            }

        val badge =
            TextView(activity).apply {
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(dp(6), dp(4), dp(6), dp(4))
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
            }

        val tv =
            TextView(activity).apply {
                typeface = Typeface.MONOSPACE
                textSize = 11f
                setTextColor(Color.WHITE)
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
                setBackgroundColor(0x66000000.toInt())
                setPadding(dp(6), dp(4), dp(6), dp(4))
            }

        stack.addView(
            badge,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(4) },
        )
        stack.addView(
            tv,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        container.addView(
            stack,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val lp =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val m = dp(8)
                setMargins(m, m, m, m)
            }

        // 데코가 인셋을 소비하지 않을 때도 상태바 아래에 오도록 배치합니다.
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val statusBarTop =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusBarTopInsetPx = statusBarTop
            val m = dp(8)
            (v.layoutParams as? FrameLayout.LayoutParams)?.let { p ->
                if (!hasUserPosition && p.topMargin != statusBarTop + m) {
                    p.topMargin = statusBarTop + m
                    p.leftMargin = m
                    p.rightMargin = m
                    p.bottomMargin = m
                    v.layoutParams = p
                }
            }
            insets
        }

        container.setOnTouchListener { v, e ->
            val decorW = decor.width
            val decorH = decor.height
            if (decorW <= 0 || decorH <= 0) return@setOnTouchListener false

            val p = (v.layoutParams as? FrameLayout.LayoutParams) ?: return@setOnTouchListener false
            val margin = dp(8)
            val minTop = statusBarTopInsetPx + margin

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 손가락과 뷰 사이 오프셋을 저장해 드래그 시 위치가 튀지 않게 합니다.
                    dragOffsetX = e.rawX - p.leftMargin
                    dragOffsetY = e.rawY - p.topMargin
                    hasUserPosition = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    var newLeft = (e.rawX - dragOffsetX).toInt()
                    var newTop = (e.rawY - dragOffsetY).toInt()

                    // 데코 뷰 경계 안으로 맞춥니다.
                    val maxLeft = decorW - v.width - margin
                    val maxTop = decorH - v.height - margin

                    if (newLeft < margin) newLeft = margin
                    if (newTop < minTop) newTop = minTop
                    if (maxLeft >= margin && newLeft > maxLeft) newLeft = maxLeft
                    if (maxTop >= minTop && newTop > maxTop) newTop = maxTop

                    p.leftMargin = newLeft
                    p.topMargin = newTop
                    p.rightMargin = margin
                    p.bottomMargin = margin
                    v.layoutParams = p
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> true

                else -> false
            }
        }

        decor.addView(container, lp)
        ViewCompat.requestApplyInsets(container)
        host = container
        stateBadge = badge
        label = tv
        visible = true
        handler.post(tick)
    }

    /** 오버레이를 제거하고 갱신을 멈춥니다. 필요 시 메인으로 포스팅해 호출해도 안전합니다. */
    fun hide() {
        if (Looper.getMainLooper().isCurrentThread) {
            hideInternal()
        } else {
            handler.post { hideInternal() }
        }
    }

    private fun hideInternal() {
        visible = false
        handler.removeCallbacks(tick)
        val h = host ?: return
        val decor = activity.window.decorView as? FrameLayout
        decor?.removeView(h)
        host = null
        stateBadge = null
        label = null
        hasUserPosition = false
    }

    private fun checkMainThread() {
        check(Looper.getMainLooper().isCurrentThread) { }
    }

    private fun dp(v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()

    private companion object {
        const val UPDATE_INTERVAL_MS = 500L

        fun formatMetrics(m: PerformanceMetrics): String =
            buildString {
                append("FPS: ")
                append(String.format(Locale.US, "%.1f", m.fps))
                append('\n')
                append("Jank: ")
                append(m.jankCount)
                append(" (")
                append(String.format(Locale.US, "%.1f%%", m.jankRate * 100f))
                append(')')
                append('\n')
                append("FrameTime: ")
                append(String.format(Locale.US, "%.2f ms", m.averageFrameTimeMs))
                append('\n')
                append("WebTime: ")
                append(String.format(Locale.US, "%.2f ms", m.averageWebRenderTimeMs))
            }

        fun TextView.applyState(state: PerformanceState) {
            text = "STATE: $state"
            val color =
                when (state) {
                    PerformanceState.GOOD -> Color.parseColor("#4CAF50")
                    PerformanceState.WARNING -> Color.parseColor("#FFC107")
                    PerformanceState.BAD -> Color.parseColor("#F44336")
                }
            setBackgroundColor(color)
        }
    }
}
