package com.example.serverresetterclient

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
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
const val MAX_HOST_LENGTH: Int = 32

class MainActivity : AppCompatActivity() {
    private var currentlyHandlingRequest: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val preferences = getPreferences(Context.MODE_PRIVATE)
        if (preferences.contains("deviceIP") && preferences.contains("port") && preferences.contains("ssid") && preferences.contains("password") && preferences.contains("hostToWatch")) {
            val deviceIP = findViewById<TextView>(R.id.deviceIP)
            val port = findViewById<TextView>(R.id.port)
            val wifiSSID = findViewById<TextView>(R.id.wifiSSID)
            val wifiPassword = findViewById<TextView>(R.id.wifiPassword)
            val hostToWatch = findViewById<TextView>(R.id.hostToWatch)

            deviceIP.text = preferences.getString("deviceIP", "")
            port.text = preferences.getString("port", "")
            wifiSSID.text = preferences.getString("ssid", "")
            wifiPassword.text = preferences.getString("password", "")
            hostToWatch.text = preferences.getString("hostToWatch", "")
        }
    }

    fun sendConfiguration(view: View) {
        if (currentlyHandlingRequest.get()) {
            return
        }

        val deviceIP = findViewById<TextView>(R.id.deviceIP)
        val port = findViewById<TextView>(R.id.port)
        val wifiSSID = findViewById<TextView>(R.id.wifiSSID)
        val wifiPassword = findViewById<TextView>(R.id.wifiPassword)
        val hostToWatch = findViewById<TextView>(R.id.hostToWatch)

        val preferences = getPreferences(Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putString("deviceIP", deviceIP.text.toString())
            putString("port", port.text.toString())
            putString("ssid", wifiSSID.text.toString())
            putString("password", wifiPassword.text.toString())
            putString("hostToWatch", hostToWatch.text.toString())

            apply()
        }

        thread {
            try {
                currentlyHandlingRequest.set(true)

                val socket = Socket(deviceIP.text.toString(), port.text.toString().toInt())
                val outputStream = socket.getOutputStream()

                val unpaddedSSIDBytes = wifiSSID.text.toString().byteInputStream().readBytes()
                val paddedSSIDBytes = padByteArray(unpaddedSSIDBytes, MAX_SSID_LENGTH)
                outputStream.write(paddedSSIDBytes)

                val unpaddedPasswordBytes = wifiPassword.text.toString().byteInputStream().readBytes()
                val paddedPasswordBytes = padByteArray(unpaddedPasswordBytes, MAX_PASSWORD_LENGTH)
                outputStream.write(paddedPasswordBytes)

                val unpaddedHostToWatchBytes = hostToWatch.text.toString().byteInputStream().readBytes()
                val paddedHostToWatchBytes = padByteArray(unpaddedHostToWatchBytes, MAX_HOST_LENGTH)
                outputStream.write(paddedHostToWatchBytes)

                outputStream.flush()

                val inputStream = socket.getInputStream()

                inputStream.read()
                runOnUiThread {
                    displayAlert(view, "Success", "Successfully uploaded credentials.")
                }

                socket.close()
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
            } finally {
                currentlyHandlingRequest.set(false)
            }
        }
    }
}