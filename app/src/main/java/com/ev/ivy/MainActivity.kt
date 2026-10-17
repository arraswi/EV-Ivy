package com.ev.ivy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.ev.ivy.ui.AppNavigation
import com.ev.ivy.ui.LocalEvContainer
import com.ev.ivy.ui.theme.EvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EvApp).container
        setContent {
            EvTheme {
                CompositionLocalProvider(LocalEvContainer provides container) {
                    AppNavigation()
                }
            }
        }
    }
}
