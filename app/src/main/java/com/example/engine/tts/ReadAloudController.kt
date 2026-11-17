package com.example.engine.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.model.TtsVoiceInfo
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ListenState(
  val isReady: Boolean = false,
  val isPlaying: Boolean = false,
  val speechRate: Float = 1.0f,
  val pitch: Float = 1.0f,
  val selectedVoiceName: String? = null,
  val availableVoices: List<TtsVoiceInfo> = emptyList(),
  val currentChunkIndex: Int = 0,
  val totalChunks: Int = 0,
  val errorMessage: String? = null,
)

class ReadAloudController(context: Context) {
  private val _state = MutableStateFlow(ListenState())
  val state: StateFlow<ListenState> = _state.asStateFlow()

  private var tts: TextToSpeech? = null
  private var rawVoices: List<Voice> = emptyList()
  private var chunks: List<String> = emptyList()
  private var onChapterFinished: (() -> Unit)? = null
  @Volatile private var volumeMultiplier: Float = 1.0f

  init {
    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.getDefault()
          rawVoices =
            runCatching {
                tts?.voices
                  ?.filter { !it.isNetworkConnectionRequired }
                  ?.sortedWith(compareBy({ it.locale.displayLanguage }, { it.name }))
                  ?.take(24)
                  .orEmpty()
              }
              .getOrDefault(emptyList())

          val mappedVoices =
            rawVoices.mapIndexed { idx, v ->
              val localeLabel =
                listOfNotNull(
                    v.locale.displayLanguage.takeIf { it.isNotBlank() },
                    v.locale.country.takeIf { it.isNotBlank() },
                  )
                  .joinToString(" · ")
              val prettyName =
                v.name
                  .substringAfterLast('-')
                  .replace('_', ' ')
                  .replaceFirstChar { it.uppercase() }
                  .ifBlank { "Voice ${idx + 1}" }
              TtsVoiceInfo(
                name = v.name,
                displayName = "$prettyName (${v.locale.language.uppercase()})",
                localeLabel = localeLabel,
              )
            }

          tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
              override fun onStart(utteranceId: String?) = Unit

              override fun onDone(utteranceId: String?) {
                if (utteranceId == "pn_sample") {
                  _state.update { it.copy(isPlaying = false) }
                  return
                }
                val idx = utteranceId?.removePrefix("pn_chunk_")?.toIntOrNull() ?: return
                val next = idx + 1
                if (next < chunks.size && _state.value.isPlaying) {
                  speakChunk(next)
                } else if (next >= chunks.size) {
                  _state.update { it.copy(isPlaying = false, currentChunkIndex = 0) }
                  onChapterFinished?.invoke()
                }
              }

              @Deprecated("Deprecated in Java")
              override fun onError(utteranceId: String?) {
                _state.update { it.copy(isPlaying = false) }
              }
            }
          )
          _state.update { it.copy(isReady = true, availableVoices = mappedVoices) }
        } else {
          _state.update {
            it.copy(isReady = false, errorMessage = "Device Text-to-Speech engine unavailable.")
          }
        }
      }
  }

  fun applyConfig(rate: Float, pitch: Float, voiceName: String?) {
    val safeRate = rate.coerceIn(0.6f, 2.0f)
    val safePitch = pitch.coerceIn(0.7f, 1.4f)
    _state.update {
      it.copy(speechRate = safeRate, pitch = safePitch, selectedVoiceName = voiceName)
    }
    tts?.setSpeechRate(safeRate)
    tts?.setPitch(safePitch)
    if (!voiceName.isNullOrBlank()) {
      rawVoices.firstOrNull { it.name == voiceName }?.let { v ->
        runCatching { tts?.voice = v }
      }
    }
  }

  fun setVolumeMultiplier(multiplier: Float) {
    volumeMultiplier = multiplier.coerceIn(0.05f, 1.0f)
  }

  fun speakSample(text: String) {
    if (!_state.value.isReady) return
    _state.update { it.copy(isPlaying = true) }
    tts?.setSpeechRate(_state.value.speechRate)
    tts?.setPitch(_state.value.pitch)
    val params = android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "pn_sample")
  }

  fun setContent(text: String, onFinished: () -> Unit) {
    stop()
    onChapterFinished = onFinished
    val split =
      text
        .split(Regex("(?<=[.!?])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    chunks = split
    _state.update {
      it.copy(currentChunkIndex = 0, totalChunks = split.size, isPlaying = false)
    }
  }

  fun togglePlayPause() {
    if (!_state.value.isReady || chunks.isEmpty()) return
    if (_state.value.isPlaying) pause() else play()
  }

  fun play() {
    if (!_state.value.isReady || chunks.isEmpty()) return
    val idx = _state.value.currentChunkIndex.coerceIn(0, chunks.lastIndex)
    _state.update { it.copy(isPlaying = true) }
    speakChunk(idx)
  }

  fun pause() {
    tts?.stop()
    _state.update { it.copy(isPlaying = false) }
  }

  fun stop() {
    tts?.stop()
    _state.update { it.copy(isPlaying = false, currentChunkIndex = 0) }
  }

  fun nextChunk(): Boolean {
    if (chunks.isEmpty()) return false
    val next = _state.value.currentChunkIndex + 1
    if (next !in chunks.indices) return false
    if (_state.value.isPlaying) {
      speakChunk(next)
    } else {
      _state.update { it.copy(currentChunkIndex = next) }
    }
    return true
  }

  fun previousChunk(): Boolean {
    if (chunks.isEmpty()) return false
    val prev = _state.value.currentChunkIndex - 1
    if (prev !in chunks.indices) return false
    if (_state.value.isPlaying) {
      speakChunk(prev)
    } else {
      _state.update { it.copy(currentChunkIndex = prev) }
    }
    return true
  }

  fun cycleSpeed(): Float {
    val rates = listOf(0.85f, 1.0f, 1.25f, 1.5f)
    val current = _state.value.speechRate
    val next = rates[(rates.indexOfFirst { kotlin.math.abs(it - current) < 0.05f } + 1) % rates.size]
    _state.update { it.copy(speechRate = next) }
    tts?.setSpeechRate(next)
    if (_state.value.isPlaying && chunks.isNotEmpty()) {
      speakChunk(_state.value.currentChunkIndex)
    }
    return next
  }

  private fun speakChunk(index: Int) {
    if (index !in chunks.indices) return
    _state.update { it.copy(currentChunkIndex = index) }
    tts?.setSpeechRate(_state.value.speechRate)
    tts?.setPitch(_state.value.pitch)
    val params =
      android.os.Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeMultiplier)
      }
    tts?.speak(chunks[index], TextToSpeech.QUEUE_FLUSH, params, "pn_chunk_$index")
  }

  fun release() {
    runCatching {
      tts?.stop()
      tts?.shutdown()
    }
    tts = null
  }
}
