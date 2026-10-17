package com.ev.ivy.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ev.ivy.assistant.AssistantState
import com.ev.ivy.service.IvyWakeService
import com.ev.ivy.ui.IvyOrb
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.components.CardSubtitle
import com.ev.ivy.ui.components.CardTitle
import com.ev.ivy.ui.components.GlassCard
import com.ev.ivy.ui.components.SectionTitle
import com.ev.ivy.ui.theme.EvBlue
import com.ev.ivy.ui.theme.EvWhite

@Composable
fun HomeScreen(onOpenAi: () -> Unit, onOpenVoice: () -> Unit, onOpenAppearance: () -> Unit, onOpenPrivacy: () -> Unit) {
    val container = LocalEvContainer.current
    val context = LocalContext.current
    val settingsState by container.settings.state.collectAsState()
    val assistantState by container.assistant.state.collectAsState()
    val amplitude by container.assistant.amplitude.collectAsState()

    var hasMic by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }
    var hasNotif by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMic = hasPermission(context, Manifest.permission.RECORD_AUDIO)
                hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                hasOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasMic = it }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasNotif = it }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlay = Settings.canDrawOverlays(context) }

    fun startAssistant() {
        if (!hasMic || !hasOverlay) return
        container.settings.update { it.copy(assistantEnabled = true, wakeEnabled = true) }
        ContextCompat.startForegroundService(
            context,
            Intent(context, IvyWakeService::class.java)
        )
    }

    fun stopAssistant() {
        container.settings.update { it.copy(assistantEnabled = false, wakeEnabled = false) }
        context.stopService(Intent(context, IvyWakeService::class.java))
    }

    var testText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IvyOrb(
                modifier = Modifier.size(190.dp),
                speaking = assistantState is AssistantState.Speaking,
                amplitude = amplitude,
                active = assistantState !is AssistantState.Idle
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Sua assistente. Seu espaço.",
            color = EvWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        CardSubtitle("Diga \u201cIvy\u201d a qualquer momento para começar.")
        Spacer(Modifier.height(20.dp))

        GlassCard {
            SectionTitle("ASSISTENTE")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CardTitle(if (settingsState.assistantEnabled) "Ativa" else "Desativada")
                    CardSubtitle(
                        if (settingsState.assistantEnabled) "Ouvindo a palavra-chave localmente."
                        else "Ative para ouvir \u201cIvy\u201d sobre outros apps."
                    )
                }
                Switch(
                    checked = settingsState.assistantEnabled,
                    onCheckedChange = { checked -> if (checked) startAssistant() else stopAssistant() }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(hasMic)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasMic) "Microfone concedido" else "Microfone não concedido",
                    color = EvWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!hasMic) {
                    OutlinedButton(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Permitir")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(hasOverlay)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasOverlay) "Sobreposição concedida" else "Sobreposição não concedida",
                    color = EvWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!hasOverlay) {
                    OutlinedButton(onClick = {
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }) { Text("Permitir") }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotif) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(false)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Notificações desativadas",
                        color = EvWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Permitir") }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard {
            SectionTitle("TESTE")
            Spacer(Modifier.height(8.dp))
            CardSubtitle("Escreva um comando ou pergunta para testar a resposta.")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex.: quanto é 25 vezes 48") },
                singleLine = false,
                maxLines = 3
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Button(
                    onClick = {
                        container.assistant.submitText(testText)
                        testText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Enviar") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = {
                        container.assistant.beginSession(null)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Ouvir agora") }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavTile("Inteligência", Modifier.weight(1f), onOpenAi)
            NavTile("Voz", Modifier.weight(1f), onOpenVoice)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavTile("Aparência", Modifier.weight(1f), onOpenAppearance)
            NavTile("Privacidade", Modifier.weight(1f), onOpenPrivacy)
        }
    }
}

@Composable
private fun StatusDot(granted: Boolean) {
    Box(
        Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .then(
                Modifier.background(
                    if (granted) Color(0xFF3DDC84) else Color(0xFFFF6B6B)
                )
            )
    )
}

@Composable
private fun NavTile(label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(label, color = EvBlue, fontWeight = FontWeight.SemiBold)
    }
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
