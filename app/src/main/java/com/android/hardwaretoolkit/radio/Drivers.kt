package com.android.hardwaretoolkit.radio

import com.android.hardwaretoolkit.core.*

data class SubGhzCapture(
    val frequencyHz: Long,
    val timestampNanos: Long,
    val data: ByteArray
)

interface SubGhzDriver {
    val provider: HardwareProvider
    suspend fun configure(frequencyHz: Long, bandwidthHz: Long): Result<Unit>
    suspend fun receiveStart(onCapture:(SubGhzCapture)->Unit): Result<Unit>
    suspend fun receiveStop(): Result<Unit>
    suspend fun transmit(protocol:String, payload:ByteArray): Result<Unit>
}

interface LfRfidDriver {
    val provider: HardwareProvider
    suspend fun configure(frequencyHz:Int):Result<Unit>
    suspend fun read():Result<ByteArray>
    suspend fun write(data:ByteArray):Result<Unit>
}

/**
 * Driver factory. A driver is returned only for a known adapter protocol.
 * Unknown USB devices deliberately return null rather than being faked as radios.
 */
interface RadioDriverFactory {
    fun subGhzFor(provider:HardwareProvider):SubGhzDriver?
    fun lfRfidFor(provider:HardwareProvider):LfRfidDriver?
}
