package com.example.engine.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class TargetLanguageOption(
  val code: String,
  val label: String,
)

data class TranslationOutcome(
  val sourceLanguageCode: String,
  val targetLanguageCode: String,
  val translatedText: String,
)

class TextTranslator {

  val supportedTargets: List<TargetLanguageOption> = listOf(
    TargetLanguageOption(TranslateLanguage.PERSIAN, "فارسی (Persian)"),
    TargetLanguageOption(TranslateLanguage.ENGLISH, "English"),
    TargetLanguageOption(TranslateLanguage.ARABIC, "العربية (Arabic)"),
    TargetLanguageOption(TranslateLanguage.FRENCH, "Français"),
    TargetLanguageOption(TranslateLanguage.GERMAN, "Deutsch"),
    TargetLanguageOption(TranslateLanguage.SPANISH, "Español"),
    TargetLanguageOption(TranslateLanguage.TURKISH, "Türkçe"),
  )

  suspend fun translate(text: String, targetLangCode: String): Result<TranslationOutcome> =
    runCatching {
      val clean = text.trim()
      if (clean.isBlank()) error("No text selected.")

      val detected = detectSourceLanguage(clean)
      val sourceCode =
        when {
          detected == "und" -> guessByScript(clean)
          TranslateLanguage.fromLanguageTag(detected) != null ->
            TranslateLanguage.fromLanguageTag(detected)!!
          else -> guessByScript(clean)
        }

      val resolvedTarget =
        if (sourceCode == targetLangCode) {
          if (targetLangCode == TranslateLanguage.PERSIAN) TranslateLanguage.ENGLISH
          else TranslateLanguage.PERSIAN
        } else {
          targetLangCode
        }

      val options =
        TranslatorOptions.Builder()
          .setSourceLanguage(sourceCode)
          .setTargetLanguage(resolvedTarget)
          .build()
      val client = Translation.getClient(options)
      try {
        val conditions = DownloadConditions.Builder().build()
        awaitTask(client.downloadModelIfNeeded(conditions))
        val result = awaitTask(client.translate(clean))
        TranslationOutcome(
          sourceLanguageCode = sourceCode,
          targetLanguageCode = resolvedTarget,
          translatedText = result,
        )
      } finally {
        client.close()
      }
    }

  private suspend fun detectSourceLanguage(text: String): String {
    val identifier = LanguageIdentification.getClient()
    return try {
      awaitTask(identifier.identifyLanguage(text))
    } catch (_: Exception) {
      "und"
    } finally {
      identifier.close()
    }
  }

  private fun guessByScript(text: String): String {
    val hasArabicScript = text.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\uFB50'..'\uFDFF' }
    return if (hasArabicScript) TranslateLanguage.PERSIAN else TranslateLanguage.ENGLISH
  }

  private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
    suspendCancellableCoroutine { cont ->
      task
        .addOnSuccessListener { result ->
          if (cont.isActive) cont.resume(result)
        }
        .addOnFailureListener { err ->
          if (cont.isActive) cont.resumeWithException(err)
        }
    }
}
