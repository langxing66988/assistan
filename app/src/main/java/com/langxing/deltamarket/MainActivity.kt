package com.langxing.deltamarket

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(18))
            setBackgroundColor(Color.rgb(245, 248, 253))
        }
        root.addView(TextView(this).apply {
            text = "三角洲市场助手"
            textSize = 25f
            setTextColor(Color.rgb(24, 54, 92))
        })
        root.addView(TextView(this).apply {
            text = "中国版 · 全部可交易物品 · 手动刷新"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(6), 0, dp(18))
        })
        root.addView(Button(this).apply {
            text = "手动刷新行情"
            setOnClickListener {
                status.text = "行情 API 尚未配置。当前没有真实价格数据。"
            }
        })
        status = TextView(this).apply {
            text = "数据源未连接"
            textSize = 14f
            setTextColor(Color.rgb(160, 90, 20))
            setPadding(0, dp(14), 0, dp(14))
        }
        root.addView(status)

        val scroll = ScrollView(this)
        val cards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rows = listOf(
            "物品列表" to "接入数据源后显示全部可交易物品",
            "当前价格" to "--",
            "历史价格" to "--",
            "价格更新时间" to "--"
        )
        rows.forEach { (heading, value) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(Color.WHITE)
            }
            card.addView(TextView(this).apply {
                text = heading
                textSize = 14f
                setTextColor(Color.GRAY)
            })
            card.addView(TextView(this).apply {
                text = value
                textSize = 17f
                setTextColor(Color.rgb(24, 54, 92))
                setPadding(0, dp(5), 0, 0)
            })
            cards.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            })
        }
        scroll.addView(cards)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "初始工程框架：真实行情需要接入经过验证、允许使用的第三方 API。"
            textSize = 12f
            setTextColor(Color.GRAY)
        })
        setContentView(root)
    }
}
