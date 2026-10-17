package com.ev.ivy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.ev.ivy.EvContainer

val LocalEvContainer = staticCompositionLocalOf<EvContainer> {
    error("EvContainer não fornecido")
}
