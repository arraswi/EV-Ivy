package com.ev.ivy.ai

import android.content.Context
import com.ev.ivy.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow

class AIProviderManager(
    private val context: Context,
    private val settings: SettingsRepository,
    private val http: HttpClient = HttpClient()
) {

    fun currentProvider(): AIProvider {
        val s = settings.state.value
        val preset = AiPresets.byId(s.providerId)
        val baseUrl = s.baseUrl.ifBlank { preset.baseUrl }
        val key = settings.apiKey(s.providerId).orEmpty()
        val headers = if (s.providerId == AiPresets.openRouter.id) {
            mapOf(
                "HTTP-Referer" to "https://github.com/arraswi/EV-Ivy",
                "X-Title" to "E.V Ivy"
            )
        } else {
            emptyMap()
        }
        return OpenAIProvider(
            id = s.providerId,
            displayName = preset.name,
            baseUrl = baseUrl,
            apiKey = key,
            http = http,
            extraHeaders = headers,
            temperature = s.temperature,
            maxTokens = s.maxTokens
        )
    }

    fun streamResponse(messages: List<ChatMessage>, model: String): Flow<String> =
        currentProvider().streamResponse(messages, model)

    suspend fun listModels(): Result<List<String>> = try {
        Result.success(currentProvider().listModels())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun testConnection(): Result<Int> = currentProvider().testConnection()

    fun validateConfiguration(): String? {
        val s = settings.state.value
        val preset = AiPresets.byId(s.providerId)
        val baseUrl = s.baseUrl.ifBlank { preset.baseUrl }
        if (baseUrl.isBlank()) return "Informe uma URL base compatível com OpenAI."
        if (preset.requiresKey && !settings.hasApiKey(s.providerId)) {
            return "Informe a API Key para ${preset.name}."
        }
        if (s.model.isBlank()) return "Selecione um modelo."
        return null
    }
}
