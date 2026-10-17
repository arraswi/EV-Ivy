package com.ev.ivy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.components.CardSubtitle
import com.ev.ivy.ui.components.DropdownSelector
import com.ev.ivy.ui.components.GlassCard
import com.ev.ivy.ui.components.SectionTitle
import com.ev.ivy.ui.theme.EvWhite
import kotlinx.coroutines.launch

@Composable
fun VoiceSettingsScreen() {
    val container = LocalEvContainer.current
    val s by container.settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    var voices by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(s.languageTag) {
        container.textToSpeech.waitUntilReady(4000L)
        voices = container.textToSpeech.availableVoices(s.languageTag)
        status = if (voices.isEmpty()) {
            "Nenhuma voz local encontrada para ${s.languageTag}. Instale vozes nas configurações de texto-para-fala do Android."
        } else {
            "${voices.size} vozes disponíveis."
        }
    }

    Column(Modifier.fillMaxWidth()) {
        GlassCard {
            SectionTitle("SAÍDA DE VOZ")
            Spacer(Modifier.height(10.dp))
            val selected = s.ttsVoice ?: "Padrão do sistema"
            DropdownSelector(
                selectedLabel = selected,
                options = listOf<String?>(null) + voices,
                optionLabel = { it ?: "Padrão do sistema" },
                onSelect = { voice -> container.settings.update { it.copy(ttsVoice = voice) } }
            )
            Spacer(Modifier.height(10.dp))
            Text("Velocidade: ${"%.2f".format(s.speechRate)}x", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.speechRate,
                onValueChange = { value -> container.settings.update { it.copy(speechRate = value) } },
                valueRange = 0.6f..1.8f
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = s.languageTag,
                onValueChange = { value -> container.settings.update { it.copy(languageTag = value) } },
                label = { Text("Idioma (ex.: pt-BR)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Button(
                    onClick = {
                        scope.launch {
                            val ready = container.textToSpeech.isReady ||
                                container.textToSpeech.waitUntilReady(3000L)
                            if (!ready) {
                                status = "Síntese de voz indisponível."
                                return@launch
                            }
                            container.textToSpeech.speak(
                                text = "Olá, eu sou a E.V. Estou pronta para ajudar.",
                                languageTag = s.languageTag,
                                voiceName = s.ttsVoice,
                                rate = s.speechRate,
                                onStart = {},
                                onDone = {},
                                onError = { status = it }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Testar voz") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = { container.textToSpeech.stop() },
                    modifier = Modifier.weight(1f)
                ) { Text("Parar") }
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                CardSubtitle(status)
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("RECONHECIMENTO DE FALA")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Preferir reconhecimento offline", color = EvWhite, fontSize = 14.sp)
                    CardSubtitle("Usa o motor local do Android quando disponível.")
                }
                Switch(
                    checked = s.preferOfflineStt,
                    onCheckedChange = { value -> container.settings.update { it.copy(preferOfflineStt = value) } }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sensibilidade da palavra-chave", color = EvWhite, fontSize = 14.sp)
                    CardSubtitle("Ajusta o tempo de silêncio para detectar \u201cIvy\u201d.")
                }
            }
            Slider(
                value = s.wakeSensitivity,
                onValueChange = { value -> container.settings.update { it.copy(wakeSensitivity = value) } },
                valueRange = 0.2f..1f
            )
        }
    }
}
