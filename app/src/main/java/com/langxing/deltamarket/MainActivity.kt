package com.langxing.deltamarket

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var tabs: LinearLayout
    private var selectedPath = "zhanbei"

    private data class MarketTab(val title: String, val path: String)
    private val marketTabs = listOf(
        MarketTab("装备/枪械", "zhanbei"),
        MarketTab("子弹", "ammo"),
        MarketTab("钥匙卡", "keys"),
        MarketTab("收集品", "collection"),
        MarketTab("消耗品", "consume")
    )

    private val blue = Color.rgb(31, 111, 210)
    private val ink = Color.rgb(25, 48, 78)
    private val pale = Color.rgb(242, 247, 253)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 10): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            setPadding(dp(12), dp(12), dp(12), dp(6))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleBox.addView(TextView(this).apply {
            text = "三角洲市场助手"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        titleBox.addView(TextView(this).apply {
            text = "免费公开行情 · 手动刷新"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))

        val refreshButton = TextView(this).apply {
            text = "刷新"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(blue, 9)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener {
                if (::webView.isInitialized) {
                    status.text = "正在刷新公开行情……"
                    webView.reload()
                }
            }
        }
        header.addView(refreshButton)
        root.addView(header)

        status = TextView(this).apply {
            text = "数据来源：Orzice 公开行情网页"
            textSize = 12f
            setTextColor(Color.rgb(65, 110, 160))
            setPadding(dp(2), dp(10), dp(2), dp(8))
        }
        root.addView(status)

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        marketTabs.forEach { tab ->
            val chip = TextView(this).apply {
                text = tab.title
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(9), dp(14), dp(9))
                setOnClickListener { openCategory(tab.path) }
            }
            tabs.addView(chip, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(7)
            })
        }
        tabScroll.addView(tabs)
        root.addView(tabScroll)

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return false
                    return url.scheme != "https" && url.scheme != "http"
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    status.text = "正在读取行情页面……"
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    status.text = "行情页面已加载 · 页面内可筛选和搜索"
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        status.text = "网页暂时无法连接，请检查网络后点刷新"
                    }
                }
            }
        }
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(9)
        })

        root.addView(TextView(this).apply {
            text = "行情由第三方公开网站提供；价格以游戏内交易行为准。"
            textSize = 10f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })

        setContentView(root)
        openCategory(selectedPath)
    }

    private fun openCategory(path: String) {
        selectedPath = path
        for (i in 0 until tabs.childCount) {
            val view = tabs.getChildAt(i) as TextView
            val tab = marketTabs[i]
            val active = tab.path == path
            view.setTextColor(if (active) Color.WHITE else ink)
            view.background = rounded(if (active) blue else Color.WHITE, 9)
        }
        val title = marketTabs.firstOrNull { it.path == path }?.title ?: "行情"
        status.text = "正在读取" + title + "……"
        webView.loadUrl("https://orzice.com/v/" + path)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
