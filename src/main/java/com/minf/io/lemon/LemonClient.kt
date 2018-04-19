package com.minf.io.lemon

import com.minf.io.lemon.socket.IOAck
import com.minf.io.lemon.socket.IOCallback
import com.minf.io.lemon.socket.SocketIO
import com.minf.io.lemon.socket.SocketIOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

class LemonClient(val url: String, port: Int, val listener: SocketListener? = null, reconnectTime: Long = 3000L) {

    companion object {
        private const val JSONARRAY_FLAG = "["
    }


    private var reqId: Int = 0
    private var socket: SocketIO? = null
    private val cbs = HashMap<Int, DataCallBack?>()
    private val listeners = HashMap<String, ArrayList<DataListener>>()


    init {
        val buff = StringBuffer()
        if (!url.contains("http://"))
            buff.append("http://")
        buff.append("$url:$port")
        socket = SocketIO(buff.toString(), object : IOCallback {
            override fun onReconnect() {
                listener?.onReConnect()
            }

            override fun onConnected() {
                listener?.onConnected()
            }

            override fun onMessage(json: JSONObject?, ack: IOAck?) {
                Logger.w("pomelo send message of string.")
            }

            // get messages from the server side
            override fun onMessage(data: String?, ack: IOAck?) {
                if (data == null) return
                if (data.indexOf(JSONARRAY_FLAG) == 0) {
                    processMessageBatch(data)
                } else {
                    processMessage(data)
                }
            }

            override fun onError(socketIOException: SocketIOException) {
                Logger.i("connection is terminated.")
                emit("disconnect", null)
                socket = null
                socketIOException.printStackTrace()
                listener?.onError(socketIOException)
            }

            override fun onDisconnect() {
                Logger.i("connection is terminated.")
                emit("disconnect", null)
                socket = null
                listener?.onDisconnect()
            }

            override fun on(event: String, ack: IOAck?, vararg args: Any) {
                Logger.i("socket.io emit events.")
            }

        }, reconnectTime = reconnectTime)
    }

    private fun sendMessage(reqId: Int, route: String, msg: JSONObject) {
        socket?.send(Protocol.encode(reqId, route, msg))
    }

    fun request(vararg args: Any) {
        if (args.size < 2 || args.size > 3) {
            throw RuntimeException("the request arguments is error.")
        }
        // first argument must be string
        if (args[0] !is String) {
            throw RuntimeException("the route of request is error.")
        }

        val route = args[0].toString()
        var msg: JSONObject? = null
        var cb: DataCallBack? = null

        if (args.size == 2) {
            if (args[1] is JSONObject)
                msg = args[1] as JSONObject
            else if (args[1] is DataCallBack)
                cb = args[1] as DataCallBack
        } else {
            msg = args[1] as JSONObject
            cb = args[2] as DataCallBack
        }
        msg = filter(msg)
        reqId++
        cbs[reqId] = cb
        sendMessage(reqId, route, msg)
    }

    /**
     * Notify the server without response
     *
     * @param route
     * @param msg
     */
    fun inform(route: String, msg: JSONObject) {
        request(route, msg)
    }

    /**
     * Add timestamp to message.
     *
     * @param msg
     * @return msg
     */
    private fun filter(msg: JSONObject?): JSONObject {
        val msg = msg ?: JSONObject()

        val date = System.currentTimeMillis()
        try {
            msg.put("timestamp", date)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return msg
    }

    /**
     * Disconnect the connection with the server.
     */
    fun disconnect() {
        socket?.disconnect()
    }

    fun reconnect() {
        socket?.reconnect()
    }

    fun cancelReconnect() {
        socket?.cancelReconnect()
    }

    /**
     * Process the message from the server.
     *
     * @param msg
     */
    private fun processMessage(msg: String) {
        try {
            val jsonObject = JSONObject(msg)
            // request message
            if (jsonObject.has("id")) {
                val id = jsonObject.getInt("id")
                cbs[id]?.responseData(jsonObject.getJSONObject("body"))
                cbs.remove(id)
            } else
                emit(jsonObject.getString("route"), jsonObject)// broadcast message
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    /**
     * Process message in batch.
     *
     * @param msgs
     */
    private fun processMessageBatch(msgs: String) {
        try {
            val jsonArray = JSONArray(msgs)
            for (i in 0 until jsonArray.length()) {
                processMessage(jsonArray.getJSONObject(i).toString())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    /**
     * Add event listener and wait for broadcast message.
     *
     * @param route
     * @param listener
     */
    fun on(route: String, listener: DataListener) {
        (listeners[route] ?: ArrayList()).apply {
            add(listener)
            listeners[route] = this
        }
    }

    /**
     * Touch off the event and call listeners corresponding route.
     *
     * @param route
     * @param message
     * @return true if call success, false if there is no listeners for this
     * route.
     */
    private fun emit(route: String, message: JSONObject?) {
        val list = listeners[route]
        if (list == null) {
            Logger.w("there is no listeners.")
            return
        }
        for (listener in list) {
            val event = DataEvent(this, message)
            listener.receiveData(event)
        }
    }

}