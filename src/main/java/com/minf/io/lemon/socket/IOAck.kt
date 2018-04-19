package com.minf.io.lemon.socket
import org.json.JSONArray

interface IOAck {

    /**
     * Acknowledges a socket.io message.
     *
     * @param args may be all types which can be serialized by [JSONArray.put]
     */
    fun ack(vararg args: Any?)
}