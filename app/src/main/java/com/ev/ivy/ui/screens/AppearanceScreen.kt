package com.ev.ivy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.components.CardSubtitle
import com.ev.ivy.ui.components.GlassCard
import com.ev.ivy.ui.components.SectionTitle
import com.ev.ivy.ui.theme.EvWhite

private val accentChoices = listOf(
    0xFF0A63FF, 0xFF438BFF, 0xFF00C2FF, 0xFF7C5CFF, 0xFF00E0B8, 0xFFFF5C8A
)

@Composable
fun AppearanceScreen() {
    val container = LocalEvContainer.current
    val s by container.settings.state.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        GlassCard {
            SectionTitle("LIQUID GLASS")
            Spacer(Modifier.height(10.dp))
            Text("Desfoque de fundo: ${s.blurRadiusDp} dp", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.blurRadiusDp.toFloat(),
                onValueChange = { value -> container.settings.update { it.copy(blurRadiusDp = value.toInt()) } },
                valueRange = 0f..60f
            )
            CardSubtitle("Aplicado quando o Android suporta desfoque de janela (Android 12+).")
            Spacer(Modifier.height(12.dp))
            Text("Transparência do vidro: ${(s.glassOpacity * 100).toInt()}%", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.glassOpacity,
                onValueChange = { value -> container.settings.update { it.copy(glassOpacity = value) } },
                valueRange = 0.10f..0.80f
            )
            Spacer(Modifier.height(12.dp))
            Text("Largura da cápsula: ${(s.capsuleWidthFraction * 100).toInt()}%", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.capsuleWidthFraction,
                onValueChange = { value -> container.settings.update { it.copy(capsuleWidthFraction = value) } },
                valueRange = 0.6f..1f
            )
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("COR DE DESTAQUE")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                accentChoices.forEach { value ->
                    val selected = s.accentColor == value
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(value))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) EvWhite else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { container.settings.update { it.copy(accentColor = value) } }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("COMPORTAMENTO")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Animações", color = EvWhite, fontSize = 14.sp)
                    CardSubtitle("Transições suaves ao abrir e fechar.")
                }
                Switch(
                    checked = s.animationsEnabled,
                    onCheckedChange = { value -> container.settings.update { it.copy(animationsEnabled = value) } }
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Fechar automaticamente: ${s.autoDismissMs / 1000}s", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.autoDismissMs.toFloat(),
                onValueChange = { value -> container.settings.update { it.copy(autoDismissMs = value.toLong()) } },
                valueRange = 3000f..20000f
            )
        }
    }
}
