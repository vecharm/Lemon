package com.minf.io.lemon.socket
import java.io.IOException

 interface IOTransport {


    fun getName():String
    /**
     * Instructs the IOTransport to connect.
     */
    fun connect()

    /**
     * Instructs the IOTransport to disconnect.
     */
    fun disconnect()

    /**
     * Instructs the IOTransport to send a Message
     *
     * @param text
     * the text to be sent
     * @throws IOException
     * Signals that an I/O exception has occurred.
     */
    @Throws(Exception::class)
    fun send(text: String)

    /**
     * return true if the IOTransport prefers to send multiple messages at a
     * time.
     *
     * @return true, if successful
     */
    fun canSendBulk(): Boolean

    /**
     * Instructs the IOTransport to send multiple messages. This is only called
     * when canSendBulk returns true.
     *
     * @param texts
     * the texts
     * @throws IOException
     * Signals that an I/O exception has occurred.
     */
    @Throws(IOException::class)
    fun sendBulk(texts: Array<String>)

    /**
     * Instructs the IOTransport to invalidate. DO NOT DISCONNECT from the
     * server. just make sure, that events are not populated to the
     * [IOConnection]
     */
    fun invalidate()
}