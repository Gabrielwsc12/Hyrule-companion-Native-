package com.hyrulecompanion.app.ui

import android.content.Context
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.json.JSONObject
import java.util.Locale

internal const val LANGUAGE_KEY = "hc-app-language"

internal val languageChoices = listOf(
    "pt" to "Português (Brasil)", "en" to "English", "es" to "Español",
    "ru" to "Русский", "zh" to "简体中文", "ja" to "日本語"
)

internal fun deviceLanguage() = Locale.getDefault().language.takeIf { code -> languageChoices.any { it.first == code } } ?: "en"

private val language = mutableStateOf("pt")
private lateinit var appContext: Context
private val translations = mutableMapOf<String, TranslationDictionary>()
private val parameter = Regex("\u001f([0-9]+)\u001f")
private data class TranslationDictionary(val words: Map<String, String>, val templates: List<Pair<Regex, String>>)

internal fun initTranslations(context: Context, selection: String?) {
    appContext = context.applicationContext
    selectLanguage(selection)
}

internal fun selectLanguage(selection: String?) {
    val code = selection?.takeIf { selected -> languageChoices.any { it.first == selected } } ?: deviceLanguage()
    language.value = code
}

internal fun activeLanguage(): String = language.value

private fun dictionary(code: String): TranslationDictionary = synchronized(translations) {
    translations.getOrPut(code) {
        val data = appContext.assets.open("$code-text.json").bufferedReader().use { JSONObject(it.readText()) }
        val words = data.keys().asSequence().associateWith { data.getString(it) }
        val templates = words.entries.asSequence().filter { it.key.contains('\u001f') }
            .sortedByDescending { it.key.length }
            .filter { it.key.any(Char::isLetter) }
            .map { (key, value) ->
                val regex = key.split('\u001f').joinToString("(.*?)", "^", "$") { Regex.escape(it) }.toRegex()
                regex to value
            }.toList()
        TranslationDictionary(words, templates)
    }
}

internal fun translate(text: String): String {
    val code = activeLanguage()
    if (code == "pt" || text.isBlank() || !::appContext.isInitialized) return text
    val dictionary = dictionary(code)
    dictionary.words[text]?.let { return it }
    if (!text.any(Char::isLetter)) return text
    for ((pattern, localized) in dictionary.templates) {
        val match = pattern.matchEntire(text) ?: continue
        fun arg(index: Int): String {
            val value = match.groupValues.getOrElse(index + 1) { "" }
            return dictionary.words[value] ?: value
        }
        if (parameter.containsMatchIn(localized)) {
            return parameter.replace(localized) { arg(it.groupValues[1].toInt()) }
        }
        return localized.split('\u001f').mapIndexed { index, part ->
            if (index == 0) part else arg(index - 1) + part
        }.joinToString("")
    }
    return if (code == "en") text else dictionary("en").words[text] ?: text
}

@Composable
internal fun Text(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null, style: TextStyle = LocalTextStyle.current
) {
    androidx.compose.material3.Text(translate(text), modifier, color, fontSize, fontStyle, fontWeight, fontFamily,
        letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style)
}
