package com.ev.ivy.wakeword

import java.text.Normalizer

data class WakeDetection(val keyword: String, val remainder: String)

interface WakeWordEngine {
    val isAvailable: Boolean
    fun start(sensitivity: Float, onDetected: (WakeDetection) -> Unit)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}

object KeywordMatcher {

    private val variants = listOf("ivy", "i v", "iv", "e v", "ev", "ei vi")

    fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    fun normalize(input: String): String =
        stripAccents(input.lowercase())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun containsKeyword(input: String): Boolean = find(input) != null

    fun find(input: String): WakeDetection? {
        val normalized = normalize(input)
        if (normalized.isEmpty()) return null
        for (variant in variants) {
            val index = normalized.indexOf(variant)
            if (index < 0) continue
            val beforeOk = index == 0 || normalized[index - 1] == ' '
            val end = index + variant.length
            val afterOk = end >= normalized.length || normalized[end] == ' '
            if (beforeOk && afterOk) {
                val remainder = normalized.substring(end).trim()
                return WakeDetection(variant, remainder)
            }
        }
        return null
    }
}
