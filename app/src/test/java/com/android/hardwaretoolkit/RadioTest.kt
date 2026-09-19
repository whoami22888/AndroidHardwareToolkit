package com.android.hardwaretoolkit

import com.android.hardwaretoolkit.core.*
import com.android.hardwaretoolkit.radio.*
import org.junit.Assert.*
import org.junit.Test

private class FakeSubGhzDriver(override val provider: HardwareProvider) : SubGhzDriver {
    var configured = false
    var receiving = false
    override suspend fun configure(frequencyHz: Long, bandwidthHz: Long): Result<Unit> {
        configured = true
        return Result.success(Unit)
    }

    override suspend fun receiveStart(onCapture: (SubGhzCapture) -> Unit): Result<Unit> {
        receiving = true
        return Result.success(Unit)
    }

    override suspend fun receiveStop(): Result<Unit> {
        receiving = false
        return Result.success(Unit)
    }

    override suspend fun transmit(protocol: String, payload: ByteArray): Result<Unit> =
        Result.success(Unit)
}

private class FakeLfRfidDriver(override val provider: HardwareProvider) : LfRfidDriver {
    override suspend fun configure(frequencyHz: Int): Result<Unit> = Result.success(Unit)
    override suspend fun read(): Result<ByteArray> = Result.success(byteArrayOf(0x01))
    override suspend fun write(data: ByteArray): Result<Unit> = Result.success(Unit)
}

/** Returns a driver only for the exact provider instance it was built for. */
private class FakeFactory(private val subGhzIds: Set<String> = emptySet(), private val lfIds: Set<String> = emptySet()) :
    RadioDriverFactory {
    override fun subGhzFor(provider: HardwareProvider): SubGhzDriver? =
        if (provider.id in subGhzIds) FakeSubGhzDriver(provider) else null

    override fun lfRfidFor(provider: HardwareProvider): LfRfidDriver? =
        if (provider.id in lfIds) FakeLfRfidDriver(provider) else null
}

class RadioTest {
    private fun provider(
        id: String,
        capabilities: Set<Capability>,
        connected: Boolean = true,
        ready: Boolean = true,
        transport: Transport = Transport.USB
    ) = HardwareProvider(id, id, transport, capabilities, connected, ready, "")

    @Test
    fun subGhzDriverReturnedOnlyForReadyConnectedCapableProvider() {
        val registry = ProviderRegistry()
        // Registered but not ready: must not be selected.
        registry.upsert(provider("adapter-a", setOf(Capability.SUB_GHZ_RX), ready = false))
        val factory = FakeFactory(subGhzIds = setOf("adapter-a"))
        val controller = SubGhzController(registry, factory)
        assertNull(controller.receiveDriver())

        // Becomes ready: now selectable.
        registry.upsert(provider("adapter-a", setOf(Capability.SUB_GHZ_RX), ready = true))
        val driver = controller.receiveDriver()
        assertNotNull(driver)
        assertEquals("adapter-a", driver?.provider?.id)
    }

    @Test
    fun disconnectedProviderIsNeverSelected() {
        val registry = ProviderRegistry()
        registry.upsert(provider("adapter-b", setOf(Capability.SUB_GHZ_TX), connected = false))
        val controller = SubGhzController(registry, FakeFactory(subGhzIds = setOf("adapter-b")))
        assertNull(controller.transmitDriver())
    }

    @Test
    fun factoryWithoutDriverForProviderReturnsNull() {
        val registry = ProviderRegistry()
        registry.upsert(provider("unknown-usb", setOf(Capability.SUB_GHZ_RX)))
        val controller = SubGhzController(registry, FakeFactory())
        assertNull(controller.receiveDriver())
    }

    @Test
    fun firstMatchingProviderWins() {
        val registry = ProviderRegistry()
        registry.upsert(provider("lf-1", setOf(Capability.LF_RFID_RX)))
        registry.upsert(provider("lf-2", setOf(Capability.LF_RFID_RX)))
        val controller = LfRfidController(registry, FakeFactory(lfIds = setOf("lf-1", "lf-2")))
        assertEquals("lf-1", controller.reader()?.provider?.id)
    }

    @Test
    fun providerWithoutRxOrTxCapabilityIsIgnored() {
        val registry = ProviderRegistry()
        registry.upsert(provider("nfc-only", setOf(Capability.NFC), transport = Transport.PHONE_NATIVE))
        val controller = SubGhzController(registry, FakeFactory(subGhzIds = setOf("nfc-only")))
        assertNull(controller.receiveDriver())
        assertNull(controller.transmitDriver())
    }

    @Test
    fun removingDetachedProviderDropsItFromSelection() {
        val registry = ProviderRegistry()
        registry.upsert(provider("adapter-c", setOf(Capability.LF_RFID_TX)))
        val controller = LfRfidController(registry, FakeFactory(lfIds = setOf("adapter-c")))
        assertNotNull(controller.writer())

        registry.remove("adapter-c")
        assertNull(controller.writer())
    }

    @Test
    fun radioControllerMirrorsSpecializedControllers() {
        val registry = ProviderRegistry()
        registry.upsert(provider("adapter-d", setOf(Capability.SUB_GHZ_RX, Capability.SUB_GHZ_TX)))
        val factory = FakeFactory(subGhzIds = setOf("adapter-d"))
        val controller = RadioController(registry, factory)
        assertEquals("adapter-d", controller.subGhzRxDriver()?.provider?.id)
        assertEquals("adapter-d", controller.subGhzTxDriver()?.provider?.id)
        assertNull(controller.lfReader())
        assertNull(controller.lfWriter())
    }
}
