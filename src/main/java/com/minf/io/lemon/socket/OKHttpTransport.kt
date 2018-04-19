package com.minf.io.lemon.socket

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URI
import java.net.URL
import java.util.regex.Pattern

class OKHttpTransport(val url: String, var connection: ITransport?) : IOTransport, WebSocketListener() {

    companion object {

        private val PATTERN_HTTP = Pattern.compile("^http")

        @JvmStatic
        fun create(url: URL, connection: ITransport): IOTransport {
            val uri = URI(PATTERN_HTTP.matcher(url.toString()).replaceFirst("ws")
                    + IOConnection.SOCKET_IO_1 + "websocket"
                    + "/" + connection.getSession())

            return OKHttpTransport(uri.toString(), connection)
        }


    }


    private var socket: WebSocket? = null

    override fun connect() {

        val request = Request.Builder().url(url).also { rq ->
            connection?.getHeaders()?.forEach {
                rq.addHeader(it.key as String, it.value as String)
            }
        }.build()

        Connector.Client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        socket = webSocket
        connection?.transportConnected()
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        connection?.transportError(t)
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {

    }

    override fun onMessage(webSocket: WebSocket?, text: String) {
        connection?.transportData(text)

    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        connection?.transportDisconnected()
    }

    override fun disconnect() {
        socket?.close(1008, null)
//        socket?.cancel()
    }

    override fun send(text: String) {
        socket?.send(text)
    }

    override fun canSendBulk() = false

    override fun sendBulk(texts: Array<String>) {
    }

    override fun invalidate() {
        socket?.cancel()
        connection = null
    }

    override fun getName() = "okHttp-socket"

}

