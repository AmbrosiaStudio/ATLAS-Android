package com.example.ui.writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.WriterBookEntity
import com.example.data.local.WriterChapterEntity
import com.example.data.repository.ReaderRepository
import com.example.data.repository.WriterRepository
import com.example.engine.writer.WriterEpubExporter
import com.example.model.ReaderSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WriterBookSummary(
  val book: WriterBookEntity,
  val chapterCount: Int,
  val totalWords: Int,
)

data class StudioUiState(
  val settings: ReaderSettings = ReaderSettings(),
  val manuscripts: List<WriterBookSummary> = emptyList(),
  val publishingBookId: String? = null,
  val lastPublishedLibraryBookId: String? = null,
  val message: String? = null,
)

data class EditorUiState(
  val settings: ReaderSettings = ReaderSettings(),
  val book: WriterBookEntity? = null,
  val chapters: List<WriterChapterEntity> = emptyList(),
  val selectedChapterId: Long? = null,
  val draftTitle: String = "",
  val draftBody: String = "",
  val isSaving: Boolean = false,
  val isPublishing: Boolean = false,
  val lastSavedAt: Long = 0L,
  val publishedLibraryBookId: String? = null,
  val message: String? = null,
  val errorMessage: String? = null,
) {
  val activeChapterWords: Int
    get() = WriterEpubExporter.countWords(draftBody)

  val totalBookWords: Int
    get() =
      chapters.sumOf { ch ->
        if (ch.id == selectedChapterId) activeChapterWords
        else WriterEpubExporter.countWords(ch.bodyText)
      }
}

class WriterStudioViewModel(
  private val writerRepository: WriterRepository,
  readerRepository: ReaderRepository,
) : ViewModel() {

  private val publishingId = MutableStateFlow<String?>(null)
  private val publishedLibraryId = MutableStateFlow<String?>(null)
  private val statusMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<StudioUiState> =
    combine(
      readerRepository.settingsFlow,
      writerRepository.allBooksFlow,
      writerRepository.allChaptersFlow,
      publishingId,
      combine(publishedLibraryId, statusMessage) { pubId, msg -> pubId to msg },
    ) { settings, books, chapters, busyId, (libId, msg) ->
      val grouped = chapters.groupBy { it.bookId }
      val summaries =
        books.map { book ->
          val chs = grouped[book.id].orEmpty()
          WriterBookSummary(
            book = book,
            chapterCount = chs.size,
            totalWords = chs.sumOf { WriterEpubExporter.countWords(it.bodyText) },
          )
        }
      StudioUiState(
        settings = settings,
        manuscripts = summaries,
        publishingBookId = busyId,
        lastPublishedLibraryBookId = libId,
        message = msg,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = StudioUiState(),
    )

  fun createManuscript(
    title: String,
    author: String,
    synopsis: String,
    isRtl: Boolean,
    onCreated: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val created = writerRepository.createManuscript(title, author, synopsis, isRtl)
      onCreated(created.id)
    }
  }

  fun deleteManuscript(bookId: String) {
    viewModelScope.launch {
      writerRepository.deleteManuscript(bookId)
    }
  }

  fun publishManuscript(bookId: String) {
    if (publishingId.value != null) return
    viewModelScope.launch {
      publishingId.value = bookId
      val result = writerRepository.publishToLibrary(bookId)
      publishingId.value = null
      result
        .onSuccess { libraryBook ->
          publishedLibraryId.value = libraryBook.id
        }
        .onFailure { err ->
          statusMessage.value = err.message ?: "Unable to publish book."
        }
    }
  }

  fun clearMessage() {
    statusMessage.value = null
    publishedLibraryId.value = null
  }

  class Factory(
    private val writerRepository: WriterRepository,
    private val readerRepository: ReaderRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return WriterStudioViewModel(writerRepository, readerRepository) as T
    }
  }
}

@OptIn(FlowPreview::class)
class WriterEditorViewModel(
  private val bookId: String,
  private val writerRepository: WriterRepository,
  private val readerRepository: ReaderRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditorUiState())
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private var autosaveJob: Job? = null
  private var dirtyChapterId: Long? = null

  init {
    viewModelScope.launch {
      combine(
        readerRepository.settingsFlow,
        writerRepository.observeBook(bookId),
        writerRepository.observeChapters(bookId),
      ) { settings, book, chapters ->
        Triple(settings, book, chapters)
      }.collect { (settings, book, chapters) ->
        _uiState.update { current ->
          val currentSelectedId = current.selectedChapterId
          val activeChapter =
            chapters.firstOrNull { it.id == currentSelectedId }
              ?: chapters.firstOrNull()

          val switchingChapter = activeChapter?.id != currentSelectedId
          val nextTitle =
            if (switchingChapter) activeChapter?.title.orEmpty()
            else current.draftTitle
          val nextBody =
            if (switchingChapter) activeChapter?.bodyText.orEmpty()
            else current.draftBody

          current.copy(
            settings = settings,
            book = book,
            chapters = chapters,
            selectedChapterId = activeChapter?.id,
            draftTitle = nextTitle,
            draftBody = nextBody,
          )
        }
      }
    }
  }

  fun onChapterTitleChange(newTitle: String) {
    val chId = _uiState.value.selectedChapterId ?: return
    dirtyChapterId = chId
    _uiState.update { it.copy(draftTitle = newTitle, isSaving = true) }
    scheduleAutosave(chId)
  }

  fun onChapterBodyChange(newBody: String) {
    val chId = _uiState.value.selectedChapterId ?: return
    dirtyChapterId = chId
    _uiState.update { it.copy(draftBody = newBody, isSaving = true) }
    scheduleAutosave(chId)
  }

  private fun scheduleAutosave(chapterId: Long) {
    autosaveJob?.cancel()
    autosaveJob =
      viewModelScope.launch {
        delay(300L)
        persistChapterDraft(chapterId)
      }
  }

  fun selectChapter(chapterId: Long) {
    val state = _uiState.value
    if (chapterId == state.selectedChapterId) return
    val target = state.chapters.firstOrNull { it.id == chapterId } ?: return
    viewModelScope.launch {
      flushPendingDraft()
      _uiState.update {
        it.copy(
          selectedChapterId = target.id,
          draftTitle = target.title,
          draftBody = target.bodyText,
          isSaving = false,
        )
      }
    }
  }

  fun addChapter(title: String? = null) {
    viewModelScope.launch {
      flushPendingDraft()
      val created = writerRepository.addChapter(bookId, title)
      _uiState.update {
        it.copy(
          selectedChapterId = created.id,
          draftTitle = created.title,
          draftBody = created.bodyText,
          isSaving = false,
        )
      }
    }
  }

  fun renameActiveChapter(newTitle: String) {
    val chId = _uiState.value.selectedChapterId ?: return
    val clean = newTitle.trim().ifBlank { return }
    dirtyChapterId = chId
    _uiState.update { it.copy(draftTitle = clean, isSaving = true) }
    viewModelScope.launch {
      flushPendingDraft()
    }
  }

  fun deleteActiveChapter() {
    val state = _uiState.value
    val chId = state.selectedChapterId ?: return
    if (state.chapters.size <= 1) return
    val nextFallback = state.chapters.firstOrNull { it.id != chId }
    viewModelScope.launch {
      autosaveJob?.cancel()
      dirtyChapterId = null
      writerRepository.deleteChapter(bookId, chId)
      if (nextFallback != null) {
        _uiState.update {
          it.copy(
            selectedChapterId = nextFallback.id,
            draftTitle = nextFallback.title,
            draftBody = nextFallback.bodyText,
            isSaving = false,
          )
        }
      }
    }
  }

  fun updateBookDetails(
    title: String,
    author: String,
    synopsis: String,
    isRtl: Boolean,
  ) {
    viewModelScope.launch {
      writerRepository.updateBookDetails(bookId, title, author, synopsis, isRtl)
    }
  }

  fun flushDraftNow() {
    viewModelScope.launch {
      flushPendingDraft()
    }
  }

  private suspend fun flushPendingDraft() {
    autosaveJob?.cancel()
    val chId = dirtyChapterId ?: _uiState.value.selectedChapterId ?: return
    persistChapterDraft(chId)
  }

  private suspend fun persistChapterDraft(chapterId: Long) {
    val state = _uiState.value
    if (state.selectedChapterId != chapterId) return
    val titleToSave = state.draftTitle.ifBlank { "Untitled Chapter" }
    val bodyToSave = state.draftBody
    withContext(NonCancellable) {
      writerRepository.saveChapter(
        bookId = bookId,
        chapterId = chapterId,
        title = titleToSave,
        bodyText = bodyToSave,
      )
    }
    if (dirtyChapterId == chapterId) {
      dirtyChapterId = null
    }
    _uiState.update { it.copy(isSaving = false, lastSavedAt = System.currentTimeMillis()) }
  }

  fun publishToLibrary(successMessage: String) {
    if (_uiState.value.isPublishing) return
    viewModelScope.launch {
      flushPendingDraft()
      _uiState.update { it.copy(isPublishing = true, errorMessage = null) }
      val result = writerRepository.publishToLibrary(bookId)
      result
        .onSuccess { bookEntity ->
          _uiState.update {
            it.copy(
              isPublishing = false,
              publishedLibraryBookId = bookEntity.id,
              message = successMessage,
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isPublishing = false,
              errorMessage = err.message ?: "Unable to build EPUB.",
            )
          }
        }
    }
  }

  suspend fun buildMarkdownShareText(): String {
    flushPendingDraft()
    return writerRepository.buildMarkdownShareText(bookId)
  }

  fun clearMessages() {
    _uiState.update { it.copy(message = null, errorMessage = null) }
  }

  override fun onCleared() {
    val chId = dirtyChapterId ?: _uiState.value.selectedChapterId
    val state = _uiState.value
    if (chId != null && state.selectedChapterId == chId) {
      autosaveJob?.cancel()
      kotlinx.coroutines.CoroutineScope(NonCancellable + kotlinx.coroutines.Dispatchers.IO).launch {
        writerRepository.saveChapter(
          bookId = bookId,
          chapterId = chId,
          title = state.draftTitle.ifBlank { "Untitled Chapter" },
          bodyText = state.draftBody,
        )
      }
    }
    super.onCleared()
  }

  class Factory(
    private val bookId: String,
    private val writerRepository: WriterRepository,
    private val readerRepository: ReaderRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return WriterEditorViewModel(bookId, writerRepository, readerRepository) as T
    }
  }
}
