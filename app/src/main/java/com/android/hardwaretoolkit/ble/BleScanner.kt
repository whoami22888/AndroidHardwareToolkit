package com.android.hardwaretoolkit.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class BleAdvertisement(
    val address: String,
    val name: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    val manufacturerDataHex: String
)

class BleScanner(private val context: Context) {
    private val scanner get() = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
    var onAdvertisement: ((BleAdvertisement) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord
            val manufacturer = record?.manufacturerSpecificData?.let { sparse ->
                (0 until sparse.size()).joinToString(" ") { i ->
                    val data = sparse.valueAt(i).joinToString("") { "%02X".format(it.toInt() and 255) }
                    "%04X:$data".format(sparse.keyAt(i))
                }
            } ?: ""
            onAdvertisement?.invoke(
                BleAdvertisement(
                    result.device.address,
                    runCatching { result.device.name }.getOrNull() ?: "Unknown",
                    result.rssi,
                    record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList(),
                    manufacturer
                )
            )
        }
        override fun onScanFailed(errorCode: Int) { onError?.invoke(errorCode) }
    }

    fun start(): Result<Unit> = runCatching {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED) { "BLUETOOTH_SCAN permission required" }
        check(context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true) {
            "Bluetooth is disabled"
        }
        scanner?.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            callback
        ) ?: error("BLE scanner unavailable")
    }
    fun stop() { runCatching { scanner?.stopScan(callback) } }
}
