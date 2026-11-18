package com.example.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.config.MonetizationConfig
import com.example.data.local.BookEntity
import com.example.data.local.ShelfEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.model.AppLanguage
import com.example.model.LibraryLayoutMode
import com.example.model.LibrarySortOrder
import com.example.model.LibraryStatusFilter
import com.example.model.ReaderSettings
import com.example.model.ReadingStatus
import com.example.model.TipPromptMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
  val books: List<BookEntity> = emptyList(),
  val totalLibraryCount: Int = 0,
  val recentBooks: List<BookEntity> = emptyList(),
  val continueBook: BookEntity? = null,
  val lastSessionBookId: String? = null,
  val shelves: List<ShelfEntity> = emptyList(),
  val bookShelfMap: Map<String, Set<Long>> = emptyMap(),
  val selectedStatusFilter: LibraryStatusFilter = LibraryStatusFilter.ALL,
  val selectedShelfId: Long? = null,
  val searchQuery: String = "",
  val layoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID,
  val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENT,
  val settings: ReaderSettings = ReaderSettings(),
  val dailyGoalMinutes: Int = 20,
  val todayReadingSeconds: Int = 0,
  val totalReadingMinutesAllTime: Int = 0,
  val isImporting: Boolean = false,
  val shouldSuggestTipJar: Boolean = false,
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

  val hasActiveFilterOrSearch: Boolean
    get() = searchQuery.isNotBlank() || selectedStatusFilter != LibraryStatusFilter.ALL || selectedShelfId != null
}

class LibraryViewModel(
  private val libraryRepository: LibraryRepository,
  private val readerRepository: ReaderRepository,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val statusFilter = MutableStateFlow(LibraryStatusFilter.ALL)
  private val selectedShelfId = MutableStateFlow<Long?>(null)
  private val isImporting = MutableStateFlow(false)
  private val errorMessage = MutableStateFlow<String?>(null)
  private val infoMessage = MutableStateFlow<String?>(null)

  private data class FilterState(
    val query: String,
    val status: LibraryStatusFilter,
    val shelfId: Long?,
  )

  private data class StatusTuple(
    val importing: Boolean,
    val error: String?,
    val info: String?,
  )

  val uiState: StateFlow<LibraryUiState> =
    combine(
      libraryRepository.booksFlow,
      libraryRepository.shelvesFlow,
      libraryRepository.bookShelfRefsFlow,
      readerRepository.settingsFlow,
      combine(
        readerRepository.lastOpenedBookIdFlow,
        combine(searchQuery, statusFilter, selectedShelfId) { q, st, sh -> FilterState(q, st, sh) },
        combine(isImporting, errorMessage, infoMessage) { imp, err, info -> StatusTuple(imp, err, info) },
      ) { lastId, filters, status -> Triple(lastId, filters, status) },
    ) { allBooks, shelves, shelfRefs, settings, aux ->
      val lastSessionId = aux.first
      val filters = aux.second
      val status = aux.third

      val shelfMap = mutableMapOf<String, MutableSet<Long>>()
      shelfRefs.forEach { ref ->
        shelfMap.getOrPut(ref.bookId) { mutableSetOf() }.add(ref.shelfId)
      }
      val activeShelfId =
        filters.shelfId?.takeIf { id -> shelves.any { it.id == id } }

      val afterShelf =
        if (activeShelfId == null) {
          allBooks
        } else {
          allBooks.filter { shelfMap[it.id]?.contains(activeShelfId) == true }
        }

      val afterStatus =
        when (filters.status) {
          LibraryStatusFilter.ALL -> afterShelf
          LibraryStatusFilter.READING -> afterShelf.filter { it.status() == ReadingStatus.READING }
          LibraryStatusFilter.UNREAD -> afterShelf.filter { it.status() == ReadingStatus.UNREAD }
          LibraryStatusFilter.FINISHED -> afterShelf.filter { it.status() == ReadingStatus.FINISHED }
          LibraryStatusFilter.FAVORITES -> afterShelf.filter { it.isFavorite }
        }

      val filtered =
        if (filters.query.isBlank()) {
          afterStatus
        } else {
          val q = filters.query.trim()
          afterStatus.filter {
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
      val cooldownMs = MonetizationConfig.MIN_COOLDOWN_HOURS_BETWEEN_PROMPTS * 3_600_000L
      val cooldownMet =
        settings.lastTipPromptShownAtMillis == 0L ||
          (System.currentTimeMillis() - settings.lastTipPromptShownAtMillis) >= cooldownMs
      val suggestTipJar =
        !settings.isProUnlocked &&
          !settings.hasSupportedViaTipJar &&
          settings.tipPromptMode != TipPromptMode.OFF &&
          cooldownMet &&
          settings.totalReadingSecondsAllTime >= settings.nextTipPromptAtReadingSeconds

      LibraryUiState(
        books = sorted,
        totalLibraryCount = allBooks.size,
        recentBooks = recent,
        continueBook = continueBook,
        lastSessionBookId = lastSessionId?.takeIf { id -> allBooks.any { it.id == id } },
        shelves = shelves,
        bookShelfMap = shelfMap,
        selectedStatusFilter = filters.status,
        selectedShelfId = activeShelfId,
        searchQuery = filters.query,
        layoutMode = settings.libraryLayout,
        sortOrder = settings.librarySortOrder,
        settings = settings,
        dailyGoalMinutes = settings.dailyGoalMinutes,
        todayReadingSeconds = todaySec,
        totalReadingMinutesAllTime = (settings.totalReadingSecondsAllTime / 60L).toInt(),
        isImporting = status.importing,
        shouldSuggestTipJar = suggestTipJar,
        errorMessage = status.error,
        infoMessage = status.info,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = LibraryUiState(),
    )

  fun setSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun setStatusFilter(filter: LibraryStatusFilter) {
    statusFilter.value = filter
  }

  fun selectShelfFilter(shelfId: Long?) {
    selectedShelfId.value = if (selectedShelfId.value == shelfId) null else shelfId
  }

  fun createShelf(name: String) {
    val clean = name.trim()
    if (clean.isBlank()) return
    viewModelScope.launch {
      val id = libraryRepository.createShelf(clean)
      if (id != null && id > 0L) {
        infoMessage.value = "Created shelf \"$clean\""
      }
    }
  }

  fun deleteShelf(shelf: ShelfEntity) {
    viewModelScope.launch {
      if (selectedShelfId.value == shelf.id) {
        selectedShelfId.value = null
      }
      libraryRepository.deleteShelf(shelf.id)
      infoMessage.value = "Removed shelf \"${shelf.name}\""
    }
  }

  fun toggleBookShelf(bookId: String, shelfId: Long, assign: Boolean) {
    viewModelScope.launch {
      libraryRepository.toggleBookShelf(bookId, shelfId, assign)
    }
  }

  fun setBookReadingStatus(bookId: String, status: ReadingStatus) {
    viewModelScope.launch {
      libraryRepository.setReadingStatus(bookId, status)
    }
  }

  fun toggleBookFavorite(book: BookEntity) {
    viewModelScope.launch {
      val next = !book.isFavorite
      libraryRepository.toggleFavorite(book.id, next)
    }
  }

  fun updateBookMetadataAndCover(
    book: BookEntity,
    newTitle: String,
    newAuthor: String,
    newStatus: ReadingStatus,
    isFavorite: Boolean,
    pickedCoverUri: Uri?,
    resetCover: Boolean,
  ) {
    viewModelScope.launch {
      libraryRepository
        .updateBookMetadataAndCover(
          book = book,
          newTitle = newTitle,
          newAuthor = newAuthor,
          newStatus = newStatus,
          isFavorite = isFavorite,
          pickedCoverUri = pickedCoverUri,
          resetCover = resetCover,
        )
        .onSuccess {
          infoMessage.value = "Updated \"${newTitle.trim().ifBlank { book.title }}\""
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to update book metadata."
        }
    }
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

  fun importAudioFilesFromUris(uris: List<Uri>, onImported: (String) -> Unit = {}) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      isImporting.value = true
      errorMessage.value = null
      libraryRepository
        .importAudiobookFiles(uris)
        .onSuccess { book ->
          infoMessage.value = "Imported \"${book.title}\""
          onImported(book.id)
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to import selected audio files."
        }
      isImporting.value = false
    }
  }

  fun importAudioFolderFromUri(treeUri: Uri, onImported: (String) -> Unit = {}) {
    viewModelScope.launch {
      isImporting.value = true
      errorMessage.value = null
      libraryRepository
        .importAudiobookFolder(treeUri)
        .onSuccess { book ->
          infoMessage.value = "Imported \"${book.title}\""
          onImported(book.id)
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to import audiobook folder."
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

  fun dismissTipJarPrompt() {
    viewModelScope.launch {
      val nowMillis = System.currentTimeMillis()
      readerRepository.updateSettings { current ->
        val nextDelay =
          MonetizationConfig.computeNextPromptDelaySeconds(
            isRandomMode = current.tipPromptMode == TipPromptMode.RANDOM_USAGE,
            fixedMinutes = current.tipFixedMinutes,
          )
        current.copy(
          nextTipPromptAtReadingSeconds = current.totalReadingSecondsAllTime + nextDelay,
          lastTipPromptShownAtMillis = nowMillis,
        )
      }
    }
  }

  fun markSupportedViaTipJar() {
    viewModelScope.launch {
      val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
      readerRepository.updateSettings {
        it.copy(
          hasSupportedViaTipJar = true,
          lastTipPromptShownAtMillis = System.currentTimeMillis(),
        )
      }
      infoMessage.value =
        if (isFa) "از حمایت ارزشمند شما سپاسگزاریم!"
        else "Thank you so much for supporting PageNest!"
    }
  }

  fun redeemSupporterCode(code: String): Boolean {
    if (!MonetizationConfig.isValidSupporterCode(code)) return false
    viewModelScope.launch {
      val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
      readerRepository.updateSettings {
        it.copy(
          isProUnlocked = true,
          hasSupportedViaTipJar = true,
          proSupporterNote = code.trim().uppercase(),
        )
      }
      infoMessage.value =
        if (isFa) "نسخه پیج‌نست Pro فعال شد — سپاس از حمایت شما!"
        else "PageNest Pro activated — thank you for your support!"
    }
    return true
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
