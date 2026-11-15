package com.example.engine.fonts

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import com.example.model.CustomFontEntry
import com.example.model.ReaderFontOption
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.VazirmatnFontFamily
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FontManager(private val context: Context) {
  private val customFontsDir: File = File(context.filesDir, "custom_fonts").apply { mkdirs() }
  private val _customFonts = MutableStateFlow<List<CustomFontEntry>>(loadExistingFonts())
  val customFontsFlow: StateFlow<List<CustomFontEntry>> = _customFonts.asStateFlow()

  private fun loadExistingFonts(): List<CustomFontEntry> {
    return customFontsDir
      .listFiles { file ->
        file.isFile &&
          (file.extension.equals("ttf", true) || file.extension.equals("otf", true))
      }
      ?.sortedBy { it.name.lowercase() }
      ?.map { file ->
        CustomFontEntry(
          id = file.name,
          displayName = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim(),
          filePath = file.absolutePath,
        )
      }
      .orEmpty()
  }

  suspend fun importFontFromUri(uri: Uri): Result<CustomFontEntry> =
    withContext(Dispatchers.IO) {
      runCatching {
        val rawName = resolveDisplayName(uri) ?: "CustomFont.ttf"
        val ext =
          rawName.substringAfterLast('.', "").lowercase().let {
            if (it == "ttf" || it == "otf") it else "ttf"
          }
        val cleanBase =
          rawName
            .substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9_\\- ]"), "")
            .trim()
            .ifBlank { "Custom_Font_${System.currentTimeMillis()}" }
            .replace(' ', '_')
        val targetFile = File(customFontsDir, "$cleanBase.$ext")

        val tempFile = File(context.cacheDir, "font_check_${System.currentTimeMillis()}.$ext")
        try {
          context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
          } ?: error("Could not read selected font file.")

          if (tempFile.length() < 1024L) {
            error("Selected file is not a valid font.")
          }
          // Verify Android Typeface parser accepts the font file
          val tf = runCatching { Typeface.createFromFile(tempFile) }.getOrNull()
          if (tf == null || tf == Typeface.DEFAULT) {
            // Try still copying if header looks like SFNT (TTF/OTF), else reject
            val header = ByteArray(4)
            tempFile.inputStream().use { it.read(header) }
            val sig = String(header, Charsets.US_ASCII)
            val isTrueType = header[0] == 0x00.toByte() && header[1] == 0x01.toByte()
            if (!isTrueType && sig != "OTTO" && sig != "true") {
              error("Unsupported font format. Please import a .ttf or .otf typeface.")
            }
          }
          tempFile.copyTo(targetFile, overwrite = true)
        } finally {
          tempFile.delete()
        }

        val updated = loadExistingFonts()
        _customFonts.value = updated
        updated.first { it.id == targetFile.name }
      }
    }

  suspend fun deleteCustomFont(fontId: String) =
    withContext(Dispatchers.IO) {
      val target = File(customFontsDir, fontId)
      if (target.exists()) target.delete()
      _customFonts.value = loadExistingFonts()
    }

  fun findCustomFont(fontId: String?): CustomFontEntry? {
    if (fontId.isNullOrBlank()) return null
    return _customFonts.value.firstOrNull { it.id == fontId }
  }

  fun resolveComposeFontFamily(fontOption: ReaderFontOption, customFontId: String?): FontFamily {
    val custom = findCustomFont(customFontId)
    if (custom != null) {
      val tf = runCatching { Typeface.createFromFile(File(custom.filePath)) }.getOrNull()
      if (tf != null) return FontFamily(tf)
    }
    return when (fontOption) {
      ReaderFontOption.LITERATA -> LiterataFontFamily
      ReaderFontOption.LORA -> LoraFontFamily
      ReaderFontOption.NEWSREADER -> NewsreaderFontFamily
      ReaderFontOption.VAZIRMATN -> VazirmatnFontFamily
      ReaderFontOption.NOTO_NASKH -> NotoNaskhFontFamily
      ReaderFontOption.PLUS_JAKARTA -> PlusJakartaSansFamily
      ReaderFontOption.JETBRAINS_MONO -> JetBrainsMonoFontFamily
    }
  }

  private fun resolveDisplayName(uri: Uri): String? {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) return cursor.getString(idx)
        }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }
}
