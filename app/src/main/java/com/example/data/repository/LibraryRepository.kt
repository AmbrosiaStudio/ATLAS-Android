package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.BookEntity
import com.example.data.local.BookShelfCrossRef
import com.example.data.local.PageNestDao
import com.example.data.local.ShelfEntity
import com.example.engine.cbz.CbzEngine
import com.example.engine.epub.EpubParser
import com.example.engine.formats.TextFormatsConverter
import com.example.engine.pdf.PdfEngine
import com.example.model.BookFormat
import com.example.model.ReadingStatus
import com.example.widget.PageNestContinueWidget
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LibraryRepository(
  private val context: Context,
  private val dao: PageNestDao,
  private val epubParser: EpubParser,
  private val pdfEngine: PdfEngine,
  private val textFormatsConverter: TextFormatsConverter = TextFormatsConverter(context),
  private val cbzEngine: CbzEngine = CbzEngine(context),
) {
  val booksFlow: Flow<List<BookEntity>> = dao.observeAllBooks()
  val shelvesFlow: Flow<List<ShelfEntity>> = dao.observeAllShelves()
  val bookShelfRefsFlow: Flow<List<BookShelfCrossRef>> = dao.observeAllBookShelfRefs()

  suspend fun createShelf(name: String): Long? =
    withContext(Dispatchers.IO) {
      val clean = name.trim()
      if (clean.isBlank()) return@withContext null
      dao.insertShelf(ShelfEntity(name = clean))
    }

  suspend fun deleteShelf(shelfId: Long) =
    withContext(Dispatchers.IO) {
      dao.deleteShelfById(shelfId)
    }

  suspend fun toggleBookShelf(bookId: String, shelfId: Long, assign: Boolean) =
    withContext(Dispatchers.IO) {
      if (assign) {
        dao.insertBookShelfRef(BookShelfCrossRef(bookId = bookId, shelfId = shelfId))
      } else {
        dao.deleteBookShelfRef(bookId = bookId, shelfId = shelfId)
      }
    }

  suspend fun setReadingStatus(bookId: String, status: ReadingStatus) =
    withContext(Dispatchers.IO) {
      dao.updateBookReadingStatus(bookId, status.name)
      PageNestContinueWidget.requestUpdate(context)
    }

  suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) =
    withContext(Dispatchers.IO) {
      dao.updateBookFavorite(bookId, isFavorite)
    }

  suspend fun updateBookMetadataAndCover(
    book: BookEntity,
    newTitle: String,
    newAuthor: String,
    newStatus: ReadingStatus,
    isFavorite: Boolean,
    pickedCoverUri: Uri?,
    resetCover: Boolean,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanTitle = newTitle.trim().ifBlank { book.title }
        val cleanAuthor = newAuthor.trim().ifBlank { book.author }
        var nextCoverPath = book.coverPath

        if (resetCover) {
          book.coverPath?.let { oldPath -> runCatching { File(oldPath).delete() } }
          nextCoverPath = null
        }

        if (pickedCoverUri != null) {
          val importedCover = saveCoverFromImageUri(pickedCoverUri, book.id)
          if (importedCover != null) {
            if (book.coverPath != null && book.coverPath != importedCover) {
              runCatching { File(book.coverPath).delete() }
            }
            nextCoverPath = importedCover
          } else {
            error("Unable to read the selected cover image.")
          }
        }

        dao.updateBookMetadata(
          bookId = book.id,
          title = cleanTitle,
          author = cleanAuthor,
          readingStatus = newStatus.name,
          isFavorite = isFavorite,
          coverPath = nextCoverPath,
        )
        PageNestContinueWidget.requestUpdate(context)
      }
    }

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

          val format =
            detectFormat(tempFile, displayName, mimeType)
              ?: error("Unsupported format. Choose EPUB, PDF, TXT, MD, FB2, or CBZ.")

          val entity = importLocalFileInternal(tempFile, displayName, format)
          PageNestContinueWidget.requestUpdate(context)
          entity
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
        PageNestContinueWidget.requestUpdate(context)
        imported
      }
    }

  private suspend fun importLocalFileInternal(
    sourceFile: File,
    originalName: String,
    format: BookFormat,
  ): BookEntity {
    val bookId = UUID.randomUUID().toString()
    val ext = format.extension
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
        BookFormat.TXT, BookFormat.MD, BookFormat.FB2 -> {
          val result = textFormatsConverter.extractMetadataAndCover(destFile, format, bookId, fallbackTitle)
          val meta =
            result.getOrElse { err ->
              destFile.delete()
              throw IllegalStateException(
                err.message ?: "Could not parse ${format.name} document."
              )
            }
          BookEntity(
            id = bookId,
            title = meta.title,
            author = meta.author,
            format = format.name,
            filePath = destFile.absolutePath,
            coverPath = meta.coverPath,
            fileSizeBytes = destFile.length(),
            totalUnits = meta.totalUnits,
            addedAt = System.currentTimeMillis(),
          )
        }
        BookFormat.CBZ -> {
          val result = cbzEngine.inspectAndExtractCover(destFile, bookId)
          val (pageCount, coverPath) =
            result.getOrElse { err ->
              destFile.delete()
              throw IllegalStateException(
                err.message ?: "Could not open CBZ comic archive."
              )
            }
          BookEntity(
            id = bookId,
            title = fallbackTitle,
            author = "Comic Archive (CBZ)",
            format = BookFormat.CBZ.name,
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

  suspend fun importOrUpdateStudioEpub(
    epubFile: File,
    existingBookId: String?,
    fallbackTitle: String,
  ): Result<BookEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val existingBook = existingBookId?.takeIf { it.isNotBlank() }?.let { dao.getBookById(it) }
        val targetBookId = existingBook?.id ?: UUID.randomUUID().toString()
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val destFile = File(booksDir, "$targetBookId.epub")
        epubFile.copyTo(destFile, overwrite = true)

        // Invalidate stale extracted XHTML/images so re-opened books reflect the newest draft edits
        runCatching { File(context.filesDir, "epub_cache/$targetBookId").deleteRecursively() }

        val (meta, coverPath) =
          epubParser
            .extractMetadataAndCover(destFile, targetBookId, fallbackTitle)
            .getOrThrow()

        val now = System.currentTimeMillis()
        val entity =
          if (existingBook != null) {
            existingBook.copy(
              title = meta.title,
              author = meta.author,
              format = BookFormat.EPUB.name,
              filePath = destFile.absolutePath,
              coverPath = coverPath ?: existingBook.coverPath,
              fileSizeBytes = destFile.length(),
              totalUnits = meta.totalChapters,
              currentChapterOrPage = existingBook.currentChapterOrPage.coerceIn(0, (meta.totalChapters - 1).coerceAtLeast(0)),
            )
          } else {
            BookEntity(
              id = targetBookId,
              title = meta.title,
              author = meta.author,
              format = BookFormat.EPUB.name,
              filePath = destFile.absolutePath,
              coverPath = coverPath,
              fileSizeBytes = destFile.length(),
              totalUnits = meta.totalChapters,
              addedAt = now,
            )
          }
        dao.upsertBook(entity)
        PageNestContinueWidget.requestUpdate(context)
        entity
      }
    }

  suspend fun deleteBook(book: BookEntity) =
    withContext(Dispatchers.IO) {
      dao.deleteBookById(book.id)
      runCatching { File(book.filePath).delete() }
      book.coverPath?.let { runCatching { File(it).delete() } }
      runCatching { File(context.filesDir, "epub_cache/${book.id}").deleteRecursively() }
      PageNestContinueWidget.requestUpdate(context)
    }

  private fun saveCoverFromImageUri(uri: Uri, bookId: String): String? =
    runCatching {
      val resolver = context.contentResolver
      val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
      if (bytes.isEmpty()) return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

      val maxWidth = 520
      var sample = 1
      while (bounds.outWidth / sample > maxWidth * 2) {
        sample *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
      val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
      val scaled =
        if (decoded.width > maxWidth) {
          val h = (decoded.height.toFloat() * maxWidth / decoded.width).toInt().coerceAtLeast(1)
          Bitmap.createScaledBitmap(decoded, maxWidth, h, true).also {
            if (it != decoded) decoded.recycle()
          }
        } else {
          decoded
        }
      val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
      val outFile = File(coversDir, "${bookId}_custom_${System.currentTimeMillis()}.jpg")
      FileOutputStream(outFile).use { fos -> scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos) }
      scaled.recycle()
      outFile.absolutePath
    }.getOrNull()

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
    val header = ByteArray(256)
    val read = file.inputStream().use { it.read(header) }
    val lowerName = fileName.lowercase()
    val lowerMime = mimeType.lowercase()

    if (read >= 4 && String(header, 0, 4, Charsets.US_ASCII) == "%PDF") {
      return BookFormat.PDF
    }
    if (lowerMime == "application/pdf" || lowerName.endsWith(".pdf")) {
      return BookFormat.PDF
    }

    val isZipSignature = read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
    if (isZipSignature) {
      if (lowerName.endsWith(".cbz") || lowerMime.contains("comicbook") || lowerMime == "application/vnd.comicbook+zip") {
        return BookFormat.CBZ
      }
      if (lowerName.endsWith(".fb2") || lowerName.endsWith(".fb2.zip")) {
        return BookFormat.FB2
      }
      if (lowerName.endsWith(".epub") || lowerMime == "application/epub+zip") {
        return BookFormat.EPUB
      }
      // Inspect zip contents to classify unambiguously when extension or mime is generic
      runCatching {
        ZipFile(file).use { zip ->
          if (zip.getEntry("META-INF/container.xml") != null) return BookFormat.EPUB
          val entries = zip.entries().asSequence().filter { !it.isDirectory }.take(50).toList()
          if (entries.any { it.name.lowercase().endsWith(".fb2") }) return BookFormat.FB2
          val imgExts = setOf("jpg", "jpeg", "png", "webp", "gif")
          if (entries.any { it.name.substringAfterLast('.', "").lowercase() in imgExts }) return BookFormat.CBZ
        }
      }
      return BookFormat.EPUB
    }

    if (lowerName.endsWith(".fb2") || lowerMime.contains("fictionbook")) {
      return BookFormat.FB2
    }
    if (read > 0) {
      val snippet = String(header, 0, read, Charsets.UTF_8)
      if (snippet.contains("<FictionBook", ignoreCase = true)) {
        return BookFormat.FB2
      }
    }
    if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown") || lowerMime == "text/markdown") {
      return BookFormat.MD
    }
    if (lowerName.endsWith(".txt") || lowerMime == "text/plain") {
      return BookFormat.TXT
    }
    return null
  }
}
