package com.android.hardwaretoolkit.core

import android.content.Context
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class SessionEvent(
    val timestamp: String = Instant.now().toString(),
    val providerId: String,
    val module: String,
    val action: String,
    val result: String
)

class SessionStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, "sessions.json")
    @Synchronized fun append(event: SessionEvent) {
        val a = loadArray()
        a.put(JSONObject().apply {
            put("timestamp", event.timestamp)
            put("providerId", event.providerId)
            put("module", event.module)
            put("action", event.action)
            put("result", event.result)
        })
        file.writeText(a.toString())
    }
    @Synchronized fun readAll(): List<SessionEvent> {
        val a = loadArray()
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            SessionEvent(o.getString("timestamp"), o.getString("providerId"),
                o.getString("module"), o.getString("action"), o.getString("result"))
        }
    }
    @Synchronized fun clear() { file.delete() }
    fun exportJson(): String = loadArray().toString(2)
    private fun loadArray(): JSONArray =
        if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() } else JSONArray()
}
