package com.ev.ivy.speech

data class TranscriptionResult(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float
)

interface SpeechToTextProvider {
    val isAvailable: Boolean

    fun start(
        languageTag: String,
        preferOffline: Boolean,
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onResult: (TranscriptionResult) -> Unit,
        onError: (String) -> Unit,
        onEnd: () -> Unit
    )

    fun stop()
    fun cancel()
    fun release()
}
