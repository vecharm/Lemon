package com.minf.io.lemon

import android.os.Handler
import android.os.Looper

class Timer {

    var isQuit = false
        private set

    private val queue = HashMap<String, Runnable>()

    private var handler: Handler? = Handler(Looper.getMainLooper())

    fun post(time: Long, token: String, callBack: () -> Unit) {
        cancelByToken(token)
        val run = Runnable {
            callBack.invoke()
            post(time, token, callBack)
        }
        if (isQuit) return
        queue[token] = run
        handler?.postDelayed(run, time)

    }

    fun cancelByToken(token: String) {
        queue[token]?.let { handler?.removeCallbacks(it) }
    }

    fun quitSafely() {
        isQuit = true
        queue.clear()
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }
}