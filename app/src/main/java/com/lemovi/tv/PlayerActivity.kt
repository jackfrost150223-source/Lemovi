package com.lemovi.tv

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {
    companion object {
        const val EXTRA_URL = "lemovi.video_url"
        const val EXTRA_USER_AGENT = "lemovi.user_agent"
        const val EXTRA_COOKIE = "lemovi.cookie"
        const val EXTRA_REFERER = "lemovi.referer"
        const val EXTRA_HEADERS = "lemovi.headers"
        const val EXTRA_SUBTITLES = "lemovi.subtitles"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "No se recibió un enlace de video", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setBackgroundColor(android.graphics.Color.BLACK)
            setKeepContentOnPlayerReset(true)
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(playerView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        })
        // Los controles del reproductor también muestran claramente el foco del mando.
        playerView.post {
            val controls = intArrayOf(
                androidx.media3.ui.R.id.exo_play,
                androidx.media3.ui.R.id.exo_pause,
                androidx.media3.ui.R.id.exo_rew,
                androidx.media3.ui.R.id.exo_ffwd,
                androidx.media3.ui.R.id.exo_prev,
                androidx.media3.ui.R.id.exo_next,
                androidx.media3.ui.R.id.exo_settings,
                androidx.media3.ui.R.id.exo_subtitle
            )
            controls.forEach { id ->
                playerView.findViewById<View>(id)?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setOnFocusChangeListener { v, focused ->
                        v.setBackgroundColor(if (focused) android.graphics.Color.rgb(18, 75, 105) else android.graphics.Color.TRANSPARENT)
                        v.elevation = if (focused) 10f else 0f
                        v.scaleX = if (focused) 1.12f else 1f
                        v.scaleY = if (focused) 1.12f else 1f
                    }
                }
            }
        }

        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
        val cookie = intent.getStringExtra(EXTRA_COOKIE).orEmpty()
        val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
        val requestHeaders = mutableMapOf<String, String>()
        val addonHeaders = try { org.json.JSONObject(intent.getStringExtra(EXTRA_HEADERS).orEmpty()) } catch (_: Exception) { org.json.JSONObject() }
        val safeHeaderNames = setOf("user-agent", "referer", "origin", "cookie", "authorization", "accept", "accept-language")
        addonHeaders.keys().forEach { key ->
            val normalized = when (key.lowercase()) {
                "user-agent" -> "User-Agent"
                "referer" -> "Referer"
                "origin" -> "Origin"
                "cookie" -> "Cookie"
                "authorization" -> "Authorization"
                "accept" -> "Accept"
                "accept-language" -> "Accept-Language"
                else -> ""
            }
            val value = addonHeaders.optString(key)
            if (normalized.isNotBlank() && value.isNotBlank() && value.length <= 4096) requestHeaders[normalized] = value
        }
        if (userAgent.isNotBlank() && "User-Agent" !in requestHeaders) requestHeaders["User-Agent"] = userAgent
        if (cookie.isNotBlank() && "Cookie" !in requestHeaders) requestHeaders["Cookie"] = cookie
        if (referer.isNotBlank() && "Referer" !in requestHeaders) {
            requestHeaders["Referer"] = referer
            try {
                val origin = android.net.Uri.parse(referer).let { "${it.scheme}://${it.host}" }
                if (origin != "null://null" && "Origin" !in requestHeaders) requestHeaders["Origin"] = origin
            } catch (_: Exception) { }
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setDefaultRequestProperties(requestHeaders)

        // Inicio rápido sin vaciar el búfer ante pequeñas variaciones de red.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // búfer mínimo
                50_000, // máximo
                1_000,  // empezar cuando haya 1 segundo
                2_500   // reanudar tras interrupción con 2,5 segundos
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpFactory))
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
            .also { exo ->
                playerView.player = exo
                exo.setHandleAudioBecomingNoisy(true)
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            this@PlayerActivity,
                            "No se pudo reproducir esta fuente. Puede requerir el reproductor original o un formato compatible.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
                val subtitleConfigurations = mutableListOf<MediaItem.SubtitleConfiguration>()
                val subtitleArray = try {
                    org.json.JSONArray(intent.getStringExtra(EXTRA_SUBTITLES).orEmpty())
                } catch (_: Exception) { org.json.JSONArray() }
                for (i in 0 until subtitleArray.length()) {
                    val item = subtitleArray.optJSONObject(i) ?: continue
                    val subtitleUrl = item.optString("url")
                    if (!subtitleUrl.startsWith("https://") && !subtitleUrl.startsWith("http://")) continue
                    val lowerSubtitle = subtitleUrl.lowercase()
                    val mime = item.optString("mimeType").ifBlank {
                        when {
                            lowerSubtitle.contains(".vtt") -> MimeTypes.TEXT_VTT
                            lowerSubtitle.contains(".srt") -> MimeTypes.APPLICATION_SUBRIP
                            else -> MimeTypes.TEXT_VTT
                        }
                    }
                    val language = item.optString("lang").ifBlank { item.optString("language") }.ifBlank { "und" }
                    val label = item.optString("label").ifBlank { language }
                    try {
                        subtitleConfigurations.add(
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                                .setMimeType(mime)
                                .setLanguage(language)
                                .setLabel(label)
                                .setSelectionFlags(0)
                                .build()
                        )
                    } catch (_: Exception) { }
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setSubtitleConfigurations(subtitleConfigurations)
                    .apply {
                        val lower = url.lowercase()
                        when {
                            lower.contains(".m3u8") || lower.contains("mpegurl") -> setMimeType(MimeTypes.APPLICATION_M3U8)
                            lower.contains(".mpd") || lower.contains("dash+xml") -> setMimeType(MimeTypes.APPLICATION_MPD)
                        }
                    }
                    .build()
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    override fun onStop() {
        if (::playerView.isInitialized) playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    override fun onBackPressed() {
        finish()
    }
}
