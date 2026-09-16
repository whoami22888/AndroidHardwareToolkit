package com.android.hardwaretoolkit.usb

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.android.hardwaretoolkit.core.*

data class UsbDeviceInfo(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val hasPermission: Boolean,
    val interfaces: Int
)

class UsbManagerBridge(private val context: Context, private val registry: ProviderRegistry) {
    companion object {
        const val ACTION_PERMISSION = "com.android.hardwaretoolkit.USB_PERMISSION"
    }

    private val manager: UsbManager? = context.getSystemService(UsbManager::class.java)
    private var receiver: BroadcastReceiver? = null

    fun start(onChanged: () -> Unit) {
        if (receiver != null) return
        val usbManager = manager ?: return

        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != ACTION_PERMISSION) return
                val device = getUsbDevice(intent) ?: return
                register(device, runCatching { usbManager.hasPermission(device) }.getOrDefault(false))
                onChanged()
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(ACTION_PERMISSION),
            Context.RECEIVER_NOT_EXPORTED
        )
        refresh(onChanged)
    }

    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    fun devices(): List<UsbDeviceInfo> {
        val usbManager = manager ?: return emptyList()
        return usbManager.deviceList.values.map { device ->
            UsbDeviceInfo(
                name = device.deviceName,
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturer = device.manufacturerName,
                product = device.productName,
                hasPermission = runCatching { usbManager.hasPermission(device) }.getOrDefault(false),
                interfaces = device.interfaceCount
            )
        }
    }

    fun requestPermission(device: UsbDevice): Result<Unit> = runCatching {
        val usbManager = requireNotNull(manager) { "USB Host service unavailable" }
        val intent = Intent(ACTION_PERMISSION).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(
            context,
            device.deviceId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, pi)
    }

    fun refresh(onChanged: () -> Unit = {}) {
        val usbManager = manager ?: return
        usbManager.deviceList.values.forEach { device ->
            val ready = runCatching { usbManager.hasPermission(device) }.getOrDefault(false)
            register(device, ready)
        }
        onChanged()
    }

    private fun register(device: UsbDevice, ready: Boolean) {
        registry.upsert(
            HardwareProvider(
                id = "usb:${device.deviceName}",
                name = device.productName ?: device.deviceName,
                transport = Transport.USB,
                capabilities = setOf(Capability.USB_HOST),
                connected = true,
                ready = ready,
                detail = "VID=%04X PID=%04X interfaces=%d".format(
                    device.vendorId,
                    device.productId,
                    device.interfaceCount
                )
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun getUsbDevice(intent: Intent): UsbDevice? =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
}
