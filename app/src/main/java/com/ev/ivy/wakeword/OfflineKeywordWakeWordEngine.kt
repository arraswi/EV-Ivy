package com.ev.ivy.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.ev.ivy.speech.VoiceActivityDetector

class OfflineKeywordWakeWordEngine(
    private val context: Context,
    private val languageTag: String = "pt-BR"
) : WakeWordEngine {

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private val vad = VoiceActivityDetector()

    private var running = false
    private var paused = false
    private var detected = false
    private var sensitivity = 0.6f
    private var callback: ((WakeDetection) -> Unit)? = null

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private val restartRunnable = Runnable { if (running && !paused && !detected) listen() }

    override fun start(sensitivity: Float, onDetected: (WakeDetection) -> Unit) {
        this.sensitivity = sensitivity
        this.callback = onDetected
        running = true
        paused = false
        detected = false
        main.post {
            ensureRecognizer()
            listen()
        }
    }

    private fun ensureRecognizer() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }
    }

    private fun silenceMs(): Long {
        val base = 900L
        return (base + (1f - sensitivity) * 1600f).toLong()
    }

    private fun listen() {
        if (!running || paused || detected || !isAvailable) return
        ensureRecognizer()
        vad.reset()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                silenceMs()
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        runCatching { recognizer?.startListening(intent) }
    }

    private fun scheduleRestart() {
        main.removeCallbacks(restartRunnable)
        main.postDelayed(restartRunnable, RESTART_DELAY_MS)
    }

    private fun handle(text: String?) {
        val detection = KeywordMatcher.find(text.orEmpty()) ?: return
        detected = true
        main.removeCallbacks(restartRunnable)
        runCatching { recognizer?.cancel() }
        callback?.invoke(detection)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) {
            vad.normalizeRms(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (KeywordMatcher.containsKeyword(text.orEmpty())) {
                handle(text)
            } else {
                scheduleRestart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (KeywordMatcher.containsKeyword(text.orEmpty())) {
                handle(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun pause() {
        main.post {
            paused = true
            detected = false
            main.removeCallbacks(restartRunnable)
            runCatching { recognizer?.cancel() }
        }
    }

    override fun resume() {
        main.post {
            if (!running) return@post
            paused = false
            detected = false
            listen()
        }
    }

    override fun stop() {
        main.post {
            running = false
            paused = false
            detected = false
            main.removeCallbacks(restartRunnable)
            runCatching { recognizer?.cancel() }
        }
    }

    override fun release() {
        main.post {
            running = false
            main.removeCallbacks(restartRunnable)
            runCatching { recognizer?.destroy() }
            recognizer = null
            callback = null
        }
    }

    private companion object {
        const val RESTART_DELAY_MS = 350L
    }
}
