package dev.openhands.mobile

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private val prefs by lazy { getSharedPreferences("openhands_mobile", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showConnectScreen(prefs.getString("server_url", "") ?: "")
    }

    private fun showConnectScreen(savedUrl: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(Color.rgb(248, 250, 252))
        }
        val title = TextView(this).apply {
            text = "OpenHands Mobile"
            textSize = 26f
            setTextColor(Color.rgb(15, 23, 42))
            gravity = Gravity.CENTER_VERTICAL
        }
        val intro = TextView(this).apply {
            text = "Kết nối tới máy chủ Agent Canvas đang chạy. APK này là ứng dụng khách; nó không cài máy chủ AI lên điện thoại."
            textSize = 15f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, dp(12), 0, dp(18))
        }
        address = EditText(this).apply {
            hint = "https://địa-chỉ-máy-chủ"
            setSingleLine(true)
            textSize = 16f
            setText(savedUrl)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val connect = Button(this).apply {
            text = "Kết nối"
            setOnClickListener { connectToServer() }
        }
        status = TextView(this).apply {
            text = "Nhập URL máy chủ Agent Canvas, ví dụ https://your-server.example"
            textSize = 13f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(title)
        root.addView(intro)
        root.addView(address, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(connect, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        root.addView(status)
        setContentView(root)
    }

    private fun connectToServer() {
        var url = address.text.toString().trim()
        if (url.isBlank()) {
            status.text = "Hãy nhập địa chỉ máy chủ trước."
            return
        }
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) {
            url = "https://$url"
        }
        val parsed = android.net.Uri.parse(url)
        if (parsed.host.isNullOrBlank()) {
            status.text = "Địa chỉ không hợp lệ. Hãy kiểm tra URL."
            return
        }
        prefs.edit().putString("server_url", url).apply()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        showBrowser(url)
    }

    private fun showBrowser(url: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val home = Button(this).apply {
            text = "Máy chủ"
            setOnClickListener { showConnectScreen(prefs.getString("server_url", "") ?: "") }
        }
        val reload = Button(this).apply {
            text = "Tải lại"
            setOnClickListener { webView.reload() }
        }
        toolbar.addView(home)
        toolbar.addView(reload)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.setSupportZoom(true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    this@MainActivity.progress.progress = newProgress
                    this@MainActivity.progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    status.text = "Đang tải..."
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    status.text = url ?: ""
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(this@MainActivity, "Không truy cập được máy chủ. Kiểm tra URL và kết nối mạng.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            loadUrl(url)
        }
        status = TextView(this).apply {
            textSize = 10f
            setTextColor(Color.DKGRAY)
            maxLines = 1
        }
        root.addView(toolbar)
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(status, LinearLayout.LayoutParams(-1, dp(18)))
        setContentView(root)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else showConnectScreen(prefs.getString("server_url", "") ?: "")
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
