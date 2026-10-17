package com.ev.ivy.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class OpenAIProvider(
    override val id: String,
    override val displayName: String,
    private val baseUrl: String,
    private val apiKey: String,
    private val http: HttpClient,
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val temperature: Float = 0.7f,
    private val maxTokens: Int = 512
) : AIProvider {

    private fun endpoint(path: String): String = baseUrl.trimEnd('/') + path

    private fun buildMessages(messages: List<ChatMessage>): JSONArray {
        val array = JSONArray()
        messages.forEach { m ->
            array.put(JSONObject().put("role", m.role).put("content", m.content))
        }
        return array
    }

    private fun newRequest(path: String, bodyJson: String?): Request {
        val builder = Request.Builder().url(endpoint(path))
        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
        if (apiKey.isNotBlank()) builder.addHeader("Authorization", "Bearer $apiKey")
        if (bodyJson != null) {
            builder.post(bodyJson.toRequestBody(JSON_MEDIA))
        } else {
            builder.get()
        }
        return builder.build()
    }

    override suspend fun generateResponse(
        messages: List<ChatMessage>,
        model: String
    ): AIResponse = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", model)
            .put("messages", buildMessages(messages))
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", false)
            .toString()

        http.client.newCall(newRequest("/chat/completions", body)).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${extractError(text)}")
            }
            val json = JSONObject(text)
            val content = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            AIResponse(content.trim(), json.optString("model", model))
        }
    }

    override fun streamResponse(messages: List<ChatMessage>, model: String): Flow<String> = flow {
        val body = JSONObject()
            .put("model", model)
            .put("messages", buildMessages(messages))
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", true)
            .toString()

        http.client.newCall(newRequest("/chat/completions", body)).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw IOException("HTTP ${response.code}: ${extractError(err)}")
            }
            val source = response.body?.source() ?: throw IOException("Resposta vazia")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty() || data == "[DONE]") continue
                val delta = try {
                    JSONObject(data).optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                } catch (_: Exception) {
                    ""
                }
                if (delta.isNotEmpty()) emit(delta)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        http.client.newCall(newRequest("/models", null)).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${extractError(text)}")
            }
            val data = JSONObject(text).optJSONArray("data") ?: return@use emptyList()
            (0 until data.length()).mapNotNull { data.optJSONObject(it)?.optString("id") }
                .filter { it.isNotBlank() }
                .sorted()
        }
    }

    override suspend fun testConnection(): Result<Int> = try {
        Result.success(listModels().size)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun extractError(body: String): String = try {
        JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(200)
    } catch (_: Exception) {
        body.take(200)
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
