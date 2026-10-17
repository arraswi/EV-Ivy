package com.ev.ivy.data.settings

data class AppSettings(
    val assistantEnabled: Boolean = false,
    val wakeEnabled: Boolean = false,
    val wakeSensitivity: Float = 0.6f,
    val providerId: String = "openai",
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "https://api.openai.com/v1",
    val streaming: Boolean = true,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val ttsVoice: String? = null,
    val speechRate: Float = 1.0f,
    val languageTag: String = "pt-BR",
    val preferOfflineStt: Boolean = true,
    val blurRadiusDp: Int = 26,
    val glassOpacity: Float = 0.32f,
    val accentColor: Long = 0xFF0A63FF,
    val capsuleWidthFraction: Float = 0.92f,
    val animationsEnabled: Boolean = true,
    val keepHistory: Boolean = true,
    val autoDismissMs: Long = 9000L,
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Você é a E.V (pronunciada Ivy), uma assistente pessoal objetiva, calorosa e " +
                "prestativa. Responda em português do Brasil, de forma natural e concisa. " +
                "Evite emojis. Quando não souber algo, diga claramente."
    }
}
