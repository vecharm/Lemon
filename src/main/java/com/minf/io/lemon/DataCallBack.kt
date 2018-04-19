package com.minf.io.lemon

import org.json.JSONObject

interface DataCallBack {
    fun responseData(message: JSONObject)
}