package com.android.hardwaretoolkit.radio

import com.android.hardwaretoolkit.core.*

class RadioController(private val registry:ProviderRegistry, private val factory:RadioDriverFactory) {
    fun subGhzRxDriver():SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_RX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()
    fun subGhzTxDriver():SubGhzDriver? =
        registry.find(Capability.SUB_GHZ_TX).asSequence().mapNotNull(factory::subGhzFor).firstOrNull()
    fun lfReader():LfRfidDriver? =
        registry.find(Capability.LF_RFID_RX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()
    fun lfWriter():LfRfidDriver? =
        registry.find(Capability.LF_RFID_TX).asSequence().mapNotNull(factory::lfRfidFor).firstOrNull()
}
