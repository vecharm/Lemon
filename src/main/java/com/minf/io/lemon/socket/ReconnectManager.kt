package com.minf.io.lemon.socket

import com.minf.io.lemon.Logger
import com.minf.io.lemon.Timer


class ReconnectManager(private val reconnectTime: Long) {

    private val timer = lazy { Timer() }
    private var isCancel = false
    private var isStart = false


    fun start(call: () -> Unit) {
        if (isStart) return
        isStart = true
        isCancel = false
        if (reconnectTime == -1L) return
        timer.value.post(reconnectTime, "reconnect") {
            if (isCancel) return@post
            call.invoke()
            Logger.i("================================Reconnect===========================")
        }
    }

    fun cancel() {
        isCancel = true
        timer.value.cancelByToken("reconnect")
        isStart = false
    }

    fun quit() {
        isCancel = true
        isStart = false
        timer.value.quitSafely()
    }


}