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
    private var connected: Boolean = false


    //innit socket
    init {
        try {
            println("Init Socket")
            println("Connected to server at $address on port $port")
            connected = true
        } catch (e : Exception) {
            Log.i(TAG, e.toString())
        }
    }

    private val reader: Scanner = Scanner(connection.getInputStream())
    private val writer: OutputStream = connection.getOutputStream()

    //send msg to socket
    fun write(message: String) {
        thread {
            writer.write((message).toByteArray(Charset.defaultCharset()))
        }
    }


    //for now we arent receiving any msg from the robot so this function wont be used.
    fun read() {
//            thread {
//                while (connected)
//                    println(reader.nextLine())
//            }
    }

    fun closeConnection(){
        connection.close()
        connected = false
    }
}