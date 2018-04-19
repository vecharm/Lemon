package com.minf.io.lemon
import org.json.JSONObject

object Protocol {


    @JvmStatic
    fun encode(id: Int, route: String, msg: JSONObject): String {
        val str = msg.toString()
        if (route.length > 255) {
            throw RuntimeException("route max length is overflow.")
        }
        val arr = ByteArray(5 + route.length)
        var index = 0
        arr[index++] = (id shr 24 and 0xFF).toByte()
        arr[index++] = (id shr 16 and 0xFF).toByte()
        arr[index++] = (id shr 8 and 0xFF).toByte()
        arr[index++] = (id and 0xFF).toByte()
        arr[index++] = (route.length and 0xFF).toByte()

        for (i in 0 until route.length) {
            arr[index++] = route.codePointAt(i).toByte()
        }
        return bt2Str(arr, 0, arr.size) + str
    }

    private fun bt2Str(arr: ByteArray, start: Int, end: Int): String {
        val buff = StringBuffer()
        var i = start
        while (i < arr.size && i < end) {
            buff.append(String(Character.toChars((arr[i] + 256) % 256)))
            i++
        }
        return buff.toString()
    }


}