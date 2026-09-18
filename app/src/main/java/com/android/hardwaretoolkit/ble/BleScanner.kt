package com.android.hardwaretoolkit.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class BleAdvertisement(
    val address: String,
    val name: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    val manufacturerDataHex: String
)

class BleScanner(private val context: Context) {
    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    private val scanner get() = bluetoothManager?.adapter?.bluetoothLeScanner
    private var scanning = false

    var onAdvertisement: ((BleAdvertisement) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun safeDeviceName(device: BluetoothDevice): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Unknown"
        }
        return runCatching { device.name }.getOrNull() ?: "Unknown"
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

            val record = result.scanRecord
            val manufacturer = record?.manufacturerSpecificData?.let { sparse ->
                buildString {
                    for (i in 0 until sparse.size()) {
                        if (i > 0) append(' ')
                        append("%04X:".format(sparse.keyAt(i)))
                        sparse.valueAt(i).forEach { byte ->
                            append("%02X".format(byte.toInt() and 0xFF))
                        }
                    }
                }
            } ?: ""

            val name = safeDeviceName(result.device)
            onAdvertisement?.invoke(
                BleAdvertisement(
                    address = result.device.address,
                    name = name,
                    rssi = result.rssi,
                    serviceUuids = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList(),
                    manufacturerDataHex = manufacturer
                )
            )
        }

        override fun onScanFailed(errorCode: Int) {
            synchronized(this@BleScanner) { scanning = false }
            onError?.invoke(errorCode)
        }
    }

    @Synchronized
    fun start(): Result<Unit> {
        if (scanning) return Result.success(Unit)

        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return Result.failure(SecurityException("BLUETOOTH_SCAN permission required"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            return Result.failure(SecurityException("BLUETOOTH_CONNECT permission required"))
        }

        val adapter = bluetoothManager?.adapter
            ?: return Result.failure(IllegalStateException("Bluetooth adapter unavailable"))
        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is disabled"))
        }

        val bleScanner = try {
            adapter.bluetoothLeScanner
        } catch (e: SecurityException) {
            return Result.failure(e)
        } ?: return Result.failure(IllegalStateException("BLE scanner unavailable"))

        return try {
            bleScanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                callback
            )
            scanning = true
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    @Synchronized
    fun stop() {
        if (!scanning) return

        if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            val bleScanner = try {
                scanner
            } catch (_: SecurityException) {
                null
            }
            if (bleScanner != null) {
                try {
                    bleScanner.stopScan(callback)
                } catch (_: SecurityException) {
                    // Permission can be revoked between the explicit check and the API call.
                }
            }
        }

        scanning = false
    }
}
