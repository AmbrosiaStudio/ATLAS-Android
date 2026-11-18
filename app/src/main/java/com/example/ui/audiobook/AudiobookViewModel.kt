package com.example.ui.audiobook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.engine.audio.AmbientSoundPlayer
import com.example.engine.audiobook.AudiobookEngine
import com.example.engine.audiobook.AudiobookManifest
import com.example.engine.audiobook.AudiobookPlaybackState
import com.example.engine.audiobook.AudiobookPlayerController
import com.example.engine.insights.ReadingInsightsEngine
import com.example.engine.tts.TtsMediaBridge
import com.example.engine.tts.TtsMediaCallbacks
import com.example.model.AmbientSoundType
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.ReaderSettings
import com.example.model.SleepTimerPreset
import com.example.ui.reader.SleepTimerState
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudiobookUiState(
  val book: BookEntity? = null,
  val manifest: AudiobookManifest? = null,
  val playback: AudiobookPlaybackState = AudiobookPlaybackState(),
  val settings: ReaderSettings = ReaderSettings(),
  val companionBook: BookEntity? = null,
  val eligibleCompanionBooks: List<BookEntity> = emptyList(),
  val sleepTimer: SleepTimerState = SleepTimerState(),
  val activeAmbientSound: AmbientSoundType = AmbientSoundType.NONE,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val toastMessage: String? = null,
) {
  val overallProgressPercent: Float
    get() {
      val m = manifest ?: return book?.progressPercent ?: 0f
      return AudiobookEngine.computeOverallProgressPercent(
        manifest = m,
        trackIndex = playback.currentTrackIndex,
        positionInTrackMs = playback.positionInTrackMs,
      )
    }
}

class AudiobookViewModel(
  context: Context,
  private val bookId: String,
  private val readerRepository: ReaderRepository,
  private val libraryRepository: LibraryRepository,
  private val audiobookEngine: AudiobookEngine,
  private val readingInsightsEngine: ReadingInsightsEngine? = null,
) : ViewModel() {

  private val appContext = context.applicationContext
  private val ambientPlayer = AmbientSoundPlayer()
  private val player =
    AudiobookPlayerController(
      context = appContext,
      onTrackOrBookEnded = { finishedIdx, wasLast ->
        handleTrackCompleted(finishedIdx, wasLast)
      },
      onPlaybackError = { msg ->
        showToast(msg)
      },
    )

  private val manifestState = MutableStateFlow<AudiobookManifest?>(null)
  private val isLoading = MutableStateFlow(true)
  private val loadError = MutableStateFlow<String?>(null)
  private val sleepTimerState = MutableStateFlow(SleepTimerState())
  private val activeAmbientSound = MutableStateFlow(AmbientSoundType.NONE)
  private val toastMessage = MutableStateFlow<String?>(null)

  private var manifestFile: File? = null
  private var listeningTickerJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var unflushedListeningSeconds = 0
  private var lastPersistedAtMillis = 0L
  private var lastMediaBridgeSyncAtMillis = 0L

  val uiState: StateFlow<AudiobookUiState> =
    combine(
      readerRepository.observeBook(bookId),
      libraryRepository.booksFlow,
      readerRepository.settingsFlow,
      combine(
        manifestState,
        player.state,
        isLoading,
        loadError,
        combine(sleepTimerState, activeAmbientSound, toastMessage) { sl, am, toast ->
          Triple(sl, am, toast)
        },
      ) { m, pb, loading, err, aux ->
        AudiobookAux(m, pb, loading, err, aux.first, aux.second, aux.third)
      },
    ) { book, allBooks, settings, aux ->
      val m = aux.manifest
      val linkedId = m?.linkedBookId
      val companion = linkedId?.let { id -> allBooks.firstOrNull { it.id == id } }
      val eligible =
        allBooks.filter {
          it.id != bookId && it.bookFormat() != BookFormat.AUDIOBOOK
        }
      AudiobookUiState(
        book = book,
        manifest = m,
        playback = aux.playback,
        settings = settings,
        companionBook = companion,
        eligibleCompanionBooks = eligible,
        sleepTimer = aux.sleepTimer,
        activeAmbientSound = aux.ambient,
        isLoading = aux.loading,
        errorMessage = aux.error,
        toastMessage = aux.toast,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AudiobookUiState(),
    )

  private data class AudiobookAux(
    val manifest: AudiobookManifest?,
    val playback: AudiobookPlaybackState,
    val loading: Boolean,
    val error: String?,
    val sleepTimer: SleepTimerState,
    val ambient: AmbientSoundType,
    val toast: String?,
  )

  init {
    loadAudiobook()
    observePlaybackForStatsAndMediaSession()
  }

  private fun loadAudiobook() {
    viewModelScope.launch {
      isLoading.value = true
      val book = readerRepository.getBookById(bookId)
      if (book == null) {
        loadError.value = "Audiobook not found in library."
        isLoading.value = false
        return@launch
      }
      val file = File(book.filePath)
      val loaded = audiobookEngine.loadManifest(file)
      if (loaded == null || loaded.tracks.isEmpty()) {
        loadError.value = "Unable to read audiobook track manifest."
        isLoading.value = false
        return@launch
      }
      manifestFile = file
      manifestState.value = loaded
      player.bindManifest(
        nextManifest = loaded,
        initialTrackIndex = loaded.currentTrackIndex,
        initialPositionInTrackMs = loaded.currentPositionMs,
        initialSpeed = loaded.playbackSpeed,
      )
      readerRepository.setLastOpenedBookId(bookId)
      isLoading.value = false
    }
  }

  private fun observePlaybackForStatsAndMediaSession() {
    TtsMediaBridge.callbacks =
      object : TtsMediaCallbacks {
        override fun onMediaPlay() = play()
        override fun onMediaPause() = pause()
        override fun onMediaNext() = nextTrack()
        override fun onMediaPrevious() = previousTrack()
        override fun onMediaStop() = pause()
        override fun onMediaSeekTo(positionMs: Long) {
          seekTrackPosition(positionMs)
        }
      }

    viewModelScope.launch {
      player.state.collect { pb ->
        val m = manifestState.value
        val now = System.currentTimeMillis()
        if (m != null) {
          val trackTitle = m.tracks.getOrNull(pb.currentTrackIndex)?.title ?: m.title
          if (pb.isPlaying && (now - lastMediaBridgeSyncAtMillis >= 950L || !TtsMediaBridge.isPlaying)) {
            lastMediaBridgeSyncAtMillis = now
            TtsMediaBridge.syncState(
              context = appContext,
              bookId = bookId,
              bookTitle = m.title,
              chapterTitle = trackTitle,
              isPlaying = true,
              durationMs = pb.trackDurationMs,
              positionMs = pb.positionInTrackMs,
              playbackSpeed = pb.playbackSpeed,
            )
          } else if (!pb.isPlaying && TtsMediaBridge.isPlaying) {
            TtsMediaBridge.syncState(
              context = appContext,
              bookId = bookId,
              bookTitle = m.title,
              chapterTitle = trackTitle,
              isPlaying = false,
              durationMs = pb.trackDurationMs,
              positionMs = pb.positionInTrackMs,
              playbackSpeed = pb.playbackSpeed,
            )
          }
          if (pb.isPlaying && now - lastPersistedAtMillis >= 4000L) {
            lastPersistedAtMillis = now
            persistStateInternal(pb, m)
          }
        }
      }
    }

    listeningTickerJob =
      viewModelScope.launch {
        while (true) {
          delay(1000L)
          if (player.state.value.isPlaying) {
            unflushedListeningSeconds += 1
            if (unflushedListeningSeconds >= 10) {
              val delta = unflushedListeningSeconds
              unflushedListeningSeconds = 0
              readerRepository.recordReadingTime(delta)
              readingInsightsEngine?.recordSessionProgress(bookId = bookId, deltaSeconds = delta)
            }
          }
        }
      }
  }

  fun play() {
    player.play()
  }

  fun pause() {
    player.pause()
    flushAndSaveProgressNow()
  }

  fun togglePlayPause() {
    if (player.state.value.isPlaying) pause() else play()
  }

  fun selectTrack(index: Int, autoPlay: Boolean = player.state.value.isPlaying) {
    player.selectTrack(index, positionInTrackMs = 0L, autoPlay = autoPlay)
    flushAndSaveProgressNow()
  }

  fun nextTrack() {
    val m = manifestState.value ?: return
    val next = player.state.value.currentTrackIndex + 1
    if (next < m.tracks.size) {
      selectTrack(next)
    } else {
      player.seekToInCurrentTrack(player.state.value.trackDurationMs)
      pause()
    }
  }

  fun previousTrack() {
    val pb = player.state.value
    if (pb.positionInTrackMs > 5000L || pb.currentTrackIndex == 0) {
      player.seekToInCurrentTrack(0L)
    } else {
      selectTrack(pb.currentTrackIndex - 1)
    }
  }

  fun seekTrackPosition(positionMs: Long) {
    player.seekToInCurrentTrack(positionMs)
  }

  fun skipSeconds(deltaSeconds: Int) {
    player.skipSeconds(deltaSeconds)
  }

  fun setPlaybackSpeed(speed: Float) {
    player.setPlaybackSpeed(speed)
    val m = manifestState.value ?: return
    val updated = m.copy(playbackSpeed = speed.coerceIn(0.75f, 2.5f))
    manifestState.value = updated
    manifestFile?.let { file -> audiobookEngine.saveManifest(file, updated) }
  }

  fun selectSleepTimer(preset: SleepTimerPreset) {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    player.setVolumeMultiplier(1.0f)

    if (preset == SleepTimerPreset.OFF) {
      sleepTimerState.value = SleepTimerState()
      return
    }
    if (preset == SleepTimerPreset.END_OF_CHAPTER) {
      sleepTimerState.value = SleepTimerState(preset = preset, remainingSeconds = 0, isFadingOut = false)
      return
    }
    val totalSec = preset.minutes * 60
    sleepTimerState.value = SleepTimerState(preset = preset, remainingSeconds = totalSec, isFadingOut = false)
    sleepTimerJob =
      viewModelScope.launch {
        var remaining = totalSec
        while (remaining > 0) {
          delay(1000L)
          if (!player.state.value.isPlaying) continue
          remaining -= 1
          val isFading = remaining <= 15
          if (isFading) {
            player.setVolumeMultiplier((remaining / 15f).coerceIn(0.05f, 1.0f))
          }
          sleepTimerState.value =
            SleepTimerState(preset = preset, remainingSeconds = remaining, isFadingOut = isFading)
        }
        pause()
        player.setVolumeMultiplier(1.0f)
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

  fun linkCompanionBook(targetBookId: String?) {
    val m = manifestState.value ?: return
    val updated = m.copy(linkedBookId = targetBookId, lastSyncedAt = System.currentTimeMillis())
    manifestState.value = updated
    manifestFile?.let { file -> audiobookEngine.saveManifest(file, updated) }
  }

  fun pullProgressFromCompanionBook() {
    val companion = uiState.value.companionBook ?: return
    val m = manifestState.value ?: return
    val (targetTrack, targetOffsetMs) =
      AudiobookEngine.resolveTrackAndPositionForPercent(m, companion.progressPercent)
    player.selectTrack(targetTrack, targetOffsetMs, autoPlay = player.state.value.isPlaying)
    val updated =
      m.copy(
        currentTrackIndex = targetTrack,
        currentPositionMs = targetOffsetMs,
        lastSyncedAt = System.currentTimeMillis(),
      )
    manifestState.value = updated
    manifestFile?.let { file -> audiobookEngine.saveManifest(file, updated) }
    flushAndSaveProgressNow()
    val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
    showToast(if (isFa) "پخش روی پیشرفت کتاب تنظیم شد" else "Jumped audio to companion book progress")
  }

  fun pushProgressToCompanionBook() {
    val companion = uiState.value.companionBook ?: return
    val pct = uiState.value.overallProgressPercent
    viewModelScope.launch {
      readerRepository.syncCompanionBookProgress(companion.id, pct)
      val m = manifestState.value
      if (m != null) {
        val updated = m.copy(lastSyncedAt = System.currentTimeMillis())
        manifestState.value = updated
        manifestFile?.let { file -> audiobookEngine.saveManifest(file, updated) }
      }
      val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
      showToast(if (isFa) "درصد پیشرفت به کتاب همراه منتقل شد" else "Synced audio progress to companion book")
    }
  }

  private fun handleTrackCompleted(finishedIdx: Int, wasLastTrack: Boolean) {
    if (sleepTimerState.value.preset == SleepTimerPreset.END_OF_CHAPTER) {
      sleepTimerState.value = SleepTimerState()
      player.setVolumeMultiplier(1.0f)
      pause()
      val isFa = uiState.value.settings.appLanguage == AppLanguage.PERSIAN
      showToast(if (isFa) "تایمر خواب در پایان بخش متوقف شد" else "Sleep timer stopped at end of chapter")
      return
    }
    if (!wasLastTrack) {
      player.selectTrack(finishedIdx + 1, 0L, autoPlay = true)
    } else {
      pause()
      flushAndSaveProgressNow()
    }
  }

  fun flushAndSaveProgressNow() {
    viewModelScope.launch {
      withContext(NonCancellable) {
        if (unflushedListeningSeconds > 0) {
          val delta = unflushedListeningSeconds
          unflushedListeningSeconds = 0
          readerRepository.recordReadingTime(delta)
          readingInsightsEngine?.recordSessionProgress(bookId = bookId, deltaSeconds = delta)
        }
        val m = manifestState.value ?: return@withContext
        persistStateInternal(player.state.value, m)
      }
    }
  }

  private suspend fun persistStateInternal(pb: AudiobookPlaybackState, m: AudiobookManifest) {
    val pct =
      AudiobookEngine.computeOverallProgressPercent(
        manifest = m,
        trackIndex = pb.currentTrackIndex,
        positionInTrackMs = pb.positionInTrackMs,
      )
    val trackDur = pb.trackDurationMs.coerceAtLeast(1L)
    val trackFrac = (pb.positionInTrackMs.toFloat() / trackDur.toFloat()).coerceIn(0f, 1f)
    val nextManifest =
      m.copy(
        currentTrackIndex = pb.currentTrackIndex,
        currentPositionMs = pb.positionInTrackMs,
        playbackSpeed = pb.playbackSpeed,
      )
    manifestState.value = nextManifest
    manifestFile?.let { file -> audiobookEngine.saveManifest(file, nextManifest) }

    readerRepository.saveProgress(
      bookId = bookId,
      chapterOrPage = pb.currentTrackIndex,
      scrollFraction = trackFrac,
      progressPercent = pct,
    )
    readerRepository.recordBookProgressSnapshot(
      bookId = bookId,
      progressPercent = pct,
      pageOrUnitIndex = pb.currentTrackIndex,
      forceInitialOnly = false,
    )
    val companion = uiState.value.companionBook
    if (companion != null && abs(companion.progressPercent - pct) >= 2.5f) {
      // Keep manifest timestamp fresh; explicit Push/Pull controls handle cross-sync so user has control
    }
  }

  fun clearToast() {
    toastMessage.value = null
  }

  private fun showToast(msg: String) {
    toastMessage.value = msg
  }

  override fun onCleared() {
    super.onCleared()
    listeningTickerJob?.cancel()
    sleepTimerJob?.cancel()
    TtsMediaBridge.callbacks = null
    TtsMediaBridge.stopPlaybackService(appContext)
    ambientPlayer.release()
    player.release()
  }

  class Factory(
    private val context: Context,
    private val bookId: String,
    private val readerRepository: ReaderRepository,
    private val libraryRepository: LibraryRepository,
    private val audiobookEngine: AudiobookEngine,
    private val readingInsightsEngine: ReadingInsightsEngine? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AudiobookViewModel(
        context = context,
        bookId = bookId,
        readerRepository = readerRepository,
        libraryRepository = libraryRepository,
        audiobookEngine = audiobookEngine,
        readingInsightsEngine = readingInsightsEngine,
      ) as T
    }
  }
}
