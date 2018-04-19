package com.minf.io.lemon

import android.util.Log

object Logger {

    const val TAG = "com.minf.io.lemon"
    var enable = false
    infix fun i(message: String) {
        if (enable)
            Log.i(TAG, message)
    }

    infix fun w(message: String) {
        if (enable)
            Log.w(TAG, message)
    }

}