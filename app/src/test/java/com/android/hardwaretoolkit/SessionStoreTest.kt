package com.android.hardwaretoolkit

import com.android.hardwaretoolkit.core.SessionEvent
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class SessionStoreTest {
    private fun event(module: String = "ble", action: String = "scan", result: String = "ok") =
        SessionEvent(
            timestamp = "2026-01-01T00:00:00Z",
            providerId = "phone:ble",
            module = module,
            action = action,
            result = result
        )

    @Test
    fun sessionEventJsonRoundTrip() {
        val source = event()
        val json = JSONArray()
            .put(
                org.json.JSONObject().apply {
                    put("timestamp", source.timestamp)
                    put("providerId", source.providerId)
                    put("module", source.module)
                    put("action", source.action)
                    put("result", source.result)
                }
            )
        val value = json.getJSONObject(0)
        assertEquals(source.providerId, value.getString("providerId"))
        assertEquals(source.module, value.getString("module"))
        assertEquals(source.action, value.getString("action"))
        assertEquals(source.result, value.getString("result"))
        assertEquals(source.timestamp, value.getString("timestamp"))
    }

    @Test
    fun eventDefaultsTimestampToInstantNow() {
        val created = SessionEvent(providerId = "p", module = "m", action = "a", result = "r")
        assertTrue(created.timestamp.isNotBlank())
        java.time.Instant.parse(created.timestamp)
    }
}
