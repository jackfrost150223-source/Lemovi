package com.lemovi.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.view.inputmethod.EditorInfo

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var bar: LinearLayout
    private val history = mutableListOf<String>()
    private var historyIndex = -1
    private var immersive = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(10, 8, 18)
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 8, 18))
            setPadding(22, 16, 22, 10)
        }

        val brand = TextView(this).apply {
            text = "LEMOVI"
            textSize = 25f
            setTextColor(Color.rgb(205, 145, 255))
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            letterSpacing = 0.18f
            setPadding(4, 0, 0, 10)
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, 48))

        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            hint = "Pegá la dirección de la película o página"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setSelectAllOnFocus(true)
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.rgb(38, 29, 52))
        }
        val go = makeButton("IR") { openAddress() }
        val back = makeButton("◀") { if (browser.canGoBack()) browser.goBack() }
        val forward = makeButton("▶") { if (browser.canGoForward()) browser.goForward() }
        val full = makeButton("⛶") { toggleImmersive() }

        bar.addView(address, LinearLayout.LayoutParams(0, 54, 1f))
        bar.addView(go, buttonParams())
        bar.addView(back, buttonParams())
        bar.addView(forward, buttonParams())
        bar.addView(full, buttonParams())
        root.addView(bar)

        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                }
            }
        }
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { openAddress(); true } else false
        }
        address.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) address.setSelection(address.text.length)
        }
        address.setText("https://")
        address.requestFocus()
    }

    private fun buttonParams() = LinearLayout.LayoutParams(64, 54).apply { leftMargin = 7 }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(105, 45, 150))
        setOnClickListener { action() }
        setOnFocusChangeListener { v, focused ->
            v.alpha = if (focused) 1f else 0.82f
        }
    }

    private fun openAddress() {
        var url = address.text.toString().trim()
        if (url.isEmpty()) return
        if (!url.contains("://")) url = "https://$url"
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            Toast.makeText(this, "Dirección no válida", Toast.LENGTH_SHORT).show()
            return
        }
        address.setText(url)
        browser.loadUrl(url)
        if (history.lastOrNull() != url) history.add(url)
        historyIndex = history.lastIndex
        browser.requestFocus()
    }

    private fun toggleImmersive() {
        immersive = !immersive
        bar.visibility = if (immersive) View.GONE else View.VISIBLE
        window.decorView.systemUiVisibility = if (immersive) {
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else 0
    }

    override fun onBackPressed() {
        if (immersive) {
            toggleImmersive()
        } else if (browser.canGoBack()) {
            browser.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        if (::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
