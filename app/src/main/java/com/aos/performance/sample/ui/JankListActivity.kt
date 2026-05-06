package com.aos.performance.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aos.performance.sample.R
import com.aos.performance.sample.databinding.ItemJankBinding

class JankListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jank_list)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = JankAdapter(ITEM_COUNT)
    }

    private class JankAdapter(
        private val count: Int,
    ) : RecyclerView.Adapter<JankAdapter.VH>() {

        class VH(
            val binding: ItemJankBinding,
        ) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): VH {
            val binding = ItemJankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int,
        ) {
            simulateJankOnMainThread()
            holder.binding.text.text = "Row $position — scroll to feel jank"
        }

        override fun getItemCount(): Int = count

        /**
         * 메인(UI) 스레드에서 무거운 연산을 해 스크롤 시 프레임 드롭·젠크가 SDK 오버레이에 보이게 합니다.
         */
        private fun simulateJankOnMainThread() {
            var acc = 0L
            repeat(BUSY_ITERATIONS) { i ->
                acc += (i * 31).toLong() % 997
            }
            if (acc == 0L) Thread.yield()
        }
    }

    private companion object {
        const val ITEM_COUNT = 400
        const val BUSY_ITERATIONS = 350_000
    }
}
