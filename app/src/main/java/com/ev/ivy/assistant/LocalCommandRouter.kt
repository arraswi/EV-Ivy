package com.ev.ivy.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

sealed interface CommandResult {
    data class Handled(val reply: String) : CommandResult
    data class Failed(val reply: String) : CommandResult
    data object NotHandled : CommandResult
}

class LocalCommandRouter(private val context: Context) {

    private val ptBr = Locale.forLanguageTag("pt-BR")

    suspend fun route(input: String): CommandResult {
        val raw = input.trim()
        if (raw.isBlank()) return CommandResult.NotHandled
        val normalized = normalize(raw)

        return mathCommand(normalized)
            ?: timeCommand(normalized)
            ?: dateCommand(normalized)
            ?: settingsCommand(normalized)
            ?: appCommand(normalized)
            ?: webCommand(raw, normalized)
            ?: CommandResult.NotHandled
    }

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "").trim()
    }

    private fun timeCommand(normalized: String): CommandResult? {
        if (!(normalized.contains("que horas") || normalized.contains("horas sao") ||
                normalized.contains("hora atual") || normalized == "horas")
        ) return null
        val time = SimpleDateFormat("HH:mm", ptBr).format(Date())
        return CommandResult.Handled("Agora são $time.")
    }

    private fun dateCommand(normalized: String): CommandResult? {
        if (!(normalized.contains("que dia") || normalized.contains("data de hoje") ||
                normalized.contains("dia de hoje") || normalized == "hoje")
        ) return null
        val date = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", ptBr).format(Date())
        return CommandResult.Handled("Hoje é $date.")
    }

    private fun settingsCommand(normalized: String): CommandResult? {
        if (!(normalized.contains("configuracoes") || normalized.contains("configuracao") ||
                normalized.contains("ajustes") || normalized.contains("settings"))
        ) return null
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launch(intent)) CommandResult.Handled("Abrindo as configurações.")
        else CommandResult.Failed("Não consegui abrir as configurações.")
    }

    private fun appCommand(normalized: String): CommandResult? {
        val triggers = listOf(
            "abrir o", "abrir a", "abre o", "abre a", "abri o", "abri a",
            "iniciar o", "iniciar a", "inicia o", "inicia a",
            "lancar o", "lancar a", "executar o", "executar a", "abrir", "abre", "abri"
        )
        val trigger = triggers.firstOrNull { normalized.contains(it) } ?: return null
        var target = normalized.substringAfter(trigger).trim()
        target = target.removePrefix("app ").removePrefix("aplicativo ")
            .removePrefix("o ").removePrefix("a ").removePrefix("meu ").removePrefix("minha ")
            .trim()
        if (target.isBlank()) return null

        val alias = ALIASES[target]
        val resolved = resolveApp(target, alias) ?: return CommandResult.NotHandled
        val (label, intent) = resolved
        return if (launch(intent)) CommandResult.Handled("Abrindo o $label.")
        else CommandResult.Failed("Não consegui abrir o $label.")
    }

    private fun resolveApp(target: String, alias: String?): Pair<String, Intent>? {
        val pm = context.packageManager
        if (alias != null) {
            val intent = pm.getLaunchIntentForPackage(alias)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(alias, 0)).toString()
                }.getOrDefault(target)
                return label to intent
            }
        }
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activities = pm.queryIntentActivities(launchIntent, 0)
        var best: Pair<String, Intent>? = null
        var bestScore = 0
        for (info in activities) {
            val label = info.loadLabel(pm).toString()
            val normalizedLabel = normalize(label)
            val score = when {
                normalizedLabel == target -> 100
                normalizedLabel.startsWith(target) -> 80
                normalizedLabel.contains(target) -> 60
                else -> 0
            }
            if (score > bestScore) {
                bestScore = score
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(info.activityInfo.packageName, info.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                best = label to intent
            }
        }
        return best
    }

    private fun webCommand(raw: String, normalized: String): CommandResult? {
        val url = extractUrl(raw)
        if (url != null) return openUrl(url, raw)

        val searchTriggers = listOf("pesquisa", "pesquisar", "procura", "procurar", "busca", "buscar")
        if (searchTriggers.any { normalized.contains(it) } && normalized.contains("google")) {
            val query = normalized.substringAfterLast("google").trim().ifBlank {
                normalized.substringAfter(searchTriggers.first { normalized.contains(it) }).trim()
            }
            if (query.isNotBlank()) {
                val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                return if (launch(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    CommandResult.Handled("Pesquisando por $query.")
                else CommandResult.Failed("Não consegui abrir o navegador.")
            }
        }

        val openSiteTriggers = listOf("abrir site", "abre site", "abrir o site", "abre o site",
            "abrir pagina", "abre pagina", "abrir link", "abre link")
        val siteTrigger = openSiteTriggers.firstOrNull { normalized.contains(it) }
        if (siteTrigger != null) {
            val site = normalized.substringAfter(siteTrigger).trim().replace(" ", "")
            if (site.isNotBlank()) {
                val target = if (site.contains(".")) "https://$site" else "https://www.$site.com"
                return openUrl(target, raw)
            }
        }
        return null
    }

    private fun extractUrl(raw: String): String? {
        val regex = Regex("(https?://\\S+|www\\.\\S+|\\b[a-z0-9-]+\\.(com|net|org|io|br|dev|app)(\\.br)?(/\\S*)?)")
        val match = regex.find(raw.lowercase()) ?: return null
        val found = match.value
        return if (found.startsWith("http")) found else "https://$found"
    }

    private fun openUrl(url: String, raw: String): CommandResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launch(intent)) CommandResult.Handled("Abrindo $url.")
        else CommandResult.Failed("Não consegui abrir o link.")
    }

    private fun mathCommand(normalized: String): CommandResult? {
        val replaced = replaceOperators(normalized)
        val expr = extractExpression(replaced) ?: return null
        val value = MathEvaluator.evaluate(expr) ?: return null
        return CommandResult.Handled("O resultado é ${formatNumber(value)}.")
    }

    private fun replaceOperators(text: String): String {
        var s = text
        val replacements = listOf(
            "dividido por" to "/",
            "dividida por" to "/",
            "dividir por" to "/",
            "multiplicado por" to "*",
            "multiplicada por" to "*",
            "multiplicar por" to "*",
            "somado a" to "+",
            "somada a" to "+",
            "vezes" to "*",
            "dividido" to "/",
            "subtraido" to "-",
            "subtrair" to "-",
            "menos" to "-",
            "mais" to "+"
        )
        replacements.forEach { (word, symbol) -> s = s.replace(word, " $symbol ") }
        return s
    }

    private fun extractExpression(text: String): String? {
        val allowed = text.replace(Regex("[^0-9+\\-*/.() ]"), "")
        val compact = allowed.replace(" ", "")
        if (compact.isEmpty()) return null
        if (compact.none { it.isDigit() }) return null
        if (compact.none { it in "+-*/" }) return null
        if (!(compact.first().isDigit() || compact.first() == '(')) return null
        return compact
    }

    private fun formatNumber(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(ptBr)
        return if (value == floor(value) && !value.isInfinite()) {
            formatter.maximumFractionDigits = 0
            formatter.format(value)
        } else {
            formatter.maximumFractionDigits = 4
            formatter.format(value)
        }
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }

    private companion object {
        val ALIASES = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "camera" to "com.android.camera",
            "galeria" to "com.android.gallery3d",
            "fotos" to "com.google.android.apps.photos"
        )
    }
}
