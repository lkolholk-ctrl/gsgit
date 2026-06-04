package gs.git.vps.util

import android.content.Context
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

    suspend fun ensureModelDownloaded(from: String = TranslateLanguage.ENGLISH, to: String = TranslateLanguage.RUSSIAN): Boolean {
        return try {
            val t = getTranslator(from, to)
            t.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun translateMarkdown(md: String, from: String = TranslateLanguage.ENGLISH, to: String = TranslateLanguage.RUSSIAN): String {
        val lines = md.lines()
        val result = mutableListOf<String>()
        val codeBuffer = mutableListOf<String>()
        var inCodeBlock = false

        for (line in lines) {
            if (line.trimStart().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                codeBuffer.add(line)
                if (!inCodeBlock) {
                    result.addAll(codeBuffer)
                    codeBuffer.clear()
                }
                continue
            }
            if (inCodeBlock) {
                codeBuffer.add(line)
                continue
            }
            if (line.isBlank()) {
                result.add(line)
                continue
            }
            if (line.trimStart().startsWith("#")) {
                val m = Regex("^(#+\\s*)(.*)$").find(line.trimStart())!!
                result.add(line.substring(0, line.length - line.trimStart().length) + m.groupValues[1] + translateText(m.groupValues[2], from, to))
                continue
            }
            if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                val prefix = line.substring(0, line.length - line.trimStart().length)
                val bullet = line.trimStart().substring(0, 2)
                result.add(prefix + bullet + translateText(line.trimStart().substring(2), from, to))
                continue
            }
            if (line.trimStart().startsWith("> ")) {
                val prefix = line.substring(0, line.length - line.trimStart().length)
                result.add(prefix + "> " + translateText(line.trimStart().substring(2), from, to))
                continue
            }
            if (line.trimStart().startsWith("|")) {
                result.add(line.substring(0, line.length - line.trimStart().length) + translateText(line.trimStart(), from, to))
                continue
            }
            result.add(translateText(line, from, to))
        }
        return result.joinToString("\n")
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
