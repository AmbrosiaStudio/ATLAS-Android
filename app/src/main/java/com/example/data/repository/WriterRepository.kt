package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.BookEntity
import com.example.data.local.PageNestDao
import com.example.data.local.WriterBookEntity
import com.example.data.local.WriterChapterEntity
import com.example.engine.writer.WriterEpubExporter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WriterRepository(
  private val context: Context,
  private val dao: PageNestDao,
  private val libraryRepository: LibraryRepository,
) {
  val allBooksFlow: Flow<List<WriterBookEntity>> = dao.observeAllWriterBooks()
  val allChaptersFlow: Flow<List<WriterChapterEntity>> = dao.observeAllWriterChapters()

  fun observeBook(bookId: String): Flow<WriterBookEntity?> = dao.observeWriterBook(bookId)

  fun observeChapters(bookId: String): Flow<List<WriterChapterEntity>> = dao.observeWriterChapters(bookId)

  suspend fun createManuscript(
    title: String,
    author: String,
    synopsis: String,
    isRtl: Boolean,
  ): WriterBookEntity =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val bookId = UUID.randomUUID().toString()
      val cleanTitle = title.trim().ifBlank { if (isRtl) "کتاب بی‌نام" else "Untitled Manuscript" }
      val cleanAuthor = author.trim().ifBlank { if (isRtl) "نویسنده پیج‌نست" else "PageNest Author" }
      val book =
        WriterBookEntity(
          id = bookId,
          title = cleanTitle,
          author = cleanAuthor,
          synopsis = synopsis.trim(),
          isRtl = isRtl,
          createdAt = now,
          updatedAt = now,
        )
      dao.upsertWriterBook(book)
      dao.insertWriterChapter(
        WriterChapterEntity(
          bookId = bookId,
          orderIndex = 0,
          title = if (isRtl) "فصل اول" else "Chapter 1",
          bodyText = "",
          updatedAt = now,
        )
      )
      book
    }

  suspend fun updateBookDetails(
    bookId: String,
    title: String,
    author: String,
    synopsis: String,
    isRtl: Boolean,
  ) =
    withContext(Dispatchers.IO) {
      val existing = dao.getWriterBook(bookId) ?: return@withContext
      val now = System.currentTimeMillis()
      dao.upsertWriterBook(
        existing.copy(
          title = title.trim().ifBlank { existing.title },
          author = author.trim().ifBlank { existing.author },
          synopsis = synopsis.trim(),
          isRtl = isRtl,
          updatedAt = now,
        )
      )
    }

  suspend fun addChapter(bookId: String, defaultTitle: String? = null): WriterChapterEntity =
    withContext(Dispatchers.IO) {
      val existing = dao.getWriterChapters(bookId)
      val book = dao.getWriterBook(bookId)
      val isRtl = book?.isRtl == true
      val nextIndex = existing.size
      val title =
        defaultTitle?.trim()?.takeIf { it.isNotBlank() }
          ?: if (isRtl) "فصل ${nextIndex + 1}" else "Chapter ${nextIndex + 1}"
      val now = System.currentTimeMillis()
      val entity =
        WriterChapterEntity(
          bookId = bookId,
          orderIndex = nextIndex,
          title = title,
          bodyText = "",
          updatedAt = now,
        )
      val rowId = dao.insertWriterChapter(entity)
      dao.touchWriterBookUpdatedAt(bookId, now)
      entity.copy(id = rowId)
    }

  suspend fun saveChapter(
    bookId: String,
    chapterId: Long,
    title: String,
    bodyText: String,
  ) =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      dao.updateWriterChapterContent(
        chapterId = chapterId,
        title = title,
        bodyText = bodyText,
        updatedAt = now,
      )
      dao.touchWriterBookUpdatedAt(bookId, now)
    }

  suspend fun deleteChapter(bookId: String, chapterId: Long) =
    withContext(Dispatchers.IO) {
      val remaining = dao.getWriterChapters(bookId).filter { it.id != chapterId }
      dao.deleteWriterChapter(chapterId)
      remaining.forEachIndexed { idx, ch ->
        if (ch.orderIndex != idx) {
          dao.updateWriterChapterOrder(ch.id, idx)
        }
      }
      dao.touchWriterBookUpdatedAt(bookId, System.currentTimeMillis())
    }

  suspend fun moveChapter(bookId: String, chapterId: Long, delta: Int): Boolean =
    withContext(Dispatchers.IO) {
      val ordered = dao.getWriterChapters(bookId).sortedBy { it.orderIndex }.toMutableList()
      val currentIndex = ordered.indexOfFirst { it.id == chapterId }
      if (currentIndex == -1) return@withContext false
      val targetIndex = currentIndex + delta
      if (targetIndex !in ordered.indices || targetIndex == currentIndex) return@withContext false
      val moved = ordered.removeAt(currentIndex)
      ordered.add(targetIndex, moved)
      ordered.forEachIndexed { idx, ch ->
        if (ch.orderIndex != idx) {
          dao.updateWriterChapterOrder(ch.id, idx)
        }
      }
      dao.touchWriterBookUpdatedAt(bookId, System.currentTimeMillis())
      true
    }

  suspend fun updateBookCover(
    bookId: String,
    coverThemeId: Int,
    pickedCoverUri: Uri? = null,
    clearCustomImage: Boolean = false,
  ) =
    withContext(Dispatchers.IO) {
      val existing = dao.getWriterBook(bookId) ?: return@withContext
      val coversDir = File(context.filesDir, "studio_covers").apply { mkdirs() }
      var nextImagePath = existing.coverImagePath

      if (clearCustomImage) {
        existing.coverImagePath?.let { path -> File(path).delete() }
        nextImagePath = null
      } else if (pickedCoverUri != null) {
        val destFile = File(coversDir, "studio_cover_${bookId}_${System.currentTimeMillis()}.jpg")
        val copied =
          runCatching {
            context.contentResolver.openInputStream(pickedCoverUri)?.use { input ->
              FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile.exists() && destFile.length() > 0L
          }.getOrDefault(false)
        if (copied) {
          existing.coverImagePath?.let { oldPath -> File(oldPath).delete() }
          nextImagePath = destFile.absolutePath
        } else {
          destFile.delete()
        }
      }

      dao.updateWriterBookCover(
        bookId = bookId,
        coverThemeId = coverThemeId.coerceIn(0, WriterEpubExporter.coverThemes.lastIndex),
        coverImagePath = nextImagePath,
        updatedAt = System.currentTimeMillis(),
      )
    }

  suspend fun deleteManuscript(bookId: String) =
    withContext(Dispatchers.IO) {
      dao.getWriterBook(bookId)?.coverImagePath?.let { path -> File(path).delete() }
      dao.deleteWriterBook(bookId)
    }

  suspend fun publishToLibrary(bookId: String): Result<BookEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val book = dao.getWriterBook(bookId) ?: error("Manuscript not found.")
        val chapters = dao.getWriterChapters(bookId)
        val epubFile = WriterEpubExporter.buildEpubFile(context, book, chapters)
        try {
          val libraryBook =
            libraryRepository
              .importOrUpdateStudioEpub(
                epubFile = epubFile,
                existingBookId = book.publishedBookId,
                fallbackTitle = book.title,
              )
              .getOrThrow()
          dao.updateWriterBookPublishedId(book.id, libraryBook.id, System.currentTimeMillis())
          libraryBook
        } finally {
          epubFile.delete()
        }
      }
    }

  suspend fun buildMarkdownShareText(bookId: String): String =
    withContext(Dispatchers.IO) {
      val book = dao.getWriterBook(bookId) ?: return@withContext ""
      val chapters = dao.getWriterChapters(bookId)
      WriterEpubExporter.buildMarkdownExport(book, chapters)
    }
}
