package com.ev.ivy.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

class AndroidTextToSpeech(private val context: Context) : TextToSpeechProvider {

    private var tts: TextToSpeech? = null
    private val readySignal = CompletableDeferred<Boolean>()
    private var pendingVoice: String? = null
    private var pendingRate: Float = 1.0f
    private var pendingLocale: Locale = Locale.forLanguageTag("pt-BR")

    @Volatile
    override var isReady: Boolean = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            val ok = status == TextToSpeech.SUCCESS
            isReady = ok
            if (ok) {
                tts?.setOnUtteranceProgressListener(progressListener)
                applyLocale(pendingLocale)
            }
            readySignal.complete(ok)
        }
    }

    override suspend fun waitUntilReady(timeoutMs: Long): Boolean = try {
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { readySignal.await() } ?: false
    } catch (_: Exception) {
        false
    }

    private fun applyLocale(locale: Locale): Boolean {
        val engine = tts ?: return false
        val result = engine.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun applyVoice(voiceName: String?) {
        val engine = tts ?: return
        if (voiceName.isNullOrBlank()) return
        engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.voice = it }
    }

    override fun speak(
        text: String,
        languageTag: String,
        voiceName: String?,
        rate: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        val engine = tts ?: run {
            onError("Síntese de voz indisponível.")
            return false
        }
        pendingLocale = Locale.forLanguageTag(languageTag)
        pendingVoice = voiceName
        pendingRate = rate
        applyLocale(pendingLocale)
        applyVoice(voiceName)
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

        utteranceStart = onStart
        utteranceDone = onDone
        utteranceError = onError

        val id = "ev-" + System.currentTimeMillis()
        val params = Bundle()
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
        if (result == TextToSpeech.ERROR) {
            onError("Falha ao sintetizar a fala.")
            return false
        }
        return true
    }

    private var utteranceStart: (() -> Unit)? = null
    private var utteranceDone: (() -> Unit)? = null
    private var utteranceError: ((String) -> Unit)? = null

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            utteranceStart?.invoke()
        }

        override fun onDone(utteranceId: String?) {
            utteranceDone?.invoke()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceError?.invoke("Erro na reprodução de voz.")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceError?.invoke("Erro na reprodução de voz ($errorCode).")
        }
    }

    override fun stop() {
        runCatching { tts?.stop() }
    }

    override fun availableVoices(languageTag: String): List<String> {
        val engine = tts ?: return emptyList()
        val language = Locale.forLanguageTag(languageTag).language
        return engine.voices
            ?.filter { it.locale.language == language && !it.isNetworkConnectionRequired }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun availableVoicesIncludingNetwork(languageTag: String): List<Voice> {
        val engine = tts ?: return emptyList()
        val language = Locale.forLanguageTag(languageTag).language
        return engine.voices?.filter { it.locale.language == language }?.sortedBy { it.name }
            ?: emptyList()
    }

    override fun release() {
        runCatching { tts?.shutdown() }
        tts = null
        isReady = false
    }
}
