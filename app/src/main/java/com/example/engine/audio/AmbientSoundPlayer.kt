package com.example.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.AmbientSoundType
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Offline procedural ambient soundscape generator for reading focus.
 * Uses Android AudioTrack to synthesize gentle Rain, Fireplace, Brown Noise, and Ocean Waves
 * without requiring external audio downloads or cloud bandwidth.
 */
class AmbientSoundPlayer {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var playbackJob: Job? = null
  private var audioTrack: AudioTrack? = null

  @Volatile private var currentType: AmbientSoundType = AmbientSoundType.NONE
  @Volatile private var volume: Float = 0.45f

  fun setVolume(newVolume: Float) {
    val safe = newVolume.coerceIn(0.0f, 1.0f)
    volume = safe
    runCatching { audioTrack?.setVolume(safe) }
  }

  fun start(type: AmbientSoundType, initialVolume: Float = volume) {
    volume = initialVolume.coerceIn(0.0f, 1.0f)
    if (type == AmbientSoundType.NONE) {
      stop()
      return
    }
    if (currentType == type && playbackJob?.isActive == true) {
      setVolume(volume)
      return
    }
    stop()
    currentType = type

    playbackJob =
      scope.launch {
        val sampleRate = 22050
        val minBuf =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val bufferSamples = maxOf(2048, minBuf / 2)

        val track =
          runCatching {
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_MEDIA)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(bufferSamples * 2)
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()
          }.getOrNull() ?: return@launch

        audioTrack = track
        runCatching {
          track.setVolume(volume)
          track.play()
        }.onFailure {
          runCatching { track.release() }
          audioTrack = null
          return@launch
        }

        val samples = ShortArray(bufferSamples)
        var brown = 0f
        var lpRain = 0f
        var dropletEnv = 0f
        var lpFire = 0f
        var crackleEnv = 0f
        var phaseWave = 0.0
        val twoPi = Math.PI * 2.0

        try {
          while (isActive && currentType == type) {
            for (i in samples.indices) {
              val white = Random.nextFloat() * 2f - 1f
              val signal: Float =
                when (type) {
                  AmbientSoundType.BROWN_NOISE -> {
                    brown = (brown + 0.025f * white) / 1.025f
                    (brown * 3.1f).coerceIn(-0.95f, 0.95f)
                  }
                  AmbientSoundType.RAIN -> {
                    lpRain = lpRain * 0.78f + white * 0.22f
                    if (Random.nextFloat() < 0.0022f) {
                      dropletEnv = 0.45f + Random.nextFloat() * 0.35f
                    }
                    dropletEnv *= 0.965f
                    val drop = white * dropletEnv * 0.65f
                    (lpRain * 1.45f + drop).coerceIn(-0.95f, 0.95f)
                  }
                  AmbientSoundType.FIREPLACE -> {
                    lpFire = lpFire * 0.92f + white * 0.08f
                    if (Random.nextFloat() < 0.00055f) {
                      crackleEnv = 0.65f + Random.nextFloat() * 0.35f
                    }
                    crackleEnv *= 0.92f
                    val snap = (Random.nextFloat() * 2f - 1f) * crackleEnv
                    (lpFire * 2.4f + snap * 0.55f).coerceIn(-0.95f, 0.95f)
                  }
                  AmbientSoundType.OCEAN_WAVES -> {
                    phaseWave += (twoPi * 0.115) / sampleRate
                    if (phaseWave >= twoPi) phaseWave -= twoPi
                    val swell = (0.28 + 0.72 * ((sin(phaseWave) + 1.0) * 0.5)).toFloat()
                    brown = (brown + 0.04f * white) / 1.04f
                    (brown * 2.8f * swell).coerceIn(-0.95f, 0.95f)
                  }
                  AmbientSoundType.NONE -> 0f
                }
              samples[i] = (signal * 24500f).toInt().coerceIn(-32767, 32767).toShort()
            }
            track.write(samples, 0, samples.size)
          }
        } catch (_: Throwable) {
          // Stopped or interrupted cleanly
        } finally {
          runCatching {
            track.pause()
            track.flush()
            track.release()
          }
          if (audioTrack === track) {
            audioTrack = null
          }
        }
      }
  }

  fun stop() {
    currentType = AmbientSoundType.NONE
    playbackJob?.cancel()
    playbackJob = null
    val old = audioTrack
    audioTrack = null
    runCatching {
      old?.pause()
      old?.flush()
      old?.release()
    }
  }

  fun release() {
    stop()
    scope.cancel()
  }
}
