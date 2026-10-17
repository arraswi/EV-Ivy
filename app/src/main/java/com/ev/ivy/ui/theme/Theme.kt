package com.ev.ivy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun EvTheme(
    accent: Color = EvBlue,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = EvWhite,
        secondary = EvBlueSecondary,
        background = EvBackground,
        onBackground = EvWhite,
        surface = EvSurface,
        onSurface = EvWhite,
        surfaceVariant = EvSurfaceVariant,
        onSurfaceVariant = EvGray
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
