package com.ev.ivy

import android.app.Application
import android.content.Context
import com.ev.ivy.ai.AIProviderManager
import com.ev.ivy.assistant.AssistantController
import com.ev.ivy.assistant.LocalCommandRouter
import com.ev.ivy.core.security.SecureStore
import com.ev.ivy.data.local.ConversationStore
import com.ev.ivy.data.settings.SettingsRepository
import com.ev.ivy.overlay.IvyOverlayManager
import com.ev.ivy.speech.AndroidSpeechToText
import com.ev.ivy.speech.AndroidTextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EvApp : Application() {
    lateinit var container: EvContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = EvContainer(this)
    }
}

class EvContainer(context: Context) {

    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val secureStore = SecureStore(appContext)
    val settings = SettingsRepository(appContext, secureStore)
    val conversations = ConversationStore(appContext)
    val aiProviders = AIProviderManager(appContext, settings)
    val speechToText = AndroidSpeechToText(appContext)
    val textToSpeech = AndroidTextToSpeech(appContext)
    val localCommands = LocalCommandRouter(appContext)

    val assistant = AssistantController(
        context = appContext,
        settings = settings,
        conversations = conversations,
        aiProviders = aiProviders,
        speechToText = speechToText,
        textToSpeech = textToSpeech,
        commands = localCommands,
        scope = scope
    )

    val overlay = IvyOverlayManager(appContext, assistant, settings, scope)
}
