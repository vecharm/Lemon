package com.minf.io.lemon
import java.util.*

interface DataListener :EventListener{

    fun receiveData(event: DataEvent)

}