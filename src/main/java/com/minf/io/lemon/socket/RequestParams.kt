package com.minf.io.lemon.socket

import android.os.Build.HOST
import android.text.TextUtils
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Administrator on 2017/5/3 0003.
 */
//@keep
class RequestParams {

    var METHOD = POST

    var PROTOCOL = HTTPS

    //@keep
    companion object Factory {

        const val POST = 0
        const val GET = 1

        const val HTTP = 0
        const val HTTPS = 1

        @JvmStatic
        fun createHttpGetParams(): RequestParams {
            return RequestParams().http().get()
        }

        @JvmStatic
        fun createHttpPostParams(): RequestParams {
            return RequestParams().http().post()
        }

        @JvmStatic
        fun createHttpsGetParams(): RequestParams {
            return RequestParams().https().get()
        }

        @JvmStatic
        fun createHttpsPostParams(): RequestParams {
            return RequestParams().https().post()
        }


    }


    val urlParams: HashMap<String, String> = HashMap()
    val jsonParams: HashMap<String, List<Any>> = HashMap()
    val headParams: HashMap<String, String> = HashMap()
    val fileParams: WeakHashMap<String, File>? = WeakHashMap()


    fun put(key: String?, value: String?): RequestParams {
        if (key != null && value != null) {
            urlParams.put(key, value)
        }
        return this
    }

    fun put(key: String, value: Int): RequestParams {
        if (!TextUtils.isEmpty(key)) {
            urlParams.put(key, value.toString())
        }
        return this
    }

    fun put(key: String, value: Long): RequestParams {
        if (!TextUtils.isEmpty(key)) {
            urlParams.put(key, value.toString())
        }
        return this
    }

    fun put(key: String, value: Boolean): RequestParams {
        if (!TextUtils.isEmpty(key)) {
            urlParams.put(key, if (value) "1" else "0")
        }
        return this
    }

    fun setHost(value: String?): RequestParams {
        if (value != null) {
            urlParams.put(HOST, value)
        }
        return this
    }

    fun setHandler(value: String?): RequestParams {
        if (value != null) {
            urlParams.put("handler", value)
        }
        return this
    }


    fun addHeader(key: String?, value: String?): RequestParams {
        if (key != null && value != null) {
            headParams.put(key, value)
        }
        return this
    }

    fun put(key: String, value: List<Any>) {
        jsonParams.put(key, value)
    }

    @Throws(FileNotFoundException::class)
    fun put(key: String, value: File?): RequestParams {
        if (!TextUtils.isEmpty(key) && value != null) {
            if (!value.exists()) {
//                throw FileNotFoundException("file " + value.absolutePath + " is not exist!")
                return this
            }
            fileParams?.put(key, value)
        }
        return this
    }


    fun get(): RequestParams {
        METHOD = GET
        return this
    }

    fun post(): RequestParams {
        METHOD = POST
        return this
    }

    fun http(): RequestParams {
        PROTOCOL = HTTP
        return this
    }

    fun https(): RequestParams {
        PROTOCOL = HTTPS
        return this
    }


    override fun toString(): String {
        return "urlParams:" + urlParams + ", fileParams: " + (fileParams ?: "")
    }


    fun genRequestBody(): RequestBody {
        val urlParams = urlParams
        val fileParams = fileParams

        val bodySb = StringBuilder()
        bodySb.append("{")
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        for (key in urlParams.keys) {
            val value = urlParams[key]
            builder.addFormDataPart(key, value)
            bodySb.append(key).append("=").append(value).append(", ")
        }

        if (fileParams != null) {
            for (key in fileParams.keys) {
                val file = fileParams[key]
                builder.addFormDataPart(key, file?.name, RequestBody.create(null, file))
                bodySb.append(key).append("=").append(file?.name).append(", ")
            }
        }

        bodySb.append("}")

        return builder.build()
    }

}