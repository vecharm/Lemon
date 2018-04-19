package com.minf.io.lemon.socket
class SocketIOException(message: String?, ex: Throwable?) : Exception(message ,ex) {
   constructor(message: String?):this(message,null)
}