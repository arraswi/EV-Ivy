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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.ai.AiPresets
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.components.CardSubtitle
import com.ev.ivy.ui.components.DropdownSelector
import com.ev.ivy.ui.components.GlassCard
import com.ev.ivy.ui.components.SectionTitle
import com.ev.ivy.ui.theme.EvWhite
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen() {
    val container = LocalEvContainer.current
    val s by container.settings.state.collectAsState()
    val scope = rememberCoroutineScope()

    val preset = AiPresets.byId(s.providerId)
    var apiKeyInput by remember(s.providerId) {
        mutableStateOf(container.settings.apiKey(s.providerId).orEmpty())
    }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        GlassCard {
            SectionTitle("PROVEDOR")
            Spacer(Modifier.height(10.dp))
            DropdownSelector(
                selectedLabel = preset.name,
                options = AiPresets.all,
                optionLabel = { it.name },
                onSelect = { chosen ->
                    container.settings.update {
                        it.copy(
                            providerId = chosen.id,
                            baseUrl = chosen.baseUrl.ifBlank { it.baseUrl },
                            model = chosen.defaultModel.ifBlank { it.model }
                        )
                    }
                    models = emptyList()
                    status = ""
                }
            )
            Spacer(Modifier.height(8.dp))
            CardSubtitle(preset.docsHint)
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("CONEXÃO")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.baseUrl,
                onValueChange = { value -> container.settings.update { it.copy(baseUrl = value) } },
                label = { Text("Base URL (…/v1)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Button(
                    onClick = {
                        container.settings.setApiKey(s.providerId, apiKeyInput.trim())
                        status = "Chave salva com segurança."
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Salvar chave") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Testando conexão..."
                            val result = container.aiProviders.testConnection()
                            status = result.fold(
                                onSuccess = { "Conectado. $it modelos disponíveis." },
                                onFailure = { "Falha: ${it.message}" }
                            )
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !busy
                ) { Text("Testar") }
            }
            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                CardSubtitle(status)
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("MODELO")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.model,
                onValueChange = { value -> container.settings.update { it.copy(model = value) } },
                label = { Text("Modelo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Carregando modelos..."
                        container.aiProviders.listModels().fold(
                            onSuccess = {
                                models = it
                                status = "${it.size} modelos carregados."
                            },
                            onFailure = { status = "Falha: ${it.message}" }
                        )
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) { Text("Carregar modelos disponíveis") }
            if (models.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                DropdownSelector(
                    selectedLabel = s.model.ifBlank { "Selecione um modelo" },
                    options = models,
                    optionLabel = { it },
                    onSelect = { chosen -> container.settings.update { it.copy(model = chosen) } }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("PARÂMETROS")
            Spacer(Modifier.height(10.dp))
            Text("Temperatura: ${"%.2f".format(s.temperature)}", color = EvWhite, fontSize = 13.sp)
            Slider(
                value = s.temperature,
                onValueChange = { value -> container.settings.update { it.copy(temperature = value) } },
                valueRange = 0f..2f
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = s.maxTokens.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { tokens ->
                        container.settings.update { it.copy(maxTokens = tokens.coerceIn(64, 4096)) }
                    }
                },
                label = { Text("Máximo de tokens") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Resposta em streaming", color = EvWhite, fontSize = 14.sp)
                    CardSubtitle("Mostra a resposta conforme é gerada.")
                }
                Switch(
                    checked = s.streaming,
                    onCheckedChange = { value -> container.settings.update { it.copy(streaming = value) } }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("PERSONALIDADE")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.systemPrompt,
                onValueChange = { value -> container.settings.update { it.copy(systemPrompt = value) } },
                label = { Text("Instruções do sistema") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6
            )
        }
    }
}
