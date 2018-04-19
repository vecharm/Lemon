package com.minf.io.lemon.socket

import java.util.*

interface ITransport {

    fun transportConnected()
    fun transportError(e: Throwable?)
    fun transportData(text: String)
    fun transportDisconnected()
    fun getHeaders():Properties?
    fun getSession():String?

}