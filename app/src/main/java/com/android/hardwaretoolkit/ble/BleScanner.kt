package com.android.hardwaretoolkit.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.util.size

data class BleAdvertisement(
    val address: String,
    val name: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    val manufacturerDataHex: String
)

class BleScanner(private val context: Context) {
    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    private var scanning = false

    var onAdvertisement: ((BleAdvertisement) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH) &&
                hasPermission(Manifest.permission.BLUETOOTH_ADMIN) &&
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

    private fun safeDeviceName(device: BluetoothDevice): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Unknown"
        }

        return try {
            device.name ?: "Unknown"
        } catch (_: SecurityException) {
            "Unknown"
        }
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasScanPermission() || !hasConnectPermission()) return

            val device = result.device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val record = result.scanRecord
            val manufacturer = record?.manufacturerSpecificData?.let { sparse ->
                buildString {
                    for (i in 0 until sparse.size) {
                        if (i > 0) append(' ')
                        append("%04X:".format(sparse.keyAt(i)))
                        sparse.valueAt(i).forEach { byte ->
                            append("%02X".format(byte.toInt() and 0xFF))
                        }
                    }
                }
            } ?: ""

            onAdvertisement?.invoke(
                BleAdvertisement(
                    address = device.address,
                    name = safeDeviceName(device),
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

        if (!hasScanPermission()) {
            return Result.failure(SecurityException(requiredScanPermission()))
        }
        if (!hasConnectPermission()) {
            return Result.failure(SecurityException("BLUETOOTH_CONNECT permission required"))
        }

        val adapter = try {
            bluetoothManager?.adapter
        } catch (e: SecurityException) {
            return Result.failure(e)
        } ?: return Result.failure(IllegalStateException("Bluetooth adapter unavailable"))

        val enabled = try {
            adapter.isEnabled
        } catch (e: SecurityException) {
            return Result.failure(e)
        }

        if (!enabled) {
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

        if (hasScanPermission() && hasConnectPermission()) {
            val bleScanner = runCatching {
                bluetoothManager?.adapter?.bluetoothLeScanner
            }.getOrNull()

            if (bleScanner != null) {
                try {
                    bleScanner.stopScan(callback)
                } catch (_: SecurityException) {
                    onError?.invoke(ERROR_STOP_PERMISSION)
                }
            }
        }

        scanning = false
    }

    private fun requiredScanPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "BLUETOOTH_SCAN permission required"
        } else {
            "BLUETOOTH, BLUETOOTH_ADMIN and ACCESS_FINE_LOCATION permissions required"
        }

    private companion object {
        const val ERROR_STOP_PERMISSION = -1001
    }
}
