package com.android.hardwaretoolkit

import com.android.hardwaretoolkit.core.*
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun hexRoundTrip() {
        assertEquals("0001FEFF", Hex.encode(Hex.decode("00 01 FE FF")))
    }

    @Test fun hexHandlesWhitespaceAndEmptyInput() {
        assertArrayEquals(byteArrayOf(0x0A, 0xBC.toByte()), Hex.decode("\n0A\tBC "))
        assertArrayEquals(byteArrayOf(), Hex.decode("   \t\n"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hexRejectsOddLength() {
        Hex.decode("ABC")
    }

    @Test(expected = IllegalArgumentException::class)
    fun hexRejectsInvalidCharacters() {
        Hex.decode("GG")
    }

    @Test fun registryRequiresReadyProvider() {
        val r = ProviderRegistry()
        r.upsert(HardwareProvider("x", "X", Transport.USB, setOf(Capability.SUB_GHZ_RX), true, false, ""))
        assertTrue(r.find(Capability.SUB_GHZ_RX).isEmpty())
        r.upsert(HardwareProvider("x", "X", Transport.USB, setOf(Capability.SUB_GHZ_RX), true, true, ""))
        assertEquals(1, r.find(Capability.SUB_GHZ_RX).size)
    }

    @Test fun registryRejectsDisconnectedProviderFromCapabilityLookup() {
        val r = ProviderRegistry()
        r.upsert(HardwareProvider("x", "X", Transport.USB, setOf(Capability.LF_RFID_RX), false, true, ""))
        assertTrue(r.find(Capability.LF_RFID_RX).isEmpty())
    }
}
