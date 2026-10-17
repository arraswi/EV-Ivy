package com.ev.ivy.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.ev.ivy.core.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context, private val secureStore: SecureStore) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_ivy_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            assistantEnabled = prefs.getBoolean("assistantEnabled", d.assistantEnabled),
            wakeEnabled = prefs.getBoolean("wakeEnabled", d.wakeEnabled),
            wakeSensitivity = prefs.getFloat("wakeSensitivity", d.wakeSensitivity),
            providerId = prefs.getString("providerId", d.providerId) ?: d.providerId,
            model = prefs.getString("model", d.model) ?: d.model,
            baseUrl = prefs.getString("baseUrl", d.baseUrl) ?: d.baseUrl,
            streaming = prefs.getBoolean("streaming", d.streaming),
            temperature = prefs.getFloat("temperature", d.temperature),
            maxTokens = prefs.getInt("maxTokens", d.maxTokens),
            systemPrompt = prefs.getString("systemPrompt", d.systemPrompt) ?: d.systemPrompt,
            ttsVoice = prefs.getString("ttsVoice", null),
            speechRate = prefs.getFloat("speechRate", d.speechRate),
            languageTag = prefs.getString("languageTag", d.languageTag) ?: d.languageTag,
            preferOfflineStt = prefs.getBoolean("preferOfflineStt", d.preferOfflineStt),
            blurRadiusDp = prefs.getInt("blurRadiusDp", d.blurRadiusDp),
            glassOpacity = prefs.getFloat("glassOpacity", d.glassOpacity),
            accentColor = prefs.getLong("accentColor", d.accentColor),
            capsuleWidthFraction = prefs.getFloat(
                "capsuleWidthFraction", d.capsuleWidthFraction
            ),
            animationsEnabled = prefs.getBoolean("animationsEnabled", d.animationsEnabled),
            keepHistory = prefs.getBoolean("keepHistory", d.keepHistory),
            autoDismissMs = prefs.getLong("autoDismissMs", d.autoDismissMs),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        persist(next)
        _state.value = next
    }

    private fun persist(s: AppSettings) {
        prefs.edit()
            .putBoolean("assistantEnabled", s.assistantEnabled)
            .putBoolean("wakeEnabled", s.wakeEnabled)
            .putFloat("wakeSensitivity", s.wakeSensitivity)
            .putString("providerId", s.providerId)
            .putString("model", s.model)
            .putString("baseUrl", s.baseUrl)
            .putBoolean("streaming", s.streaming)
            .putFloat("temperature", s.temperature)
            .putInt("maxTokens", s.maxTokens)
            .putString("systemPrompt", s.systemPrompt)
            .putString("ttsVoice", s.ttsVoice)
            .putFloat("speechRate", s.speechRate)
            .putString("languageTag", s.languageTag)
            .putBoolean("preferOfflineStt", s.preferOfflineStt)
            .putInt("blurRadiusDp", s.blurRadiusDp)
            .putFloat("glassOpacity", s.glassOpacity)
            .putLong("accentColor", s.accentColor)
            .putFloat("capsuleWidthFraction", s.capsuleWidthFraction)
            .putBoolean("animationsEnabled", s.animationsEnabled)
            .putBoolean("keepHistory", s.keepHistory)
            .putLong("autoDismissMs", s.autoDismissMs)
            .apply()
    }

    fun apiKey(providerId: String): String? = secureStore.get("api_key_$providerId")

    fun setApiKey(providerId: String, key: String) {
        if (key.isBlank()) secureStore.remove("api_key_$providerId")
        else secureStore.put("api_key_$providerId", key)
    }

    fun hasApiKey(providerId: String): Boolean = !apiKey(providerId).isNullOrBlank()
}
