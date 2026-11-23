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
import android.webkit.JavascriptInterface
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
    private lateinit var addressRow: LinearLayout
    private lateinit var browserActions: LinearLayout
    private var catalogMode = true
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

        addressRow = LinearLayout(this).apply {
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

        browserActions = LinearLayout(this).apply {
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
                    if (!url.isNullOrBlank()) {
                        address.setText(url)
                        setCatalogMode(url.startsWith("https://v3-cinemeta.strem.io/"))
                    }
                    videoPanel.visibility = View.GONE
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(this@MainActivity, "No se pudo cargar la página. Revisá el enlace o la conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        browser.addJavascriptInterface(AddonBridge(), "LemoviNative")
        browserHost = FrameLayout(this)
        browserHost.addView(browser, FrameLayout.LayoutParams(-1, -1))
        pageRoot.addView(browserHost, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(pageRoot)
        // Al abrir Lemovi, dejar visible la primera selección para el control remoto.
        go.requestFocus()

        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { openAddress(); true } else false
        }
        address.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) address.setSelection(address.text.length)
        }
        address.setText("https://")
        showHomePage()
    }

    private fun setCatalogMode(enabled: Boolean) {
        catalogMode = enabled
        val visibility = if (enabled) View.GONE else View.VISIBLE
        if (::addressRow.isInitialized) addressRow.visibility = visibility
        if (::bar.isInitialized) bar.visibility = visibility
        if (::browserActions.isInitialized) browserActions.visibility = visibility
        if (::searchVideosButton.isInitialized) searchVideosButton.visibility = visibility
        if (enabled && ::videoPanel.isInitialized) videoPanel.visibility = View.GONE
    }

    private fun openBrowserMode() {
        setCatalogMode(false)
        if (browser.url.orEmpty().startsWith("https://v3-cinemeta.strem.io/")) {
            address.setText("https://www.google.com/")
            openAddress()
        }
    }

    private fun showHomePage() {
        if (!::browser.isInitialized) return
        setCatalogMode(true)
        videoPanel.visibility = View.GONE
        address.setText("")
        val html = """
            <!doctype html>
            <html lang="es"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
            <style>
            *{box-sizing:border-box}body{margin:0;background:#080b14;color:#f4f7ff;font-family:Arial,sans-serif}
            header{position:sticky;top:0;z-index:5;background:rgba(8,11,20,.97);border-bottom:1px solid #183b59;padding:14px 4vw}
            .brand{font-size:25px;font-weight:900;letter-spacing:5px;color:#9ceeff;text-shadow:0 0 16px #087cff}
            nav{display:flex;gap:10px;flex-wrap:wrap;margin-top:12px}
            button,input{font:inherit;color:#fff;background:#142238;border:1px solid #315b80;border-radius:10px;padding:12px 16px}
            button{cursor:pointer;font-weight:700}button:focus,input:focus,.card:focus{outline:3px solid #62eaff;outline-offset:3px;background:#17466a}
            button.active{background:#17466a;border-color:#62eaff}
            .search{display:flex;gap:8px;margin-top:12px}.search input{flex:1;min-width:100px}
            main{padding:18px 4vw 45px}.hero{padding:20px;border-radius:18px;background:linear-gradient(110deg,#102e4b,#17112d 75%);border:1px solid #285a80;margin-bottom:22px}
            h1{font-size:clamp(24px,4vw,38px);margin:0 0 8px}.muted{color:#a9b8ce}.section{margin:24px 0}
            .section h2{font-size:21px;color:#a6eaff;margin:0 0 13px}
            .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(135px,1fr));gap:14px}
            .card{background:#111a2b;border:1px solid #203b59;border-radius:12px;overflow:hidden;min-width:0;cursor:pointer}
            .card:focus{transform:scale(1.035);border-color:#62eaff}
            .card img{width:100%;aspect-ratio:2/3;object-fit:cover;background:#1c2b40;display:block}
            .card .info{padding:9px}.card b{display:block;font-size:14px;line-height:1.25}.card small{display:block;color:#9eb0c8;margin-top:5px}
            .detail{display:grid;grid-template-columns:minmax(120px,220px) 1fr;gap:20px;background:#101a2b;border:1px solid #2b4e70;border-radius:16px;padding:18px}
            .detail img{width:100%;border-radius:10px}.detail h2{margin:0 0 10px;color:#a6eaff}.episodes{display:flex;gap:8px;flex-wrap:wrap;margin-top:14px}
            .status{padding:22px;color:#c2d3e8}.hidden{display:none!important}
            @media(max-width:560px){.detail{grid-template-columns:1fr}.detail img{max-width:160px}.grid{grid-template-columns:repeat(auto-fill,minmax(112px,1fr))}}
            </style></head><body>
            <header><div class="brand">LEMOVI</div><nav>
            <button id="homeTab" class="active">INICIO</button><button id="moviesTab">PELÍCULAS</button><button id="seriesTab">SERIES</button><button id="listTab">MI LISTA</button><button id="addonsTab">COMPLEMENTOS</button><button id="browserTab">NAVEGADOR</button>
            </nav><div class="search"><input id="query" placeholder="Buscar películas o series..." aria-label="Buscar películas o series"><button id="searchBtn">BUSCAR</button></div></header>
            <main><div id="hero" class="hero"><h1>Tu cine, a tu manera.</h1><div class="muted">Explorá películas y series, descubrí información y encontrá opciones para verlas.</div></div>
            <div id="content"><div class="status">Cargando catálogo...</div></div></main>
            <script>
            const API='https://v3-cinemeta.strem.io';
            const content=document.getElementById('content'), query=document.getElementById('query');
            let currentType='movie';
            let backStack=[];
            let currentScreen='home';
            let lastDetail=null;
            window.lemoviHandleBack=()=>{const previous=backStack.pop();if(!previous)return false;previous();return true;};
            function savedItems(){try{return JSON.parse(localStorage.getItem('lemovi_my_list')||'[]');}catch(_){return [];}}
            function saveItems(items){localStorage.setItem('lemovi_my_list',JSON.stringify(items));}
            function toggleSaved(item){const items=savedItems();const index=items.findIndex(x=>x.id===item.id&&x.type===item.type);if(index>=0)items.splice(index,1);else items.push(item);saveItems(items);return index<0;}
            function myList(){backStack=[];currentScreen='list';setTabs('list');const items=savedItems();if(!items.length){content.innerHTML='<div class="status">Tu lista está vacía. Abrí una película o serie y elegí AGREGAR A MI LISTA.</div>';return;}content.innerHTML='<section class="section"><h2>Mi lista</h2><div class="grid">'+items.map(card).join('')+'</div></section>';bindCards();}
            function safe(v){return String(v||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
            function card(m){const img=m.poster||'';return '<div class="card" tabindex="0" role="button" data-id="'+safe(m.id)+'" data-type="'+safe(m.type||currentType)+'"><img loading="lazy" src="'+safe(img)+'" onerror="this.style.visibility=\'hidden\'"><div class="info"><b>'+safe(m.name)+'</b><small>'+safe(m.year||'')+(m.imdbRating?' · ★ '+safe(m.imdbRating):'')+'</small></div></div>';}
            async function catalog(type,term){backStack=[];currentScreen='catalog';currentType=type;setTabs(type);content.innerHTML='<div class="status">Buscando títulos...</div>';let path='/catalog/'+type+'/top.json';if(term)path='/catalog/'+type+'/top/search='+encodeURIComponent(term)+'.json';try{const res=await fetch(API+path);if(!res.ok)throw new Error('HTTP '+res.status);const data=await res.json();const items=data.metas||[];if(!items.length){content.innerHTML='<div class="status">No encontramos resultados. Probá con otro título.</div>';return;}content.innerHTML='<section class="section"><h2>'+(term?'Resultados para “'+safe(term)+'”':type==='movie'?'Películas populares':'Series populares')+'</h2><div class="grid">'+items.map(card).join('')+'</div></section>';bindCards();}catch(e){content.innerHTML='<div class="status">No se pudo conectar con el catálogo. Revisá tu conexión e intentá nuevamente.<br><small>'+safe(e.message)+'</small></div>';}}
            function bindCards(){content.querySelectorAll('.card').forEach(el=>{el.addEventListener('click',()=>details(el.dataset.type,el.dataset.id));el.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();details(el.dataset.type,el.dataset.id);}});});}
            async function homeCatalog(){backStack=[];currentScreen='home';setTabs('home');content.innerHTML='<div class="status">Cargando películas y series...</div>';try{const results=await Promise.all([fetch(API+'/catalog/movie/top.json').then(r=>r.json()),fetch(API+'/catalog/series/top.json').then(r=>r.json())]);const movies=results[0].metas||[],series=results[1].metas||[];content.innerHTML=(movies.length?'<section class="section"><h2>Películas populares</h2><div class="grid">'+movies.slice(0,20).map(m=>{m.type='movie';return card(m);}).join('')+'</div></section>':'')+(series.length?'<section class="section"><h2>Series populares</h2><div class="grid">'+series.slice(0,20).map(m=>{m.type='series';return card(m);}).join('')+'</div></section>':'');if(!movies.length&&!series.length)content.innerHTML='<div class="status">El catálogo no devolvió títulos.</div>';bindCards();}catch(e){content.innerHTML='<div class="status">No se pudo cargar el catálogo. Revisá tu conexión e intentá nuevamente.</div>';}}
            async function searchAll(term){backStack=[];currentScreen='search';content.innerHTML='<div class="status">Buscando en películas y series...</div>';try{const results=await Promise.all([fetch(API+'/catalog/movie/top/search='+encodeURIComponent(term)+'.json').then(r=>r.json()),fetch(API+'/catalog/series/top/search='+encodeURIComponent(term)+'.json').then(r=>r.json())]);const movies=results[0].metas||[],series=results[1].metas||[];movies.forEach(m=>m.type='movie');series.forEach(m=>m.type='series');content.innerHTML=(movies.length?'<section class="section"><h2>Películas</h2><div class="grid">'+movies.map(card).join('')+'</div></section>':'')+(series.length?'<section class="section"><h2>Series</h2><div class="grid">'+series.map(card).join('')+'</div></section>':'');if(!movies.length&&!series.length)content.innerHTML='<div class="status">No encontramos resultados para “'+safe(term)+'”.</div>';bindCards();}catch(e){content.innerHTML='<div class="status">No se pudo completar la búsqueda.</div>';}}
            function setTabs(type){document.getElementById('homeTab').classList.toggle('active',type==='home');document.getElementById('moviesTab').classList.toggle('active',type==='movie');document.getElementById('seriesTab').classList.toggle('active',type==='series');document.getElementById('listTab').classList.toggle('active',type==='list');}
            async function details(type,id){lastDetail={type:type,id:id};const previousScreen=currentScreen,previousType=currentType,previousQuery=query.value; if(currentScreen!=='details'&&currentScreen!=='streams')backStack.push(()=>{if(previousScreen==='search')searchAll(previousQuery);else if(previousScreen==='catalog')catalog(previousType,'');else if(previousScreen==='list')myList();else homeCatalog();});currentScreen='details';content.innerHTML='<div class="status">Cargando ficha...</div>';try{const res=await fetch(API+'/meta/'+type+'/'+encodeURIComponent(id)+'.json');if(!res.ok)throw new Error('No disponible');const data=await res.json(),m=data.meta;if(!m)throw new Error('Sin ficha');const saved=savedItems().some(x=>x.id===m.id&&x.type===type);let html='<section class="detail"><img src="'+safe(m.poster)+'"><div><h2>'+safe(m.name)+'</h2><p class="muted">'+safe(m.year||'')+' · '+safe(m.genres?m.genres.join(' / '):'')+' · '+(m.imdbRating?'★ '+safe(m.imdbRating):'')+'</p><p>'+safe(m.description||'Sin descripción disponible.')+'</p><button id="streamsBtn">BUSCAR FUENTES</button> <button id="saveListBtn">'+(saved?'QUITAR DE MI LISTA':'AGREGAR A MI LISTA')+'</button> <button id="backBtn">VOLVER AL CATÁLOGO</button>';if(type==='series'&&m.videos&&m.videos.length){html+='<h3>Temporadas y episodios</h3><div class="episodes">'+m.videos.map(v=>'<button class="episode" data-episode="'+safe(v.id)+'">'+('T'+(v.season||0)+' · E'+(v.episode||0)+' — '+safe(v.title||'Episodio'))+'</button>').join('')+'</div>';}html+='</div></section>';content.innerHTML=html;document.getElementById('backBtn').onclick=()=>window.lemoviHandleBack()||homeCatalog();document.getElementById('saveListBtn').onclick=()=>{const added=toggleSaved({id:m.id,type:type,name:m.name,poster:m.poster,year:m.year,imdbRating:m.imdbRating});document.getElementById('saveListBtn').textContent=added?'QUITAR DE MI LISTA':'AGREGAR A MI LISTA';};document.getElementById('streamsBtn').onclick=()=>findStreams(type,id);content.querySelectorAll('.episode').forEach(b=>b.onclick=()=>findStreams('series',b.dataset.episode));}catch(e){content.innerHTML='<div class="status">No se pudo cargar la ficha. <button id="retry">VOLVER</button></div>';document.getElementById('retry').onclick=()=>catalog(type,'');}}
            function addons(){const prev=currentScreen,prevType=currentType,prevQuery=query.value,prevDetail=lastDetail;if(currentScreen!=='addons')backStack.push(()=>{if(prev==='details'&&prevDetail)details(prevDetail.type,prevDetail.id);else if(prev==='streams'&&prevDetail)details(prevDetail.type,prevDetail.id);else if(prev==='search')searchAll(prevQuery);else if(prev==='catalog')catalog(prevType,'');else if(prev==='list')myList();else homeCatalog();});currentScreen='addons';const saved=JSON.parse(LemoviNative.getAddonList()||'[]');content.innerHTML='<section class="section"><h2>Complementos de reproducción</h2><p class="muted">WatchHub ya viene integrado para consultar servicios oficiales disponibles en tu región. También podés agregar complementos compatibles con el protocolo Stremio para otras fuentes.</p><div class="search"><input id="addonUrl" placeholder="https://servidor/manifest.json"><button id="addAddonBtn">AGREGAR</button></div><div id="addonStatus" class="status"></div><div id="addonList"></div></section>';renderAddons(saved);document.getElementById('addAddonBtn').onclick=()=>{const u=document.getElementById('addonUrl').value.trim();const status=document.getElementById('addonStatus');status.textContent='Verificando complemento...';try{const result=JSON.parse(LemoviNative.addAddon(u));if(result.error)throw new Error(result.error);status.textContent='Complemento agregado: '+(result.name||result.id);renderAddons(JSON.parse(LemoviNative.getAddonList()||'[]'));document.getElementById('addonUrl').value='';}catch(e){status.textContent='No se pudo agregar: '+e.message;}};}
function renderAddons(items){const box=document.getElementById('addonList');if(!box)return;box.innerHTML=items.length?items.map((u,i)=>'<div class="hero"><b>'+(u.includes('watchhub.strem.io')?'WatchHub · servicios oficiales':'Complemento '+(i+1))+'</b><div class="muted">'+safe(u)+'</div><button data-remove="'+i+'">QUITAR</button></div>').join(''):'<p class="muted">Todavía no agregaste complementos.</p>';box.querySelectorAll('[data-remove]').forEach(b=>b.onclick=()=>{LemoviNative.removeAddon(items[Number(b.dataset.remove)]);renderAddons(JSON.parse(LemoviNative.getAddonList()||'[]'));});}
function findStreams(type,id){if(currentScreen!=='streams'){const prev=currentScreen,prevType=currentType,prevQuery=query.value;backStack.push(()=>{if(prev==='details')details(type,id);else if(prev==='search')searchAll(prevQuery);else if(prev==='catalog')catalog(prevType,'');else homeCatalog();});}currentScreen='streams';const configured=JSON.parse(LemoviNative.getAddonList()||'[]');if(!configured.length){content.innerHTML='<div class="status">Primero agregá un complemento compatible desde COMPLEMENTOS.<br><button id="goAddons">CONFIGURAR COMPLEMENTOS</button></div>';document.getElementById('goAddons').onclick=addons;return;}content.innerHTML='<div class="status">Consultando fuentes de reproducción...</div>';const all=[];for(const url of configured){try{const data=JSON.parse(LemoviNative.getStreams(url,type,id));if(data.error){all.push({name:'Error de complemento',title:data.error,_error:true});continue;}(data.streams||[]).forEach(st=>all.push({...st,_addon:url}));}catch(e){all.push({name:'Error de complemento',title:String(e.message||e),_error:true});}}if(!all.length){content.innerHTML='<div class="status">Los complementos no devolvieron fuentes para este título.</div>';return;}content.innerHTML='<section class="section"><h2>Fuentes disponibles</h2>'+all.map((st,i)=>{const action=st.url&&/^https?:/i.test(st.url)?'<button data-play="'+i+'">REPRODUCIR</button>':st.externalUrl?'<button data-external="'+i+'">ABRIR SERVICIO</button>':st.ytId?'<button data-youtube="'+i+'">VER EN YOUTUBE</button>':st.infoHash||String(st.url||'').startsWith('magnet:')?'<span class="muted">Fuente torrent: motor P2P todavía no integrado.</span>':'<span class="muted">Formato no compatible con el reproductor actual.</span>';return '<div class="hero"><b>'+safe(st.name||st.title||'Fuente '+(i+1))+'</b><p class="muted">'+safe(st.description||st.behaviorHints?.filename||st._addon||'')+'</p>'+(st._error?'<small>'+safe(st.title)+'</small>':action)+'</div>';}).join('')+'</section>';content.querySelectorAll('[data-play]').forEach(b=>b.onclick=()=>{const st=all[Number(b.dataset.play)];if(st&&st.url){const headers=st.behaviorHints&&st.behaviorHints.proxyHeaders&&st.behaviorHints.proxyHeaders.request?JSON.stringify(st.behaviorHints.proxyHeaders.request):'{}';LemoviNative.playStream(st.url,st.name||'Lemovi',st._addon,headers,JSON.stringify(st.subtitles||[]));}});content.querySelectorAll('[data-external]').forEach(b=>b.onclick=()=>{const st=all[Number(b.dataset.external)];if(st&&st.externalUrl)LemoviNative.openExternal(st.externalUrl);});content.querySelectorAll('[data-youtube]').forEach(b=>b.onclick=()=>{const st=all[Number(b.dataset.youtube)];if(st&&st.ytId)LemoviNative.openExternal('https://www.youtube.com/watch?v='+encodeURIComponent(st.ytId));});}
            document.getElementById('homeTab').onclick=()=>homeCatalog();document.getElementById('moviesTab').onclick=()=>catalog('movie','');document.getElementById('seriesTab').onclick=()=>catalog('series','');document.getElementById('listTab').onclick=()=>myList();document.getElementById('addonsTab').onclick=()=>addons();document.getElementById('browserTab').onclick=()=>LemoviNative.openBrowser();
            document.getElementById('searchBtn').onclick=()=>{const q=query.value.trim();if(q)searchAll(q);else homeCatalog();};
            query.addEventListener('keydown',e=>{if(e.key==='Enter')document.getElementById('searchBtn').click();});
            homeCatalog();
            </script></body></html>
        """.trimIndent()
        browser.loadDataWithBaseURL("https://v3-cinemeta.strem.io/", html, "text/html", "UTF-8", null)
        browser.postDelayed({
            browser.evaluateJavascript("(function(){var e=document.getElementById('homeTab');if(e)e.focus();})()", null)
            browser.requestFocus()
        }, 350)
    }


    inner class AddonBridge {
        private fun trustedPage(): Boolean =
            ::browser.isInitialized && browser.url.orEmpty().startsWith("https://v3-cinemeta.strem.io/")

        private fun validateHttps(raw: String): URL {
            val url = URL(raw.trim())
            require(url.protocol.equals("https", true)) { "Los complementos deben usar HTTPS." }
            require(url.host.isNotBlank() && !url.host.contains("@")) { "Dirección no válida." }
            val host = url.host.lowercase()
            require(host != "localhost" && !host.endsWith(".local")) { "Host local no permitido." }
            val address = try { java.net.InetAddress.getByName(host) } catch (_: Exception) { null }
            if (address != null) require(!address.isAnyLocalAddress && !address.isLoopbackAddress &&
                !address.isLinkLocalAddress && !address.isSiteLocalAddress) { "Dirección privada no permitida." }
            return url
        }

        private fun validateMediaUrl(raw: String): URL {
            val url = URL(raw.trim())
            require(url.protocol.equals("https", true) || url.protocol.equals("http", true)) {
                "La fuente debe ser una URL HTTP o HTTPS."
            }
            require(url.host.isNotBlank() && !url.host.contains("@")) { "Fuente no válida." }
            return url
        }

        private fun requestJson(raw: String): JSONObject {
            var current = validateHttps(raw)
            repeat(5) { hop ->
                val connection = (current.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Lemovi/1.3")
                }
                try {
                    val code = connection.responseCode
                    if (code in 301..308) {
                        val location = connection.getHeaderField("Location")
                            ?: throw IllegalArgumentException("Redirección sin destino.")
                        require(hop < 4) { "Demasiadas redirecciones." }
                        current = validateHttps(URL(current, location).toString())
                    } else {
                        require(code in 200..299) { "El servidor respondió HTTP $code." }
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        require(body.length <= 2_000_000) { "Respuesta demasiado grande." }
                        return JSONObject(body)
                    }
                } finally { connection.disconnect() }
            }
            throw IllegalArgumentException("No se pudo obtener el manifiesto.")
        }

        private fun supportsStream(manifest: JSONObject, type: String, id: String): Boolean {
            val resources = manifest.optJSONArray("resources") ?: return false
            var streamResource: JSONObject? = null
            for (i in 0 until resources.length()) {
                when (val item = resources.opt(i)) {
                    is String -> if (item == "stream") streamResource = JSONObject().put("name", "stream")
                    is JSONObject -> if (item.optString("name") == "stream") streamResource = item
                }
            }
            val resource = streamResource ?: return false
            val types = resource.optJSONArray("types") ?: manifest.optJSONArray("types")
            if (types != null && (0 until types.length()).none { types.optString(it) == type }) return false
            val prefixes = resource.optJSONArray("idPrefixes") ?: manifest.optJSONArray("idPrefixes")
            if (prefixes != null && prefixes.length() > 0 &&
                (0 until prefixes.length()).none { id.startsWith(prefixes.optString(it)) }) return false
            return true
        }

        @JavascriptInterface fun getAddonList(): String {
            if (!trustedPage()) return "[]"
            val prefs = getPreferences(MODE_PRIVATE)
            val list = JSONArray(prefs.getString("lemovi_addons", "[]"))
            val officialWatchHub = "https://watchhub.strem.io/manifest.json"
            if ((0 until list.length()).none { list.optString(it) == officialWatchHub }) {
                list.put(officialWatchHub)
                prefs.edit().putString("lemovi_addons", list.toString()).apply()
            }
            return list.toString()
        }

        @JavascriptInterface fun addAddon(raw: String): String {
            if (!trustedPage()) return JSONObject().put("error", "Pantalla no autorizada").toString()
            return try {
                var manifestUrl = raw.trim()
                require(manifestUrl.isNotBlank()) { "Ingresá la URL del manifiesto." }
                if (!manifestUrl.endsWith("/manifest.json")) manifestUrl = manifestUrl.trimEnd('/') + "/manifest.json"
                validateHttps(manifestUrl)
                val manifest = requestJson(manifestUrl)
                require(manifest.optString("id").isNotBlank() && manifest.optString("name").isNotBlank() &&
                    manifest.optString("version").isNotBlank() && manifest.optJSONArray("types") != null) {
                    "El manifiesto no contiene los campos obligatorios."
                }
                val resources = manifest.optJSONArray("resources") ?: JSONArray()
                require((0 until resources.length()).any { resources.opt(it) == "stream" ||
                    resources.optJSONObject(it)?.optString("name") == "stream" }) {
                    "El complemento no declara el recurso stream."
                }
                val list = JSONArray(getPreferences(MODE_PRIVATE).getString("lemovi_addons", "[]"))
                for (i in 0 until list.length()) require(list.optString(i) != manifestUrl) { "Ese complemento ya está agregado." }
                list.put(manifestUrl)
                getPreferences(MODE_PRIVATE).edit().putString("lemovi_addons", list.toString()).apply()
                manifest.put("manifestUrl", manifestUrl)
                manifest
            } catch (e: Exception) { JSONObject().put("error", e.message ?: "No se pudo verificar el complemento.") }.toString()
        }

        @JavascriptInterface fun removeAddon(raw: String) {
            if (!trustedPage()) return
            val old = JSONArray(getPreferences(MODE_PRIVATE).getString("lemovi_addons", "[]"))
            val next = JSONArray()
            for (i in 0 until old.length()) if (old.optString(i) != raw) next.put(old.optString(i))
            getPreferences(MODE_PRIVATE).edit().putString("lemovi_addons", next.toString()).apply()
        }

        @JavascriptInterface fun getStreams(manifestUrl: String, type: String, id: String): String {
            if (!trustedPage()) return JSONObject().put("error", "Pantalla no autorizada").toString()
            return try {
                require(type == "movie" || type == "series") { "Tipo de contenido no válido." }
                val saved = JSONArray(getPreferences(MODE_PRIVATE).getString("lemovi_addons", "[]"))
                var registered = false
                for (i in 0 until saved.length()) if (saved.optString(i) == manifestUrl) registered = true
                require(registered) { "Complemento no registrado." }
                val manifest = validateHttps(manifestUrl)
                val manifestData = requestJson(manifestUrl)
                require(supportsStream(manifestData, type, id)) {
                    "Este complemento no admite ${if (type == "movie") "películas" else "este episodio"} o el identificador del título."
                }
                val basePath = manifest.path.substringBeforeLast("/")
                val encodedId = Uri.encode(id, "-_.!~*'()")
                val route = "$basePath/stream/$type/$encodedId.json"
                val endpoint = URL(manifest.protocol, manifest.host, manifest.port, route)
                val requestUrl = endpoint.toString() + (manifest.query?.let { "?$it" } ?: "")
                requestJson(requestUrl).toString()
            } catch (e: Exception) { JSONObject().put("error", e.message ?: "Error consultando fuentes.").toString() }
        }

        @JavascriptInterface fun openBrowser() {
            if (!trustedPage()) return
            runOnUiThread { openBrowserMode() }
        }

        @JavascriptInterface fun openExternal(raw: String) {
            if (!trustedPage()) return
            try {
                val url = validateMediaUrl(raw)
                runOnUiThread { setCatalogMode(false); browser.loadUrl(url.toString()) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, e.message ?: "Enlace externo no válido", Toast.LENGTH_LONG).show() }
            }
        }

        @JavascriptInterface fun playStream(raw: String, label: String, addonUrl: String, headersRaw: String, subtitlesRaw: String) {
            if (!trustedPage()) return
            try {
                val media = validateMediaUrl(raw)
                val addon = validateHttps(addonUrl)
                val origin = "\${addon.protocol}://\${addon.host}" + if (addon.port != -1) ":\${addon.port}" else ""
                val headers = try { JSONObject(headersRaw).toString() } catch (_: Exception) { "{}" }
                runOnUiThread { openNativePlayer(media.toString(), origin, headers, subtitlesRaw) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, e.message ?: "Fuente no válida", Toast.LENGTH_LONG).show() }
            }
        }
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

    private fun openNativePlayer(url: String, refererOverride: String? = null, headersJson: String? = null, subtitlesJson: String? = null) {
        val cookie = CookieManager.getInstance().getCookie(url).orEmpty()
        val referer = refererOverride?.takeIf { it.isNotBlank() } ?: browser.url.orEmpty()
        startActivity(android.content.Intent(this, PlayerActivity::class.java)
            .putExtra(PlayerActivity.EXTRA_URL, url)
            .putExtra(PlayerActivity.EXTRA_USER_AGENT, browser.settings.userAgentString)
            .putExtra(PlayerActivity.EXTRA_COOKIE, cookie)
            .putExtra(PlayerActivity.EXTRA_REFERER, referer)
            .putExtra(PlayerActivity.EXTRA_HEADERS, headersJson.orEmpty())
            .putExtra(PlayerActivity.EXTRA_SUBTITLES, subtitlesJson.orEmpty()))
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
        } else if (::browser.isInitialized && browser.url.orEmpty().startsWith("https://v3-cinemeta.strem.io/")) {
            browser.evaluateJavascript("(window.lemoviHandleBack ? window.lemoviHandleBack() : false)") { handled ->
                if (handled != "true") {
                    if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
                }
            }
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
