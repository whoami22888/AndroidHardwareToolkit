package com.android.hardwaretoolkit.core

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter

/** Discovers capabilities physically exposed by the Android device without throwing on restricted APIs. */
class NativeHardwareDetector(private val context: Context) {
    private val pm = context.packageManager

    fun detect(): List<HardwareProvider> = buildList {
        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            val nfc = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()
            val enabled = runCatching { nfc?.isEnabled == true }.getOrDefault(false)
            add(
                HardwareProvider(
                    id = "phone:nfc",
                    name = "Phone NFC controller",
                    transport = Transport.PHONE_NATIVE,
                    capabilities = setOf(Capability.NFC),
                    connected = nfc != null,
                    ready = enabled,
                    detail = "Android NFC controller; enabled=$enabled"
                )
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
            val enabled = runCatching { adapter?.isEnabled == true }.getOrDefault(false)
            add(
                HardwareProvider(
                    id = "phone:ble",
                    name = "Phone Bluetooth LE",
                    transport = Transport.PHONE_NATIVE,
                    capabilities = setOf(Capability.BLE),
                    connected = adapter != null,
                    ready = enabled,
                    detail = if (adapter == null) {
                        "Android BLE controller unavailable"
                    } else {
                        "Android BLE controller; enabled=$enabled"
                    }
                )
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)) {
            val ir = runCatching { context.getSystemService(ConsumerIrManager::class.java) }.getOrNull()
            val available = runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)
            add(
                HardwareProvider(
                    id = "phone:ir",
                    name = "Phone consumer IR",
                    transport = Transport.PHONE_NATIVE,
                    capabilities = setOf(Capability.IR),
                    connected = ir != null,
                    ready = available,
                    detail = "Android Consumer IR emitter; available=$available"
                )
            )
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            add(
                HardwareProvider(
                    id = "phone:usb-host",
                    name = "Phone USB Host / OTG",
                    transport = Transport.PHONE_NATIVE,
                    capabilities = setOf(Capability.USB_HOST),
                    connected = true,
                    ready = true,
                    detail = "Android USB Host capability exposed"
                )
            )
        }
    }
}
