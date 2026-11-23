package com.lemovi.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var browserHost: FrameLayout
    private var adPopup: WebView? = null
    private lateinit var address: EditText
    private lateinit var bar: LinearLayout
    private lateinit var pageRoot: LinearLayout
    private lateinit var videoPanel: LinearLayout
    private lateinit var videoResults: LinearLayout
    private lateinit var searchVideosButton: Button
    private var immersive = false
    private var adBlockEnabled = true
    private lateinit var adBlockButton: Button
    private var customVideoView: View? = null
    private var customVideoCallback: WebChromeClient.CustomViewCallback? = null

    private val blockedAdHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "admob.com", "pagead2.googlesyndication.com",
        "adnxs.com", "adsystem.com", "adsafeprotected.com",
        "adform.net", "adform.com", "adroll.com", "advertising.com",
        "bidswitch.net", "casalemedia.com", "criteo.com", "criteo.net",
        "demdex.net", "eyereturn.com", "media.net", "outbrain.com",
        "outbrainimg.com", "popads.net", "popcash.net", "propellerads.com",
        "pubmatic.com", "rubiconproject.com", "sharethrough.com",
        "smartadserver.com", "taboola.com", "teads.tv", "lijit.com",
        "undertone.com", "yieldmo.com", "zedo.com", "trafficjunky.net",
        "exoclick.com", "juicyads.com", "adsterra.com", "hilltopads.net",
        "monetag.com", "onclicka.com", "adcash.com", "popunder.net",
        "ad-maven.com", "trafficstars.com", "adnium.com", "adspyglass.com",
        "adtng.com", "adnami.io", "adpushup.com", "adthrive.com",
        "mediavine.com", "revcontent.com", "mgid.com", "dianomi.com",
        "zemanta.com", "shareasale.com", "impact.com", "awin.com",
        "partnerize.com", "rakutenadvertising.com", "thetradedesk.com",
        "openx.net", "indexexchange.com", "appnexus.com", "33across.com",
        "triplelift.com", "sovrn.com", "conversantmedia.com", "adkernel.com",
        "adzerk.net", "bluekai.com", "krxd.net", "rlcdn.com", "contextweb.com",
        "imrworldwide.com", "moatads.com", "adsrvr.org", "serving-sys.com",
        "sizmek.com", "hotjar.com", "scorecardresearch.com", "quantserve.com",
        "branch.io", "app-measurement.com", "analytics.yahoo.com",
        "matomo.cloud", "mixpanel.com", "segment.io", "segment.com", "amplitude.com"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(10, 8, 18)
        window.navigationBarColor = Color.BLACK
        // Bloqueo de publicidad activado por defecto.
        adBlockEnabled = getPreferences(MODE_PRIVATE).getBoolean("ad_block_enabled", true)
        getPreferences(MODE_PRIVATE).edit().putBoolean("ad_block_enabled", adBlockEnabled).apply()

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
        val go = makeButton("IR", "Abrir dirección web") { openAddress() }
        addressRow.addView(address, LinearLayout.LayoutParams(0, if (mobileLayout) 48 else 52, 1f))
        addressRow.addView(go, LinearLayout.LayoutParams(if (mobileLayout) 56 else 76, if (mobileLayout) 48 else 52).apply { leftMargin = 7 })
        pageRoot.addView(addressRow, LinearLayout.LayoutParams(-1, if (mobileLayout) 48 else 52).apply { topMargin = 10 })

        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = makeButton("‹ Atrás", "Volver a la página anterior") {
            if (browser.canGoBack()) browser.goBack() else Toast.makeText(this, "No hay página anterior", Toast.LENGTH_SHORT).show()
        }
        val forward = makeButton("Adelante ›", "Avanzar a la página siguiente") {
            if (browser.canGoForward()) browser.goForward() else Toast.makeText(this, "No hay página siguiente", Toast.LENGTH_SHORT).show()
        }
        val full = makeButton("⛶ Pantalla", "Activar o salir de pantalla completa") { toggleImmersive() }
        val historyButton = makeButton("Historial", "Ver páginas visitadas recientemente") { showHistory() }
        adBlockButton = makeButton(if (adBlockEnabled) "AD: ON" else "AD: OFF", "Activar o desactivar bloqueador de anuncios") {
            adBlockEnabled = !adBlockEnabled
            getPreferences(MODE_PRIVATE).edit().putBoolean("ad_block_enabled", adBlockEnabled).apply()
            adBlockButton.text = if (adBlockEnabled) "AD: ON" else "AD: OFF"
            Toast.makeText(this, if (adBlockEnabled) "Bloqueador activado" else "Anuncios y recursos permitidos", Toast.LENGTH_SHORT).show()
            browser.reload()
        }
        if (mobileLayout) {
            bar.addView(back, LinearLayout.LayoutParams(0, 42, 1f))
            bar.addView(forward, LinearLayout.LayoutParams(0, 42, 1f).apply { leftMargin = 4 })
            bar.addView(full, LinearLayout.LayoutParams(0, 42, 1f).apply { leftMargin = 4 })
            bar.addView(historyButton, LinearLayout.LayoutParams(0, 42, 1f).apply { leftMargin = 4 })
            bar.addView(adBlockButton, LinearLayout.LayoutParams(0, 42, 1f).apply { leftMargin = 4 })
            pageRoot.addView(bar, LinearLayout.LayoutParams(-1, 42).apply { topMargin = 8; bottomMargin = 7 })
        } else {
            bar.addView(back, buttonParams())
            bar.addView(forward, buttonParams())
            bar.addView(full, buttonParams())
            bar.addView(historyButton, buttonParams())
            bar.addView(adBlockButton, LinearLayout.LayoutParams(90, 52).apply { leftMargin = 7 })
            pageRoot.addView(bar, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 8; bottomMargin = 7 })
        }

        val browserActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        browserActions.addView(makeButton("⌂ INICIO", "Abrir la pantalla inicial de Lemovi") { showHomePage() },
            LinearLayout.LayoutParams(0, if (mobileLayout) 40 else 44, 1f))
        browserActions.addView(makeButton("↻ RECARGAR", "Volver a cargar la página actual") {
            if (::browser.isInitialized && !browser.url.isNullOrBlank()) browser.reload() else showHomePage()
        }, LinearLayout.LayoutParams(0, if (mobileLayout) 40 else 44, 1f).apply { leftMargin = 6 })
        browserActions.addView(makeButton("★ FAVORITOS", "Abrir páginas guardadas") { showBookmarks() },
            LinearLayout.LayoutParams(0, if (mobileLayout) 40 else 44, 1f).apply { leftMargin = 6 })
        browserActions.addView(makeButton("＋ GUARDAR", "Guardar la página actual en favoritos") { saveBookmark() },
            LinearLayout.LayoutParams(0, if (mobileLayout) 40 else 44, 1f).apply { leftMargin = 6 })
        pageRoot.addView(browserActions, LinearLayout.LayoutParams(-1, if (mobileLayout) 40 else 44).apply { bottomMargin = 6 })

        searchVideosButton = makeButton("▶  BUSCAR VIDEOS", "Detectar videos disponibles en la página abierta") {
            detectVideos()
        }
        searchVideosButton.textSize = if (mobileLayout) 13f else 15f
        pageRoot.addView(searchVideosButton, LinearLayout.LayoutParams(-1, if (mobileLayout) 44 else 48).apply {
            bottomMargin = 6
        })

        videoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(25, 18, 38))
            setPadding(8, 7, 8, 7)
            visibility = View.GONE
        }
        val panelHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val panelTitle = TextView(this).apply {
            text = "VIDEOS ENCONTRADOS"
            textSize = 12f
            setTextColor(Color.rgb(218, 166, 255))
            typeface = Typeface.DEFAULT_BOLD
        }
        panelHeader.addView(panelTitle, LinearLayout.LayoutParams(0, -2, 1f))
        panelHeader.addView(makeButton("CERRAR", "Cerrar lista de videos") {
            videoPanel.visibility = View.GONE
        }, LinearLayout.LayoutParams(82, 34))
        videoPanel.addView(panelHeader)
        val resultsScroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        videoResults = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        resultsScroll.addView(videoResults)
        videoPanel.addView(resultsScroll, LinearLayout.LayoutParams(-1, if (mobileLayout) 150 else 190))
        pageRoot.addView(videoPanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 })

        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                // Algunos sitios rechazan el identificador "wv" de Android WebView.
                // Conservamos el motor y las capacidades de WebView, pero usamos un UA
                // con formato de navegador Chrome Android para mejorar compatibilidad.
                userAgentString = userAgentString
                    .replace("; wv", "")
                    .replace(" Version/4.0", "")
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
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
            CookieManager.getInstance().flush()
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                    // No crear ventanas emergentes: bloquea popunders y anuncios target=_blank.
                    return false
                    /*if (resultMsg == null || !::browserHost.isInitialized) return false
                    closeAdPopup()
                    val popup = WebView(this@MainActivity).apply {
                        setBackgroundColor(Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.loadsImagesAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = browser.settings.userAgentString
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean = false
                        }
                        webChromeClient = WebChromeClient()
                    }
                    adPopup = popup
                    browserHost.addView(popup, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    val transport = resultMsg.obj as? WebView.WebViewTransport ?: run { closeAdPopup(); return false }
                    transport.webView = popup
                    resultMsg.sendToTarget()
                    popup.postDelayed({ if (adPopup === popup) closeAdPopup() }, 3000)
                    return true*/
                }
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

                override fun onHideCustomView() { hideCustomVideo() }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (isDirectVideo(uri)) { openNativePlayer(uri.toString()); return true }
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    // Con el bloqueador apagado, no inspeccionar cada recurso:
                    // las páginas cargan decenas o cientos de scripts, imágenes y fuentes.
                    if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)
                    val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                    if (request.isForMainFrame) return super.shouldInterceptRequest(view, request)
                    val host = uri.host?.lowercase() ?: return super.shouldInterceptRequest(view, request)
                    val isAdHost = blockedAdHosts.any { host == it || host.endsWith(".$it") } ||
                        isLikelyAdResource(uri)
                    if (isAdHost) {
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                    videoPanel.visibility = View.GONE
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(this@MainActivity, "No se pudo cargar la página. Revisá el enlace o la conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        browserHost = FrameLayout(this)
        browserHost.addView(browser, FrameLayout.LayoutParams(-1, -1))
        pageRoot.addView(browserHost, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(pageRoot)

        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { openAddress(); true } else false
        }
        address.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) address.setSelection(address.text.length)
        }
        address.setText("https://")
    }

    private fun showHomePage() {
        if (!::browser.isInitialized) return
        videoPanel.visibility = View.GONE
        address.setText("")
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>*{box-sizing:border-box}body{margin:0;background:#090711;color:#fff;font-family:Arial,sans-serif;
            display:flex;min-height:100vh;align-items:center;justify-content:center;text-align:center}
            main{width:92%;max-width:900px;padding:28px}h1{font-size:clamp(34px,7vw,64px);letter-spacing:8px;
            color:#8feaff;text-shadow:0 0 18px #087cff}p{color:#c7b6d8;font-size:clamp(15px,2.4vw,22px)}
            .links{display:flex;flex-wrap:wrap;gap:14px;justify-content:center;margin-top:30px}
            a{display:block;text-decoration:none;color:white;background:#241638;border:1px solid #4e83ff;
            border-radius:12px;padding:18px 24px;font-size:18px;min-width:150px}a:focus{background:#4a2580;outline:3px solid #8feaff}</style>
            </head><body><main><h1>LEMOVI</h1><p>Tu espacio para explorar y reproducir contenido.</p>
            <div class="links"><a href="https://www.google.com">Buscar en Google</a>
            <a href="https://www.youtube.com">YouTube</a>
            <a href="https://miraculous.now/es-419/watch?s=7&amp;e=701">Miraculous</a></div>
            <p>Escribí una dirección o lo que querés buscar en la barra superior.</p></main></body></html>
        """.trimIndent()
        browser.loadDataWithBaseURL("https://lemovi.home/", html, "text/html", "UTF-8", null)
    }

    private fun saveBookmark() {
        if (!::browser.isInitialized) return
        val url = browser.url.orEmpty()
        if (url.isBlank() || url == "about:blank" || url.startsWith("https://lemovi.home/")) {
            Toast.makeText(this, "Abrí una página antes de guardarla", Toast.LENGTH_SHORT).show(); return
        }
        val prefs = getPreferences(MODE_PRIVATE)
        val list = JSONArray(prefs.getString("lemovi_bookmarks", "[]"))
        for (i in 0 until list.length()) if (list.optJSONObject(i)?.optString("url") == url) {
            Toast.makeText(this, "Esta página ya está en favoritos", Toast.LENGTH_SHORT).show(); return
        }
        list.put(JSONObject().put("title", browser.title?.takeIf { it.isNotBlank() } ?: url).put("url", url))
        prefs.edit().putString("lemovi_bookmarks", list.toString()).apply()
        Toast.makeText(this, "Página guardada en favoritos", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val list = JSONArray(getPreferences(MODE_PRIVATE).getString("lemovi_bookmarks", "[]"))
        if (list.length() == 0) { Toast.makeText(this, "Todavía no guardaste favoritos", Toast.LENGTH_SHORT).show(); return }
        val labels = ArrayList<String>(); val urls = ArrayList<String>()
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue; val url = item.optString("url")
            if (url.isNotBlank()) { urls.add(url); labels.add(item.optString("title", url)) }
        }
        if (urls.isEmpty()) return
        AlertDialog.Builder(this).setTitle("Favoritos").setItems(labels.toTypedArray()) { _, which ->
            if (which in urls.indices) { address.setText(urls[which]); openAddress() }
        }.setNegativeButton("Cerrar", null).setNeutralButton("Borrar todos") { _, _ ->
            getPreferences(MODE_PRIVATE).edit().remove("lemovi_bookmarks").apply()
            Toast.makeText(this, "Favoritos eliminados", Toast.LENGTH_SHORT).show()
        }.show()
    }
    private fun isLikelyAdResource(uri: Uri): Boolean {
        val path = uri.encodedPath?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        if (host.isBlank()) return false
        val markers = listOf("/ads/", "/adserver/", "/popunder/", "/popup/", "/prebid/", "/vast/")
        return markers.any { path.contains(it) } ||
            host.startsWith("ads.") || host.startsWith("adserver.") || host.startsWith("tracking.")
    }

    private fun closeAdPopup() {
        val popup = adPopup ?: return
        adPopup = null
        try { popup.stopLoading(); browserHost.removeView(popup); popup.loadUrl("about:blank"); popup.destroy() } catch (_: Exception) { }
    }

    private fun showHistory() {
        if (!::browser.isInitialized) return
        val history = browser.copyBackForwardList()
        if (history.size == 0) { Toast.makeText(this, "Todavía no hay páginas en el historial", Toast.LENGTH_SHORT).show(); return }
        val first = (history.currentIndex - 19).coerceAtLeast(0)
        val indices = (first..history.currentIndex).toList().reversed()
        val labels = indices.map { i -> val item = history.getItemAtIndex(i); item.title?.takeIf { it.isNotBlank() } ?: item.url }
        AlertDialog.Builder(this).setTitle("Historial de navegación").setItems(labels.toTypedArray()) { _, which ->
            val delta = indices[which] - history.currentIndex
            if (delta != 0) browser.goBackOrForward(delta)
        }.setNegativeButton("Cerrar", null).show()
    }
    private fun buttonParams() = LinearLayout.LayoutParams(88, 52).apply { leftMargin = 7 }

    private fun focusBackground(base: Int, radius: Float = 12f): StateListDrawable {
        fun shape(fill: Int, stroke: Int, width: Int): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(fill)
                setStroke(width, stroke)
            }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused),
                shape(Color.rgb(27, 72, 104), Color.rgb(82, 226, 255), 4))
            addState(intArrayOf(android.R.attr.state_pressed),
                shape(Color.rgb(55, 38, 100), Color.rgb(190, 126, 255), 3))
            addState(intArrayOf(), shape(base, Color.rgb(76, 53, 105), 1))
        }
    }

    private fun applyFocusFeedback(view: View, base: Int = Color.rgb(36, 27, 51)) {
        view.isFocusable = true
        view.isFocusableInTouchMode = false
        view.background = focusBackground(base, 12f)
        view.setOnFocusChangeListener { v, focused ->
            v.animate().scaleX(if (focused) 1.045f else 1f)
                .scaleY(if (focused) 1.045f else 1f)
                .alpha(if (focused) 1f else 0.94f)
                .setDuration(110).start()
            v.elevation = if (focused) 12f else 0f
        }
    }

    private fun makeButton(label: String, description: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (resources.configuration.screenWidthDp < 600) 10.5f else 12f
        isAllCaps = false
        setTextColor(Color.WHITE)
        contentDescription = description
        setPadding(5, 0, 5, 0)
        applyFocusFeedback(this, Color.rgb(105, 45, 150))
        setOnClickListener { action() }
    }

    private fun openAddress() {
        var url = address.text.toString().trim()
        if (url.isEmpty()) return
        val looksLikeAddress = url.contains("://") || (url.contains(".") && !url.contains(" "))
        if (!looksLikeAddress) {
            url = "https://www.google.com/search?q=" + Uri.encode(url)
        } else if (!url.contains("://")) {
            url = "https://$url"
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            Toast.makeText(this, "Dirección no válida", Toast.LENGTH_SHORT).show()
            return
        }
        address.setText(url)
        videoPanel.visibility = View.GONE
        val uri = Uri.parse(url)
        if (isDirectVideo(uri)) openNativePlayer(url) else browser.loadUrl(url)
        browser.requestFocus()
    }

    private fun isDirectVideo(uri: Uri): Boolean {
        val path = uri.path?.lowercase() ?: ""
        val query = uri.getQueryParameter("mime")?.lowercase().orEmpty()
        return listOf(".mp4", ".m3u8", ".mpd", ".webm", ".m4v", ".3gp").any { path.endsWith(it) } ||
            query.startsWith("video/") || query.contains("mpegurl") || query.contains("dash+xml")
    }

    private fun openNativePlayer(url: String) {
        val cookie = CookieManager.getInstance().getCookie(url).orEmpty()
        val referer = browser.url.orEmpty()
        startActivity(android.content.Intent(this, PlayerActivity::class.java)
            .putExtra(PlayerActivity.EXTRA_URL, url)
            .putExtra(PlayerActivity.EXTRA_USER_AGENT, browser.settings.userAgentString)
            .putExtra(PlayerActivity.EXTRA_COOKIE, cookie)
            .putExtra(PlayerActivity.EXTRA_REFERER, referer))
    }

    // La detección lee solo elementos y recursos expuestos por la página actual.
    // No atraviesa DRM, iframes de otro origen ni extrae manifiestos/segmentos protegidos.
    private fun detectVideos() {
        if (!::browser.isInitialized || browser.url.isNullOrBlank() || browser.url == "about:blank") {
            Toast.makeText(this, "Primero abrí una página", Toast.LENGTH_SHORT).show()
            return
        }
        searchVideosButton.isEnabled = false
        searchVideosButton.text = "BUSCANDO..."
        val script = """
            (function() {
              try {
                const out = [], seen = new Set();
                const title = document.title || location.hostname || 'Video';
                const add = (url, poster, duration, label, index, kind) => {
                  if (!url || seen.has(url)) return;
                  seen.add(url);
                  out.push({url:String(url), poster:poster||'', duration:Number.isFinite(duration)?duration:0,
                    title:label||title, index:index, kind:kind||'media'});
                };
                const videos = Array.from(document.querySelectorAll('video'));
                videos.forEach((v,i) => {
                  const poster = v.poster || '';
                  const duration = Number.isFinite(v.duration) ? v.duration : 0;
                  const current = v.currentSrc || v.src || '';
                  if (current) add(current, poster, duration, v.getAttribute('aria-label') || title, i, 'video');
                  Array.from(v.querySelectorAll('source')).forEach(s => add(s.src, poster, duration, title, i, 'video'));
                });
                Array.from(document.querySelectorAll('a[href]')).forEach(a => {
                  const u = a.href || '';
                  if (/\.(mp4|m3u8|mpd|webm|m4v|3gp)(?:$|[?#])/i.test(u))
                    add(u, '', 0, (a.innerText||a.title||title).trim(), -1, 'link');
                });
                try {
                  performance.getEntriesByType('resource').forEach(e => {
                    const u = e.name || '';
                    if (/\.(mp4|m3u8|mpd|webm|m4v|3gp)(?:$|[?#])/i.test(u))
                      add(u, '', 0, title, -1, 'resource');
                  });
                } catch (_) {}
                return JSON.stringify({page:title, count:out.length, items:out});
              } catch(e) {
                return JSON.stringify({page:document.title||'', count:0, items:[], error:String(e)});
              }
            })();
        """.trimIndent()
        browser.evaluateJavascript(script) { raw ->
            searchVideosButton.isEnabled = true
            searchVideosButton.text = "▶  BUSCAR VIDEOS"
            val decoded = try {
                if (raw.isNullOrBlank() || raw == "null") "" else org.json.JSONTokener(raw).nextValue() as? String ?: ""
            } catch (_: Exception) { "" }
            if (decoded.isBlank()) {
                showVideoMessage("No se pudo analizar esta página.")
                return@evaluateJavascript
            }
            try {
                val result = JSONObject(decoded)
                val items = result.optJSONArray("items") ?: JSONArray()
                videoResults.removeAllViews()
                if (items.length() == 0) {
                    showVideoMessage("No se encontraron videos detectables. Algunos sitios usan reproductores protegidos, DRM o iframes externos que no permiten inspeccionar sus fuentes.")
                } else {
                    for (i in 0 until items.length()) addVideoRow(items.getJSONObject(i))
                    videoPanel.visibility = View.VISIBLE
                    Toast.makeText(this, "Se encontraron ${items.length()} fuentes de video", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                showVideoMessage("La página no devolvió una lista de videos válida.")
            }
        }
    }

    private fun showVideoMessage(message: String) {
        videoResults.removeAllViews()
        videoResults.addView(TextView(this).apply {
            text = message
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(8, 14, 8, 14)
        })
        videoPanel.visibility = View.VISIBLE
    }

    private fun addVideoRow(item: JSONObject) {
        val url = item.optString("url")
        val poster = item.optString("poster")
        val title = item.optString("title").ifBlank { "Video disponible" }
        val duration = item.optDouble("duration", 0.0)
        val index = item.optInt("index", -1)
        val kind = item.optString("kind", "media")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(5, 6, 5, 6)
            isClickable = true
            applyFocusFeedback(this, Color.rgb(36, 27, 51))
        }
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(62, 43, 80))
            contentDescription = "Miniatura de $title"
        }
        row.addView(thumb, LinearLayout.LayoutParams(92, 58).apply { rightMargin = 9 })
        if (poster.startsWith("http")) loadThumbnail(poster, thumb)
        val textColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textColumn.addView(TextView(this).apply {
            text = title
            textSize = 13f
            maxLines = 2
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        val durationText = if (duration > 0) "Duración: ${formatDuration(duration)}" else "Duración no informada"
        textColumn.addView(TextView(this).apply {
            text = "$durationText  •  ${if (kind == "link") "Enlace directo" else "Fuente de página"}"
            textSize = 11f
            setTextColor(Color.rgb(195, 170, 215))
        })
        row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
        row.setOnClickListener {
            videoPanel.visibility = View.GONE
            when {
                url.startsWith("blob:") || url.startsWith("data:") -> playInPage(index)
                url.startsWith("http://") || url.startsWith("https://") -> openNativePlayer(url)
                else -> Toast.makeText(this, "Esta fuente no se puede abrir directamente.", Toast.LENGTH_LONG).show()
            }
        }
        videoResults.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 5 })
    }

    private fun playInPage(videoIndex: Int) {
        if (videoIndex < 0) {
            Toast.makeText(this, "Esta página no expone una fuente reproducible fuera de su reproductor.", Toast.LENGTH_LONG).show()
            return
        }
        val js = "(function(){var v=document.querySelectorAll('video')[$videoIndex];if(!v)return false;v.controls=true;v.muted=false;var p=v.play();if(p&&p.catch)p.catch(function(){});return true;})()"
        browser.evaluateJavascript(js) { result ->
            if (result != "true") Toast.makeText(this, "No se pudo iniciar el reproductor original de la página.", Toast.LENGTH_LONG).show()
            else Toast.makeText(this, "Reproducción iniciada en la página. Usá sus controles.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadThumbnail(imageUrl: String, target: ImageView) {
        val userAgent = browser.settings.userAgentString
        Thread {
            var conn: HttpURLConnection? = null
            try {
                val activeConnection = URL(imageUrl).openConnection() as HttpURLConnection
                conn = activeConnection
                activeConnection.connectTimeout = 5000
                activeConnection.readTimeout = 7000
                activeConnection.instanceFollowRedirects = true
                activeConnection.setRequestProperty("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(imageUrl)?.let { activeConnection.setRequestProperty("Cookie", it) }
                activeConnection.connect()
                val bitmap = activeConnection.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) runOnUiThread { target.setImageBitmap(bitmap) }
            } catch (_: Exception) {
                // Si el sitio bloquea la miniatura, la fila conserva el fondo de vista previa.
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun formatDuration(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
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
        } else if (videoPanel.visibility == View.VISIBLE) {
            videoPanel.visibility = View.GONE
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

    override fun onPause() {
        if (::browser.isInitialized) CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        if (::browser.isInitialized) {
            closeAdPopup()
            browser.stopLoading()
            browser.loadUrl("about:blank")
            browser.destroy()
        }
        super.onDestroy()
    }
}
