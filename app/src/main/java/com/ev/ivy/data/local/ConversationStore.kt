package com.ev.ivy.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ConversationEntry(
    val role: String,
    val text: String,
    val timestamp: Long
)

class ConversationStore(context: Context) {

    private val file = File(context.filesDir, "conversations.json")
    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<ConversationEntry>> = _history.asStateFlow()

    private fun load(): List<ConversationEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                ConversationEntry(
                    role = o.optString("role"),
                    text = o.optString("text"),
                    timestamp = o.optLong("timestamp")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<ConversationEntry>) {
        try {
            val array = JSONArray()
            list.forEach { e ->
                array.put(
                    JSONObject()
                        .put("role", e.role)
                        .put("text", e.text)
                        .put("timestamp", e.timestamp)
                )
            }
            file.writeText(array.toString())
        } catch (_: Exception) {
        }
    }

    fun append(entry: ConversationEntry) {
        val next = (_history.value + entry).takeLast(MAX_ENTRIES)
        _history.value = next
        save(next)
    }

    fun clear() {
        _history.value = emptyList()
        if (file.exists()) file.delete()
    }

    private companion object {
        const val MAX_ENTRIES = 200
    }
}
