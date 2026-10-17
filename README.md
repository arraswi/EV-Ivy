# E.V / IVY — Android UI starter

Projeto nativo Kotlin + Jetpack Compose. Inclui tela inicial e overlay demonstrativo, sem microfone, wake word, IA ou TTS nesta etapa.

## Abrir
1. Abra a pasta `EV-Ivy-Android` no Android Studio (versão recente com AGP 8.9.2 e JDK 17).
2. Sincronize o Gradle e execute o módulo `app` num Android 8+.
3. Toque em **Permitir sobreposição**, autorize nas configurações, volte ao app e toque em **Abrir E.V**.
4. Toque no X do overlay para fechá-lo.

Não inclui Gradle Wrapper (`gradlew` e `gradle-wrapper.jar`); o Android Studio pode sincronizar com o Gradle configurado. Para adicionar wrapper posteriormente, execute `gradle wrapper --gradle-version 8.11.1` com Gradle instalado.

**Limites:** sem reconhecimento de voz, sem ativação em background, sem fala; apenas visual. Não foi compilado neste ambiente (Android SDK/Gradle não disponíveis).
