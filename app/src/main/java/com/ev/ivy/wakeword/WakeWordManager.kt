package com.ev.ivy.wakeword

import android.content.Context

class WakeWordManager(context: Context) {

    private val engine: WakeWordEngine = OfflineKeywordWakeWordEngine(context)
    private var started = false

    val isAvailable: Boolean get() = engine.isAvailable

    fun start(sensitivity: Float, onDetected: (WakeDetection) -> Unit) {
        started = true
        engine.start(sensitivity, onDetected)
    }

    fun pause() {
        if (started) engine.pause()
    }

    fun resume() {
        if (started) engine.resume()
    }

    fun stop() {
        started = false
        engine.stop()
    }

    fun release() {
        started = false
        engine.release()
    }
}
