package io.maido.m8client

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import io.maido.m8client.M8SDLActivity.Companion.startM8SDLActivity
import io.maido.m8client.M8Util.copyGameControllerDB
import io.maido.m8client.M8Util.isM8

class M8StartActivity : AppCompatActivity(R.layout.nodevice) {
    companion object {
        private const val ACTION_USB_PERMISSION = "io.maido.m8client.USB_PERMISSION"
        private const val TAG = "M8StartActivity"
    }

    private var showButtons = true
    private var audioDevice = 0
    private var audioDriver: String? = null

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = getExtraDevice(intent)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && isM8(device)) {
                            connectToM8(device)
                        } else {
                            Log.d(TAG, "Device was not M8")
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device $device")
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Log.d(TAG, "Device was detached!")
            }
        }
    }

    @Suppress("Deprecation")
    private fun getExtraDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        registerReceiver(usbReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))
        copyGameControllerDB(this)
        val start = findViewById<Button>(R.id.startButton)
        start.setOnClickListener { start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(usbReceiver)
    }

    private fun start() {
        readPreferenceValues()
        Log.i(TAG, "Searching for an M8 device")
        val usbManager =
            getSystemService<UsbManager>() ?: throw RuntimeException("Service not found!")
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            if (isM8(device)) {
                connectToM8WithPermission(usbManager, device)
                break
            }
        }
    }

    private fun readPreferenceValues() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        audioDevice = preferences.getString(getString(R.string.audio_device_pref), "0")!!.toInt()
        audioDriver = preferences.getString(getString(R.string.audio_driver_pref), "AAudio")
        showButtons = preferences.getBoolean(getString(R.string.buttons_pref), true)
    }

    private fun connectToM8WithPermission(usbManager: UsbManager, usbDevice: UsbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "Permission granted!")
            connectToM8(usbDevice)
        } else {
            Log.i(TAG, "Requesting USB device permission")
            requestM8Permission(usbManager, usbDevice)
        }
    }

    private fun requestM8Permission(
        usbManager: UsbManager,
        usbDevice: UsbDevice
    ) {
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(
                ACTION_USB_PERMISSION
            ), PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        usbManager.requestPermission(usbDevice, permissionIntent)
    }

    private fun connectToM8(usbDevice: UsbDevice) {
        startM8SDLActivity(
            this,
            usbDevice,
            audioDevice,
            showButtons,
            audioDriver
        )
    }
}