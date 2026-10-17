package com.ev.ivy.wakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    @Test
    fun detectsKeywordAndExtractsRemainder() {
        val detection = KeywordMatcher.find("Ivy, abre o YouTube")
        assertEquals("abre o youtube", detection?.remainder)
    }

    @Test
    fun detectsUppercaseAndAccents() {
        assertTrue(KeywordMatcher.containsKeyword("IVY"))
        assertTrue(KeywordMatcher.containsKeyword("ívy"))
    }

    @Test
    fun detectsKeywordAlone() {
        val detection = KeywordMatcher.find("E.V")
        assertTrue(detection != null)
    }

    @Test
    fun doesNotMatchInsideOtherWords() {
        assertFalse(KeywordMatcher.containsKeyword("ivysaurus"))
        assertFalse(KeywordMatcher.containsKeyword("levar o carro"))
    }

    @Test
    fun ignoresUnrelatedSpeech() {
        assertNull(KeywordMatcher.find("bom dia pessoal tudo bem"))
    }
}
