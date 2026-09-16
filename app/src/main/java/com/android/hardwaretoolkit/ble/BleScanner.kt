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
    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    private val scanner get() = bluetoothManager?.adapter?.bluetoothLeScanner
    private var scanning = false

    var onAdvertisement: ((BleAdvertisement) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
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

            val name = runCatching { result.device.name }.getOrNull() ?: "Unknown"
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
    fun start(): Result<Unit> = runCatching {
        if (scanning) return Result.success(Unit)
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        ) { "BLUETOOTH_SCAN permission required" }
        check(bluetoothManager?.adapter?.isEnabled == true) { "Bluetooth is disabled" }
        val bleScanner = scanner ?: error("BLE scanner unavailable")
        bleScanner.startScan(
            null,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            callback
        )
        scanning = true
    }

    @Synchronized
    fun stop() {
        if (!scanning) return
        runCatching { scanner?.stopScan(callback) }
        scanning = false
    }
}
