package com.example.engine.audiobook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AudiobookPlaybackState(
  val isPrepared: Boolean = false,
  val isPlaying: Boolean = false,
  val currentTrackIndex: Int = 0,
  val positionInTrackMs: Long = 0L,
  val trackDurationMs: Long = 0L,
  val globalElapsedMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val playbackSpeed: Float = 1.0f,
  val errorMessage: String? = null,
)

/**
 * Hardware MediaPlayer controller for MP3 / M4B / M4A / OGG audiobooks with:
 * - Multi-track playlists & single-file M4B chapter boundary tracking
 * - PlaybackParams speed control (0.75x - 2.5x)
 * - Sleep timer gentle fade-out volume multiplier
 * - AudioFocus & Becoming-Noisy headphone unplug handling
 */
class AudiobookPlayerController(
  private val context: Context,
  private val onTrackOrBookEnded: (finishedTrackIndex: Int, wasLastTrack: Boolean) -> Unit = { _, _ -> },
  private val onPlaybackError: (String) -> Unit = {},
) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var mediaPlayer: MediaPlayer? = null
  private var loadedDataSourceKey: String? = null
  private var manifest: AudiobookManifest? = null
  private var volumeMultiplier: Float = 1.0f
  private var tickerJob: Job? = null
  private var noisyRegistered: Boolean = false
  private var focusRequest: AudioFocusRequest? = null

  private val _state = MutableStateFlow(AudiobookPlaybackState())
  val state: StateFlow<AudiobookPlaybackState> = _state.asStateFlow()

  private val becomingNoisyReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
          pause()
        }
      }
    }

  private val focusListener =
    AudioManager.OnAudioFocusChangeListener { focusChange ->
      when (focusChange) {
        AudioManager.AUDIOFOCUS_LOSS -> pause()
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          runCatching { mediaPlayer?.setVolume(0.25f * volumeMultiplier, 0.25f * volumeMultiplier) }
        }
        AudioManager.AUDIOFOCUS_GAIN -> {
          runCatching { mediaPlayer?.setVolume(volumeMultiplier, volumeMultiplier) }
        }
      }
    }

  fun bindManifest(
    nextManifest: AudiobookManifest,
    initialTrackIndex: Int = nextManifest.currentTrackIndex,
    initialPositionInTrackMs: Long = nextManifest.currentPositionMs,
    initialSpeed: Float = nextManifest.playbackSpeed,
  ) {
    manifest = nextManifest
    val safeTrack = initialTrackIndex.coerceIn(0, (nextManifest.tracks.size - 1).coerceAtLeast(0))
    val track = nextManifest.tracks.getOrNull(safeTrack)
    val safePos = initialPositionInTrackMs.coerceIn(0L, (track?.durationMs ?: 0L).coerceAtLeast(0L))
    val safeSpeed = initialSpeed.coerceIn(0.75f, 2.5f)
    _state.value =
      AudiobookPlaybackState(
        isPrepared = false,
        isPlaying = false,
        currentTrackIndex = safeTrack,
        positionInTrackMs = safePos,
        trackDurationMs = track?.durationMs ?: 0L,
        globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(nextManifest, safeTrack, safePos),
        totalDurationMs = nextManifest.totalDurationMs,
        playbackSpeed = safeSpeed,
      )
    if (track != null) {
      prepareTrackInternal(safeTrack, safePos, autoPlay = false)
    }
  }

  fun play() {
    val activeManifest = manifest ?: return
    val currentIdx = _state.value.currentTrackIndex
    val track = activeManifest.tracks.getOrNull(currentIdx) ?: return
    if (!_state.value.isPrepared || mediaPlayer == null) {
      prepareTrackInternal(currentIdx, _state.value.positionInTrackMs, autoPlay = true)
      return
    }
    if (!requestFocus()) return
    registerNoisyIfNeeded()
    runCatching {
      val mp = mediaPlayer ?: return
      applySpeedToPlayer(mp, _state.value.playbackSpeed)
      mp.setVolume(volumeMultiplier, volumeMultiplier)
      if (!mp.isPlaying) mp.start()
      _state.update { it.copy(isPlaying = true, errorMessage = null) }
      startTicker()
    }.onFailure { err ->
      val msg = err.message ?: "Audio playback failed"
      _state.update { it.copy(isPlaying = false, errorMessage = msg) }
      onPlaybackError(msg)
    }
  }

  fun pause() {
    tickerJob?.cancel()
    tickerJob = null
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.pause()
      }
    }
    syncPositionSnapshot()
    _state.update { it.copy(isPlaying = false) }
  }

  fun togglePlayPause() {
    if (_state.value.isPlaying) pause() else play()
  }

  fun selectTrack(trackIndex: Int, positionInTrackMs: Long = 0L, autoPlay: Boolean = _state.value.isPlaying) {
    val m = manifest ?: return
    if (m.tracks.isEmpty()) return
    val safeTrack = trackIndex.coerceIn(0, m.tracks.lastIndex)
    val track = m.tracks[safeTrack]
    val safePos = positionInTrackMs.coerceIn(0L, track.durationMs.coerceAtLeast(0L))
    val key = trackSourceKey(track)

    if (_state.value.isPrepared && loadedDataSourceKey == key && mediaPlayer != null) {
      val mp = mediaPlayer!!
      val absoluteSeekMs = if (track.isChapterSlice) track.startMs + safePos else safePos
      runCatching { mp.seekTo(absoluteSeekMs.toInt()) }
      _state.update {
        it.copy(
          currentTrackIndex = safeTrack,
          positionInTrackMs = safePos,
          trackDurationMs = track.durationMs,
          globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(m, safeTrack, safePos),
        )
      }
      if (autoPlay && !mp.isPlaying) {
        play()
      }
    } else {
      prepareTrackInternal(safeTrack, safePos, autoPlay = autoPlay)
    }
  }

  fun seekToInCurrentTrack(positionInTrackMs: Long) {
    val m = manifest ?: return
    val idx = _state.value.currentTrackIndex
    val track = m.tracks.getOrNull(idx) ?: return
    val clamped = positionInTrackMs.coerceIn(0L, track.durationMs.coerceAtLeast(0L))
    val targetAbsoluteMs = if (track.isChapterSlice) track.startMs + clamped else clamped
    runCatching {
      if (_state.value.isPrepared) {
        mediaPlayer?.seekTo(targetAbsoluteMs.toInt())
      }
    }
    _state.update {
      it.copy(
        positionInTrackMs = clamped,
        globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(m, idx, clamped),
      )
    }
  }

  fun skipSeconds(deltaSeconds: Int) {
    val m = manifest ?: return
    val tracks = m.tracks
    if (tracks.isEmpty()) return
    val curIdx = _state.value.currentTrackIndex.coerceIn(0, tracks.lastIndex)
    val curTrack = tracks[curIdx]
    val nextPos = _state.value.positionInTrackMs + (deltaSeconds * 1000L)

    when {
      nextPos < 0L -> {
        if (curIdx > 0) {
          val prevTrack = tracks[curIdx - 1]
          val targetInPrev = (prevTrack.durationMs + nextPos).coerceAtLeast(0L)
          selectTrack(curIdx - 1, targetInPrev, autoPlay = _state.value.isPlaying)
        } else {
          seekToInCurrentTrack(0L)
        }
      }
      nextPos >= curTrack.durationMs -> {
        if (curIdx + 1 < tracks.size) {
          val overflow = (nextPos - curTrack.durationMs).coerceAtLeast(0L)
          selectTrack(curIdx + 1, overflow, autoPlay = _state.value.isPlaying)
        } else {
          seekToInCurrentTrack(curTrack.durationMs)
        }
      }
      else -> seekToInCurrentTrack(nextPos)
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    val safe = speed.coerceIn(0.75f, 2.5f)
    _state.update { it.copy(playbackSpeed = safe) }
    val mp = mediaPlayer ?: return
    if (_state.value.isPrepared) {
      applySpeedToPlayer(mp, safe)
    }
  }

  fun setVolumeMultiplier(multiplier: Float) {
    volumeMultiplier = multiplier.coerceIn(0.05f, 1.0f)
    runCatching {
      mediaPlayer?.setVolume(volumeMultiplier, volumeMultiplier)
    }
  }

  private fun prepareTrackInternal(
    trackIndex: Int,
    positionInTrackMs: Long,
    autoPlay: Boolean,
  ) {
    val m = manifest ?: return
    val track = m.tracks.getOrNull(trackIndex) ?: return
    tickerJob?.cancel()
    tickerJob = null
    releasePlayerOnly()

    val mp = MediaPlayer()
    mediaPlayer = mp
    loadedDataSourceKey = trackSourceKey(track)

    runCatching {
      mp.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
      )
      val localFile = track.localPath?.let { File(it) }?.takeIf { it.exists() }
      if (localFile != null) {
        mp.setDataSource(localFile.absolutePath)
      } else {
        mp.setDataSource(context, Uri.parse(track.uriString))
      }
      mp.setOnPreparedListener { preparedPlayer ->
        val rawDur = preparedPlayer.duration.toLong().coerceAtLeast(1000L)
        val effectiveTrackDur = if (track.isChapterSlice) track.durationMs else rawDur
        val clampedPos = positionInTrackMs.coerceIn(0L, effectiveTrackDur)
        val absPos = if (track.isChapterSlice) track.startMs + clampedPos else clampedPos
        if (absPos > 0L) {
          runCatching { preparedPlayer.seekTo(absPos.toInt()) }
        }
        preparedPlayer.setVolume(volumeMultiplier, volumeMultiplier)
        applySpeedToPlayer(preparedPlayer, _state.value.playbackSpeed)
        _state.update {
          it.copy(
            isPrepared = true,
            currentTrackIndex = trackIndex,
            positionInTrackMs = clampedPos,
            trackDurationMs = effectiveTrackDur,
            globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(m, trackIndex, clampedPos),
            errorMessage = null,
          )
        }
        if (autoPlay) {
          play()
        }
      }
      mp.setOnCompletionListener {
        handleNaturalEndOfTrack()
      }
      mp.setOnErrorListener { _, what, extra ->
        val msg = "Audio error ($what/$extra)"
        _state.update { it.copy(isPrepared = false, isPlaying = false, errorMessage = msg) }
        onPlaybackError(msg)
        true
      }
      _state.update {
        it.copy(
          isPrepared = false,
          currentTrackIndex = trackIndex,
          positionInTrackMs = positionInTrackMs,
          trackDurationMs = track.durationMs,
          globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(m, trackIndex, positionInTrackMs),
        )
      }
      mp.prepareAsync()
    }.onFailure { err ->
      releasePlayerOnly()
      val msg = err.message ?: "Unable to open audio file"
      _state.update { it.copy(isPrepared = false, isPlaying = false, errorMessage = msg) }
      onPlaybackError(msg)
    }
  }

  private fun startTicker() {
    tickerJob?.cancel()
    tickerJob =
      scope.launch {
        while (true) {
          delay(250L)
          val mp = mediaPlayer ?: break
          if (!_state.value.isPlaying) break
          syncPositionSnapshot()
          val m = manifest ?: continue
          val idx = _state.value.currentTrackIndex
          val track = m.tracks.getOrNull(idx) ?: continue
          if (track.isChapterSlice && _state.value.positionInTrackMs >= track.durationMs - 150L) {
            handleNaturalEndOfTrack()
          } else if (!mp.isPlaying && _state.value.positionInTrackMs >= track.durationMs - 400L) {
            handleNaturalEndOfTrack()
          }
        }
      }
  }

  private fun syncPositionSnapshot() {
    val mp = mediaPlayer ?: return
    val m = manifest ?: return
    if (!_state.value.isPrepared) return
    val idx = _state.value.currentTrackIndex
    val track = m.tracks.getOrNull(idx) ?: return
    val rawPos = runCatching { mp.currentPosition.toLong() }.getOrDefault(0L).coerceAtLeast(0L)
    val inTrack =
      if (track.isChapterSlice) (rawPos - track.startMs).coerceIn(0L, track.durationMs)
      else rawPos.coerceIn(0L, _state.value.trackDurationMs.coerceAtLeast(track.durationMs))
    _state.update {
      it.copy(
        positionInTrackMs = inTrack,
        globalElapsedMs = AudiobookEngine.computeGlobalElapsedMs(m, idx, inTrack),
      )
    }
  }

  private fun handleNaturalEndOfTrack() {
    val m = manifest ?: return
    val idx = _state.value.currentTrackIndex
    val isLast = idx >= m.tracks.lastIndex
    onTrackOrBookEnded(idx, isLast)
  }

  private fun applySpeedToPlayer(mp: MediaPlayer, speed: Float) {
    runCatching {
      val params: PlaybackParams = mp.playbackParams ?: PlaybackParams()
      params.speed = speed.coerceIn(0.75f, 2.5f)
      val wasPlaying = mp.isPlaying
      mp.playbackParams = params
      if (!wasPlaying && mp.isPlaying) {
        mp.pause()
      }
    }
  }

  private fun trackSourceKey(track: AudioTrackEntry): String =
    track.localPath ?: track.uriString

  private fun requestFocus(): Boolean {
    val mgr = audioManager ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setOnAudioFocusChangeListener(focusListener)
          .build()
      focusRequest = req
      mgr.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      @Suppress("DEPRECATION")
      mgr.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  private fun abandonFocus() {
    val mgr = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { runCatching { mgr.abandonAudioFocusRequest(it) } }
      focusRequest = null
    } else {
      @Suppress("DEPRECATION")
      runCatching { mgr.abandonAudioFocus(focusListener) }
    }
  }

  private fun registerNoisyIfNeeded() {
    if (noisyRegistered) return
    runCatching {
      context.registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
      noisyRegistered = true
    }
  }

  private fun unregisterNoisyIfNeeded() {
    if (!noisyRegistered) return
    noisyRegistered = false
    runCatching { context.unregisterReceiver(becomingNoisyReceiver) }
  }

  private fun releasePlayerOnly() {
    loadedDataSourceKey = null
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.reset()
        mp.release()
      }
    }
    mediaPlayer = null
  }

  fun release() {
    tickerJob?.cancel()
    tickerJob = null
    unregisterNoisyIfNeeded()
    abandonFocus()
    releasePlayerOnly()
    scope.cancel()
  }
}
