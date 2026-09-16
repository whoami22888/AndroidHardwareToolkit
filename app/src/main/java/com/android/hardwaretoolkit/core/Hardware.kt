package com.android.hardwaretoolkit.core

enum class Transport { PHONE_NATIVE, USB, BLUETOOTH, UNKNOWN }
enum class Capability { NFC, BLE, IR, USB_HOST, SUB_GHZ_RX, SUB_GHZ_TX, LF_RFID_RX, LF_RFID_TX, GPIO, SPI, I2C }

data class HardwareProvider(
    val id: String,
    val name: String,
    val transport: Transport,
    val capabilities: Set<Capability>,
    val connected: Boolean,
    val ready: Boolean,
    val detail: String
)

class ProviderRegistry {
    private val providers = linkedMapOf<String, HardwareProvider>()
    @Synchronized fun upsert(p: HardwareProvider) { providers[p.id] = p }
    @Synchronized fun remove(id: String) { providers.remove(id) }
    @Synchronized fun all(): List<HardwareProvider> = providers.values.toList()
    @Synchronized fun find(capability: Capability): List<HardwareProvider> =
        providers.values.filter { capability in it.capabilities && it.connected && it.ready }
}
