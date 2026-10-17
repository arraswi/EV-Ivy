package com.ev.ivy.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ev.ivy.EvApp
import com.ev.ivy.EvContainer
import com.ev.ivy.MainActivity
import com.ev.ivy.assistant.AssistantState
import com.ev.ivy.wakeword.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IvyWakeService : Service() {

    private lateinit var container: EvContainer
    private lateinit var wakeManager: WakeWordManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as EvApp).container
        wakeManager = WakeWordManager(this)
        createChannel()
        startForegroundCompat()
        container.overlay.attach()
        observeAssistant()
        startWakeIfPossible()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startWakeIfPossible() {
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            updateNotification("Permissão de microfone necessária para ouvir \u201cIvy\u201d.")
            return
        }
        if (!wakeManager.isAvailable) {
            updateNotification("Reconhecimento de fala indisponível no dispositivo.")
            return
        }
        wakeManager.start(container.settings.state.value.wakeSensitivity) { detection ->
            container.assistant.beginSession(detection.remainder.ifBlank { null })
        }
        updateNotification("Aguardando a palavra-chave \u201cIvy\u201d.")
    }

    private fun observeAssistant() {
        observeJob = serviceScope.launch {
            container.assistant.state.collect { state ->
                if (state is AssistantState.Idle) wakeManager.resume() else wakeManager.pause()
            }
        }
    }

    private fun startForegroundCompat() {
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && micGranted) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification("Iniciando E.V..."), type)
        } catch (_: Exception) {
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assistente E.V",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Mantém a E.V ouvindo a palavra-chave." }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("E.V \u2014 IVY")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        observeJob?.cancel()
        wakeManager.release()
        container.overlay.detach()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL_ID = "ev_ivy_wake"
        const val NOTIF_ID = 4711
    }
}
