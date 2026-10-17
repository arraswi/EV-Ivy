package com.ev.ivy.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AndroidSpeechToText(private val context: Context) : SpeechToTextProvider {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var callbacks: Callbacks? = null
    private val vad = VoiceActivityDetector()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private data class Callbacks(
        val onPartial: (String) -> Unit,
        val onAmplitude: (Float) -> Unit,
        val onResult: (TranscriptionResult) -> Unit,
        val onError: (String) -> Unit,
        val onEnd: () -> Unit
    )

    override fun start(
        languageTag: String,
        preferOffline: Boolean,
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onResult: (TranscriptionResult) -> Unit,
        onError: (String) -> Unit,
        onEnd: () -> Unit
    ) {
        callbacks = Callbacks(onPartial, onAmplitude, onResult, onError, onEnd)
        vad.reset()
        mainHandler.post { startInternal(languageTag, preferOffline) }
    }

    private fun startInternal(languageTag: String, preferOffline: Boolean) {
        if (!isAvailable) {
            callbacks?.onError?.invoke("Reconhecimento de fala indisponível no dispositivo.")
            callbacks?.onEnd?.invoke()
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (preferOffline) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            callbacks?.onError?.invoke(e.message ?: "Falha ao iniciar o reconhecimento.")
            callbacks?.onEnd?.invoke()
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) {
            callbacks?.onAmplitude?.invoke(vad.normalizeRms(rmsdB))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            callbacks?.onError?.invoke(mapError(error))
            callbacks?.onEnd?.invoke()
        }

        override fun onResults(results: Bundle?) {
            val text = bestResult(results).orEmpty()
            callbacks?.onResult?.invoke(TranscriptionResult(text, true, confidence(results)))
            callbacks?.onEnd?.invoke()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = bestResult(partialResults).orEmpty()
            if (text.isNotBlank()) {
                callbacks?.onPartial?.invoke(text)
                callbacks?.onResult?.invoke(TranscriptionResult(text, false, 0f))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun bestResult(bundle: Bundle?): String? {
        val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return list?.firstOrNull()
    }

    private fun confidence(bundle: Bundle?): Float {
        val array = bundle?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES) ?: return 0f
        return array.firstOrNull() ?: 0f
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio."
        SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente de reconhecimento."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone ausente."
        SpeechRecognizer.ERROR_NETWORK -> "Falha de rede no reconhecimento."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tempo de rede esgotado."
        SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi, pode repetir?"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor ocupado."
        SpeechRecognizer.ERROR_SERVER -> "Erro do servidor de reconhecimento."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada."
        else -> "Erro de reconhecimento ($error)."
    }

    override fun stop() {
        mainHandler.post { runCatching { recognizer?.stopListening() } }
    }

    override fun cancel() {
        mainHandler.post { runCatching { recognizer?.cancel() } }
        callbacks = null
    }

    override fun release() {
        mainHandler.post {
            runCatching { recognizer?.destroy() }
            recognizer = null
            callbacks = null
        }
    }

    fun preferredLocale(tag: String): Locale = Locale.forLanguageTag(tag)
}
