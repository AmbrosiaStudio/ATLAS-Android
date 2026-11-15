package com.example.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.data.repository.ReaderRepository
import com.example.engine.cbz.CbzEngine
import com.example.engine.cbz.CbzSession
import com.example.engine.epub.EpubParser
import com.example.engine.fonts.FontManager
import com.example.engine.formats.TextFormatsConverter
import com.example.engine.pdf.PdfEngine
import com.example.engine.pdf.PdfSession
import com.example.engine.translate.TargetLanguageOption
import com.example.engine.translate.TextTranslator
import com.example.engine.translate.TranslationOutcome
import com.example.engine.tts.ListenState
import com.example.engine.tts.ReadAloudController
import com.example.model.BookFormat
import com.example.model.BookSearchMatch
import com.example.model.ComfortPreset
import com.example.model.CustomFontEntry
import com.example.model.EpubChapter
import com.example.model.EpubTocItem
import com.example.model.HighlightPalette
import com.example.model.ParsedEpubBook
import com.example.model.ReaderFontOption
import com.example.model.ReaderMode
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.SelectionAnchor
import com.example.model.TextAlignOption
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReaderDocumentState {
  data object Loading : ReaderDocumentState
  data class EpubReady(
    val book: ParsedEpubBook,
    val fontsDir: File,
  ) : ReaderDocumentState
  data class PdfReady(
    val session: PdfSession,
  ) : ReaderDocumentState
  data class CbzReady(
    val session: CbzSession,
  ) : ReaderDocumentState
  data class Error(val message: String) : ReaderDocumentState
}

sealed interface TranslationUiState {
  data object Idle : TranslationUiState
  data class Loading(val targetLangCode: String) : TranslationUiState
  data class Success(val outcome: TranslationOutcome) : TranslationUiState
  data class Error(val message: String, val targetLangCode: String) : TranslationUiState
}

enum class ReaderSheetType {
  NONE,
  THEME_BRIGHTNESS,
  TYPOGRAPHY,
  INDEX_TOOLS,
}

data class ReaderUiState(
  val book: BookEntity? = null,
  val documentState: ReaderDocumentState = ReaderDocumentState.Loading,
  val settings: ReaderSettings = ReaderSettings(),
  val customFonts: List<CustomFontEntry> = emptyList(),
  val bookmarks: List<BookmarkEntity> = emptyList(),
  val highlights: List<HighlightEntity> = emptyList(),
  val currentChapterOrPage: Int = 0,
  val currentScrollFraction: Float = 0f,
  val pendingAnchor: String? = null,
  val jumpToPdfPageSignal: Pair<Int, Int> = 0 to 0,
  val controlsVisible: Boolean = true,
  val dockExpanded: Boolean = true,
  val readerMode: ReaderMode = ReaderMode.READ,
  val activeSheet: ReaderSheetType = ReaderSheetType.NONE,
  val initialToolsTab: Int = 0,
  val selectionAnchor: SelectionAnchor = SelectionAnchor(),
  val clearSelectionSignal: Int = 0,
  val translationState: TranslationUiState = TranslationUiState.Idle,
  val supportedTranslationTargets: List<TargetLanguageOption> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<BookSearchMatch> = emptyList(),
  val isSearching: Boolean = false,
  val listenState: ListenState = ListenState(),
  val sessionElapsedSeconds: Int = 0,
  val toastMessage: String? = null,
) {
  val selectedText: String
    get() = selectionAnchor.text

  val currentChapter: EpubChapter?
    get() = (documentState as? ReaderDocumentState.EpubReady)?.book?.chapters?.getOrNull(currentChapterOrPage)

  val tocItems: List<EpubTocItem>
    get() = (documentState as? ReaderDocumentState.EpubReady)?.book?.toc.orEmpty()

  val totalUnits: Int
    get() =
      when (val doc = documentState) {
        is ReaderDocumentState.EpubReady -> doc.book.chapters.size
        is ReaderDocumentState.PdfReady -> doc.session.pageCount
        is ReaderDocumentState.CbzReady -> doc.session.pageCount
        else -> book?.totalUnits ?: 1
      }.coerceAtLeast(1)

  val overallProgressPercent: Float
    get() =
      when (documentState) {
        is ReaderDocumentState.EpubReady -> {
          val total = totalUnits.toFloat()
          (((currentChapterOrPage + currentScrollFraction) / total) * 100f).coerceIn(0f, 100f)
        }
        is ReaderDocumentState.PdfReady, is ReaderDocumentState.CbzReady -> {
          if (totalUnits <= 1) 100f
          else (((currentChapterOrPage + 1).toFloat() / totalUnits.toFloat()) * 100f).coerceIn(0f, 100f)
        }
        else -> book?.progressPercent ?: 0f
      }

  val formattedTimeRemaining: String
    get() {
      val totalMinutes =
        when (documentState) {
          is ReaderDocumentState.EpubReady -> {
            val remainingUnits = (totalUnits - currentChapterOrPage - currentScrollFraction).coerceAtLeast(0.1f)
            remainingUnits * 4.5f
          }
          is ReaderDocumentState.PdfReady, is ReaderDocumentState.CbzReady -> {
            val remainingPages = (totalUnits - currentChapterOrPage).coerceAtLeast(1)
            remainingPages * 1.4f
          }
          else -> 12f
        }
      val totalSeconds = (totalMinutes * 60f).roundToInt().coerceAtLeast(30)
      val mins = (totalSeconds / 60).coerceAtMost(999)
      val secs = totalSeconds % 60
      return String.format("%d:%02d", mins, secs)
    }

  val isCurrentLocationBookmarked: Boolean
    get() {
      val isFixedPage = book?.bookFormat()?.isReflowableText == false
      return bookmarks.any { bm ->
        bm.chapterOrPageIndex == currentChapterOrPage &&
          (isFixedPage || abs(bm.scrollFraction - currentScrollFraction) < 0.08f)
      }
    }

  val currentChapterHighlights: List<HighlightEntity>
    get() = highlights.filter { it.chapterOrPageIndex == currentChapterOrPage }
}

class ReaderViewModel(
  context: Context,
  private val bookId: String,
  private val readerRepository: ReaderRepository,
  private val epubParser: EpubParser,
  private val pdfEngine: PdfEngine,
  private val textFormatsConverter: TextFormatsConverter,
  private val cbzEngine: CbzEngine,
  private val fontManager: FontManager,
  private val textTranslator: TextTranslator,
) : ViewModel() {

  private val readAloud = ReadAloudController(context)

  private val docState = MutableStateFlow<ReaderDocumentState>(ReaderDocumentState.Loading)
  private val positionState = MutableStateFlow(PositionState())
  private val chromeState = MutableStateFlow(ChromeState())
  private val searchState = MutableStateFlow(SearchState())
  private val translationState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
  private val sessionElapsedSeconds = MutableStateFlow(0)

  private var saveJob: Job? = null
  private var searchJob: Job? = null
  private var translateJob: Job? = null
  private var readingTimerJob: Job? = null
  private var unflushedReadingSeconds: Int = 0
  private val visitedPageKeysInSession = mutableSetOf<String>()
  private var isSessionPageBaselineRecorded = false
  private var pdfSessionRef: PdfSession? = null
  private var cbzSessionRef: CbzSession? = null

  private data class PositionState(
    val chapterOrPage: Int = 0,
    val scrollFraction: Float = 0f,
    val pendingAnchor: String? = null,
    val jumpToPdfPageSignal: Pair<Int, Int> = 0 to 0,
  )

  private data class ChromeState(
    val controlsVisible: Boolean = true,
    val dockExpanded: Boolean = true,
    val readerMode: ReaderMode = ReaderMode.READ,
    val activeSheet: ReaderSheetType = ReaderSheetType.NONE,
    val initialToolsTab: Int = 0,
    val selectionAnchor: SelectionAnchor = SelectionAnchor(),
    val clearSelectionSignal: Int = 0,
    val toastMessage: String? = null,
  )

  private data class SearchState(
    val query: String = "",
    val results: List<BookSearchMatch> = emptyList(),
    val isSearching: Boolean = false,
  )

  private data class AuxTuple(
    val doc: ReaderDocumentState,
    val pos: PositionState,
    val chrome: ChromeState,
    val search: SearchState,
    val tts: ListenState,
    val customFonts: List<CustomFontEntry>,
    val translation: TranslationUiState,
    val sessionSeconds: Int,
  )

  val uiState: StateFlow<ReaderUiState> =
    combine(
      readerRepository.observeBook(bookId),
      readerRepository.settingsFlow,
      readerRepository.observeBookmarks(bookId),
      readerRepository.observeHighlights(bookId),
      combine(
        docState,
        positionState,
        chromeState,
        searchState,
        combine(
          readAloud.state,
          fontManager.customFontsFlow,
          translationState,
          sessionElapsedSeconds,
        ) { tts, fonts, trans, sec -> SessionAux(tts, fonts, trans, sec) },
      ) { d, p, c, s, sa ->
        AuxTuple(d, p, c, s, sa.tts, sa.customFonts, sa.translation, sa.sessionSeconds)
      },
    ) { book, settings, bookmarks, highlights, aux ->
      ReaderUiState(
        book = book,
        documentState = aux.doc,
        settings = settings,
        customFonts = aux.customFonts,
        bookmarks = bookmarks,
        highlights = highlights,
        currentChapterOrPage = aux.pos.chapterOrPage,
        currentScrollFraction = aux.pos.scrollFraction,
        pendingAnchor = aux.pos.pendingAnchor,
        jumpToPdfPageSignal = aux.pos.jumpToPdfPageSignal,
        controlsVisible = aux.chrome.controlsVisible,
        dockExpanded = aux.chrome.dockExpanded,
        readerMode = aux.chrome.readerMode,
        activeSheet = aux.chrome.activeSheet,
        initialToolsTab = aux.chrome.initialToolsTab,
        selectionAnchor = aux.chrome.selectionAnchor,
        clearSelectionSignal = aux.chrome.clearSelectionSignal,
        translationState = aux.translation,
        supportedTranslationTargets = textTranslator.supportedTargets,
        searchQuery = aux.search.query,
        searchResults = aux.search.results,
        isSearching = aux.search.isSearching,
        listenState = aux.tts,
        sessionElapsedSeconds = aux.sessionSeconds,
        toastMessage = aux.chrome.toastMessage,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ReaderUiState(supportedTranslationTargets = textTranslator.supportedTargets),
    )

  private data class SessionAux(
    val tts: ListenState,
    val customFonts: List<CustomFontEntry>,
    val translation: TranslationUiState,
    val sessionSeconds: Int,
  )

  init {
    loadDocument()
    observeTtsConfig()
  }

  private fun observeTtsConfig() {
    viewModelScope.launch {
      combine(readerRepository.settingsFlow, readAloud.state) { settings, tts ->
        Triple(settings.ttsSpeechRate, settings.ttsPitch, settings.ttsVoiceName) to tts.isReady
      }.collect { (cfg, ready) ->
        if (ready) {
          readAloud.applyConfig(cfg.first, cfg.second, cfg.third)
        }
      }
    }
  }

  private fun loadDocument() {
    viewModelScope.launch {
      docState.value = ReaderDocumentState.Loading
      val book = readerRepository.getBookById(bookId)
      if (book == null) {
        docState.value = ReaderDocumentState.Error("This book could not be found in your library.")
        return@launch
      }

      readerRepository.setLastOpenedBookId(book.id)

      val file = File(book.filePath)
      if (!file.exists()) {
        docState.value = ReaderDocumentState.Error("The local book file is missing.")
        return@launch
      }

      val bookFormat = book.bookFormat()
      when (bookFormat) {
        BookFormat.EPUB, BookFormat.TXT, BookFormat.MD, BookFormat.FB2 -> {
          val fontsDir = epubParser.ensureFontsCopied()
          val loadResult =
            if (bookFormat == BookFormat.EPUB) {
              epubParser.loadBook(file, book.id, book.title)
            } else {
              textFormatsConverter.loadBook(file, bookFormat, book.id, book.title)
            }
          loadResult
            .onSuccess { parsed ->
              val safeIndex = book.currentChapterOrPage.coerceIn(0, parsed.chapters.lastIndex)
              positionState.value =
                PositionState(
                  chapterOrPage = safeIndex,
                  scrollFraction = book.currentScrollFraction.coerceIn(0f, 1f),
                )
              readerRepository.updateTotalUnits(book.id, parsed.chapters.size)
              docState.value = ReaderDocumentState.EpubReady(parsed, fontsDir)
              val startPercent =
                ((safeIndex + book.currentScrollFraction.coerceIn(0f, 1f)) /
                    parsed.chapters.size.coerceAtLeast(1).toFloat()) * 100f
              readerRepository.recordBookProgressSnapshot(
                bookId = book.id,
                progressPercent = startPercent,
                pageOrUnitIndex = safeIndex,
                forceInitialOnly = true,
              )
              persistProgressNow()
              startReadingTimer()
            }
            .onFailure { err ->
              docState.value =
                ReaderDocumentState.Error(err.message ?: "Failed to open ${bookFormat.name} document.")
            }
        }
        BookFormat.PDF -> {
          pdfEngine
            .openSession(file)
            .onSuccess { session ->
              pdfSessionRef = session
              val safePage = book.currentChapterOrPage.coerceIn(0, session.pageCount - 1)
              positionState.value =
                PositionState(
                  chapterOrPage = safePage,
                  scrollFraction = 0f,
                  jumpToPdfPageSignal = safePage to 1,
                )
              readerRepository.updateTotalUnits(book.id, session.pageCount)
              docState.value = ReaderDocumentState.PdfReady(session)
              val startPercent =
                if (session.pageCount <= 1) 100f
                else (((safePage + 1).toFloat() / session.pageCount.toFloat()) * 100f)
              readerRepository.recordBookProgressSnapshot(
                bookId = book.id,
                progressPercent = startPercent,
                pageOrUnitIndex = safePage,
                forceInitialOnly = true,
              )
              persistProgressNow()
              startReadingTimer()
            }
            .onFailure { err ->
              docState.value =
                ReaderDocumentState.Error(err.message ?: "Failed to render PDF document.")
            }
        }
        BookFormat.CBZ -> {
          cbzEngine
            .openSession(file)
            .onSuccess { session ->
              cbzSessionRef = session
              val safePage = book.currentChapterOrPage.coerceIn(0, session.pageCount - 1)
              positionState.value =
                PositionState(
                  chapterOrPage = safePage,
                  scrollFraction = 0f,
                  jumpToPdfPageSignal = safePage to 1,
                )
              readerRepository.updateTotalUnits(book.id, session.pageCount)
              docState.value = ReaderDocumentState.CbzReady(session)
              val startPercent =
                if (session.pageCount <= 1) 100f
                else (((safePage + 1).toFloat() / session.pageCount.toFloat()) * 100f)
              readerRepository.recordBookProgressSnapshot(
                bookId = book.id,
                progressPercent = startPercent,
                pageOrUnitIndex = safePage,
                forceInitialOnly = true,
              )
              persistProgressNow()
              startReadingTimer()
            }
            .onFailure { err ->
              docState.value =
                ReaderDocumentState.Error(err.message ?: "Failed to open CBZ comic archive.")
            }
        }
      }
    }
  }

  private fun startReadingTimer() {
    if (readingTimerJob?.isActive == true) return
    readingTimerJob =
      viewModelScope.launch {
        while (true) {
          delay(1000L)
          sessionElapsedSeconds.update { it + 1 }
          unflushedReadingSeconds++
          if (unflushedReadingSeconds >= 5) {
            val delta = unflushedReadingSeconds
            unflushedReadingSeconds = 0
            readerRepository.recordReadingTime(delta)
          }
        }
      }
  }

  fun toggleControls() {
    chromeState.update { it.copy(controlsVisible = !it.controlsVisible) }
  }

  fun toggleDockExpanded() {
    chromeState.update { it.copy(dockExpanded = !it.dockExpanded) }
  }

  fun openSheet(sheet: ReaderSheetType, initialToolsTab: Int = 0) {
    chromeState.update { it.copy(activeSheet = sheet, initialToolsTab = initialToolsTab) }
  }

  fun closeSheet() {
    chromeState.update { it.copy(activeSheet = ReaderSheetType.NONE) }
  }

  fun setReaderMode(mode: ReaderMode) {
    chromeState.update { it.copy(readerMode = mode) }
    if (mode == ReaderMode.LISTEN) {
      prepareTtsForCurrentChapter()
    } else {
      readAloud.pause()
    }
  }

  fun togglePlayPauseTts() {
    if (uiState.value.listenState.totalChunks == 0) {
      prepareTtsForCurrentChapter(autoPlay = true)
    } else {
      readAloud.togglePlayPause()
    }
  }

  fun cycleTtsSpeed() {
    val next = readAloud.cycleSpeed()
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(ttsSpeechRate = next) }
    }
  }

  private fun prepareTtsForCurrentChapter(autoPlay: Boolean = false) {
    viewModelScope.launch {
      val doc = docState.value
      if (doc is ReaderDocumentState.EpubReady) {
        val chapter = doc.book.chapters.getOrNull(positionState.value.chapterOrPage) ?: return@launch
        val text = epubParser.extractChapterPlainText(chapter)
        if (text.isBlank()) {
          showToast("No spoken text found in this section.")
          return@launch
        }
        readAloud.setContent(text) {
          val next = positionState.value.chapterOrPage + 1
          if (next < doc.book.chapters.size) {
            goToChapter(next)
            prepareTtsForCurrentChapter(autoPlay = true)
          }
        }
        if (autoPlay) readAloud.play()
      } else {
        showToast("Listen mode works with reflowable text books.")
      }
    }
  }

  fun onEpubScrollFractionChanged(fraction: Float) {
    positionState.update { it.copy(scrollFraction = fraction.coerceIn(0f, 1f), pendingAnchor = null) }
    scheduleProgressSave()
  }

  fun onReadingUnitViewed(unitKey: String) {
    if (unitKey.isBlank()) return
    if (!isSessionPageBaselineRecorded) {
      isSessionPageBaselineRecorded = true
      visitedPageKeysInSession.add(unitKey)
      return
    }
    if (visitedPageKeysInSession.add(unitKey)) {
      viewModelScope.launch {
        readerRepository.recordPagesRead(1)
      }
    }
  }

  fun flushReadingStatsNow() {
    viewModelScope.launch {
      if (unflushedReadingSeconds > 0) {
        val delta = unflushedReadingSeconds
        unflushedReadingSeconds = 0
        readerRepository.recordReadingTime(delta)
      }
      persistProgressSync()
    }
  }

  fun onPdfPageChanged(page: Int) {
    val total = uiState.value.totalUnits
    val safe = page.coerceIn(0, total - 1)
    if (positionState.value.chapterOrPage != safe) {
      positionState.update { it.copy(chapterOrPage = safe, scrollFraction = 0f) }
      scheduleProgressSave()
    }
  }

  fun jumpToPdfPage(page: Int) {
    val total = uiState.value.totalUnits
    val safe = page.coerceIn(0, total - 1)
    val nonce = positionState.value.jumpToPdfPageSignal.second + 1
    positionState.update {
      it.copy(chapterOrPage = safe, scrollFraction = 0f, jumpToPdfPageSignal = safe to nonce)
    }
    persistProgressNow()
  }

  fun goToChapter(chapterIndex: Int, scrollFraction: Float = 0f, anchor: String? = null) {
    val total = uiState.value.totalUnits
    val safe = chapterIndex.coerceIn(0, total - 1)
    positionState.update {
      it.copy(
        chapterOrPage = safe,
        scrollFraction = scrollFraction.coerceIn(0f, 1f),
        pendingAnchor = anchor,
      )
    }
    chromeState.update { it.copy(selectionAnchor = SelectionAnchor(), activeSheet = ReaderSheetType.NONE) }
    translationState.value = TranslationUiState.Idle
    if (chromeState.value.readerMode == ReaderMode.LISTEN) {
      prepareTtsForCurrentChapter(autoPlay = uiState.value.listenState.isPlaying)
    }
    persistProgressNow()
  }

  fun navigateChapterDelta(delta: Int) {
    val target = positionState.value.chapterOrPage + delta
    if (target in 0 until uiState.value.totalUnits) {
      goToChapter(target, 0f)
    }
  }

  fun scrubToOverallProgress(percent: Float) {
    val clamped = percent.coerceIn(0f, 100f)
    when (val doc = docState.value) {
      is ReaderDocumentState.EpubReady -> {
        val total = doc.book.chapters.size.coerceAtLeast(1)
        val exact = (clamped / 100f) * total
        val chapterIdx = exact.toInt().coerceIn(0, total - 1)
        val frac = (exact - chapterIdx).coerceIn(0f, 0.98f)
        goToChapter(chapterIdx, frac)
      }
      is ReaderDocumentState.PdfReady -> {
        val total = doc.session.pageCount.coerceAtLeast(1)
        val page = (((clamped / 100f) * (total - 1)).roundToInt()).coerceIn(0, total - 1)
        jumpToPdfPage(page)
      }
      is ReaderDocumentState.CbzReady -> {
        val total = doc.session.pageCount.coerceAtLeast(1)
        val page = (((clamped / 100f) * (total - 1)).roundToInt()).coerceIn(0, total - 1)
        jumpToPdfPage(page)
      }
      else -> Unit
    }
  }

  fun onSelectionChanged(anchor: SelectionAnchor) {
    chromeState.update { it.copy(selectionAnchor = anchor) }
  }

  fun clearSelection() {
    chromeState.update {
      it.copy(selectionAnchor = SelectionAnchor(), clearSelectionSignal = it.clearSelectionSignal + 1)
    }
  }

  fun highlightSelection(colorHex: String, note: String = "") {
    val text = chromeState.value.selectionAnchor.text
    if (text.isBlank()) return
    viewModelScope.launch {
      readerRepository.addHighlight(
        bookId = bookId,
        chapterOrPageIndex = positionState.value.chapterOrPage,
        selectedText = text,
        colorHex = colorHex,
        note = note.trim(),
      )
      clearSelection()
      showToast(if (note.isNotBlank()) "Highlight & note saved" else "Passage highlighted")
    }
  }

  fun speakSelection() {
    val text = chromeState.value.selectionAnchor.text
    if (text.isBlank()) return
    readAloud.speakSample(text)
    showToast("Speaking selection")
  }

  fun translateSelection(targetLangCode: String = uiState.value.settings.translateTargetLang) {
    val text = chromeState.value.selectionAnchor.text
    if (text.isBlank()) return
    translateJob?.cancel()
    translateJob =
      viewModelScope.launch {
        readerRepository.updateSettings { it.copy(translateTargetLang = targetLangCode) }
        translationState.value = TranslationUiState.Loading(targetLangCode)
        textTranslator
          .translate(text, targetLangCode)
          .onSuccess { outcome ->
            translationState.value = TranslationUiState.Success(outcome)
          }
          .onFailure { err ->
            translationState.value =
              TranslationUiState.Error(
                message = err.message ?: "Unable to download or run translation model.",
                targetLangCode = targetLangCode,
              )
          }
      }
  }

  fun saveTranslationAsNote(colorHex: String = HighlightPalette.LAVENDER) {
    val current = translationState.value as? TranslationUiState.Success ?: return
    val text = chromeState.value.selectionAnchor.text
    if (text.isBlank()) return
    viewModelScope.launch {
      readerRepository.addHighlight(
        bookId = bookId,
        chapterOrPageIndex = positionState.value.chapterOrPage,
        selectedText = text,
        colorHex = colorHex,
        note = current.outcome.translatedText,
      )
      translationState.value = TranslationUiState.Idle
      clearSelection()
      showToast("Translation saved as note")
    }
  }

  fun dismissTranslation() {
    translateJob?.cancel()
    translationState.value = TranslationUiState.Idle
  }

  fun bookmarkSelection() {
    val text = chromeState.value.selectionAnchor.text
    val pos = positionState.value
    val label = uiState.value.currentChapter?.title ?: "Page ${pos.chapterOrPage + 1}"
    viewModelScope.launch {
      readerRepository.addBookmark(
        bookId = bookId,
        title = label,
        chapterOrPageIndex = pos.chapterOrPage,
        scrollFraction = pos.scrollFraction,
        snippet = text,
      )
      clearSelection()
      showToast("Bookmark saved")
    }
  }

  fun toggleCurrentBookmark() {
    val state = uiState.value
    val pos = positionState.value
    val isFixedPage = state.book?.bookFormat()?.isReflowableText == false
    viewModelScope.launch {
      if (state.isCurrentLocationBookmarked) {
        if (isFixedPage) {
          state.bookmarks
            .filter { it.chapterOrPageIndex == pos.chapterOrPage }
            .forEach { readerRepository.deleteBookmark(it.id) }
        } else {
          readerRepository.removeBookmarkNear(bookId, pos.chapterOrPage, pos.scrollFraction)
        }
        showToast("Bookmark removed")
      } else {
        val title =
          if (isFixedPage) "Page ${pos.chapterOrPage + 1}"
          else state.currentChapter?.title ?: "Section ${pos.chapterOrPage + 1}"
        val snippet =
          if (isFixedPage)
            "Bookmarked at ${(state.overallProgressPercent).roundToInt()}%"
          else "Saved at ${(pos.scrollFraction * 100).roundToInt()}% of section"
        readerRepository.addBookmark(
          bookId = bookId,
          title = title,
          chapterOrPageIndex = pos.chapterOrPage,
          scrollFraction = pos.scrollFraction,
          snippet = snippet,
        )
        showToast("Bookmark saved")
      }
    }
  }

  fun deleteBookmark(id: Long) {
    viewModelScope.launch { readerRepository.deleteBookmark(id) }
  }

  fun deleteHighlight(id: Long) {
    viewModelScope.launch { readerRepository.deleteHighlight(id) }
  }

  fun runSearch(query: String) {
    searchState.update { it.copy(query = query) }
    searchJob?.cancel()
    if (query.trim().length < 2) {
      searchState.update { it.copy(results = emptyList(), isSearching = false) }
      return
    }
    searchJob =
      viewModelScope.launch {
        searchState.update { it.copy(isSearching = true) }
        delay(220)
        val matches =
          when (val doc = docState.value) {
            is ReaderDocumentState.EpubReady -> epubParser.searchInBook(doc.book.chapters, query)
            is ReaderDocumentState.PdfReady -> {
              val path = uiState.value.book?.filePath
              if (path != null) pdfEngine.searchTextInPdf(File(path), query) else emptyList()
            }
            else -> emptyList()
          }
        searchState.update { it.copy(results = matches, isSearching = false) }
      }
  }

  fun setReadingLayoutMode(mode: ReadingLayoutMode) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(readingLayoutMode = mode) } }
  }

  fun setThemeMode(mode: ReadingThemeMode) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(themeMode = mode) } }
  }

  fun setBrightnessOverride(brightness: Float) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(brightnessOverride = brightness) } }
  }

  fun setFontOption(font: ReaderFontOption) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(fontOption = font, customFontId = null) }
    }
  }

  fun setCustomFont(fontId: String) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(customFontId = fontId) }
    }
  }

  fun setFontSize(sizeSp: Int) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(fontSizeSp = sizeSp.coerceIn(12, 32)) } }
  }

  fun setLineHeight(multiplier: Float) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(lineHeightMultiplier = multiplier.coerceIn(1.15f, 2.4f)) } }
  }

  fun setHorizontalMargin(marginDp: Int) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(horizontalMarginDp = marginDp.coerceIn(8, 56)) } }
  }

  fun setVerticalMargin(marginDp: Int) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(verticalMarginDp = marginDp.coerceIn(8, 60)) } }
  }

  fun setParagraphSpacing(em: Float) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(paragraphSpacingEm = em.coerceIn(0.4f, 2.0f)) } }
  }

  fun setLetterSpacing(em: Float) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(letterSpacingEm = em.coerceIn(-0.02f, 0.12f)) } }
  }

  fun setFontWeight(weight: Int) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(fontWeightValue = weight.coerceIn(300, 700)) } }
  }

  fun setEyeComfortWarmth(warmth: Float) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(eyeComfortWarmth = warmth.coerceIn(0.0f, 1.0f)) } }
  }

  fun setShowPageBorder(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(showPageBorder = enabled) } }
  }

  fun setPaperTextureEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(paperTextureEnabled = enabled) } }
  }

  fun setKeepScreenOn(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(keepScreenOn = enabled) } }
  }

  fun setBatterySaverModeEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(batterySaverModeEnabled = enabled) } }
  }

  fun setVolumeKeyPageTurnEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(volumeKeyPageTurnEnabled = enabled) } }
  }

  fun applyComfortPreset(preset: ComfortPreset) {
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        when (preset) {
          ComfortPreset.COMPACT ->
            current.copy(
              fontSizeSp = 16,
              lineHeightMultiplier = 1.4f,
              horizontalMarginDp = 14,
              verticalMarginDp = 12,
              paragraphSpacingEm = 0.6f,
              letterSpacingEm = 0.0f,
              fontWeightValue = 400,
            )
          ComfortPreset.BALANCED ->
            current.copy(
              fontSizeSp = 18,
              lineHeightMultiplier = 1.6f,
              horizontalMarginDp = 22,
              verticalMarginDp = 20,
              paragraphSpacingEm = 0.9f,
              letterSpacingEm = 0.0f,
              fontWeightValue = 400,
              eyeComfortWarmth = 0.0f,
            )
          ComfortPreset.RELAXED ->
            current.copy(
              fontSizeSp = 21,
              lineHeightMultiplier = 1.85f,
              horizontalMarginDp = 32,
              verticalMarginDp = 30,
              paragraphSpacingEm = 1.25f,
              letterSpacingEm = 0.02f,
              fontWeightValue = 400,
            )
          ComfortPreset.NIGHT_EASE ->
            current.copy(
              themeMode = ReadingThemeMode.DARK,
              fontSizeSp = 20,
              lineHeightMultiplier = 1.75f,
              horizontalMarginDp = 26,
              verticalMarginDp = 24,
              paragraphSpacingEm = 1.1f,
              letterSpacingEm = 0.015f,
              fontWeightValue = 500,
              eyeComfortWarmth = 0.35f,
            )
        }
      }
    }
  }

  fun resetComfortDefaults() {
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        current.copy(
          fontSizeSp = 18,
          lineHeightMultiplier = 1.6f,
          horizontalMarginDp = 22,
          verticalMarginDp = 20,
          paragraphSpacingEm = 0.9f,
          letterSpacingEm = 0.0f,
          fontWeightValue = 400,
          eyeComfortWarmth = 0.0f,
          showPageBorder = false,
          paperTextureEnabled = false,
          textAlign = TextAlignOption.LEFT,
        )
      }
      showToast("Reading comfort reset")
    }
  }

  fun setTextAlign(align: TextAlignOption) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(textAlign = align) } }
  }

  fun setPdfThemeFilter(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(pdfThemeFilterEnabled = enabled) } }
  }

  fun closeBookExplicitly(onClosed: () -> Unit) {
    viewModelScope.launch {
      readingTimerJob?.cancel()
      if (unflushedReadingSeconds > 0) {
        val rem = unflushedReadingSeconds
        unflushedReadingSeconds = 0
        readerRepository.recordReadingTime(rem)
      }
      persistProgressSync()
      readerRepository.setLastOpenedBookId(null)
      onClosed()
    }
  }

  fun showMessage(msg: String) {
    showToast(msg)
  }

  fun clearToast() {
    chromeState.update { it.copy(toastMessage = null) }
  }

  private fun showToast(msg: String) {
    chromeState.update { it.copy(toastMessage = msg) }
  }

  private fun scheduleProgressSave() {
    saveJob?.cancel()
    saveJob =
      viewModelScope.launch {
        delay(350)
        persistProgressSync()
      }
  }

  private fun persistProgressNow() {
    viewModelScope.launch { persistProgressSync() }
  }

  private suspend fun persistProgressSync() {
    val state = uiState.value
    readerRepository.saveProgress(
      bookId = bookId,
      chapterOrPage = positionState.value.chapterOrPage,
      scrollFraction = positionState.value.scrollFraction,
      progressPercent = state.overallProgressPercent,
    )
    readerRepository.recordBookProgressSnapshot(
      bookId = bookId,
      progressPercent = state.overallProgressPercent,
      pageOrUnitIndex = positionState.value.chapterOrPage,
      forceInitialOnly = false,
    )
  }

  override fun onCleared() {
    super.onCleared()
    readAloud.release()
    pdfSessionRef?.close()
    pdfSessionRef = null
    cbzSessionRef?.close()
    cbzSessionRef = null
  }

  class Factory(
    private val context: Context,
    private val bookId: String,
    private val readerRepository: ReaderRepository,
    private val epubParser: EpubParser,
    private val pdfEngine: PdfEngine,
    private val textFormatsConverter: TextFormatsConverter,
    private val cbzEngine: CbzEngine,
    private val fontManager: FontManager,
    private val textTranslator: TextTranslator,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ReaderViewModel(
        context = context,
        bookId = bookId,
        readerRepository = readerRepository,
        epubParser = epubParser,
        pdfEngine = pdfEngine,
        textFormatsConverter = textFormatsConverter,
        cbzEngine = cbzEngine,
        fontManager = fontManager,
        textTranslator = textTranslator,
      ) as T
    }
  }
}
