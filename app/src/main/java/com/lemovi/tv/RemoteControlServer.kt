package com.lemovi.tv

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class RemoteControlServer(
    private val port: Int,
    private val pairingCode: String,
    private val onCommand: (String) -> Unit
) {
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true
        worker = Thread({
            try {
                val socketServer = ServerSocket(port)
                socketServer.reuseAddress = true
                serverSocket = socketServer
                while (running) {
                    try {
                        val client = socketServer.accept()
                        Thread({ handle(client) }, "lemovi-remote-client").start()
                    } catch (_: Exception) {
                        if (running) { /* keep accepting */ }
                    }
                }
            } catch (_: Exception) {
                running = false
            }
        }, "lemovi-remote-server").apply { isDaemon = true; start() }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 3000
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val request = reader.readLine().orEmpty()
                while (true) {
                    val header = reader.readLine() ?: break
                    if (header.isEmpty()) break
                }
                val target = request.split(" ").getOrNull(1).orEmpty()
                val query = target.substringAfter("?", "")
                val values = query.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8") else null
                }.toMap()
                val suppliedCode = values["pin"].orEmpty()
                val command = values["key"].orEmpty().uppercase()
                val allowed = setOf("PING", "UP", "DOWN", "LEFT", "RIGHT", "OK", "BACK", "PLAY_PAUSE", "REWIND", "FORWARD", "VOL_UP", "VOL_DOWN", "MUTE")
                val valid = suppliedCode == pairingCode && command in allowed
                if (valid && command != "PING") onCommand(command)
                val body = if (valid) "LEMOVI_OK" else "DENIED"
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                val status = if (valid) "200 OK" else "403 Forbidden"
                val output = client.getOutputStream()
                output.write(("HTTP/1.1 " + status + "\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: " +
                    bytes.size + "\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n").toByteArray(StandardCharsets.UTF_8))
                output.write(bytes)
                output.flush()
            } catch (_: Exception) { }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) { }
        serverSocket = null
        worker = null
    }

    companion object {
        fun localIpv4Address(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val network = interfaces.nextElement()
                    if (!network.isUp || network.isLoopback) continue
                    val addresses = network.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                            return address.hostAddress.orEmpty()
                        }
                    }
                }
            } catch (_: Exception) { }
            return "No detectada"
        }
    }
}