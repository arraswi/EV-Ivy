package com.ev.ivy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.components.CardSubtitle
import com.ev.ivy.ui.components.GlassCard
import com.ev.ivy.ui.components.SectionTitle
import com.ev.ivy.ui.theme.EvWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyScreen() {
    val container = LocalEvContainer.current
    val history by container.conversations.history.collectAsState()
    val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))

    Column(Modifier.fillMaxWidth()) {
        GlassCard {
            SectionTitle("PRIVACIDADE")
            Spacer(Modifier.height(10.dp))
            CardSubtitle(
                "A detecção da palavra-chave \u201cIvy\u201d acontece no dispositivo. " +
                    "Gravações contínuas não são enviadas a servidores. " +
                    "Texto e áudio só são enviados ao provedor de IA quando você faz uma pergunta que exige o modelo."
            )
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("HISTÓRICO LOCAL")
            Spacer(Modifier.height(10.dp))
            CardSubtitle("${history.size} itens armazenados no dispositivo.")
            Spacer(Modifier.height(10.dp))
            if (history.isEmpty()) {
                CardSubtitle("Nenhuma conversa registrada.")
            } else {
                history.takeLast(15).reversed().forEach { entry ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = if (entry.role == "user") "Você" else "E.V",
                            color = EvWhite.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Text(entry.text, color = EvWhite, fontSize = 14.sp, maxLines = 3)
                        Text(
                            formatter.format(Date(entry.timestamp)),
                            color = EvWhite.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                OutlinedButton(
                    onClick = { container.conversations.clear() },
                    modifier = Modifier.weight(1f)
                ) { Text("Apagar histórico") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = {
                        container.conversations.clear()
                        container.settings.setApiKey(container.settings.state.value.providerId, "")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Apagar chave") }
            }
        }
    }
}
