package com.minf.io.lemon.socket

import android.util.SparseArray
import com.minf.io.lemon.Logger
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_ACK
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_CONNECT
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_DISCONNECT
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_ERROR
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_EVENT
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_HEARTBEAT
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_JSON_MESSAGE
import com.minf.io.lemon.socket.IOMessage.Companion.TYPE_MESSAGE
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class IOConnection(val url: String, val socket: SocketIO) : IOCallback, ITransport {
    override fun getHeaders() = headers

    override fun getSession() = sessionId

    companion object {

        val FRAME_DELIMITER = "\ufffd"

        /** The Constant STATE_INIT.  */
        private const val STATE_INIT = 0

        /** The Constant STATE_HANDSHAKE.  */
        private const val STATE_HANDSHAKE = 1

        /** The Constant STATE_CONNECTING.  */
        private const val STATE_CONNECTING = 2

        /** The Constant STATE_READY.  */
        private const val STATE_READY = 3

        /** The Constant STATE_INTERRUPTED.  */
        private const val STATE_INTERRUPTED = 4

        /** The Constant STATE_INVALID.  */
        private const val STATE_INVALID = 6

        /** Socket.io path.  */
        const val SOCKET_IO_1 = "/socket.io/1/"

        @JvmStatic
        fun register(origin: String, socketIO: SocketIO): IOConnection {
            return IOConnection(origin, socketIO)
        }
    }

    /** The state.  */
    private var state = STATE_INIT

    private var headers: Properties? = null

    var transport: IOTransport? = null
        private set

    var sessionId: String? = null
        private set

    private var heartbeatTimeout = 0L
    private var closingTimeout = 0L
    var reconnectTime = 3000L
    private var protocols: List<String>? = null
    private var outputBuffer = ConcurrentLinkedQueue<String>()
    private var nextId = 1
    var acknowledge = SparseArray<IOAck>()

    init {
        headers = socket.headers
        startConnect()
    }

    private fun handshake() {

        try {
            setState(STATE_HANDSHAKE)
            val requestParams = RequestParams()
            headers?.forEach {
                requestParams.addHeader(it.key as? String, it.value as? String)
            }
            val rURl = "$url$SOCKET_IO_1"
            Connector.request(rURl, requestParams) { _, s, e ->
                s?.let {
                    val data = it.split(":")
                    sessionId = data[0]
                    heartbeatTimeout = data[1].toLong() * 1000
                    closingTimeout = data[2].toLong() * 1000
                    protocols = data[3].split(",")
                    connectTransport()
                }
            }

        } catch (e: Exception) {
            error(SocketIOException(e.message))
        }
    }

    @Synchronized
    private fun connectTransport() {
        if (state == STATE_INVALID) return
        setState(STATE_CONNECTING)

        heartBeatManager?.quit()
        heartBeatManager = null

        protocols?.also {
            if (it.contains("websocket"))
                transport = OKHttpTransport.create(URL(url), this)
            else {
                error(SocketIOException(
                        "Server supports no available transports. You should reconfigure the server to support a available transport"));
                return
            }
            transport?.connect()
        }
    }


    @Synchronized
    fun disconnect(socketIO: SocketIO) {
        sendPlain("0::${socketIO.namespace}")
        socketIO.callback?.onDisconnect()
        cleanup()
    }


    private fun remoteAck(message: IOMessage): IOAck? {
        val _id = message.id

        _id?.also {
            var id = it
            if (it.isEmpty()) return null
            else if (!it.endsWith("+"))
                id = "$it+"
            val endPoint = message.endpoint
            return object : IOAck {
                override fun ack(vararg args: Any?) {
                    val jsonArray = JSONArray()
                    args.forEach {
                        try {
                            jsonArray.put(if (it == null) JSONObject.NULL else it)
                        } catch (e: Exception) {
                            error(SocketIOException(
                                    "You can only put values in IOAcknowledge.ack() which can be handled by JSONArray.put()",
                                    e))
                        }
                        endPoint?.let {
                            val ackMsg = IOMessage(IOMessage.TYPE_ACK, endPoint,
                                    id + jsonArray.toString())
                            sendPlain(ackMsg.toString())
                        }
                    }

                }
            }
        }
        return null

    }

    private fun error(e: SocketIOException) {
        socket.callback?.onError(e)
        cleanup()
    }

    @Synchronized
    private fun sendPlain(text: String) {
        if (state == STATE_READY)
            try {
                Logger.i("> $text")
                transport?.send(text)
            } catch (e: Exception) {
                Logger.i("IOEx: saving")
                outputBuffer.add(text)
            }
        else {
            outputBuffer.add(text)
        }
    }


    private fun synthesizeAck(message: IOMessage, ack: IOAck?) {
        if (ack != null) {
            val id = nextId++
            acknowledge.put(id, ack)
            message.id = "$id+"
        }
    }


    fun send(socket: SocketIO, ack: IOAck?, json: JSONObject) {
        val message = IOMessage(IOMessage.TYPE_JSON_MESSAGE,
                socket.namespace, json.toString())
        synthesizeAck(message, ack)
        sendPlain(message.toString())
    }

    fun send(socket: SocketIO, ack: IOAck?, msg: String) {
        val message = IOMessage(IOMessage.TYPE_MESSAGE,
                socket.namespace, msg)
        synthesizeAck(message, ack)
        sendPlain(message.toString())
    }


    @Synchronized
    private fun setState(state: Int) {
        if (state != STATE_INVALID)
            this.state = state
    }

    private var heartBeatManager: HeartBeatManager? = null
    private fun resetTimeout() {
        if (state != STATE_INVALID) {
            if (heartBeatManager == null) {
                heartBeatManager = HeartBeatManager(closingTimeout + heartbeatTimeout)
                heartBeatManager?.start {
                    it.quit()
                    heartBeatManager = null
                    error(SocketIOException(
                            "Timeout Error. No heartbeat from server within life time of the socket. closing.",
                            lastException))
                }
            }
            heartBeatManager?.active()
        }
    }

    @Throws(SocketIOException::class)
    private fun findCallback(message: IOMessage): IOCallback? {
        if ("" == message.endpoint)
            return this
        return socket.callback
    }


    fun transportMessage(text: String) {
        Logger.i("< $text")

        val message: IOMessage
        try {
            message = IOMessage(text)
        } catch (e: Exception) {
            error(SocketIOException("Garbage from server: $text", e))
            return
        }
        resetTimeout()
        try {

            when (message.type) {
                TYPE_DISCONNECT ->
                    findCallback(message)?.onDisconnect()
                TYPE_CONNECT -> {
                    if (message.endpoint == "") {
                        if (socket.namespace == "")
                            socket.callback?.onConnected()
                        else {
                            val connect = IOMessage(TYPE_CONNECT, socket.namespace, "")
                            sendPlain(connect.toString())
                        }
                    } else {
                        findCallback(message)?.onConnected()
                    }
                }
                TYPE_HEARTBEAT ->
                    sendPlain("2::")
                TYPE_MESSAGE ->
                    findCallback(message)?.onMessage(message.data, remoteAck(message))
                TYPE_JSON_MESSAGE ->
                    message.data?.also {
                        if (it.trim() == "null") return@also
                        findCallback(message)?.onMessage(JSONObject(it), remoteAck(message))
                    }
                TYPE_EVENT -> {
                    val event = JSONObject(message.data)
                    val eventName = event.optString("name")
                    val array = arrayListOf<Any>()
                    if (event.has("args")) {
                        val args = event.optJSONArray("args")
                        args?.let {
                            (0 until args.length()).forEach {
                                if (!args.isNull(it)) array[it] = args[it]
                            }
                        }
                    }
                    findCallback(message)?.on(eventName, remoteAck(message), array)
                }
                TYPE_ACK -> {
                    val data = message.data?.split("\\+".toRegex(), 2)
                    data?.also {
                        if (it.size == 2) {
                            val id = it[0].toInt()
                            val ack = acknowledge.get(id)
                            ack?.let {
                                val jsonArray = JSONArray(data[1])
                                val args = arrayOfNulls<Any>(jsonArray.length())
                                (0 until args.size).forEach {
                                    args[it] = jsonArray[it]
                                }
                            }
                        } else if (it.size == 1) {
                            sendPlain("6:::" + data[0])
                        }
                    }
                }
                TYPE_ERROR -> {
                    message.data?.also {
                        findCallback(message)?.onError(SocketIOException(it))
                        if (it.endsWith("+0"))
                            cleanup()
                    }
                }
                else ->
                    return
            }
        } catch (e: Exception) {
            error(SocketIOException(e.message))
        }


    }

    private fun cleanup() {
        state = STATE_INVALID
        transport?.disconnect()
        Logger.i("Cleanup")
        heartBeatManager?.quit()
        heartBeatManager = null
        reconnectManager.value.quit()
    }

    private val reconnectManager = lazy { ReconnectManager(reconnectTime) }
    @Synchronized
    fun reconnect() {
        if (state != STATE_INVALID) {
            invalidateTransport()
            setState(STATE_INTERRUPTED)
            reconnectManager.value.start {
                connectTransport()
                if (!keepAliveInQueue) {
                    sendPlain("2::")
                    keepAliveInQueue = true
                }
                onReconnect()
            }


        }
    }

    @Synchronized
    fun cancelReconnect() {
        if (state != STATE_INVALID) {
            invalidateTransport()
            setState(STATE_INTERRUPTED)
            reconnectManager.value.cancel()
        }
    }

    private fun invalidateTransport() {
        transport?.invalidate()
        transport = null
    }


    private var keepAliveInQueue = false

    override fun transportConnected() {
        setState(STATE_READY)
        reconnectManager.value.cancel()
        resetTimeout()
        transport?.also {
            if (it.canSendBulk()) {
                val outputBuffer = this.outputBuffer
                this.outputBuffer = ConcurrentLinkedQueue()
                val texts = outputBuffer.toArray(arrayOfNulls<String>(outputBuffer.size))
                val stringTexts = arrayListOf<String>()
                texts.forEach {
                    it?.also { stringTexts.add(it) }
                }
                it.sendBulk(stringTexts.toTypedArray())
            } else {
                var text = outputBuffer.poll()
                while (text != null) {
                    sendPlain(text)
                    text = outputBuffer.poll()
                }
            }
        }
        this.keepAliveInQueue = false
    }

    private var lastException: Throwable? = null

    override fun transportDisconnected() {
        this.lastException = null
        reconnect()
    }

    override fun transportError(error: Throwable?) {
        this.lastException = error
        reconnect()
    }

    fun emit(socket: SocketIO, event: String, ack: IOAck?,
             vararg args: Any) {
        try {
            val json = JSONObject().put("name", event).put("args",
                    JSONArray(Arrays.asList(*args)))
            val message = IOMessage(IOMessage.TYPE_EVENT,
                    socket.namespace, json.toString())
            synthesizeAck(message, ack)
            sendPlain(message.toString())
        } catch (e: JSONException) {
            error(SocketIOException(
                    "Error while emitting an event. Make sure you only try to send arguments, which can be serialized into JSON."))
        }

    }


    override fun transportData(text: String) {
        if (!text.startsWith(FRAME_DELIMITER)) {
            transportMessage(text)
            return
        }

        val fragments = text.split(FRAME_DELIMITER.toRegex()).listIterator(1)
        while (fragments.hasNext()) {
            val length = Integer.parseInt(fragments.next())
            val string = fragments.next()
            if (length != string.length) {
                error(SocketIOException("Garbage from server: $text"))
                return
            }
            transportMessage(string)
        }
    }


    private fun startConnect() {
        if (this@IOConnection.state == STATE_INIT)
            handshake()
        else
            connectTransport()
    }

    fun isConnected(): Boolean {
        return state == STATE_READY
    }

    override fun onDisconnect() {
        socket.callback?.onDisconnect()
    }

    override fun onConnected() {
        socket.callback?.onConnected()
    }

    override fun onReconnect() {
        socket.callback?.onReconnect()
    }

    override fun onMessage(data: String?, ack: IOAck?) {
        socket.callback?.onMessage(data, ack)
    }

    override fun onMessage(json: JSONObject?, ack: IOAck?) {
        socket.callback?.onMessage(json, ack)
    }

    override fun on(event: String, ack: IOAck?, vararg args: Any) {
        socket.callback?.on(event, ack, *args)
    }

    override fun onError(socketIOException: SocketIOException) {
        socket.callback?.onError(socketIOException)
    }


}