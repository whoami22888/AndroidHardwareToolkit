package com.android.hardwaretoolkit.usb

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import com.android.hardwaretoolkit.core.*

data class UsbDeviceInfo(
    val name:String, val vendorId:Int, val productId:Int,
    val manufacturer:String?, val product:String?, val hasPermission:Boolean,
    val interfaces:Int
)

class UsbManagerBridge(private val context: Context, private val registry: ProviderRegistry) {
    companion object { const val ACTION_PERMISSION = "com.android.hardwaretoolkit.USB_PERMISSION" }
    private val manager = context.getSystemService(UsbManager::class.java)
    private var receiver: BroadcastReceiver? = null

    fun start(onChanged:()->Unit) {
        receiver = object: BroadcastReceiver() {
            override fun onReceive(c:Context, intent:Intent) {
                if (intent.action != ACTION_PERMISSION) return
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                register(device, manager.hasPermission(device))
                onChanged()
            }
        }
        context.registerReceiver(receiver, IntentFilter(ACTION_PERMISSION), Context.RECEIVER_NOT_EXPORTED)
        refresh(onChanged)
    }
    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }
    fun devices():List<UsbDeviceInfo> = manager.deviceList.values.map {
        UsbDeviceInfo(it.deviceName,it.vendorId,it.productId,it.manufacturerName,it.productName,
            manager.hasPermission(it),it.interfaceCount)
    }
    fun requestPermission(device:UsbDevice) {
        val intent = Intent(ACTION_PERMISSION).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(
            context, device.deviceId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        manager.requestPermission(device, pi)
    }
    fun refresh(onChanged:()->Unit={}) {
        manager.deviceList.values.forEach { register(it, manager.hasPermission(it)) }
        onChanged()
    }
    private fun register(d:UsbDevice, ready:Boolean) {
        // Do not infer Sub-GHz/LF-RFID capability from USB class alone.
        registry.upsert(
            HardwareProvider(
                id="usb:${d.deviceName}",
                name=d.productName ?: d.deviceName,
                transport=Transport.USB,
                capabilities=setOf(Capability.USB_HOST),
                connected=true,
                ready=ready,
                detail="VID=%04X PID=%04X interfaces=%d".format(d.vendorId,d.productId,d.interfaceCount)
            )
        )
    }
}
