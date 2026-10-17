package com.ev.ivy.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ev.ivy.ui.components.ScreenContainer
import com.ev.ivy.ui.screens.AiSettingsScreen
import com.ev.ivy.ui.screens.AppearanceScreen
import com.ev.ivy.ui.screens.HomeScreen
import com.ev.ivy.ui.screens.PrivacyScreen
import com.ev.ivy.ui.screens.VoiceSettingsScreen

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            ScreenContainer(title = "E.V", subtitle = "IVY / PERSONAL INTELLIGENCE") {
                HomeScreen(
                    onOpenAi = { nav.navigate("ai") },
                    onOpenVoice = { nav.navigate("voice") },
                    onOpenAppearance = { nav.navigate("appearance") },
                    onOpenPrivacy = { nav.navigate("privacy") }
                )
            }
        }
        composable("ai") {
            ScreenContainer(title = "Inteligência artificial", onBack = { nav.popBackStack() }) {
                AiSettingsScreen()
            }
        }
        composable("voice") {
            ScreenContainer(title = "Voz", onBack = { nav.popBackStack() }) {
                VoiceSettingsScreen()
            }
        }
        composable("appearance") {
            ScreenContainer(title = "Aparência", onBack = { nav.popBackStack() }) {
                AppearanceScreen()
            }
        }
        composable("privacy") {
            ScreenContainer(title = "Privacidade", onBack = { nav.popBackStack() }) {
                PrivacyScreen()
            }
        }
    }
}
