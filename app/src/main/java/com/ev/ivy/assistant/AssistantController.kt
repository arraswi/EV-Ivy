package com.ev.ivy.assistant

import android.content.Context
import com.ev.ivy.ai.AIProviderManager
import com.ev.ivy.ai.ChatMessage
import com.ev.ivy.data.local.ConversationEntry
import com.ev.ivy.data.local.ConversationStore
import com.ev.ivy.data.settings.SettingsRepository
import com.ev.ivy.speech.SpeechToTextProvider
import com.ev.ivy.speech.TextToSpeechProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.sin

class AssistantController(
    private val context: Context,
    private val settings: SettingsRepository,
    private val conversations: ConversationStore,
    private val aiProviders: AIProviderManager,
    private val speechToText: SpeechToTextProvider,
    private val textToSpeech: TextToSpeechProvider,
    private val commands: LocalCommandRouter,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _userText = MutableStateFlow("")
    val userText: StateFlow<String> = _userText.asStateFlow()

    private val _assistantText = MutableStateFlow("")
    val assistantText: StateFlow<String> = _assistantText.asStateFlow()

    private var sessionJob: Job? = null
    private var amplitudeJob: Job? = null
    private var lastCommand: String? = null

    val isActive: Boolean get() = _state.value !is AssistantState.Idle

    fun beginSession(initialCommand: String? = null) {
        if (_state.value !is AssistantState.Idle) return
        sessionJob = scope.launch { runSession(initialCommand) }
    }

    fun submitText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        lastCommand = trimmed
        beginSession(trimmed)
    }

    fun retry() {
        val command = lastCommand ?: return
        if (_state.value !is AssistantState.Idle) return
        sessionJob = scope.launch { runSession(command) }
    }

    fun cancelSession() {
        speechToText.cancel()
        textToSpeech.stop()
        sessionJob?.cancel()
        sessionJob = null
        stopSyntheticAmplitude()
        _amplitude.value = 0f
        _state.value = AssistantState.Idle
    }

    fun dispose() {
        cancelSession()
        speechToText.release()
        textToSpeech.release()
    }

    private suspend fun runSession(initial: String?) {
        _state.value = AssistantState.Activating
        _userText.value = ""
        _assistantText.value = ""
        _amplitude.value = 0f
        delay(ACTIVATION_MS)
        try {
            val command = if (!initial.isNullOrBlank()) initial else captureCommand()
            if (command.isBlank()) {
                dismiss()
                return
            }
            lastCommand = command
            process(command)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            _state.value = AssistantState.Error(e.message ?: "Ocorreu um erro inesperado.")
        } finally {
            stopSyntheticAmplitude()
        }
    }

    private suspend fun captureCommand(): String {
        _state.value = AssistantState.Listening
        val deferred = CompletableDeferred<String>()
        var lastPartial = ""
        var finished = false
        speechToText.start(
            languageTag = settings.state.value.languageTag,
            preferOffline = settings.state.value.preferOfflineStt,
            onPartial = {
                lastPartial = it
                _userText.value = it
                _state.value = AssistantState.Transcribing
            },
            onAmplitude = { _amplitude.value = it },
            onResult = { result ->
                if (result.text.isNotBlank()) lastPartial = result.text
                if (result.isFinal && !finished) {
                    finished = true
                    deferred.complete(result.text.ifBlank { lastPartial })
                }
            },
            onError = {
                if (!finished) {
                    finished = true
                    deferred.complete(lastPartial)
                }
            },
            onEnd = {
                if (!finished) {
                    finished = true
                    deferred.complete(lastPartial)
                }
            }
        )
        val result = withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { deferred.await() } ?: lastPartial
        if (!finished) speechToText.cancel()
        _amplitude.value = 0f
        return result.trim()
    }

    private suspend fun process(command: String) {
        _userText.value = command
        if (settings.state.value.keepHistory) {
            conversations.append(ConversationEntry("user", command, System.currentTimeMillis()))
        }
        _state.value = AssistantState.Thinking
        val reply = when (val local = commands.route(command)) {
            is CommandResult.Handled -> local.reply
            is CommandResult.Failed -> local.reply
            CommandResult.NotHandled -> askAi(command)
        }
        if (reply.isBlank()) {
            _state.value = AssistantState.Error("Não obtive uma resposta.")
            return
        }
        _assistantText.value = reply
        if (settings.state.value.keepHistory) {
            conversations.append(
                ConversationEntry("assistant", reply, System.currentTimeMillis())
            )
        }
        speakAndWait(reply)
        dismiss()
    }

    private suspend fun askAi(command: String): String {
        val configError = aiProviders.validateConfiguration()
        if (configError != null) return "Ainda não estou configurada. $configError"
        val s = settings.state.value
        val messages = buildMessages(command)
        return try {
            if (s.streaming) {
                val builder = StringBuilder()
                aiProviders.streamResponse(messages, s.model).collect { chunk ->
                    builder.append(chunk)
                    _assistantText.value = builder.toString()
                }
                builder.toString().ifBlank { "Não recebi conteúdo do modelo." }
            } else {
                aiProviders.currentProvider().generateResponse(messages, s.model).text
            }
        } catch (e: Exception) {
            "Tive um problema ao consultar o modelo: ${e.message ?: "erro desconhecido"}."
        }
    }

    private fun buildMessages(command: String): List<ChatMessage> {
        val s = settings.state.value
        val list = mutableListOf(ChatMessage(ChatMessage.SYSTEM, s.systemPrompt))
        conversations.history.value.takeLast(MAX_HISTORY).forEach { entry ->
            list.add(ChatMessage(entry.role, entry.text))
        }
        if (list.lastOrNull()?.let { it.role == ChatMessage.USER && it.content == command } != true) {
            list.add(ChatMessage(ChatMessage.USER, command))
        }
        return list
    }

    private suspend fun speakAndWait(text: String) {
        _state.value = AssistantState.Speaking
        startSyntheticAmplitude()
        val ready = textToSpeech.isReady || textToSpeech.waitUntilReady(2500L)
        if (!ready) {
            stopSyntheticAmplitude()
            return
        }
        val done = CompletableDeferred<Unit>()
        val s = settings.state.value
        val ok = textToSpeech.speak(
            text = text,
            languageTag = s.languageTag,
            voiceName = s.ttsVoice,
            rate = s.speechRate,
            onStart = {},
            onDone = { done.complete(Unit) },
            onError = { done.complete(Unit) }
        )
        if (!ok) {
            stopSyntheticAmplitude()
            return
        }
        val timeout = max(5000L, text.length * 130L).coerceAtMost(90000L)
        withTimeoutOrNull(timeout) { done.await() }
        stopSyntheticAmplitude()
    }

    private suspend fun dismiss() {
        _state.value = AssistantState.Dismissing
        delay(DISMISS_MS)
        _amplitude.value = 0f
        _state.value = AssistantState.Idle
    }

    private fun startSyntheticAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            var t = 0f
            while (isActive) {
                t += 0.22f
                _amplitude.value = (0.4f + 0.35f * sin(t)).coerceIn(0f, 1f)
                delay(70)
            }
        }
    }

    private fun stopSyntheticAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    private companion object {
        const val ACTIVATION_MS = 220L
        const val DISMISS_MS = 320L
        const val CAPTURE_TIMEOUT_MS = 15000L
        const val MAX_HISTORY = 12
    }
}
