package com.example.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.data.repository.ReaderRepository
import com.example.engine.audio.AmbientSoundPlayer
import com.example.engine.cbz.CbzEngine
import com.example.engine.cbz.CbzSession
import com.example.engine.epub.EpubParser
import com.example.engine.fonts.FontManager
import com.example.engine.formats.TextFormatsConverter
import com.example.engine.insights.BookReadingInsights
import com.example.engine.insights.ReadingInsightsEngine
import com.example.engine.pdf.PdfEngine
import com.example.engine.pdf.PdfSession
import com.example.engine.translate.TargetLanguageOption
import com.example.engine.translate.TextTranslator
import com.example.engine.translate.TranslationOutcome
import com.example.engine.tts.ListenState
import com.example.engine.tts.ReadAloudController
import com.example.engine.tts.TtsMediaBridge
import com.example.model.AmbientSoundType
import com.example.model.AppLanguage
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
import com.example.model.ReadingRulerStyle
import com.example.model.ReadingThemeMode
import com.example.model.SelectionAnchor
import com.example.model.SleepTimerPreset
import com.example.model.TapZoneAction
import com.example.model.TextAlignOption
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
  FOCUS_AUDIO,
}

data class SleepTimerState(
  val preset: SleepTimerPreset = SleepTimerPreset.OFF,
  val remainingSeconds: Int = 0,
  val isFadingOut: Boolean = false,
)

data class FocusTimerState(
  val isRunning: Boolean = false,
  val isBreakPhase: Boolean = false,
  val remainingSeconds: Int = 25 * 60,
  val workMinutes: Int = 25,
  val breakMinutes: Int = 5,
  val completedCycles: Int = 0,
)

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
  val jumpToEpubSignal: Int = 0,
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
  val sleepTimer: SleepTimerState = SleepTimerState(),
  val activeAmbientSound: AmbientSoundType = AmbientSoundType.NONE,
  val focusTimer: FocusTimerState = FocusTimerState(),
  val liveBrightnessPreview: Float? = null,
  val sessionElapsedSeconds: Int = 0,
  val bookInsights: BookReadingInsights? = null,
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
      val insights = bookInsights
      val totalMinutes =
        if (insights != null && insights.chapters.isNotEmpty()) {
          when (documentState) {
            is ReaderDocumentState.EpubReady -> {
              val chList = insights.chapters
              val curIdx = currentChapterOrPage.coerceIn(0, chList.lastIndex)
              val curRem = chList[curIdx].estimatedTotalMinutes * (1f - currentScrollFraction.coerceIn(0f, 1f))
              val followingRem = chList.drop(curIdx + 1).sumOf { it.estimatedTotalMinutes.toDouble() }.toFloat()
              (curRem + followingRem).coerceAtLeast(0.5f)
            }
            is ReaderDocumentState.PdfReady, is ReaderDocumentState.CbzReady -> {
              val remainingPages = (totalUnits - currentChapterOrPage - 1).coerceAtLeast(1)
              val pph = insights.pagesPerHour.coerceIn(10, 180)
              (remainingPages.toFloat() * 60f) / pph.toFloat()
            }
            else -> insights.remainingEstimatedMinutes.coerceAtLeast(0.5f)
          }
        } else {
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
        }
      val totalSeconds = (totalMinutes * 60f).roundToInt().coerceAtLeast(30)
      val totalMins = totalSeconds / 60
      return if (totalMins >= 60) {
        val h = totalMins / 60
        val m = totalMins % 60
        if (m > 0) "${h}h ${m}m" else "${h}h"
      } else {
        val secs = totalSeconds % 60
        String.format("%d:%02d", totalMins, secs)
      }
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
  private val initialChapterIndex: Int = -1,
  private val readingInsightsEngine: ReadingInsightsEngine? = null,
) : ViewModel() {

  private val appContext = context.applicationContext
  private val readAloud = ReadAloudController(appContext)
  private val ambientPlayer = AmbientSoundPlayer()

  private val docState = MutableStateFlow<ReaderDocumentState>(ReaderDocumentState.Loading)
  private val positionState = MutableStateFlow(PositionState())
  private val chromeState = MutableStateFlow(ChromeState())
  private val searchState = MutableStateFlow(SearchState())
  private val translationState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
  private val sleepTimerState = MutableStateFlow(SleepTimerState())
  private val activeAmbientSound = MutableStateFlow(AmbientSoundType.NONE)
  private val focusTimerState = MutableStateFlow(FocusTimerState())
  private val liveBrightnessPreview = MutableStateFlow<Float?>(null)
  private val sessionElapsedSeconds = MutableStateFlow(0)
  private val bookInsightsState = MutableStateFlow<BookReadingInsights?>(null)

  private var saveJob: Job? = null
  private var searchJob: Job? = null
  private var translateJob: Job? = null
  private var readingTimerJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var focusTimerJob: Job? = null
  private var unflushedReadingSeconds: Int = 0
  private var pendingDeltaWords: Long = 0L
  private var pendingDeltaUnits: Int = 0
  private var lastTelemetryChapterOrPage: Int = 0
  private var lastTelemetryScrollFraction: Float = 0f
  private val visitedPageKeysInSession = mutableSetOf<String>()
  private var isSessionPageBaselineRecorded = false
  private var pdfSessionRef: PdfSession? = null
  private var cbzSessionRef: CbzSession? = null

  private data class PositionState(
    val chapterOrPage: Int = 0,
    val scrollFraction: Float = 0f,
    val pendingAnchor: String? = null,
    val jumpToEpubSignal: Int = 0,
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

  private data class AudioFocusAux(
    val sleep: SleepTimerState,
    val ambient: AmbientSoundType,
    val focus: FocusTimerState,
    val brightnessPreview: Float?,
    val insights: BookReadingInsights?,
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
    val audioFocus: AudioFocusAux,
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
          combine(
            sleepTimerState,
            activeAmbientSound,
            focusTimerState,
            liveBrightnessPreview,
            bookInsightsState,
          ) { sl, am, fo, bp, ins -> AudioFocusAux(sl, am, fo, bp, ins) },
        ) { tts, fonts, trans, sec, af -> SessionAux(tts, fonts, trans, sec, af) },
      ) { d, p, c, s, sa ->
        AuxTuple(d, p, c, s, sa.tts, sa.customFonts, sa.translation, sa.sessionSeconds, sa.audioFocus)
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
        jumpToEpubSignal = aux.pos.jumpToEpubSignal,
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
        sleepTimer = aux.audioFocus.sleep,
        activeAmbientSound = aux.audioFocus.ambient,
        focusTimer = aux.audioFocus.focus,
        liveBrightnessPreview = aux.audioFocus.brightnessPreview,
        sessionElapsedSeconds = aux.sessionSeconds,
        bookInsights = aux.audioFocus.insights,
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
    val audioFocus: AudioFocusAux,
  )

  init {
    loadDocument()
    observeTtsConfig()
    observeMediaControlsBridge()
    observeFocusConfigSync()
    observeInsightsUpdates()
  }

  private fun observeInsightsUpdates() {
    val engine = readingInsightsEngine ?: return
    viewModelScope.launch {
      engine.telemetryUpdatedTick.collect {
        val currentDoc = docState.value
        if (currentDoc !is ReaderDocumentState.Loading && currentDoc !is ReaderDocumentState.Error) {
          refreshBookInsights()
        }
      }
    }
  }

  private suspend fun refreshBookInsights() {
    val engine = readingInsightsEngine ?: return
    val book = readerRepository.getBookById(bookId) ?: return
    val daily = readerRepository.observeAllDailyStats().first()
    val history = readerRepository.observeBookProgressHistory(bookId).first()
    bookInsightsState.value = engine.buildInsightsForBook(book, daily, history)
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

  private fun observeFocusConfigSync() {
    viewModelScope.launch {
      readerRepository.settingsFlow.collect { settings ->
        focusTimerState.update { st ->
          if (!st.isRunning && st.completedCycles == 0 && !st.isBreakPhase &&
            st.remainingSeconds == st.workMinutes * 60
          ) {
            st.copy(
              workMinutes = settings.focusWorkMinutes,
              breakMinutes = settings.focusBreakMinutes,
              remainingSeconds = settings.focusWorkMinutes * 60,
            )
          } else {
            st.copy(
              workMinutes = settings.focusWorkMinutes,
              breakMinutes = settings.focusBreakMinutes,
            )
          }
        }
      }
    }
  }

  private fun observeMediaControlsBridge() {
    TtsMediaBridge.callbacks =
      object : com.example.engine.tts.TtsMediaCallbacks {
        override fun onMediaPlay() {
          if (chromeState.value.readerMode != ReaderMode.LISTEN) {
            chromeState.update { it.copy(readerMode = ReaderMode.LISTEN) }
          }
          if (uiState.value.listenState.totalChunks == 0) {
            prepareTtsForCurrentChapter(autoPlay = true)
          } else {
            readAloud.play()
          }
        }

        override fun onMediaPause() {
          readAloud.pause()
        }

        override fun onMediaNext() {
          skipTtsNext()
        }

        override fun onMediaPrevious() {
          skipTtsPrevious()
        }

        override fun onMediaStop() {
          readAloud.stop()
          chromeState.update { it.copy(readerMode = ReaderMode.READ) }
          TtsMediaBridge.stopPlaybackService(appContext)
        }
      }

    viewModelScope.launch {
      combine(
        readAloud.state,
        positionState,
        docState,
      ) { listen, pos, doc ->
        val chapterTitle =
          (doc as? ReaderDocumentState.EpubReady)?.book?.chapters?.getOrNull(pos.chapterOrPage)?.title
            ?: "Section ${pos.chapterOrPage + 1}"
        Triple(listen.isPlaying, chapterTitle, uiState.value.book?.title ?: "ATLAS")
      }.collect { (playing, chTitle, bookTitle) ->
        if (playing || TtsMediaBridge.serviceRunning) {
          TtsMediaBridge.syncState(
            context = appContext,
            bookId = bookId,
            bookTitle = bookTitle,
            chapterTitle = chTitle,
            isPlaying = playing,
          )
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
              val safeIndex =
                if (initialChapterIndex >= 0) initialChapterIndex.coerceIn(0, parsed.chapters.lastIndex)
                else book.currentChapterOrPage.coerceIn(0, parsed.chapters.lastIndex)
              val safeScroll =
                if (initialChapterIndex >= 0 && initialChapterIndex != book.currentChapterOrPage) 0f
                else book.currentScrollFraction.coerceIn(0f, 1f)
              lastTelemetryChapterOrPage = safeIndex
              lastTelemetryScrollFraction = safeScroll
              positionState.value =
                PositionState(
                  chapterOrPage = safeIndex,
                  scrollFraction = safeScroll,
                )
              readerRepository.updateTotalUnits(book.id, parsed.chapters.size)
              docState.value = ReaderDocumentState.EpubReady(parsed, fontsDir)
              val startPercent =
                ((safeIndex + safeScroll) /
                    parsed.chapters.size.coerceAtLeast(1).toFloat()) * 100f
              readerRepository.recordBookProgressSnapshot(
                bookId = book.id,
                progressPercent = startPercent,
                pageOrUnitIndex = safeIndex,
                forceInitialOnly = true,
              )
              persistProgressNow()
              refreshBookInsights()
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
              val safePage =
                if (initialChapterIndex >= 0) initialChapterIndex.coerceIn(0, session.pageCount - 1)
                else book.currentChapterOrPage.coerceIn(0, session.pageCount - 1)
              lastTelemetryChapterOrPage = safePage
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
              refreshBookInsights()
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
              val safePage =
                if (initialChapterIndex >= 0) initialChapterIndex.coerceIn(0, session.pageCount - 1)
                else book.currentChapterOrPage.coerceIn(0, session.pageCount - 1)
              lastTelemetryChapterOrPage = safePage
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
              refreshBookInsights()
              startReadingTimer()
            }
            .onFailure { err ->
              docState.value =
                ReaderDocumentState.Error(err.message ?: "Failed to open CBZ comic archive.")
            }
        }
        BookFormat.AUDIOBOOK -> {
          docState.value =
            ReaderDocumentState.Error("Audiobooks open in the dedicated Audiobook Player.")
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
            flushSessionTelemetryInternal()
          }
        }
      }
  }

  private suspend fun flushSessionTelemetryInternal() {
    val deltaSec = unflushedReadingSeconds
    val deltaWords = pendingDeltaWords
    val deltaUnits = pendingDeltaUnits
    if (deltaSec <= 0 && deltaWords <= 0L && deltaUnits <= 0) return
    unflushedReadingSeconds = 0
    pendingDeltaWords = 0L
    pendingDeltaUnits = 0
    if (deltaSec > 0) {
      readerRepository.recordReadingTime(deltaSec)
    }
    readingInsightsEngine?.recordSessionProgress(
      bookId = bookId,
      deltaSeconds = deltaSec,
      deltaWords = deltaWords,
      deltaUnits = deltaUnits,
    )
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
      selectSleepTimer(SleepTimerPreset.OFF)
      TtsMediaBridge.stopPlaybackService(appContext)
    }
  }

  fun togglePlayPauseTts() {
    if (uiState.value.listenState.totalChunks == 0) {
      prepareTtsForCurrentChapter(autoPlay = true)
    } else {
      readAloud.togglePlayPause()
    }
  }

  fun skipTtsNext() {
    if (!readAloud.nextChunk()) {
      val doc = docState.value as? ReaderDocumentState.EpubReady ?: return
      val next = positionState.value.chapterOrPage + 1
      if (next < doc.book.chapters.size) {
        goToChapter(next)
        prepareTtsForCurrentChapter(autoPlay = uiState.value.listenState.isPlaying)
      }
    }
  }

  fun skipTtsPrevious() {
    if (!readAloud.previousChunk()) {
      val prev = positionState.value.chapterOrPage - 1
      if (prev >= 0) {
        goToChapter(prev)
        prepareTtsForCurrentChapter(autoPlay = uiState.value.listenState.isPlaying)
      }
    }
  }

  fun cycleTtsSpeed() {
    val next = readAloud.cycleSpeed()
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(ttsSpeechRate = next) }
    }
  }

  fun selectSleepTimer(preset: SleepTimerPreset) {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    readAloud.setVolumeMultiplier(1.0f)

    if (preset == SleepTimerPreset.OFF) {
      sleepTimerState.value = SleepTimerState()
      return
    }
    if (preset == SleepTimerPreset.END_OF_CHAPTER) {
      sleepTimerState.value = SleepTimerState(preset = preset, remainingSeconds = 0, isFadingOut = false)
      return
    }

    val totalSeconds = preset.minutes * 60
    sleepTimerState.value = SleepTimerState(preset = preset, remainingSeconds = totalSeconds, isFadingOut = false)
    sleepTimerJob =
      viewModelScope.launch {
        var remaining = totalSeconds
        while (remaining > 0) {
          delay(1000L)
          remaining--
          val fading = remaining in 1..15
          if (fading) {
            readAloud.setVolumeMultiplier((remaining / 15f).coerceIn(0.08f, 1.0f))
          }
          sleepTimerState.value =
            SleepTimerState(
              preset = preset,
              remainingSeconds = remaining,
              isFadingOut = fading,
            )
        }
        readAloud.pause()
        readAloud.setVolumeMultiplier(1.0f)
        sleepTimerState.value = SleepTimerState()
        val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
        showToast(if (isFa) "تایمر خواب پخش را متوقف کرد" else "Sleep timer paused playback")
      }
  }

  fun selectAmbientSound(type: AmbientSoundType) {
    activeAmbientSound.value = type
    ambientPlayer.start(type, uiState.value.settings.ambientVolume)
  }

  fun setAmbientVolume(volume: Float) {
    val safe = volume.coerceIn(0.05f, 1.0f)
    ambientPlayer.setVolume(safe)
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(ambientVolume = safe) }
    }
  }

  fun toggleFocusTimer() {
    val current = focusTimerState.value
    if (current.isRunning) {
      focusTimerJob?.cancel()
      focusTimerJob = null
      focusTimerState.update { it.copy(isRunning = false) }
      return
    }

    focusTimerState.update { it.copy(isRunning = true) }
    focusTimerJob =
      viewModelScope.launch {
        while (true) {
          delay(1000L)
          val st = focusTimerState.value
          if (!st.isRunning) break
          if (st.remainingSeconds > 1) {
            focusTimerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
          } else {
            val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
            if (!st.isBreakPhase) {
              focusTimerState.update {
                it.copy(
                  isRunning = false,
                  isBreakPhase = true,
                  completedCycles = it.completedCycles + 1,
                  remainingSeconds = it.breakMinutes * 60,
                )
              }
              showToast(if (isFa) "زمان تمرکز کامل شد! وقت استراحت کوتاه" else "Focus session complete! Time for a short break")
            } else {
              focusTimerState.update {
                it.copy(
                  isRunning = false,
                  isBreakPhase = false,
                  remainingSeconds = it.workMinutes * 60,
                )
              }
              showToast(if (isFa) "استراحت پایان یافت! آماده مطالعه" else "Break finished! Ready for focus")
            }
            break
          }
        }
      }
  }

  fun resetFocusTimer() {
    focusTimerJob?.cancel()
    focusTimerJob = null
    val st = focusTimerState.value
    focusTimerState.value =
      FocusTimerState(
        isRunning = false,
        isBreakPhase = false,
        remainingSeconds = st.workMinutes * 60,
        workMinutes = st.workMinutes,
        breakMinutes = st.breakMinutes,
        completedCycles = st.completedCycles,
      )
  }

  fun setFocusDurations(workMinutes: Int, breakMinutes: Int) {
    val safeWork = workMinutes.coerceIn(5, 90)
    val safeBreak = breakMinutes.coerceIn(1, 30)
    viewModelScope.launch {
      readerRepository.updateSettings {
        it.copy(focusWorkMinutes = safeWork, focusBreakMinutes = safeBreak)
      }
    }
    if (!focusTimerState.value.isRunning) {
      focusTimerState.update { st ->
        val nextSec = if (st.isBreakPhase) safeBreak * 60 else safeWork * 60
        st.copy(workMinutes = safeWork, breakMinutes = safeBreak, remainingSeconds = nextSec)
      }
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
          if (sleepTimerState.value.preset == SleepTimerPreset.END_OF_CHAPTER) {
            sleepTimerState.value = SleepTimerState()
            readAloud.setVolumeMultiplier(1.0f)
            val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
            showToast(if (isFa) "تایمر خواب در پایان فصل متوقف شد" else "Sleep timer stopped at end of chapter")
            return@setContent
          }
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
    val clamped = fraction.coerceIn(0f, 1f)
    val ch = positionState.value.chapterOrPage
    if (ch == lastTelemetryChapterOrPage && clamped > lastTelemetryScrollFraction) {
      val deltaFrac = (clamped - lastTelemetryScrollFraction).coerceIn(0f, 0.5f)
      val chapterWords = bookInsightsState.value?.chapters?.getOrNull(ch)?.wordCount ?: 600
      val estWords = (deltaFrac * chapterWords).toLong()
      if (estWords > 0L) {
        pendingDeltaWords += estWords
      }
    }
    lastTelemetryChapterOrPage = ch
    lastTelemetryScrollFraction = clamped
    positionState.update { it.copy(scrollFraction = clamped, pendingAnchor = null) }
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
      pendingDeltaUnits += 1
      viewModelScope.launch {
        readerRepository.recordPagesRead(1)
      }
    }
  }

  fun flushReadingStatsNow() {
    saveJob?.cancel()
    viewModelScope.launch {
      withContext(NonCancellable) {
        flushSessionTelemetryInternal()
        persistProgressSync()
      }
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
    saveJob?.cancel()
    positionState.update {
      it.copy(chapterOrPage = safe, scrollFraction = 0f, jumpToPdfPageSignal = safe to nonce)
    }
    persistProgressNow()
  }

  fun goToChapter(chapterIndex: Int, scrollFraction: Float = 0f, anchor: String? = null) {
    val total = uiState.value.totalUnits
    val safe = chapterIndex.coerceIn(0, total - 1)
    val clampedFrac = scrollFraction.coerceIn(0f, 1f)
    saveJob?.cancel()
    lastTelemetryChapterOrPage = safe
    lastTelemetryScrollFraction = clampedFrac
    positionState.update {
      it.copy(
        chapterOrPage = safe,
        scrollFraction = clampedFrac,
        pendingAnchor = anchor,
        jumpToEpubSignal = it.jumpToEpubSignal + 1,
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
      val targetFraction = if (delta < 0) 1f else 0f
      goToChapter(target, targetFraction)
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
      com.example.widget.WidgetUpdater.requestAllUpdates(appContext)
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
      com.example.widget.WidgetUpdater.requestAllUpdates(appContext)
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
    viewModelScope.launch {
      readerRepository.deleteHighlight(id)
      com.example.widget.WidgetUpdater.requestAllUpdates(appContext)
    }
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
    liveBrightnessPreview.value = null
    viewModelScope.launch { readerRepository.updateSettings { it.copy(brightnessOverride = brightness) } }
  }

  fun previewBrightness(brightness: Float?) {
    liveBrightnessPreview.value = brightness?.coerceIn(0.04f, 1.0f)
  }

  fun setLeftTapAction(action: TapZoneAction) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(leftTapAction = action) } }
  }

  fun setCenterTapAction(action: TapZoneAction) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(centerTapAction = action) } }
  }

  fun setRightTapAction(action: TapZoneAction) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(rightTapAction = action) } }
  }

  fun setEdgeBrightnessSwipeEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(edgeBrightnessSwipeEnabled = enabled) } }
  }

  fun setDoubleTapBookmarkEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(doubleTapBookmarkEnabled = enabled) } }
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

  fun setReadingRulerEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(readingRulerEnabled = enabled) } }
  }

  fun setReadingRulerStyle(style: ReadingRulerStyle) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(readingRulerStyle = style) } }
  }

  fun setReadingRulerHeightDp(heightDp: Int) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(readingRulerHeightDp = heightDp.coerceIn(32, 140)) }
    }
  }

  fun setReadingRulerDimAlpha(alpha: Float) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(readingRulerDimAlpha = alpha.coerceIn(0.15f, 0.75f)) }
    }
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
    saveJob?.cancel()
    readingTimerJob?.cancel()
    viewModelScope.launch {
      withContext(NonCancellable) {
        flushSessionTelemetryInternal()
        persistProgressSync()
        readerRepository.setLastOpenedBookId(null)
      }
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
    sleepTimerJob?.cancel()
    focusTimerJob?.cancel()
    TtsMediaBridge.callbacks = null
    TtsMediaBridge.stopPlaybackService(appContext)
    ambientPlayer.release()
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
    private val initialChapterIndex: Int = -1,
    private val readingInsightsEngine: ReadingInsightsEngine? = null,
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
        initialChapterIndex = initialChapterIndex,
        readingInsightsEngine = readingInsightsEngine,
      ) as T
    }
  }
}
