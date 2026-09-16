package com.android.hardwaretoolkit.core
object Hex {
    fun encode(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    fun decode(input: String): ByteArray {
        val s = input.replace("\\s".toRegex(), "")
        require(s.length % 2 == 0) { "Hex input must contain an even number of characters" }
        require(s.matches(Regex("[0-9A-Fa-f]*"))) { "Invalid hexadecimal input" }
        return ByteArray(s.length / 2) { i -> s.substring(i*2, i*2+2).toInt(16).toByte() }
    }
}
