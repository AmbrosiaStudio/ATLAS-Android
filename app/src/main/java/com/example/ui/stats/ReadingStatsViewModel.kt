package com.example.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookProgressPointEntity
import com.example.data.local.DailyReadingStatEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.model.AppLanguage
import com.example.model.ReaderSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DayReadingEntry(
  val dateKey: String,
  val shortLabel: String,
  val fullLabel: String,
  val readingSeconds: Int,
  val pagesRead: Int,
  val goalMet: Boolean,
  val isToday: Boolean,
) {
  val readingMinutes: Int
    get() = readingSeconds / 60
}

data class ReadingStatsUiState(
  val settings: ReaderSettings = ReaderSettings(),
  val books: List<BookEntity> = emptyList(),
  val selectedBook: BookEntity? = null,
  val bookProgressHistory: List<BookProgressPointEntity> = emptyList(),
  val dailyWindowDays: Int = 7,
  val dailyEntries: List<DayReadingEntry> = emptyList(),
  val todayReadingSeconds: Int = 0,
  val allTimeReadingSeconds: Long = 0L,
  val todayPagesRead: Int = 0,
  val totalPagesReadAllTime: Int = 0,
  val activeStreakDays: Int = 0,
) {
  val todayReadingMinutes: Int
    get() =
      if (todayReadingSeconds in 1..59) 1
      else todayReadingSeconds / 60

  val dailyGoalProgressFraction: Float
    get() =
      if (settings.dailyGoalMinutes <= 0) 1f
      else (todayReadingSeconds.toFloat() / (settings.dailyGoalMinutes * 60f)).coerceIn(0f, 1f)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingStatsViewModel(
  initialBookId: String?,
  libraryRepository: LibraryRepository,
  private val readerRepository: ReaderRepository,
) : ViewModel() {

  private val selectedBookIdOverride =
    MutableStateFlow(initialBookId?.takeIf { it.isNotBlank() })
  private val dailyWindowDays = MutableStateFlow(7)

  private val resolvedBookFlow =
    combine(
      libraryRepository.booksFlow,
      readerRepository.lastOpenedBookIdFlow,
      selectedBookIdOverride,
    ) { books, lastOpenedId, overrideId ->
      books.firstOrNull { it.id == overrideId }
        ?: books.firstOrNull { it.id == lastOpenedId }
        ?: books.maxByOrNull { if (it.lastOpenedAt > 0L) it.lastOpenedAt else it.addedAt }
    }

  private val bookHistoryFlow =
    resolvedBookFlow
      .map { it?.id }
      .distinctUntilChanged()
      .flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList())
        else readerRepository.observeBookProgressHistory(bookId)
      }

  val uiState: StateFlow<ReadingStatsUiState> =
    combine(
      libraryRepository.booksFlow,
      readerRepository.settingsFlow,
      readerRepository.observeAllDailyStats(),
      resolvedBookFlow,
      combine(bookHistoryFlow, dailyWindowDays) { hist, win -> hist to win },
    ) { books, settings, dailyStats, currentBook, (history, windowDays) ->
      val todayKey = readerRepository.currentDateKey()
      val statsByDate = dailyStats.associateBy { it.dateKey }.toMutableMap()

      // Ensure today's time reflects DataStore even if Room hasn't received a flush today
      val dsTodaySec =
        if (settings.lastReadingDateKey == todayKey) settings.todayReadingSeconds else 0
      val existingToday = statsByDate[todayKey]
      val reconciledTodaySec = maxOf(existingToday?.readingSeconds ?: 0, dsTodaySec)
      val todayPages = existingToday?.pagesRead ?: 0
      if (reconciledTodaySec > 0 || todayPages > 0) {
        statsByDate[todayKey] =
          DailyReadingStatEntity(
            dateKey = todayKey,
            readingSeconds = reconciledTodaySec,
            pagesRead = todayPages,
            lastUpdatedAt = existingToday?.lastUpdatedAt ?: System.currentTimeMillis(),
          )
      }

      val entries =
        buildTrailingDayEntries(
          daysCount = windowDays,
          statsByDate = statsByDate,
          dailyGoalMinutes = settings.dailyGoalMinutes,
          language = settings.appLanguage,
        )

      val totalPagesAllTime = statsByDate.values.sumOf { it.pagesRead }
      val allTimeSeconds =
        maxOf(
          settings.totalReadingSecondsAllTime,
          statsByDate.values.sumOf { it.readingSeconds.toLong() },
        )
      val streak = com.example.model.StreakCalculator.computeActiveStreak(statsByDate)

      ReadingStatsUiState(
        settings = settings,
        books = books,
        selectedBook = currentBook,
        bookProgressHistory = history,
        dailyWindowDays = windowDays,
        dailyEntries = entries,
        todayReadingSeconds = reconciledTodaySec,
        allTimeReadingSeconds = allTimeSeconds,
        todayPagesRead = todayPages,
        totalPagesReadAllTime = totalPagesAllTime,
        activeStreakDays = streak,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ReadingStatsUiState(),
    )

  fun selectBook(bookId: String) {
    selectedBookIdOverride.value = bookId
  }

  fun setDailyWindowDays(days: Int) {
    dailyWindowDays.value = if (days >= 14) 14 else 7
  }

  private fun buildTrailingDayEntries(
    daysCount: Int,
    statsByDate: Map<String, DailyReadingStatEntity>,
    dailyGoalMinutes: Int,
    language: AppLanguage,
  ): List<DayReadingEntry> {
    val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayLocale = if (language == AppLanguage.PERSIAN) Locale("fa") else Locale.US
    val shortFormatter =
      SimpleDateFormat(if (daysCount <= 7) "EEE" else "M/d", displayLocale)
    val fullFormatter = SimpleDateFormat("MMM d", displayLocale)

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -(daysCount - 1))

    return (0 until daysCount).map { idx ->
      val key = keyFormatter.format(cal.time)
      val stat = statsByDate[key]
      val sec = stat?.readingSeconds ?: 0
      val pages = stat?.pagesRead ?: 0
      val goalMet = sec >= dailyGoalMinutes * 60
      val entry =
        DayReadingEntry(
          dateKey = key,
          shortLabel = shortFormatter.format(cal.time),
          fullLabel = fullFormatter.format(cal.time),
          readingSeconds = sec,
          pagesRead = pages,
          goalMet = goalMet,
          isToday = idx == daysCount - 1,
        )
      cal.add(Calendar.DAY_OF_YEAR, 1)
      entry
    }
  }

  class Factory(
    private val initialBookId: String?,
    private val libraryRepository: LibraryRepository,
    private val readerRepository: ReaderRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ReadingStatsViewModel(
        initialBookId = initialBookId,
        libraryRepository = libraryRepository,
        readerRepository = readerRepository,
      ) as T
    }
  }
}
