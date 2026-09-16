package com.android.hardwaretoolkit.core

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter

/** Discovers capabilities physically exposed by the Android device. */
class NativeHardwareDetector(private val context: Context) {
    private val pm = context.packageManager

    fun detect(): List<HardwareProvider> {
        val result = mutableListOf<HardwareProvider>()

        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            val nfc = NfcAdapter.getDefaultAdapter(context)
            result += HardwareProvider(
                id = "phone:nfc",
                name = "Phone NFC controller",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.NFC),
                connected = nfc != null,
                ready = nfc?.isEnabled == true,
                detail = "Android NFC controller; enabled=${nfc?.isEnabled == true}"
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            result += HardwareProvider(
                id = "phone:ble",
                name = "Phone Bluetooth LE",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.BLE),
                connected = adapter != null,
                ready = adapter?.isEnabled == true,
                detail = "Android BLE controller; enabled=${adapter?.isEnabled == true}"
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)) {
            val ir = context.getSystemService(ConsumerIrManager::class.java)
            result += HardwareProvider(
                id = "phone:ir",
                name = "Phone consumer IR",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.IR),
                connected = ir != null,
                ready = ir?.hasIrEmitter() == true,
                detail = "Android Consumer IR emitter; available=${ir?.hasIrEmitter() == true}"
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            result += HardwareProvider(
                id = "phone:usb-host",
                name = "Phone USB Host / OTG",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.USB_HOST),
                connected = true,
                ready = true,
                detail = "Android USB Host capability exposed"
            )
        }

        return result
    }
}
