package com.ev.ivy.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MathEvaluatorTest {

    @Test
    fun multiplication() {
        assertEquals(1200.0, MathEvaluator.evaluate("25*48")!!, 0.0001)
    }

    @Test
    fun respectsOperatorPrecedence() {
        assertEquals(14.0, MathEvaluator.evaluate("2+3*4")!!, 0.0001)
    }

    @Test
    fun supportsParentheses() {
        assertEquals(20.0, MathEvaluator.evaluate("(2+3)*4")!!, 0.0001)
    }

    @Test
    fun division() {
        assertEquals(2.5, MathEvaluator.evaluate("10/4")!!, 0.0001)
    }

    @Test
    fun invalidExpressionReturnsNull() {
        assertNull(MathEvaluator.evaluate("2+"))
        assertNull(MathEvaluator.evaluate("10/0"))
    }
}
