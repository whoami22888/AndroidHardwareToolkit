package com.android.hardwaretoolkit.core

import android.content.Context
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

class SessionStore(context: Context) {
    private val file = File(context.filesDir, "sessions.json")
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
                val value = array.getJSONObject(i)
                add(
                    SessionEvent(
                        timestamp = value.getString("timestamp"),
                        providerId = value.getString("providerId"),
                        module = value.getString("module"),
                        action = value.getString("action"),
                        result = value.getString("result")
                    )
                )
            }
        }
    }

    @Synchronized
    fun clear() {
        cached = JSONArray()
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Unable to delete session store")
        }
    }

    @Synchronized
    fun exportJson(): String = loadArray().toString(2)

    private fun loadArray(): JSONArray {
        cached?.let { return it }

        if (!file.exists()) {
            return JSONArray().also { cached = it }
        }

        return try {
            JSONArray(file.readText()).also { cached = it }
        } catch (error: Exception) {
            throw IllegalStateException("Session store is corrupt: ${file.absolutePath}", error)
        }
    }

    private fun persist(array: JSONArray) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        try {
            temporary.writeText(array.toString())
            try {
                Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            cached = array
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                throw IllegalStateException("Unable to remove temporary session store")
            }
        }
    }
}
