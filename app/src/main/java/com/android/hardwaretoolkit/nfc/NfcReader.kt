package com.android.hardwaretoolkit.nfc

import android.app.Activity
import android.nfc.Ndef
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.android.hardwaretoolkit.core.Hex
import java.io.IOException

data class NfcRecord(
    val tnf: Int,
    val typeHex: String,
    val payloadHex: String
)

data class NfcReadResult(
    val idHex: String,
    val technologies: List<String>,
    val records: List<NfcRecord>,
    val rawNdefHex: String?
)

class NfcReader(private val activity: Activity) {
    private var active = false

    var onTag: ((NfcReadResult) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private val adapter: NfcAdapter?
        get() = runCatching { NfcAdapter.getDefaultAdapter(activity) }.getOrNull()

    @Synchronized
    fun start(): Result<Unit> {
        if (active) return Result.success(Unit)
        val nfc = adapter ?: return Result.failure(IllegalStateException("NFC adapter unavailable"))
        if (!runCatching { nfc.isEnabled }.getOrDefault(false)) {
            return Result.failure(IllegalStateException("NFC is disabled"))
        }
        return try {
            nfc.enableReaderMode(
                activity,
                { tag -> handleTag(tag) },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
            active = true
            Result.success(Unit)
        } catch (error: RuntimeException) {
            Result.failure(error)
        }
    }

    @Synchronized
    fun stop() {
        if (!active) return
        adapter?.let { runCatching { it.disableReaderMode(activity) } }
        active = false
    }

    private fun handleTag(tag: Tag) {
        try {
            val records = mutableListOf<NfcRecord>()
            var rawNdef: ByteArray? = null
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    ndef.ndefMessage?.let { message ->
                        rawNdef = message.toByteArray()
                        message.records.forEach { record ->
                            records += NfcRecord(
                                tnf = record.tnf,
                                typeHex = Hex.encode(record.type),
                                payloadHex = Hex.encode(record.payload)
                            )
                        }
                    }
                } finally {
                    runCatching { ndef.close() }
                }
            }
            onTag?.invoke(
                NfcReadResult(
                    idHex = Hex.encode(tag.id),
                    technologies = tag.techList.toList(),
                    records = records,
                    rawNdefHex = rawNdef?.let(Hex::encode)
                )
            )
        } catch (error: IOException) {
            onError?.invoke(error)
        } catch (error: RuntimeException) {
            onError?.invoke(error)
        }
    }
}
