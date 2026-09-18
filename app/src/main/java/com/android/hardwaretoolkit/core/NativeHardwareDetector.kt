package com.android.hardwaretoolkit.core

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.content.ContextCompat

/** Discovers hardware exposed by Android and reports readiness without hiding permission failures. */
class NativeHardwareDetector(private val context: Context) {
    private val pm = context.packageManager
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

    fun detect(): List<HardwareProvider> = buildList {
        detectNfc()?.let(::add)
        detectBle()?.let(::add)
        detectIr()?.let(::add)
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

    private fun detectNfc(): HardwareProvider? {
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) return null

        val nfc = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()
        val enabled = runCatching { nfc?.isEnabled == true }.getOrDefault(false)

        return HardwareProvider(
            id = "phone:nfc",
            name = "Phone NFC controller",
            transport = Transport.PHONE_NATIVE,
            capabilities = setOf(Capability.NFC),
            connected = nfc != null,
            ready = nfc != null && enabled,
            detail = when {
                nfc == null -> "Android NFC controller unavailable"
                !enabled -> "Android NFC controller available but disabled"
                else -> "Android NFC controller ready"
            }
        )
    }

    private fun detectBle(): HardwareProvider? {
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return null

        val manager = bluetoothManager
        if (manager == null) {
            return HardwareProvider(
                id = "phone:ble",
                name = "Phone Bluetooth LE",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.BLE),
                connected = false,
                ready = false,
                detail = "Android BluetoothManager unavailable"
            )
        }

        val requiredScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

        val scanPermissionReady = hasPermission(requiredScanPermission)
        val connectPermissionReady =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

        if (!scanPermissionReady || !connectPermissionReady) {
            val missing = buildList {
                if (!scanPermissionReady) add(requiredScanPermission.substringAfterLast('.'))
                if (!connectPermissionReady) {
                    add(Manifest.permission.BLUETOOTH_CONNECT.substringAfterLast('.'))
                }
            }.joinToString(", ")

            return HardwareProvider(
                id = "phone:ble",
                name = "Phone Bluetooth LE",
                transport = Transport.PHONE_NATIVE,
                capabilities = setOf(Capability.BLE),
                connected = false,
                ready = false,
                detail = "Bluetooth permission required: $missing"
            )
        }

        val adapter = runCatching { manager.adapter }.getOrNull()
        val enabled = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

        return HardwareProvider(
            id = "phone:ble",
            name = "Phone Bluetooth LE",
            transport = Transport.PHONE_NATIVE,
            capabilities = setOf(Capability.BLE),
            connected = adapter != null,
            ready = adapter != null && enabled,
            detail = when {
                adapter == null -> "Android Bluetooth adapter unavailable"
                !enabled -> "Android Bluetooth controller available but disabled"
                else -> "Android Bluetooth LE controller ready"
            }
        )
    }

    private fun detectIr(): HardwareProvider? {
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)) return null

        val ir = runCatching { context.getSystemService(ConsumerIrManager::class.java) }.getOrNull()
        val available = runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)

        return HardwareProvider(
            id = "phone:ir",
            name = "Phone consumer IR",
            transport = Transport.PHONE_NATIVE,
            capabilities = setOf(Capability.IR),
            connected = ir != null,
            ready = available,
            detail = when {
                ir == null -> "Android Consumer IR service unavailable"
                !available -> "Consumer IR service present but no emitter available"
                else -> "Android Consumer IR emitter ready"
            }
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
