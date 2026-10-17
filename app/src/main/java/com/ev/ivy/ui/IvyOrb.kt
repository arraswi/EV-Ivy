package com.ev.ivy.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

@Composable
fun IvyOrb(modifier: Modifier = Modifier, speaking: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "ivy-orb")
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(if (speaking) 1800 else 4200, easing = LinearEasing)), label = "phase")
    Canvas(modifier) {
        val r = size.minDimension * 0.32f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFF142847), r * 1.25f, center)
        drawCircle(Color(0xFF336DB8).copy(alpha = .35f), r * 1.08f, center, style = Stroke(2f))
        for (i in 0 until 96) {
            val angle = i * (2 * PI / 96)
            val wave = sin(i * .27 + phase * 2).toFloat()
            val height = (8f + 13f * abs(wave)) * if (speaking) 1.7f else 1f
            val a = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            val b = Offset(center.x + cos(angle).toFloat() * (r + height), center.y + sin(angle).toFloat() * (r + height))
            drawLine(Color(0xFF72C7FF).copy(alpha = .65f + .3f * abs(wave)), a, b, 2.4f, StrokeCap.Round)
        }
        drawCircle(Color(0xFF88D9FF), 7f + 2f * sin(phase), center)
        drawCircle(Color(0xFF86C9FF).copy(alpha = .45f), r * .52f, center, style = Stroke(1.6f))
        drawCircle(Color(0xFF86C9FF).copy(alpha = .22f), r * .83f, center, style = Stroke(1.4f))
    }
}
