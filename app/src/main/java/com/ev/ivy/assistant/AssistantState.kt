package com.ev.ivy.assistant

sealed interface AssistantState {
    data object Idle : AssistantState
    data object Activating : AssistantState
    data object Listening : AssistantState
    data object Transcribing : AssistantState
    data object Thinking : AssistantState
    data object Speaking : AssistantState
    data object Dismissing : AssistantState
    data class Error(val message: String) : AssistantState
}
