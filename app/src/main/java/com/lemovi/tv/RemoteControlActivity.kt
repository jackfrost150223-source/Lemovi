package com.lemovi.tv

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RemoteControlActivity : Activity() {
    private lateinit var ipInput: EditText
    private lateinit var pinInput: EditText
    private lateinit var status: TextView
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("lemovi_remote", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22, 22, 22, 18)
            setBackgroundColor(Color.rgb(9, 12, 25))
        }
        root.addView(TextView(this).apply {
            text = "LEMOVI REMOTE"
            textSize = 25f
            setTextColor(Color.rgb(116, 225, 255))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(-1, 48))
        root.addView(TextView(this).apply {
            text = "Conectá el celular y la TV a la misma red Wi-Fi."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 42))
        ipInput = EditText(this).apply {
            hint = "IP de Lemovi TV (ej. 192.168.1.50)"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.rgb(30, 39, 60))
            setText(prefs.getString("ip", ""))
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        root.addView(ipInput, LinearLayout.LayoutParams(-1, 50).apply { topMargin = 10 })
        pinInput = EditText(this).apply {
            hint = "Código de 6 dígitos mostrado en la TV"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.rgb(30, 39, 60))
            setText(prefs.getString("pin", ""))
        }
        root.addView(pinInput, LinearLayout.LayoutParams(-1, 50).apply { topMargin = 8 })
        status = TextView(this).apply {
            text = "Sin conectar"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 34).apply { topMargin = 4 })
        root.addView(makeButton("CONECTAR A LA TV") { connect() }, LinearLayout.LayoutParams(-1, 48).apply { topMargin = 4 })

        val dpad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        dpad.addView(keyRow(null, "▲" to "UP", null), rowParams())
        dpad.addView(keyRow("◀" to "LEFT", "OK" to "OK", "▶" to "RIGHT"), rowParams())
        dpad.addView(keyRow(null, "▼" to "DOWN", null), rowParams())
        root.addView(dpad, LinearLayout.LayoutParams(-1, 178).apply { topMargin = 8 })
        root.addView(keyRow("ATRÁS" to "BACK", "⏯" to "PLAY_PAUSE", "AVANZAR" to "FORWARD"), rowParams())
        root.addView(keyRow("⏪" to "REWIND", "VOL −" to "VOL_DOWN", "VOL +" to "VOL_UP"), rowParams())
        root.addView(keyRow("MUTE" to "MUTE", "SALIR" to "BACK", null), rowParams())
        root.addView(makeButton("CERRAR CONTROL") { finish() }, LinearLayout.LayoutParams(-1, 42).apply { topMargin = 12 })
        setContentView(root)
    }

    private fun rowParams() = LinearLayout.LayoutParams(-1, 48).apply { topMargin = 5 }

    private fun keyRow(a: Pair<String, String>?, b: Pair<String, String>?, c: Pair<String, String>?) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            listOf(a, b, c).forEach { item ->
                val button = if (item == null) View(this@RemoteControlActivity) else makeButton(item.first) { send(item.second) }
                addView(button, LinearLayout.LayoutParams(0, 46, 1f).apply { marginStart = 4; marginEnd = 4 })
            }
        }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(22, 76, 105))
        setOnClickListener { action() }
    }

    private fun connect() {
        val ip = ipInput.text.toString().trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
        val pin = pinInput.text.toString().trim()
        if (ip.isBlank() || pin.length != 6) {
            status.text = "Ingresá la IP y el código de 6 dígitos."
            return
        }
        getSharedPreferences("lemovi_remote", MODE_PRIVATE).edit().putString("ip", ip).putString("pin", pin).apply()
        sendRequest(ip, pin, "PING") { ok ->
            connected = ok
            status.text = if (ok) "CONECTADO A LEMOVI TV" else "No se pudo conectar. Revisá IP, código y Wi-Fi."
            status.setTextColor(if (ok) Color.rgb(80, 230, 170) else Color.rgb(255, 130, 130))
        }
    }

    private fun send(command: String) {
        if (!connected) {
            status.text = "Primero conectate a la TV."
            return
        }
        sendRequest(ipInput.text.toString().trim().removePrefix("http://").removePrefix("https://").substringBefore("/"),
            pinInput.text.toString().trim(), command) { ok ->
            if (!ok) {
                connected = false
                status.text = "Se perdió la conexión. Volvé a conectar."
            }
        }
    }

    private fun sendRequest(ip: String, pin: String, command: String, result: (Boolean) -> Unit) {
        Thread {
            var ok = false
            try {
                val url = URL("http://" + ip + ":8765/key?pin=" + URLEncoder.encode(pin, "UTF-8") +
                    "&key=" + URLEncoder.encode(command, "UTF-8"))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1800
                    readTimeout = 1800
                    requestMethod = "GET"
                    useCaches = false
                }
                ok = connection.responseCode == 200 && connection.inputStream.bufferedReader().use { it.readText() } == "LEMOVI_OK"
                connection.disconnect()
            } catch (_: Exception) { }
            runOnUiThread { result(ok) }
        }.start()
    }
}