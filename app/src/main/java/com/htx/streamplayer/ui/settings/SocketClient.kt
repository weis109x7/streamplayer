package com.htx.streamplayer.ui.settings

import android.util.Log
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread


class SocketClient (address: String, port: Int){
    private val TAG = "SocketActivity"
    private val connection: Socket = Socket(address, port)
    private var connected: Boolean = true

    init {
        try {
            println("Init Socket")
            println("Connected to server at $address on port $port")

        } catch (e : Exception) {
            Log.i(TAG, e.toString())
        }
    }

    private val reader: Scanner = Scanner(connection.getInputStream())
    private val writer: OutputStream = connection.getOutputStream()


    fun write(message: String) {
        thread {
            writer.write((message).toByteArray(Charset.defaultCharset()))
        }
    }

    fun read() {
//            thread {
//                while (connected)
//                    println(reader.nextLine())
//            }
    }

    fun closeConnection(){
        connection.close()
    }
}