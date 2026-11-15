package com.example.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.model.LibraryLayoutMode
import com.example.model.LibrarySortOrder
import com.example.model.ReaderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
  val books: List<BookEntity> = emptyList(),
  val recentBooks: List<BookEntity> = emptyList(),
  val continueBook: BookEntity? = null,
  val lastSessionBookId: String? = null,
  val searchQuery: String = "",
  val layoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID,
  val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENT,
  val settings: ReaderSettings = ReaderSettings(),
  val dailyGoalMinutes: Int = 20,
  val todayReadingSeconds: Int = 0,
  val totalReadingMinutesAllTime: Int = 0,
  val isImporting: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null,
) {
  val todayReadingMinutes: Int
    get() = todayReadingSeconds / 60

  val dailyGoalProgressFraction: Float
    get() =
      if (dailyGoalMinutes <= 0) 1f
      else (todayReadingSeconds.toFloat() / (dailyGoalMinutes * 60f)).coerceIn(0f, 1f)

  val isDailyGoalMet: Boolean
    get() = todayReadingSeconds >= dailyGoalMinutes * 60
}

class LibraryViewModel(
  private val libraryRepository: LibraryRepository,
  private val readerRepository: ReaderRepository,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val isImporting = MutableStateFlow(false)
  private val errorMessage = MutableStateFlow<String?>(null)
  private val infoMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<LibraryUiState> =
    combine(
      libraryRepository.booksFlow,
      readerRepository.settingsFlow,
      readerRepository.lastOpenedBookIdFlow,
      searchQuery,
      combine(isImporting, errorMessage, infoMessage) { imp, err, info -> Triple(imp, err, info) },
    ) { allBooks, settings, lastSessionId, query, status ->
      val filtered =
        if (query.isBlank()) {
          allBooks
        } else {
          val q = query.trim()
          allBooks.filter {
            it.title.contains(q, ignoreCase = true) || it.author.contains(q, ignoreCase = true)
          }
        }

      val sorted =
        when (settings.librarySortOrder) {
          LibrarySortOrder.RECENT ->
            filtered.sortedByDescending { if (it.lastOpenedAt > 0L) it.lastOpenedAt else it.addedAt }
          LibrarySortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
          LibrarySortOrder.AUTHOR -> filtered.sortedBy { it.author.lowercase() }
          LibrarySortOrder.PROGRESS -> filtered.sortedByDescending { it.progressPercent }
        }

      val recent =
        allBooks
          .filter { it.lastOpenedAt > 0L }
          .sortedByDescending { it.lastOpenedAt }
          .take(6)

      val continueBook = recent.firstOrNull() ?: allBooks.firstOrNull()
      val isToday = settings.lastReadingDateKey == readerRepository.currentDateKey()
      val todaySec = if (isToday) settings.todayReadingSeconds else 0

      LibraryUiState(
        books = sorted,
        recentBooks = recent,
        continueBook = continueBook,
        lastSessionBookId = lastSessionId?.takeIf { id -> allBooks.any { it.id == id } },
        searchQuery = query,
        layoutMode = settings.libraryLayout,
        sortOrder = settings.librarySortOrder,
        settings = settings,
        dailyGoalMinutes = settings.dailyGoalMinutes,
        todayReadingSeconds = todaySec,
        totalReadingMinutesAllTime = (settings.totalReadingSecondsAllTime / 60L).toInt(),
        isImporting = status.first,
        errorMessage = status.second,
        infoMessage = status.third,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = LibraryUiState(),
    )

  fun setSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun toggleLayoutMode() {
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        val next =
          if (current.libraryLayout == LibraryLayoutMode.GRID) LibraryLayoutMode.LIST
          else LibraryLayoutMode.GRID
        current.copy(libraryLayout = next)
      }
    }
  }

  fun setSortOrder(order: LibrarySortOrder) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(librarySortOrder = order) }
    }
  }

  fun setDailyGoalMinutes(minutes: Int) {
    viewModelScope.launch {
      readerRepository.setDailyGoalMinutes(minutes)
    }
  }

  fun importBookFromUri(uri: Uri, onImported: (String) -> Unit = {}) {
    viewModelScope.launch {
      isImporting.value = true
      errorMessage.value = null
      libraryRepository
        .importFromUri(uri)
        .onSuccess { book ->
          infoMessage.value = "Imported \"${book.title}\""
          onImported(book.id)
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to import selected file."
        }
      isImporting.value = false
    }
  }

  fun loadSampleClassics() {
    viewModelScope.launch {
      isImporting.value = true
      errorMessage.value = null
      libraryRepository
        .importSampleClassics()
        .onSuccess { books ->
          infoMessage.value = "Added ${books.size} sample books to your library"
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to load sample books."
        }
      isImporting.value = false
    }
  }

  fun deleteBook(book: BookEntity) {
    viewModelScope.launch {
      libraryRepository.deleteBook(book)
      if (uiState.value.lastSessionBookId == book.id) {
        readerRepository.setLastOpenedBookId(null)
      }
      infoMessage.value = "Removed \"${book.title}\""
    }
  }

  fun clearMessages() {
    errorMessage.update { null }
    infoMessage.update { null }
  }

  class Factory(
    private val libraryRepository: LibraryRepository,
    private val readerRepository: ReaderRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return LibraryViewModel(libraryRepository, readerRepository) as T
    }
  }
}
