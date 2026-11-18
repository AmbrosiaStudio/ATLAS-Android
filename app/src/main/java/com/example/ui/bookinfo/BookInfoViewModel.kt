package com.example.ui.bookinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookProgressPointEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DailyReadingStatEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.ShelfEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.engine.insights.BookReadingInsights
import com.example.engine.insights.ReadingInsightsEngine
import com.example.model.ReaderSettings
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookInfoUiState(
  val book: BookEntity? = null,
  val isLoading: Boolean = true,
  val isCalculatingInsights: Boolean = true,
  val insights: BookReadingInsights? = null,
  val bookmarksCount: Int = 0,
  val highlightsCount: Int = 0,
  val shelves: List<ShelfEntity> = emptyList(),
  val assignedShelfIds: Set<Long> = emptySet(),
  val settings: ReaderSettings = ReaderSettings(),
)

class BookInfoViewModel(
  private val bookId: String,
  private val libraryRepository: LibraryRepository,
  private val readerRepository: ReaderRepository,
  private val readingInsightsEngine: ReadingInsightsEngine,
) : ViewModel() {

  private val insightsState = MutableStateFlow<BookReadingInsights?>(null)
  private val calculatingInsightsState = MutableStateFlow(true)

  private data class BookMetaTuple(
    val book: BookEntity?,
    val bookmarksCount: Int,
    val highlightsCount: Int,
    val shelves: List<ShelfEntity>,
    val assignedShelfIds: Set<Long>,
  )

  private data class StatsInputs(
    val book: BookEntity?,
    val dailyStats: List<DailyReadingStatEntity>,
    val progressHistory: List<BookProgressPointEntity>,
    val telemetryTick: Long,
  )

  val uiState: StateFlow<BookInfoUiState> =
    combine(
      combine(
        readerRepository.observeBook(bookId),
        readerRepository.observeBookmarks(bookId),
        readerRepository.observeHighlights(bookId),
        libraryRepository.shelvesFlow,
        libraryRepository.bookShelfRefsFlow,
      ) { book, bookmarks, highlights, shelves, refs ->
        BookMetaTuple(
          book = book,
          bookmarksCount = bookmarks.size,
          highlightsCount = highlights.size,
          shelves = shelves,
          assignedShelfIds = refs.filter { it.bookId == bookId }.map { it.shelfId }.toSet(),
        )
      },
      readerRepository.settingsFlow,
      insightsState,
      calculatingInsightsState,
    ) { meta, settings, insights, isCalculating ->
      BookInfoUiState(
        book = meta.book,
        isLoading = meta.book == null && isCalculating,
        isCalculatingInsights = isCalculating,
        insights = insights,
        bookmarksCount = meta.bookmarksCount,
        highlightsCount = meta.highlightsCount,
        shelves = meta.shelves,
        assignedShelfIds = meta.assignedShelfIds,
        settings = settings,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = BookInfoUiState(),
    )

  init {
    observeAndComputeInsights()
  }

  private fun observeAndComputeInsights() {
    viewModelScope.launch {
      combine(
        readerRepository.observeBook(bookId),
        readerRepository.observeAllDailyStats(),
        readerRepository.observeBookProgressHistory(bookId),
        readingInsightsEngine.telemetryUpdatedTick,
      ) { book, dailyStats, history, tick ->
        StatsInputs(
          book = book,
          dailyStats = dailyStats,
          progressHistory = history,
          telemetryTick = tick,
        )
      }.collect { inputs ->
        val book = inputs.book
        if (book == null) {
          insightsState.value = null
          calculatingInsightsState.value = false
          return@collect
        }
        val computed =
          withContext(Dispatchers.IO) {
            readingInsightsEngine.buildInsightsForBook(
              book = book,
              dailyStats = inputs.dailyStats,
              progressHistory = inputs.progressHistory,
            )
          }
        insightsState.value = computed
        calculatingInsightsState.value = false
      }
    }
  }

  fun toggleFavorite() {
    val book = uiState.value.book ?: return
    viewModelScope.launch {
      libraryRepository.toggleFavorite(book.id, !book.isFavorite)
    }
  }

  fun setReadingStatus(status: ReadingStatus) {
    val book = uiState.value.book ?: return
    viewModelScope.launch {
      libraryRepository.setReadingStatus(book.id, status)
    }
  }

  fun toggleShelf(shelfId: Long) {
    val book = uiState.value.book ?: return
    val currentlyAssigned = shelfId in uiState.value.assignedShelfIds
    viewModelScope.launch {
      libraryRepository.toggleBookShelf(book.id, shelfId, !currentlyAssigned)
    }
  }

  class Factory(
    private val bookId: String,
    private val libraryRepository: LibraryRepository,
    private val readerRepository: ReaderRepository,
    private val readingInsightsEngine: ReadingInsightsEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return BookInfoViewModel(
        bookId = bookId,
        libraryRepository = libraryRepository,
        readerRepository = readerRepository,
        readingInsightsEngine = readingInsightsEngine,
      ) as T
    }
  }
}
