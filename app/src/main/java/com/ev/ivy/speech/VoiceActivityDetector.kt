package com.ev.ivy.speech

class VoiceActivityDetector(private val speechThreshold: Float = 0.14f) {

    private var speechStarted = false
    private var lastSpeechAt = 0L

    fun normalizeRms(rmsDb: Float): Float {
        val value = (rmsDb + 2f) / 12f
        return value.coerceIn(0f, 1f)
    }

    fun update(normalizedRms: Float, nowMs: Long): Boolean {
        if (normalizedRms >= speechThreshold) {
            speechStarted = true
            lastSpeechAt = nowMs
        }
        return speechStarted
    }

    fun isSilence(normalizedRms: Float, nowMs: Long, silenceMs: Long): Boolean =
        speechStarted && normalizedRms < speechThreshold && (nowMs - lastSpeechAt) > silenceMs

    fun reset() {
        speechStarted = false
        lastSpeechAt = 0L
    }
}
