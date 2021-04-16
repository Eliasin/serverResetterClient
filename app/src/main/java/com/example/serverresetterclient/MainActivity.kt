package com.example.serverresetterclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.io.IOError
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.net.Socket
import java.net.UnknownHostException
import kotlin.concurrent.thread

fun padByteArray(byteArray: ByteArray, size: Int, padding: Byte = 0): ByteArray {
    return if (size <= byteArray.size) {
        byteArray
    } else {
        ByteArray(size) {
            if (it >= byteArray.size) {
                padding
            } else {
                byteArray[it]
            }
        }
    }
}

fun displayAlert(view: View, title: String, message: String) {
    val alertDialog = AlertDialog.Builder(view.context).create()
    alertDialog.setTitle(title)
    alertDialog.setMessage(message)
    alertDialog.show()
}

const val MAX_SSID_LENGTH: Int = 32
const val MAX_PASSWORD_LENGTH: Int = 63

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun sendConfiguration(view: View) {
        val deviceIP = findViewById<TextView>(R.id.deviceIP)
        val port = findViewById<TextView>(R.id.port)
        val wifiSSID = findViewById<TextView>(R.id.wifiSSID)
        val wifiPassword = findViewById<TextView>(R.id.wifiPassword)

        thread {
            try {
                val socket = Socket(deviceIP.text.toString(), port.text.toString().toInt())
                val outputStream = socket.getOutputStream()

                val unpaddedSSIDBytes = wifiSSID.text.toString().byteInputStream().readBytes()
                val paddedSSIDBytes = padByteArray(unpaddedSSIDBytes, MAX_SSID_LENGTH)
                outputStream.write(paddedSSIDBytes)

                val unpaddedPasswordBytes = wifiPassword.text.toString().byteInputStream().readBytes()
                val paddedPasswordBytes = padByteArray(unpaddedPasswordBytes, MAX_PASSWORD_LENGTH)
                outputStream.write(paddedPasswordBytes)

                val inputStream = socket.getInputStream()

                inputStream.read()
                runOnUiThread {
                    displayAlert(view, "Success", "Successfully uploaded credentials.")
                }
            } catch (e: NumberFormatException) {
                runOnUiThread {
                    displayAlert(view, "Input Error", "Invalid port entered.")
                }
            } catch (e: IOException) {
                runOnUiThread {
                    displayAlert(view, "Network Error", "Failed to open socket.\n$e")
                }
            } catch (e: UnknownHostException) {
                runOnUiThread {
                    displayAlert(view, "Network Error", "Could not find host.")
                }
            } catch (e: IllegalArgumentException) {
                runOnUiThread {
                    displayAlert(view, "Input Error", "Invalid port entered.")
                }
            }
        }
    }
}