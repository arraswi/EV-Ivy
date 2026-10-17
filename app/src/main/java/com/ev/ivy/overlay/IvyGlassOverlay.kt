package com.ev.ivy.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.assistant.AssistantController
import com.ev.ivy.assistant.AssistantState
import com.ev.ivy.data.settings.AppSettings
import com.ev.ivy.data.settings.SettingsRepository
import com.ev.ivy.ui.IvyOrb
import com.ev.ivy.ui.theme.EvBlue
import com.ev.ivy.ui.theme.EvWhite

@Composable
fun IvyGlassOverlay(
    controller: AssistantController,
    settings: SettingsRepository,
    s: AppSettings
) {
    val state by controller.state.collectAsState()
    val amplitude by controller.amplitude.collectAsState()
    val userText by controller.userText.collectAsState()
    val assistantText by controller.assistantText.collectAsState()

    val accent = Color(s.accentColor)
    val visible = state !is AssistantState.Idle

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            exit = fadeOut() + scaleOut(targetScale = 0.88f)
        ) {
            Capsule(
                state = state,
                amplitude = amplitude,
                userText = userText,
                assistantText = assistantText,
                accent = accent,
                glassOpacity = s.glassOpacity,
                widthFraction = s.capsuleWidthFraction,
                onCancel = { controller.cancelSession() },
                onRetry = { controller.retry() }
            )
        }
    }
}

@Composable
private fun Capsule(
    state: AssistantState,
    amplitude: Float,
    userText: String,
    assistantText: String,
    accent: Color,
    glassOpacity: Float,
    widthFraction: Float,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val shape = RoundedCornerShape(34.dp)
    val speaking = state is AssistantState.Speaking
    val active = state is AssistantState.Listening ||
        state is AssistantState.Transcribing ||
        state is AssistantState.Thinking ||
        speaking
    val expanded = assistantText.isNotBlank() && state !is AssistantState.Error

    val borderBrush = Brush.linearGradient(
        listOf(
            Color(0x66FFFFFF),
            Color(0x11FFFFFF),
            accent.copy(alpha = 0.45f),
            Color(0x55FFFFFF)
        )
    )
    val glassBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF141A28).copy(alpha = (glassOpacity + 0.34f).coerceAtMost(0.92f)),
            Color(0xFF090C13).copy(alpha = (glassOpacity + 0.20f).coerceAtMost(0.9f))
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .clip(shape)
            .background(glassBrush)
            .border(1.dp, borderBrush, shape)
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IvyOrb(
                modifier = Modifier.size(46.dp),
                speaking = speaking,
                amplitude = amplitude,
                active = active
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E.V  /  IVY",
                    color = EvWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = statusLabel(state, userText),
                    color = statusColor(state),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            IvyVoiceVisualizer(
                amplitude = amplitude,
                modifier = Modifier.size(width = 40.dp, height = 30.dp)
            )
            Spacer(Modifier.width(6.dp))
            if (state is AssistantState.Error) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Tentar novamente", tint = accent)
                }
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = EvWhite)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = assistantText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                fontSize = 14.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun statusLabel(state: AssistantState, userText: String): String = when (state) {
    is AssistantState.Activating -> "Acordando..."
    is AssistantState.Listening -> "Estou ouvindo..."
    is AssistantState.Transcribing -> userText.ifBlank { "Transcrevendo..." }
    is AssistantState.Thinking -> "Pensando..."
    is AssistantState.Speaking -> "Respondendo..."
    is AssistantState.Dismissing -> "Até logo."
    is AssistantState.Error -> state.message
    AssistantState.Idle -> ""
}

private fun statusColor(state: AssistantState): Color = when (state) {
    is AssistantState.Error -> Color(0xFFFF8A8A)
    AssistantState.Idle -> EvBlue
    else -> EvWhite.copy(alpha = 0.92f)
}
