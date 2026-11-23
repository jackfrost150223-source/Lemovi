package com.lemovi.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.net.Uri
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.view.inputmethod.EditorInfo
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var bar: LinearLayout
    private lateinit var pageRoot: LinearLayout
    private var immersive = false
    private var adBlockEnabled = true
    private lateinit var adBlockButton: Button
    private var customVideoView: View? = null
    private var customVideoCallback: WebChromeClient.CustomViewCallback? = null

    // Bloqueo local de dominios publicitarios y de seguimiento conocidos.
    // Se aplica solo a subrecursos: nunca bloquea la navegación principal ni el video del sitio.
    private val blockedAdHosts = setOf(
        // Google y redes publicitarias
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "admob.com", "pagead2.googlesyndication.com",
        "adnxs.com", "adsystem.com", "adsafeprotected.com",
        // Redes de anuncios y pop-ups
        "adform.net", "adform.com", "adroll.com", "advertising.com",
        "bidswitch.net", " casalemedia.com", "casalemedia.com",
        "criteo.com", "criteo.net", "demdex.net", "eyereturn.com",
        "media.net", "outbrain.com", "outbrainimg.com", "popads.net",
        "popcash.net", "propellerads.com", "pubmatic.com", " rubiconproject.com",
        "rubiconproject.com", "sharethrough.com", "smartadserver.com",
        " taboola.com", "taboola.com", "teads.tv", " lijit.com", "lijit.com",
        "undertone.com", "yieldmo.com", "zedo.com", "trafficjunky.net",
        "exoclick.com", " juicyads.com", "juicyads.com",
        "adsterra.com", "hilltopads.net", "monetag.com", "onclicka.com",
        "adcash.com", "popunder.net", "ad-maven.com", "trafficstars.com",
        "adnium.com", "adspyglass.com", "adtng.com", "adnami.io",
        "adpushup.com", "adthrive.com", "mediavine.com", "revcontent.com",
        "mgid.com", "dianomi.com", "zemanta.com", "shareasale.com",
        "impact.com", "awin.com", "partnerize.com", "rakutenadvertising.com",
        "thetradedesk.com", "openx.net", "indexexchange.com", "appnexus.com",
        "33across.com", "triplelift.com", "sovrn.com", "conversantmedia.com",
        "adkernel.com", "adzerk.net", "bluekai.com", "krxd.net",
        "rlcdn.com", "contextweb.com", "imrworldwide.com", "moatads.com",
        "adsrvr.org", "serving-sys.com", "sizmek.com",
        // Seguimiento y medición publicitaria
        "hotjar.com", "scorecardresearch.com", "quantserve.com",
        " branch.io", "branch.io", " app-measurement.com", "app-measurement.com",
        "analytics.yahoo.com", "matomo.cloud", "mixpanel.com",
        "segment.io", "segment.com", "amplitude.com"
    ).map { it.trim() }.toSet()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(10, 8, 18)
        window.navigationBarColor = Color.BLACK
        adBlockEnabled = getPreferences(MODE_PRIVATE).getBoolean("ad_block_enabled", true)

        val mobileLayout = resources.configuration.screenWidthDp < 600
        pageRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 8, 18))
            setPadding(if (mobileLayout) 12 else 22, if (mobileLayout) 14 else 18,
                if (mobileLayout) 12 else 22, 8)
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_lemovi)
            contentDescription = "Logo Cattleya de Lemovi"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        brandRow.addView(logo, LinearLayout.LayoutParams(if (mobileLayout) 42 else 50, if (mobileLayout) 42 else 50))
        val brand = TextView(this).apply {
            text = "LEMOVI"
            textSize = 25f
            setTextColor(Color.rgb(218, 166, 255))
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            letterSpacing = 0.18f
            setPadding(10, 0, 0, 0)
        }
        brandRow.addView(brand, LinearLayout.LayoutParams(-1, -1))
        pageRoot.addView(brandRow, LinearLayout.LayoutParams(-1, if (mobileLayout) 48 else 56))

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            hint = "Pegá el enlace de la película o página"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setSelectAllOnFocus(true)
            setPadding(15, 8, 15, 8)
            setBackgroundColor(Color.rgb(38, 29, 52))
            contentDescription = "Buscador de direcciones web"
        }
        val go = makeButton("IR", "Abrir dirección") { openAddress() }
        addressRow.addView(address, LinearLayout.LayoutParams(0, if (mobileLayout) 48 else 52, 1f))
        addressRow.addView(go, LinearLayout.LayoutParams(if (mobileLayout) 56 else 76, if (mobileLayout) 48 else 52).apply { leftMargin = 7 })
        pageRoot.addView(addressRow, LinearLayout.LayoutParams(-1, if (mobileLayout) 48 else 52).apply { topMargin = 10 })

        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = makeButton("‹ Atrás", "Volver a la página anterior") { if (browser.canGoBack()) browser.goBack() else Toast.makeText(this, "No hay página anterior", Toast.LENGTH_SHORT).show() }
        val forward = makeButton("Adelante ›", "Avanzar a la página siguiente") { if (browser.canGoForward()) browser.goForward() else Toast.makeText(this, "No hay página siguiente", Toast.LENGTH_SHORT).show() }
        val full = makeButton("⛶ Pantalla", "Activar o salir de pantalla completa") { toggleImmersive() }
        adBlockButton = makeButton(if (adBlockEnabled) "AD: ON" else "AD: OFF", "Activar o desactivar bloqueador de anuncios") {
            adBlockEnabled = !adBlockEnabled
            getPreferences(MODE_PRIVATE).edit().putBoolean("ad_block_enabled", adBlockEnabled).apply()
            adBlockButton.text = if (adBlockEnabled) "AD: ON" else "AD: OFF"
            Toast.makeText(this, if (adBlockEnabled) "Bloqueador activado" else "Bloqueador desactivado", Toast.LENGTH_SHORT).show()
            browser.reload()
        }
        if (mobileLayout) {
            val params = LinearLayout.LayoutParams(0, 44, 1f)
            bar.addView(back, params)
            bar.addView(forward, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4 })
            bar.addView(full, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4 })
            bar.addView(adBlockButton, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4 })
            pageRoot.addView(bar, LinearLayout.LayoutParams(-1, 44).apply { topMargin = 8; bottomMargin = 9 })
        } else {
            bar.addView(back, buttonParams())
            bar.addView(forward, buttonParams())
            bar.addView(full, buttonParams())
            bar.addView(adBlockButton, LinearLayout.LayoutParams(90, 52).apply { leftMargin = 7 })
            pageRoot.addView(bar, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 8; bottomMargin = 10 })
        }

        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                setSupportMultipleWindows(false)
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    if (customVideoView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customVideoView = view
                    customVideoCallback = callback
                    pageRoot.visibility = View.GONE
                    (window.decorView as FrameLayout).addView(
                        view,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }

                override fun onHideCustomView() {
                    hideCustomVideo()
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (isDirectVideo(uri)) { openNativePlayer(uri.toString()); return true }
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                    val host = uri.host?.lowercase() ?: return super.shouldInterceptRequest(view, request)
                    val isAdHost = blockedAdHosts.any { host == it || host.endsWith(".$it") }
                    if (adBlockEnabled && !request.isForMainFrame && isAdHost) {
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(this@MainActivity, "No se pudo cargar la página. Revisá el enlace o la conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        pageRoot.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(pageRoot)

        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { openAddress(); true } else false
        }
        address.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) address.setSelection(address.text.length)
        }
        address.setText("https://")
    }

    private fun buttonParams() = LinearLayout.LayoutParams(88, 52).apply { leftMargin = 7 }

    private fun makeButton(label: String, description: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (resources.configuration.screenWidthDp < 600) 10.5f else 12f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(105, 45, 150))
        contentDescription = description
        setPadding(2, 0, 2, 0)
        setOnClickListener { action() }
        setOnFocusChangeListener { v, focused -> v.alpha = if (focused) 1f else 0.86f }
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
        val uri = Uri.parse(url)
        if (isDirectVideo(uri)) openNativePlayer(url) else browser.loadUrl(url)
        browser.requestFocus()
    }

    private fun isDirectVideo(uri: Uri): Boolean {
        val path = uri.path?.lowercase() ?: return false
        return listOf(".mp4", ".m3u8", ".mpd", ".webm", ".m4v", ".3gp").any { path.endsWith(it) }
    }

    private fun openNativePlayer(url: String) {
        startActivity(android.content.Intent(this, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_URL, url))
    }

    private fun hideCustomVideo() {
        val view = customVideoView ?: return
        (window.decorView as FrameLayout).removeView(view)
        customVideoView = null
        customVideoCallback?.onCustomViewHidden()
        customVideoCallback = null
        pageRoot.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = if (immersive) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else 0
    }

    private fun toggleImmersive() {
        immersive = !immersive
        pageRoot.visibility = if (immersive) View.GONE else View.VISIBLE
        window.decorView.systemUiVisibility = if (immersive) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else 0
    }

    override fun onBackPressed() {
        if (customVideoView != null) {
            hideCustomVideo()
        } else if (immersive) {
            toggleImmersive()
        } else if (::browser.isInitialized && browser.canGoBack()) {
            browser.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return super.onKeyDown(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        if (::browser.isInitialized) {
            browser.stopLoading()
            browser.loadUrl("about:blank")
            browser.destroy()
        }
        super.onDestroy()
    }
}
