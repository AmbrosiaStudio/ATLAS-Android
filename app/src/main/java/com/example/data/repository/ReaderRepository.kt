package com.example.data.repository

import android.content.Context
import com.example.data.local.BookEntity
import com.example.data.local.BookProgressPointEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DailyReadingStatEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.PageNestDao
import com.example.data.local.ReaderPreferencesDataStore
import com.example.model.ReaderSettings
import com.example.model.ReadingStatus
import com.example.widget.PageNestContinueWidget
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReaderRepository(
  private val dao: PageNestDao,
  private val preferences: ReaderPreferencesDataStore,
  private val appContext: Context? = null,
) {
  val settingsFlow: Flow<ReaderSettings> = preferences.settingsFlow
  val lastOpenedBookIdFlow: Flow<String?> = preferences.lastOpenedBookIdFlow

  fun observeAllDailyStats(): Flow<List<DailyReadingStatEntity>> = dao.observeAllDailyStats()

  fun observeBookProgressHistory(bookId: String): Flow<List<BookProgressPointEntity>> =
    dao.observeBookProgressHistory(bookId)

  fun observeBook(bookId: String): Flow<BookEntity?> = dao.observeBook(bookId)

  suspend fun getBookById(bookId: String): BookEntity? = dao.getBookById(bookId)

  fun observeBookmarks(bookId: String): Flow<List<BookmarkEntity>> = dao.observeBookmarks(bookId)

  fun observeHighlights(bookId: String): Flow<List<HighlightEntity>> = dao.observeHighlights(bookId)

  suspend fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
    preferences.updateSettings(transform)
  }

  suspend fun setLastOpenedBookId(bookId: String?) {
    preferences.setLastOpenedBookId(bookId)
    appContext?.let { PageNestContinueWidget.requestUpdate(it) }
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
    val clampedProgress = progressPercent.coerceIn(0f, 100f)
    dao.updateReadingProgress(
      bookId = bookId,
      chapterOrPage = chapterOrPage,
      scrollFraction = scrollFraction.coerceIn(0f, 1f),
      progressPercent = clampedProgress,
      lastOpenedAt = System.currentTimeMillis(),
    )
    val existing = dao.getBookById(bookId)
    if (existing != null) {
      val currentStatus = existing.status()
      val nextStatus =
        when {
          clampedProgress >= 98f -> ReadingStatus.FINISHED
          currentStatus == ReadingStatus.UNREAD && clampedProgress >= 0.5f -> ReadingStatus.READING
          else -> currentStatus
        }
      if (nextStatus != currentStatus) {
        dao.updateBookReadingStatus(bookId, nextStatus.name)
      }
    }
    appContext?.let { PageNestContinueWidget.requestUpdate(it) }
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
    if (deltaSeconds <= 0) return
    val dateKey = currentDateKey()
    val existingStat = dao.getDailyStat(dateKey)
    val floor =
      if (existingStat == null) {
        val currentSettings = settingsFlow.first()
        if (currentSettings.lastReadingDateKey == dateKey) currentSettings.todayReadingSeconds else 0
      } else {
        0
      }
    preferences.addReadingSeconds(deltaSeconds, dateKey)
    dao.incrementDailyStats(
      dateKey = dateKey,
      deltaSeconds = deltaSeconds,
      deltaPages = 0,
      nowMillis = System.currentTimeMillis(),
      minSecondsFloorForToday = floor,
    )
    appContext?.let { PageNestContinueWidget.requestUpdate(it) }
  }

  suspend fun recordPagesRead(deltaPages: Int) {
    if (deltaPages <= 0) return
    val dateKey = currentDateKey()
    val existingStat = dao.getDailyStat(dateKey)
    val floor =
      if (existingStat == null) {
        val currentSettings = settingsFlow.first()
        if (currentSettings.lastReadingDateKey == dateKey) currentSettings.todayReadingSeconds else 0
      } else {
        0
      }
    dao.incrementDailyStats(
      dateKey = dateKey,
      deltaSeconds = 0,
      deltaPages = deltaPages,
      nowMillis = System.currentTimeMillis(),
      minSecondsFloorForToday = floor,
    )
  }

  suspend fun recordBookProgressSnapshot(
    bookId: String,
    progressPercent: Float,
    pageOrUnitIndex: Int,
    forceInitialOnly: Boolean = false,
  ) {
    val safePercent = progressPercent.coerceIn(0f, 100f)
    val safeIndex = pageOrUnitIndex.coerceAtLeast(0)
    if (forceInitialOnly) {
      if (dao.getBookProgressPointCount(bookId) > 0) return
      dao.insertBookProgressPoint(
        BookProgressPointEntity(
          bookId = bookId,
          progressPercent = safePercent,
          pageOrUnitIndex = safeIndex,
          recordedAt = System.currentTimeMillis(),
          dateKey = currentDateKey(),
        )
      )
      return
    }
    val now = System.currentTimeMillis()
    val latest = dao.getLatestBookProgressPoint(bookId)
    val shouldInsert =
      latest == null ||
        abs(latest.progressPercent - safePercent) >= 0.4f ||
        (latest.pageOrUnitIndex != safeIndex && now - latest.recordedAt >= 15_000L)
    if (shouldInsert) {
      dao.insertBookProgressPoint(
        BookProgressPointEntity(
          bookId = bookId,
          progressPercent = safePercent,
          pageOrUnitIndex = safeIndex,
          recordedAt = now,
          dateKey = currentDateKey(),
        )
      )
    }
  }

  suspend fun setDailyGoalMinutes(minutes: Int) {
    val safe = minutes.coerceIn(5, 240)
    preferences.updateSettings { it.copy(dailyGoalMinutes = safe) }
    appContext?.let { PageNestContinueWidget.requestUpdate(it) }
  }

  fun currentDateKey(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return formatter.format(java.util.Date())
  }
}
