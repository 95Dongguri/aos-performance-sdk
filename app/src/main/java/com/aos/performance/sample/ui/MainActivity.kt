package com.aos.performance.sample.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aos.performance.sdk.PerformanceMonitor
import com.aos.performance.sdk.frame.FrameTracker
import com.aos.performance.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var frameTracker: FrameTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PerformanceMonitor.start()

        frameTracker =
            FrameTracker().apply {
                bindTo(lifecycle)
                start { frameTimeMs -> PerformanceMonitor.recordFrameTimeMs(frameTimeMs) }
            }

        binding.btnWeb.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }
        binding.btnJankList.setOnClickListener {
            startActivity(Intent(this, JankListActivity::class.java))
        }
    }
}
