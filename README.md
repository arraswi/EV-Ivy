# E.V / IVY — Assistente Android (Kotlin + Jetpack Compose)

Assistente pessoal Android com ativação por voz local, overlay Liquid Glass, IA
configurável por API Key e voz feminina. Arquitetura local-first, otimizada e
modular.

## Recursos

- **Ativação por voz local** — detecta "Ivy" / "E.V" no dispositivo, sem enviar
  o microfone para servidores. A detecção usa reconhecimento de fala offline do
  Android como *keyword spotting*. É um mecanismo real, mas reconhece a palavra
  ao nível de transcrição: pode gerar falsos positivos e consome mais bateria do
  que um modelo dedicado. A interface `WakeWordEngine` permite plugar um modelo
  próprio (ex.: Porcupine/openWakeWord com modelo treinado para "Ivy") sem
  alterar o restante do app.
- **Overlay Liquid Glass** — cápsula flutuante sobre outros apps com desfoque de
  fundo (`setBlurBehindRadius`, Android 12+), transparência dinâmica, bordas
  arredondadas, reflexos e animações spring. Sem desfoque suportado, aplica
  superfície translúcida elegante como fallback.
- **Globo digital animado** — visualizador em Compose Canvas que reage à
  amplitude real do microfone (`onRmsChanged`) durante a escuta e a uma
  envoltória durante a fala.
- **IA real e configurável** — OpenAI (ChatGPT/API oficial), OpenRouter,
  OpenCode e qualquer endpoint compatível com OpenAI (`/v1/chat/completions`),
  com streaming via SSE, listagem de modelos e teste de conexão.
- **Voz feminina natural** — Android TextToSpeech, preferência por pt-BR, escolha
  de voz, velocidade e teste. Voz feminina depende do que estiver instalado no
  aparelho; o app lista as vozes disponíveis e orienta a instalação quando falta.
- **Comandos locais** — hora, data, cálculos, abrir apps, abrir sites e
  configurações, resolvidos no dispositivo sem consultar a IA quando possível.
- **Privacidade** — API Keys protegidas com Android Keystore (AES/GCM), histórico
  local removível, sem rastreamento.

## Arquitetura

```
com/ev/ivy/
├── EvApp.kt                     Application + container (DI manual)
├── MainActivity.kt
├── core/security/SecureStore.kt Keystore AES/GCM
├── data/settings/               AppSettings + SettingsRepository (StateFlow)
├── data/local/ConversationStore.kt
├── ai/                          AIProvider, OpenAIProvider (compatível), Manager
├── speech/                      STT, TTS, VAD (interfaces + Android)
├── wakeword/                    WakeWordEngine + OfflineKeyword engine + matcher
├── assistant/                   AssistantState, AssistantController, LocalCommandRouter
├── overlay/                     IvyOverlayManager, IvyGlassOverlay, visualizador
├── service/IvyWakeService.kt    Foreground service (microfone) + overlay
└── ui/                          tema, navegação e telas
```

Máquina de estados: `Idle → Activating → Listening → Transcribing → Thinking →
Speaking → Dismissing → Idle`, com `Error`. Transições conflitantes (duas
cápsulas, escuta + fala simultâneas, perguntas duplicadas) são prevenidas no
`AssistantController` e no gerenciamento do `WakeWordManager`.

## Permissões

`RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`
e `FOREGROUND_SERVICE_MICROPHONE`. `QUERY_ALL_PACKAGES` é usada apenas para
resolver íntegros de apps instalados para os comandos locais.

## Limitações reais

- Wake word via reconhecimento de fala é um fallback; para detecção robusta e de
  baixo consumo é necessário um modelo dedicado (interface pronta para isso).
- Desfoque de fundo depende do suporte do dispositivo/Android 12+.
- A validação de áudio (STT/TTS/microfone) precisa ser feita em um dispositivo
  físico; o CI apenas compila e roda testes unitários.
- OpenCode: use um servidor compatível com OpenAI que você mesmo controla
  (informe a Base URL). Nenhum endpoint é presumido.

## Build

O APK é gerado pelo GitHub Actions (`.github/workflows/android.yml`) em cada push
e em tags `v*` (release). Localmente: `./gradlew assembleDebug` com JDK 17 e
Android SDK 35.
