package com.ev.ivy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IvyOrb(
    modifier: Modifier = Modifier,
    speaking: Boolean = false,
    amplitude: Float = 0f,
    active: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "ivy-orb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (speaking) 1800 else 5200, easing = LinearEasing)
        ),
        label = "phase"
    )
    val amp = amplitude.coerceIn(0f, 1f)
    Canvas(modifier) {
        val r = size.minDimension * 0.32f
        val center = Offset(size.width / 2f, size.height / 2f)
        val glow = 1f + amp * 0.35f
        drawCircle(Color(0xFF142847), r * 1.25f * glow, center)
        drawCircle(Color(0xFF336DB8).copy(alpha = .35f), r * 1.08f * glow, center, style = Stroke(2f))
        val rays = 96
        for (i in 0 until rays) {
            val angle = i * (2 * PI / rays)
            val wave = sin(i * .27 + phase * 2).toFloat()
            val height = (8f + 13f * abs(wave)) * (if (speaking) 1.7f else 1f) * (1f + amp)
            val a = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            val b = Offset(
                center.x + cos(angle).toFloat() * (r + height),
                center.y + sin(angle).toFloat() * (r + height)
            )
            drawLine(
                Color(0xFF72C7FF).copy(alpha = .65f + .3f * abs(wave)),
                a, b, 2.4f, StrokeCap.Round
            )
        }
        drawCircle(Color(0xFF88D9FF), 7f + 2f * sin(phase) + amp * 6f, center)
        drawCircle(Color(0xFF86C9FF).copy(alpha = .45f), r * .52f, center, style = Stroke(1.6f))
        drawCircle(Color(0xFF86C9FF).copy(alpha = .22f), r * .83f, center, style = Stroke(1.4f))
        if (active) {
            drawCircle(
                Color(0xFF6FA8FF).copy(alpha = .10f + amp * .25f),
                r * (1.35f + amp * .35f),
                center
            )
        }
    }
}
