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
    private var cached: JSONArray? = null

    @Synchronized
    fun append(event: SessionEvent) {
        val array = loadArray()
        array.put(
            JSONObject().apply {
                put("timestamp", event.timestamp)
                put("providerId", event.providerId)
                put("module", event.module)
                put("action", event.action)
                put("result", event.result)
            }
        )
        persist(array)
    }

    @Synchronized
    fun readAll(): List<SessionEvent> {
        val array = loadArray()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val objectValue = array.getJSONObject(i)
                add(
                    SessionEvent(
                        timestamp = objectValue.getString("timestamp"),
                        providerId = objectValue.getString("providerId"),
                        module = objectValue.getString("module"),
                        action = objectValue.getString("action"),
                        result = objectValue.getString("result")
                    )
                )
            }
        }
    }

    @Synchronized
    fun clear() {
        cached = JSONArray()
        if (file.exists()) file.delete()
    }

    @Synchronized
    fun exportJson(): String = loadArray().toString(2)

    private fun loadArray(): JSONArray {
        cached?.let { return it }
        cached = if (file.exists()) {
            runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() }
        } else {
            JSONArray()
        }
        return cached!!
    }

    private fun persist(array: JSONArray) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(array.toString())
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IllegalStateException("Unable to atomically persist session data")
        }
        cached = array
    }
}
