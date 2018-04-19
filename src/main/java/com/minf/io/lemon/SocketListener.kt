package com.minf.io.lemon

import com.minf.io.lemon.socket.SocketIOException

interface SocketListener {


    fun onConnected()

    fun onReConnect()

    fun onError(socketIOException: SocketIOException)

    fun onDisconnect()


}