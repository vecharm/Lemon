package com.minf.io.lemon.socket

import org.json.JSONObject
import java.net.URL
import java.util.*

class SocketIO(url: String, callback: IOCallback?, var headers: Properties = Properties(), var reconnectTime: Long) {


    var callback: IOCallback? = null
        private set
    private var connection: IOConnection? = null

    var namespace = ""
        private set

    private var url: URL? = null

    init {
        if (!setAndConnect(URL(url), callback)) {
            throw RuntimeException(
                    "connect(URL, IOCallback) can only be invoked after SocketIO()")
        }
    }

    private fun setAndConnect(url: URL?, callback: IOCallback?): Boolean {
        if (this.connection != null) {
            throw RuntimeException("You can connect your SocketIO instance only once. Use a fresh instance instead.")
        }

        if (this.url != null || this.callback != null)
            return false

        this.url = url

        this.callback = callback

        url?.also {
            val origin = "${it.protocol}://${it.authority}"
            namespace = it.path
            if (namespace == "/") namespace = ""
            connection = IOConnection.register(origin, this)
            connection?.reconnectTime = reconnectTime
            return true
        }
        return false
    }

    fun emit(event: String, vararg args: Any) {
        connection?.emit(this, event, null, *args)
    }

    fun emit(event: String, ack: IOAck, vararg args: Any) {
        connection?.emit(this, event, ack, *args)
    }

    fun send(json: JSONObject) {
        connection?.send(this, null, json)
    }

    fun send(ack: IOAck, json: JSONObject) {
        connection?.send(this, ack, json)
    }

    fun send(message: String) {
        connection?.send(this, null, message)
    }

    fun disconnect() {
//        connection?.disconnect()
        connection?.disconnect(this)
        connection = null
    }

    fun reconnect() {
        connection?.reconnect()
    }

    fun cancelReconnect() {
        connection?.cancelReconnect()
    }

    fun isConnected() = connection?.isConnected() == true

    fun getTransport() = connection?.transport?.getName()

    fun getHeader(key: String) = headers[key]


}