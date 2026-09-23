package com.android.hardwaretoolkit.ir

import android.content.Context
import android.hardware.ConsumerIrManager

data class IrFrequencyRange(val minHz: Int, val maxHz: Int)

data class IrTransmitRequest(val carrierFrequencyHz: Int, val patternMicros: IntArray)

class IrTransmitter(context: Context) {
    private val manager: ConsumerIrManager? =
        runCatching { context.getSystemService(ConsumerIrManager::class.java) }.getOrNull()

    fun isAvailable(): Boolean = runCatching { manager?.hasIrEmitter() == true }.getOrDefault(false)

    fun supportedFrequencies(): List<IrFrequencyRange> =
        (runCatching { manager?.getCarrierFrequencies() }.getOrNull() ?: emptyArray())
            .map { IrFrequencyRange(it.minFrequency, it.maxFrequency) }

    fun transmit(request: IrTransmitRequest): Result<Unit> {
        val ir = manager ?: return Result.failure(IllegalStateException("Consumer IR service unavailable"))
        if (!runCatching { ir.hasIrEmitter() }.getOrDefault(false)) {
            return Result.failure(IllegalStateException("Consumer IR emitter unavailable"))
        }
        val validation = validate(request)
        if (validation.isFailure) return validation
        val ranges = supportedFrequencies()
        if (ranges.isNotEmpty() && ranges.none { request.carrierFrequencyHz in it.minHz..it.maxHz }) {
            return Result.failure(IllegalArgumentException("Carrier frequency is outside the device's supported ranges"))
        }
        return try {
            ir.transmit(request.carrierFrequencyHz, request.patternMicros)
            Result.success(Unit)
        } catch (error: RuntimeException) {
            Result.failure(error)
        }
    }

    companion object {
        fun validate(request: IrTransmitRequest): Result<Unit> = runCatching {
            require(request.carrierFrequencyHz > 0) { "Carrier frequency must be greater than zero" }
            require(request.patternMicros.isNotEmpty()) { "IR pattern must contain at least one duration" }
            request.patternMicros.forEachIndexed { index, duration ->
                require(duration > 0) { "IR pattern duration at index $index must be greater than zero" }
            }
        }

        fun parsePattern(input: String): Result<IntArray> = runCatching {
            val tokens = input.split(',', ' ', '\t', '\n', '\r').filter(String::isNotBlank)
            require(tokens.isNotEmpty()) { "IR pattern is empty" }
            IntArray(tokens.size) { index ->
                tokens[index].toInt().also { value ->
                    require(value > 0) { "IR pattern duration at index $index must be greater than zero" }
                }
            }
        }
    }
}
