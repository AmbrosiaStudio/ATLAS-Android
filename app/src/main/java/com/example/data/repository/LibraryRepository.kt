package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.BookEntity
import com.example.data.local.PageNestDao
import com.example.engine.epub.EpubParser
import com.example.engine.pdf.PdfEngine
import com.example.model.BookFormat
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LibraryRepository(
  private val context: Context,
  private val dao: PageNestDao,
  private val epubParser: EpubParser,
  private val pdfEngine: PdfEngine,
) {
  val booksFlow: Flow<List<BookEntity>> = dao.observeAllBooks()

  suspend fun importFromUri(uri: Uri): Result<BookEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val displayName = resolveDisplayName(uri) ?: "Imported Book"
        val mimeType = resolver.getType(uri).orEmpty()

        val tempFile = File(context.cacheDir, "import_${UUID.randomUUID()}")
        try {
          resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
          } ?: error("Unable to open the selected file.")

          if (!tempFile.exists() || tempFile.length() == 0L) {
            error("The selected file is empty.")
          }

          val format = detectFormat(tempFile, displayName, mimeType)
            ?: error("Unsupported file format. Please choose a valid .epub or .pdf file.")

          importLocalFileInternal(tempFile, displayName, format)
        } finally {
          tempFile.delete()
        }
      }
    }

  suspend fun importSampleClassics(): Result<List<BookEntity>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val imported = mutableListOf<BookEntity>()
        val sampleEpub = SampleLibrarySeeder.createSampleEpub(context)
        try {
          imported.add(
            importLocalFileInternal(
              sourceFile = sampleEpub,
              originalName = "Letters on Slow Reading.epub",
              format = BookFormat.EPUB,
            )
          )
        } finally {
          sampleEpub.delete()
        }

        val samplePdf = SampleLibrarySeeder.createSamplePdf(context)
        try {
          imported.add(
            importLocalFileInternal(
              sourceFile = samplePdf,
              originalName = "Editorial Typography Pocket Guide.pdf",
              format = BookFormat.PDF,
            )
          )
        } finally {
          samplePdf.delete()
        }

        val samplePersianEpub = SampleLibrarySeeder.createSamplePersianEpub(context)
        try {
          imported.add(
            importLocalFileInternal(
              sourceFile = samplePersianEpub,
              originalName = "Quiet_Persian_Reflections.epub",
              format = BookFormat.EPUB,
            )
          )
        } finally {
          samplePersianEpub.delete()
        }
        imported
      }
    }

  private suspend fun importLocalFileInternal(
    sourceFile: File,
    originalName: String,
    format: BookFormat,
  ): BookEntity {
    val bookId = UUID.randomUUID().toString()
    val ext = if (format == BookFormat.PDF) "pdf" else "epub"
    val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    val destFile = File(booksDir, "$bookId.$ext")
    sourceFile.copyTo(destFile, overwrite = true)

    val fallbackTitle =
      originalName
        .substringBeforeLast('.')
        .replace('_', ' ')
        .trim()
        .ifBlank { "Untitled Book" }

    val entity =
      when (format) {
        BookFormat.EPUB -> {
          val result = epubParser.extractMetadataAndCover(destFile, bookId, fallbackTitle)
          val (meta, coverPath) =
            result.getOrElse { err ->
              destFile.delete()
              throw IllegalStateException(
                err.message ?: "Could not parse EPUB file. The archive may be corrupted."
              )
            }
          BookEntity(
            id = bookId,
            title = meta.title,
            author = meta.author,
            format = BookFormat.EPUB.name,
            filePath = destFile.absolutePath,
            coverPath = coverPath,
            fileSizeBytes = destFile.length(),
            totalUnits = meta.totalChapters,
            addedAt = System.currentTimeMillis(),
          )
        }
        BookFormat.PDF -> {
          val result = pdfEngine.inspectAndExtractCover(destFile, bookId)
          val (pageCount, coverPath) =
            result.getOrElse { err ->
              destFile.delete()
              throw IllegalStateException(
                err.message ?: "Could not open PDF document. It may be password-protected or corrupted."
              )
            }
          BookEntity(
            id = bookId,
            title = fallbackTitle,
            author = "PDF Document",
            format = BookFormat.PDF.name,
            filePath = destFile.absolutePath,
            coverPath = coverPath,
            fileSizeBytes = destFile.length(),
            totalUnits = pageCount,
            addedAt = System.currentTimeMillis(),
          )
        }
      }

    dao.upsertBook(entity)
    return entity
  }

  suspend fun deleteBook(book: BookEntity) =
    withContext(Dispatchers.IO) {
      dao.deleteBookById(book.id)
      runCatching { File(book.filePath).delete() }
      book.coverPath?.let { runCatching { File(it).delete() } }
      runCatching { File(context.filesDir, "epub_cache/${book.id}").deleteRecursively() }
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

  private fun detectFormat(file: File, fileName: String, mimeType: String): BookFormat? {
    val header = ByteArray(4)
    val read = file.inputStream().use { it.read(header) }
    if (read >= 4) {
      val headerStr = String(header, Charsets.US_ASCII)
      if (headerStr == "%PDF") return BookFormat.PDF
    }
    val lowerName = fileName.lowercase()
    if (mimeType.equals("application/pdf", ignoreCase = true) || lowerName.endsWith(".pdf")) {
      return BookFormat.PDF
    }
    if (
      mimeType.equals("application/epub+zip", ignoreCase = true) ||
        lowerName.endsWith(".epub") ||
        (read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte())
    ) {
      return BookFormat.EPUB
    }
    return null
  }
}
