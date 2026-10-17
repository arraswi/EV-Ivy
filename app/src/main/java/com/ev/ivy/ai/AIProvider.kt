package com.ev.ivy.ai

import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String) {
    companion object {
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}

data class AIResponse(val text: String, val model: String)

interface AIProvider {
    val id: String
    val displayName: String

    suspend fun generateResponse(messages: List<ChatMessage>, model: String): AIResponse
    fun streamResponse(messages: List<ChatMessage>, model: String): Flow<String>
    suspend fun listModels(): List<String>
    suspend fun testConnection(): Result<Int>
}

data class ProviderPreset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val requiresKey: Boolean,
    val docsHint: String
)

object AiPresets {
    val openAi = ProviderPreset(
        id = "openai",
        name = "OpenAI / ChatGPT",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        requiresKey = true,
        docsHint = "Use uma API Key oficial da OpenAI. Assinatura do ChatGPT não inclui acesso à API."
    )
    val openRouter = ProviderPreset(
        id = "openrouter",
        name = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openai/gpt-4o-mini",
        requiresKey = true,
        docsHint = "Agregador compatível com o formato OpenAI. Informe a chave do OpenRouter."
    )
    val openCode = ProviderPreset(
        id = "opencode",
        name = "OpenCode (servidor compatível)",
        baseUrl = "",
        defaultModel = "",
        requiresKey = false,
        docsHint = "Aponte para um servidor OpenCode compatível com a API OpenAI (base URL que você controla)."
    )
    val custom = ProviderPreset(
        id = "custom",
        name = "Compatível com OpenAI (personalizado)",
        baseUrl = "",
        defaultModel = "",
        requiresKey = false,
        docsHint = "Qualquer endpoint /v1/chat/completions compatível com OpenAI."
    )

    val all = listOf(openAi, openRouter, openCode, custom)

    fun byId(id: String): ProviderPreset = all.firstOrNull { it.id == id } ?: openAi
}

class HttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
