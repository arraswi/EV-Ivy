package com.ev.ivy

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ivy.overlay.IvyOverlayService
import com.ev.ivy.ui.IvyOrb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var permitted by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            val blue = Color(0xFF69A6FF)
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF080B13), surface = Color(0xFF141B2A), primary = blue)) {
                Column(Modifier.fillMaxSize().background(Color(0xFF080B13)).verticalScroll(rememberScrollState()).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(40.dp))
                    Text("E.V", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("IVY  /  PERSONAL INTELLIGENCE", fontSize = 11.sp, letterSpacing = 2.sp, color = blue)
                    Spacer(Modifier.height(36.dp))
                    IvyOrb(Modifier.size(230.dp), speaking = false)
                    Spacer(Modifier.height(20.dp))
                    Text("Sua assistente. Seu espaço.", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Uma presença inteligente que estará com você em qualquer tela.", color = Color(0xFF9BA9C0), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Column(Modifier.fillMaxWidth().background(Color(0xFF141B2A), RoundedCornerShape(22.dp)).padding(20.dp)) {
                        Text("VISUALIZAÇÃO", fontSize = 12.sp, color = blue, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Overlay flutuante", fontSize = 19.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Abra uma prévia da E.V sobre outros aplicativos.", color = Color(0xFF9BA9C0))
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = {
                            if (!Settings.canDrawOverlays(this@MainActivity)) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            } else {
                                startService(Intent(this@MainActivity, IvyOverlayService::class.java))
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (permitted) "Abrir E.V" else "Permitir sobreposição") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { stopService(Intent(this@MainActivity, IvyOverlayService::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Fechar overlay") }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("PROTÓTIPO VISUAL  •  SEM ESCUTA ATIVA", fontSize = 10.sp, color = Color(0xFF7C8BA3), letterSpacing = 1.sp)
                }
            }
            LaunchedEffect(Unit) { permitted = Settings.canDrawOverlays(this@MainActivity) }
        }
    }
    override fun onResume() { super.onResume() }
}
