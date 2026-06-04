package gs.git.vps.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object MdTranslator {

    private var translatorCache = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    private fun key(from: String, to: String) = "${from}_$to"

    private fun getTranslator(from: String, to: String): com.google.mlkit.nl.translate.Translator {
        val k = key(from, to)
        return translatorCache.getOrPut(k) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
            Translation.getClient(options)
        }
    }

    data class Lang(val code: String, val name: String)

    val availableLanguages: List<Lang> = listOf(
        Lang(TranslateLanguage.RUSSIAN, "Русский"),
        Lang(TranslateLanguage.SPANISH, "Español"),
        Lang(TranslateLanguage.FRENCH, "Français"),
        Lang(TranslateLanguage.GERMAN, "Deutsch"),
        Lang(TranslateLanguage.PORTUGUESE, "Português"),
        Lang(TranslateLanguage.ITALIAN, "Italiano"),
        Lang(TranslateLanguage.JAPANESE, "日本語"),
        Lang(TranslateLanguage.KOREAN, "한국어"),
        Lang(TranslateLanguage.CHINESE, "中文"),
        Lang(TranslateLanguage.ARABIC, "العربية"),
        Lang(TranslateLanguage.HINDI, "हिन्दी"),
        Lang(TranslateLanguage.TURKISH, "Türkçe"),
        Lang(TranslateLanguage.POLISH, "Polski"),
        Lang(TranslateLanguage.DUTCH, "Nederlands"),
        Lang(TranslateLanguage.UKRAINIAN, "Українська"),
    )

    fun langByCode(code: String): Lang = availableLanguages.find { it.code == code } ?: Lang(code, code)

    suspend fun downloadModel(from: String, to: String): Boolean {
        return try {
            val t = getTranslator(from, to)
            t.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi(false).build()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun translateText(text: String, from: String = TranslateLanguage.ENGLISH, to: String = TranslateLanguage.RUSSIAN): String {
        if (text.isBlank()) return text
        return try {
            val t = getTranslator(from, to)
            t.translate(text).await()
        } catch (e: Exception) {
            text
        }
    }
}
