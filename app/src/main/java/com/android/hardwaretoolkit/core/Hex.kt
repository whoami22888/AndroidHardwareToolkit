package com.android.hardwaretoolkit.core

object Hex {
    private val HEX = "0123456789ABCDEF".toCharArray()

    fun encode(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { value ->
            val v = value.toInt() and 0xFF
            append(HEX[v ushr 4])
            append(HEX[v and 0x0F])
        }
    }

    fun decode(input: String): ByteArray {
        val out = ByteArray(countNonWhitespace(input) / 2)
        var high: Int? = null
        var outputIndex = 0

        for (ch in input) {
            if (ch.isWhitespace()) continue
            val nibble = Character.digit(ch, 16)
            require(nibble >= 0) { "Invalid hexadecimal input" }
            if (high == null) {
                high = nibble
            } else {
                out[outputIndex++] = ((high shl 4) or nibble).toByte()
                high = null
            }
        }

        require(high == null) { "Hex input must contain an even number of characters" }
        return out
    }

    private fun countNonWhitespace(input: String): Int = input.count { !it.isWhitespace() }.also {
        require(it % 2 == 0) { "Hex input must contain an even number of characters" }
    }
}
