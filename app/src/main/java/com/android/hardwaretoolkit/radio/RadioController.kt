package com.android.hardwaretoolkit.radio

import com.android.hardwaretoolkit.core.*

class RadioController(private val registry: ProviderRegistry, private val factory: RadioDriverFactory) {
    fun subGhzRxDriver(): SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_RX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()
    fun subGhzTxDriver(): SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_TX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()
    fun lfReader(): LfRfidDriver? =
        registry.find(Capability.LF_RFID_RX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()
    fun lfWriter(): LfRfidDriver? =
        registry.find(Capability.LF_RFID_TX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()
}

/**
 * Operation services over detected providers. No driver is returned for a provider
 * without a concrete driver implementation — unsupported hardware is never simulated.
 */
class SubGhzController(
    private val registry: ProviderRegistry,
    private val factory: RadioDriverFactory
) {
    fun receiveDriver(): SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_RX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()

    fun transmitDriver(): SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_TX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()
}

class LfRfidController(
    private val registry: ProviderRegistry,
    private val factory: RadioDriverFactory
) {
    fun reader(): LfRfidDriver? =
        registry.find(Capability.LF_RFID_RX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()

    fun writer(): LfRfidDriver? =
        registry.find(Capability.LF_RFID_TX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()
}
