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

        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
        val cookie = intent.getStringExtra(EXTRA_COOKIE).orEmpty()
        val referer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
        val requestHeaders = mutableMapOf<String, String>()
        if (userAgent.isNotBlank()) requestHeaders["User-Agent"] = userAgent
        if (cookie.isNotBlank()) requestHeaders["Cookie"] = cookie
        if (referer.isNotBlank()) {
            requestHeaders["Referer"] = referer
            try {
                val origin = android.net.Uri.parse(referer).let { "${it.scheme}://${it.host}" }
                if (origin != "null://null") requestHeaders["Origin"] = origin
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
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
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
