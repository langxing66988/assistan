package com.langxing.deltamarket

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.http.SslError
import android.view.Gravity
import android.webkit.*
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var tabs: LinearLayout
    private var selectedPath = "v/market_board"
    private var mainFrameHttpError: Int? = null

    private data class MarketTab(val title: String, val path: String)
    private val marketTabs = listOf(
        MarketTab("全部行情", "v/market_board"),
        MarketTab("装备配件", "v/zhanbei"),
        MarketTab("子弹", "v/ammo"),
        MarketTab("钥匙卡", "v/keys"),
        MarketTab("收集品", "v/collection"),
        MarketTab("消耗品", "v/consume")
    )

    private val baseUrl = "https://orzice.com/"
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

    private fun openCurrentPageInBrowser() {
        val url = baseUrl + selectedPath
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            status.text = "未找到可打开网页的浏览器，请先安装或启用浏览器。"
        }
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
            text = "公开行情 · 手动刷新 · 横屏查看价格走势"
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
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener {
                if (::webView.isInitialized) {
                    status.text = "正在刷新当前分类……"
                    webView.reload()
                }
            }
        }
        header.addView(refreshButton)

        val browserButton = TextView(this).apply {
            text = "浏览器"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ink)
            background = rounded(Color.WHITE, 9)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { openCurrentPageInBrowser() }
        }
        header.addView(browserButton, LinearLayout.LayoutParams(-2, -2).apply {
            leftMargin = dp(6)
        })

        val orientationButton = TextView(this).apply {
            text = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "竖屏" else "横屏"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(blue)
            background = rounded(Color.WHITE, 9)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                requestedOrientation = if (
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }
        header.addView(orientationButton, LinearLayout.LayoutParams(-2, -2).apply {
            leftMargin = dp(6)
        })
        root.addView(header)

        status = TextView(this).apply {
            text = "数据来源：Orzice 第三方公开交易行页面"
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
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return false
                    return url.scheme != "https" && url.scheme != "http"
                }

                override fun onPageStarted(
                    view: WebView?, url: String?, favicon: android.graphics.Bitmap?
                ) {
                    mainFrameHttpError = null
                    status.text = "正在连接公开行情页面……"
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val httpError = mainFrameHttpError
                    if (httpError != null) {
                        status.text = "站点返回 HTTP $httpError。可点“浏览器”尝试由系统浏览器打开；此操作不会绕过站点安全限制。"
                        return
                    }
                    view?.evaluateJavascript(
                        "(function(){return document.body ? document.body.innerText : '';})()"
                    ) { bodyText ->
                        val blocked = listOf(
                            "安全策略", "访问被拒绝", "请求被拦截",
                            "403 Forbidden", "Access Denied", "Request blocked"
                        ).any { bodyText?.contains(it, ignoreCase = true) == true }
                        status.text = if (blocked) {
                            "检测到站点安全拦截（不是 APK 构建失败）。可点“浏览器”在系统浏览器中打开；不会尝试绕过站点限制。"
                        } else {
                            "页面响应完成 · 报价时间以列表实际显示为准"
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        status.text = "连接失败：" + (error?.description ?: "网络错误") + "。可切换网络后重试，或点“浏览器”打开。"
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        mainFrameHttpError = errorResponse?.statusCode ?: 0
                        status.text = "站点返回 HTTP " + (errorResponse?.statusCode ?: "错误") + "。"
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler?, error: SslError?
                ) {
                    handler?.cancel()
                    status.text = "HTTPS 证书校验失败，已停止连接。请检查系统时间或网络证书。"
                }
            }
        }
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(9)
        })

        root.addView(TextView(this).apply {
            text = "第三方非官方行情，仅供参考；价格和走势可能延迟或缺失，以游戏内实际成交为准。"
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
        status.text = "正在读取$title……"
        webView.loadUrl(baseUrl + path)
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
