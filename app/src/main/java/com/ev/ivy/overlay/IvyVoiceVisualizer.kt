package com.ev.ivy.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun IvyVoiceVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier,
    bars: Int = 5,
    activeColor: Color = Color(0xFF72C7FF),
    idleColor: Color = Color(0xFF3A4A66)
) {
    val amp = amplitude.coerceIn(0f, 1f)
    Canvas(modifier) {
        val gap = size.width / (bars * 2f)
        val barWidth = gap
        val centerY = size.height / 2f
        for (i in 0 until bars) {
            val distance = kotlin.math.abs(i - (bars - 1) / 2f)
            val weight = 1f - distance / (bars.toFloat())
            val base = size.height * 0.14f
            val dynamic = size.height * 0.42f * amp * (0.5f + weight)
            val barHeight = (base + dynamic).coerceAtMost(size.height)
            val x = i * 2 * gap + gap
            val color = if (amp > 0.05f) activeColor else idleColor
            drawRoundRect(
                color = color.copy(alpha = 0.55f + 0.45f * amp),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = barWidth * 0.22f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
