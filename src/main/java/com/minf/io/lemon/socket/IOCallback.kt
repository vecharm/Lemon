package com.minf.io.lemon.socket
import org.json.JSONObject

interface  IOCallback{

    /**
     * On disconnect. Called when the socket disconnects and there are no further attempts to reconnect
     */
     fun onDisconnect()

    /**
     * On connect. Called when the socket becomes ready so it is now able to receive data
     */
     fun onConnected()


    fun onReconnect()

    /**
     * On message. Called when the server sends String data.
     *
     * @param data the data.
     * @param ack an [IOAck] instance, may be `null` if there's none
     */
     fun onMessage(data: String?, ack: IOAck?)

    /**
     * On message. Called when the server sends JSON data.
     *
     * @param json JSON object sent by server.
     * @param ack an [IOAck] instance, may be `null` if there's none
     */
     fun onMessage(json: JSONObject?, ack: IOAck?)

    /**
     * On [Event]. Called when server emits an event.
     *
     * @param event Name of the event
     * @param ack an [IOAck] instance, may be `null` if there's none
     * @param args Arguments of the event
     */
     fun on(event: String, ack: IOAck?, vararg args: Any)

    /**
     * On error. Called when socket is in an undefined state. No reconnect attempts will be made.
     *
     * @param socketIOException the last exception describing the error
     */
     fun onError(socketIOException: SocketIOException)

}