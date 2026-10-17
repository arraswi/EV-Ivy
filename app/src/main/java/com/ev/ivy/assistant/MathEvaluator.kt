package com.ev.ivy.assistant

internal object MathEvaluator {

    fun evaluate(expression: String): Double? = try {
        val parser = Parser(expression)
        val value = parser.parseExpression()
        if (parser.position == expression.length && value.isFinite()) value else null
    } catch (_: Exception) {
        null
    }

    private class Parser(private val text: String) {
        var position = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (position < text.length) {
                when (text[position]) {
                    '+' -> { position++; value += parseTerm() }
                    '-' -> { position++; value -= parseTerm() }
                    else -> break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (position < text.length) {
                when (text[position]) {
                    '*' -> { position++; value *= parseFactor() }
                    '/' -> {
                        position++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("divisão por zero")
                        value /= divisor
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            if (position >= text.length) throw IllegalArgumentException("fim inesperado")
            val c = text[position]
            if (c == '+') { position++; return parseFactor() }
            if (c == '-') { position++; return -parseFactor() }
            if (c == '(') {
                position++
                val value = parseExpression()
                if (position >= text.length || text[position] != ')') {
                    throw IllegalArgumentException("parêntese não fechado")
                }
                position++
                return value
            }
            val start = position
            while (position < text.length && (text[position].isDigit() || text[position] == '.')) {
                position++
            }
            if (start == position) throw IllegalArgumentException("número esperado")
            return text.substring(start, position).toDouble()
        }
    }
}
