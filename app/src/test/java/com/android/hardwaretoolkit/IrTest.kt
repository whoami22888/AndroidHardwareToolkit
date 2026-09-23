package com.android.hardwaretoolkit

import com.android.hardwaretoolkit.ir.IrTransmitRequest
import com.android.hardwaretoolkit.ir.IrTransmitter
import org.junit.Assert.*
import org.junit.Test

class IrTest {
    @Test
    fun validPatternParses() {
        val result = IrTransmitter.parsePattern("9000,4500 560 560")
        assertTrue(result.isSuccess)
        assertArrayEquals(intArrayOf(9000, 4500, 560, 560), result.getOrThrow())
    }

    @Test
    fun invalidPatternIsRejected() {
        assertTrue(IrTransmitter.parsePattern("9000,-1").isFailure)
        assertTrue(IrTransmitter.parsePattern("not-a-number").isFailure)
        assertTrue(IrTransmitter.parsePattern("   ").isFailure)
    }

    @Test
    fun invalidTransmitRequestIsRejected() {
        assertTrue(IrTransmitter.validate(IrTransmitRequest(0, intArrayOf(560))).isFailure)
        assertTrue(IrTransmitter.validate(IrTransmitRequest(38000, intArrayOf())).isFailure)
        assertTrue(IrTransmitter.validate(IrTransmitRequest(38000, intArrayOf(560, 0))).isFailure)
    }

    @Test
    fun validTransmitRequestPassesValidation() {
        assertTrue(IrTransmitter.validate(IrTransmitRequest(38000, intArrayOf(9000, 4500, 560, 560))).isSuccess)
    }
}
