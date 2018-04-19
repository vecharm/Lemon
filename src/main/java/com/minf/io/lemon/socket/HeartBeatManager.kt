package com.minf.io.lemon.socket

import com.minf.io.lemon.Logger
import com.minf.io.lemon.Timer

class HeartBeatManager(private val heartbeatTime: Long) {

    private val timer = lazy { Timer() }
    private var lastTime = 0L
    private var isStart = false

    fun start(call: (HeartBeatManager) -> Unit) {
        if (isStart) return
        isStart = true
        lastTime = System.currentTimeMillis()
        timer.value.post(1000, "heartbeat") {
            Logger.i("=================================heartbeat=================================")
            if (System.currentTimeMillis() - lastTime > heartbeatTime)
                call.invoke(this@HeartBeatManager)
        }
    }

    fun active() {
        lastTime = System.currentTimeMillis()
    }

    fun cancel() {
        isStart = false
        if (timer.isInitialized())
            timer.value.cancelByToken("heartbeat")
    }

    fun quit() {
        isStart = false
        if (timer.isInitialized())
            timer.value.quitSafely()
    }

}