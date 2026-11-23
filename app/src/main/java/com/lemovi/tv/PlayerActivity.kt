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
        @Volatile var activeInstance: PlayerActivity? = null
        const val EXTRA_URL = "lemovi.video_url"
        const val EXTRA_USER_AGENT = "lemovi.user_agent"
        const val EXTRA_COOKIE = "lemovi.cookie"
        const val EXTRA_REFERER = "lemovi.referer"
        const val EXTRA_HEADERS = "lemovi.headers"
        const val EXTRA_SUBTITLES = "lemovi.subtitles"
        const val EXTRA_MIME_TYPE = "lemovi.mime_type"
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
        val forbiddenHeaders = setOf(
            "host", "content-length", "connection", "transfer-encoding", "range",
            "accept-encoding", "proxy-authorization", "proxy-connection",
            "keep-alive", "te", "trailer", "upgrade"
        )
        val headerNamePattern = Regex("^[A-Za-z0-9!#$%&'*+.^_|~-]{1,128}$")
        addonHeaders.keys().forEach { key ->
            val value = addonHeaders.optString(key)
            if (headerNamePattern.matches(key) && key.lowercase() !in forbiddenHeaders &&
                value.isNotBlank() && value.length <= 8192 &&
                !value.contains('\r') && !value.contains('\n')) {
                requestHeaders[key] = value
            }
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

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1_000, 2_500)
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
                        Toast.makeText(this@PlayerActivity, playbackErrorMessage(error), Toast.LENGTH_LONG).show()
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
                                .setMimeType(mime).setLanguage(language).setLabel(label)
                                .setSelectionFlags(0).build()
                        )
                    } catch (_: Exception) { }
                }
                val mimeHint = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty()
                val resolvedMime = inferMimeType(url, mimeHint)
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setSubtitleConfigurations(subtitleConfigurations)
                    .apply {
                        when (resolvedMime) {
                            MimeTypes.APPLICATION_M3U8 -> setMimeType(MimeTypes.APPLICATION_M3U8)
                            MimeTypes.APPLICATION_MPD -> setMimeType(MimeTypes.APPLICATION_MPD)
                            else -> if (!resolvedMime.isNullOrBlank()) setMimeType(resolvedMime)
                        }
                    }.build()
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun inferMimeType(url: String, hint: String): String? {
        val supplied = hint.trim().lowercase()
        if (supplied.contains("/") && !supplied.contains("\\r") && !supplied.contains("\\n")) {
            return when {
                supplied.contains("mpegurl") || supplied == "application/vnd.apple.mpegurl" -> MimeTypes.APPLICATION_M3U8
                supplied.contains("dash+xml") -> MimeTypes.APPLICATION_MPD
                else -> supplied
            }
        }
        val path = try { android.net.Uri.parse(url).lastPathSegment.orEmpty().lowercase() } catch (_: Exception) { url.lowercase() }
        val candidate = (supplied.ifBlank { path }).substringBefore("?").substringAfterLast(".")
        return when (candidate) {
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            "mpd" -> MimeTypes.APPLICATION_MPD
            "mp4", "m4v" -> MimeTypes.VIDEO_MP4
            "webm" -> MimeTypes.VIDEO_WEBM
            "mkv" -> "video/x-matroska"
            "ts", "m2ts" -> MimeTypes.APPLICATION_MPEG2TS
            "ogv", "ogg" -> "video/ogg"
            "mov" -> "video/quicktime"
            "mp3" -> MimeTypes.AUDIO_MPEG
            "m4a" -> MimeTypes.AUDIO_MP4
            "aac" -> MimeTypes.AUDIO_AAC
            else -> null
        }
    }

    fun handleRemoteCommand(command: String) {
        val exo = player ?: return
        when (command) {
            "UP" -> sendPlayerKey(android.view.KeyEvent.KEYCODE_DPAD_UP)
            "DOWN" -> sendPlayerKey(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
            "LEFT" -> sendPlayerKey(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
            "RIGHT" -> sendPlayerKey(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
            "OK" -> sendPlayerKey(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
            "BACK" -> finish()
            "PLAY_PAUSE" -> if (exo.isPlaying) exo.pause() else exo.play()
            "REWIND" -> exo.seekTo((exo.currentPosition - 10000L).coerceAtLeast(0L))
            "FORWARD" -> exo.seekTo((exo.currentPosition + 10000L).coerceAtMost(exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
            "VOL_UP" -> (getSystemService(AUDIO_SERVICE) as android.media.AudioManager)
                .adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
            "VOL_DOWN" -> (getSystemService(AUDIO_SERVICE) as android.media.AudioManager)
                .adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
            "MUTE" -> (getSystemService(AUDIO_SERVICE) as android.media.AudioManager)
                .adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun sendPlayerKey(keyCode: Int) {
        playerView.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
        playerView.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
    }

    override fun onStart() {
        super.onStart()
        activeInstance = this
    }

    private fun playbackErrorMessage(error: PlaybackException): String {
        val detail = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No hay conexión con el servidor de video."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "El servidor tardó demasiado en responder."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "El servidor rechazó la solicitud HTTP."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "El contenedor del archivo no es compatible."
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "El manifiesto HLS/DASH está dañado o no es válido."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "El dispositivo no admite el códec de audio o video de esta fuente."
            else -> error.cause?.message ?: error.message ?: "Error desconocido."
        }
        return "No se pudo reproducir: $detail"
    }

    override fun onStop() {
        if (activeInstance === this) activeInstance = null
        if (::playerView.isInitialized) playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    override fun onBackPressed() {
        finish()
    }
}