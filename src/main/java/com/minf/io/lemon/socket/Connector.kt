package com.minf.io.lemon.socket

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import okhttp3.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext


object Connector {



    val Client: OkHttpClient = OkHttpClient().newBuilder().apply {
        hostnameVerifier({ _, _ -> true })
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(10, TimeUnit.SECONDS)
        writeTimeout(10, TimeUnit.SECONDS)

    }.also { builder ->
        getAppContext()?.let {
            builder.addInterceptor(ChuckInterceptor(it))
        }

    }.build()


    fun request(url: String, requestParams: RequestParams): Call {

        val request = createRequest(url, requestParams.headParams)

        return Client.newCall(request)

    }

    fun request(url: String, requestParams: RequestParams, cb: (Call, String?, IOException?) -> Unit) {

        val call = request(url, requestParams)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException?) {
                cb.invoke(call, null, e)
            }

            override fun onResponse(call: Call, response: Response?) {
                val string = response?.body()?.string()?.let {
                    String(it.toByteArray(), Charsets.UTF_8)
                }
                cb.invoke(call, string, null)
            }

        })


    }

    private fun createRequest(url: String, headParams: HashMap<String, String>): Request {

        val builder = Request.Builder().url(url)

        headParams.map {
            builder.addHeader(it.key, it.value)
        }
        return builder.build()
    }


    fun getSslContext(): SSLContext? {
        return null
    }

    private var mContextWeak: WeakReference<Context>? = null

    fun getAppContext(): Context? {
        if (mContextWeak?.get() == null) {
            try {
                @SuppressLint("PrivateApi") val clazzActivityThread = Class.forName("android.app.ActivityThread")
                val currentActivityThread = clazzActivityThread.getDeclaredMethod("currentActivityThread")
                val getApplicationMethod = clazzActivityThread.getMethod("getApplication")
                val activityThread = currentActivityThread.invoke(null)
                val application = getApplicationMethod.invoke(activityThread) as Application
                mContextWeak = WeakReference(application.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return mContextWeak?.get()
    }

}