package com.ev.ivy.speech

interface TextToSpeechProvider {
    val isReady: Boolean
    suspend fun waitUntilReady(timeoutMs: Long = 5000L): Boolean

    fun speak(
        text: String,
        languageTag: String,
        voiceName: String?,
        rate: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean

    fun stop()
    fun availableVoices(languageTag: String): List<String>
    fun release()
}
