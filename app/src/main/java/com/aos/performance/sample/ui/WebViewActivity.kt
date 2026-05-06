package com.aos.performance.sample.ui

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.aos.performance.sdk.PerformanceMonitor
import com.aos.performance.sample.R

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view === webView) {
                        PerformanceMonitor.attachWebView(webView)
                    }
                }
            }

        webView.loadDataWithBaseURL(
            null,
            HTML_DEMO,
            "text/html",
            Charsets.UTF_8.name(),
            null,
        )
    }

    override fun onDestroy() {
        PerformanceMonitor.detachWebView(webView)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        /** 주입한 rAF 루프가 document 컨텍스트를 갖도록 하는 최소 HTML 페이지입니다. */
        val HTML_DEMO =
            """
            <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width"/></head>
            <body style="margin:16px;font-family:sans-serif">
            <h3>Web performance sample</h3>
            <canvas id="c" width="300" height="120" style="border:1px solid #ccc"></canvas>
            <script>
              var c=document.getElementById('c');var x=c.getContext('2d');var t=0;
              function draw(){t+=0.08;x.clearRect(0,0,300,120);x.fillStyle='#1976D2';
              x.fillRect(150+Math.sin(t)*80,40+Math.cos(t)*20,24,24);requestAnimationFrame(draw);}
              requestAnimationFrame(draw);
            </script>
            </body></html>
            """.trimIndent()
    }
}
