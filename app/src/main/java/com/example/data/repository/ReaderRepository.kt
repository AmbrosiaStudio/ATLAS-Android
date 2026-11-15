package com.example.data.repository

import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.PageNestDao
import com.example.data.local.ReaderPreferencesDataStore
import com.example.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

class ReaderRepository(
  private val dao: PageNestDao,
  private val preferences: ReaderPreferencesDataStore,
) {
  val settingsFlow: Flow<ReaderSettings> = preferences.settingsFlow
  val lastOpenedBookIdFlow: Flow<String?> = preferences.lastOpenedBookIdFlow

  fun observeBook(bookId: String): Flow<BookEntity?> = dao.observeBook(bookId)

  suspend fun getBookById(bookId: String): BookEntity? = dao.getBookById(bookId)

  fun observeBookmarks(bookId: String): Flow<List<BookmarkEntity>> = dao.observeBookmarks(bookId)

  fun observeHighlights(bookId: String): Flow<List<HighlightEntity>> = dao.observeHighlights(bookId)

  suspend fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
    preferences.updateSettings(transform)
  }

  suspend fun setLastOpenedBookId(bookId: String?) {
    preferences.setLastOpenedBookId(bookId)
  }

  suspend fun updateTotalUnits(bookId: String, totalUnits: Int) {
    dao.updateTotalUnits(bookId, totalUnits)
  }

  suspend fun saveProgress(
    bookId: String,
    chapterOrPage: Int,
    scrollFraction: Float,
    progressPercent: Float,
  ) {
    dao.updateReadingProgress(
      bookId = bookId,
      chapterOrPage = chapterOrPage,
      scrollFraction = scrollFraction.coerceIn(0f, 1f),
      progressPercent = progressPercent.coerceIn(0f, 100f),
      lastOpenedAt = System.currentTimeMillis(),
    )
  }

  suspend fun addBookmark(
    bookId: String,
    title: String,
    chapterOrPageIndex: Int,
    scrollFraction: Float,
    snippet: String,
  ) {
    dao.insertBookmark(
      BookmarkEntity(
        bookId = bookId,
        title = title,
        chapterOrPageIndex = chapterOrPageIndex,
        scrollFraction = scrollFraction,
        snippet = snippet.take(180),
      )
    )
  }

  suspend fun removeBookmarkNear(bookId: String, chapterOrPageIndex: Int, scrollFraction: Float) {
    dao.deleteBookmarkNearLocation(bookId, chapterOrPageIndex, scrollFraction)
  }

  suspend fun deleteBookmark(bookmarkId: Long) {
    dao.deleteBookmarkById(bookmarkId)
  }

  suspend fun addHighlight(
    bookId: String,
    chapterOrPageIndex: Int,
    selectedText: String,
    colorHex: String,
    note: String = "",
  ) {
    val clean = selectedText.trim()
    if (clean.isBlank()) return
    dao.insertHighlight(
      HighlightEntity(
        bookId = bookId,
        chapterOrPageIndex = chapterOrPageIndex,
        selectedText = clean,
        note = note,
        colorHex = colorHex,
      )
    )
  }

  suspend fun deleteHighlight(highlightId: Long) {
    dao.deleteHighlightById(highlightId)
  }

  suspend fun recordReadingTime(deltaSeconds: Int) {
    preferences.addReadingSeconds(deltaSeconds, currentDateKey())
  }

  suspend fun setDailyGoalMinutes(minutes: Int) {
    val safe = minutes.coerceIn(5, 240)
    preferences.updateSettings { it.copy(dailyGoalMinutes = safe) }
  }

  fun currentDateKey(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return formatter.format(java.util.Date())
  }
}
