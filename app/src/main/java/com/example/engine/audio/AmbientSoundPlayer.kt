package com.example.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.AmbientSoundType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Offline 44.1 kHz stereo physical-modeling ambient soundscape synthesizer for calm reading focus.
 * Combines Paul Kellet pink-noise filtering, 2-pole biquad resonant sweeps, polyphonic damped
 * water-droplet pings, organic fireplace flame flutter & wood crackle transients, and
 * phase-swept ocean surf without static hiss or external audio dependencies.
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
        val sampleRate = SAMPLE_RATE
        val minBufBytes =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
          ).coerceAtLeast(8192)
        // 4096 stereo frames (16384 bytes) keeps write overhead low while staying responsive
        val framesPerChunk = 4096
        val bufferBytes = maxOf(minBufBytes * 2, framesPerChunk * 4)

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
                  .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                  .build()
              )
              .setBufferSizeInBytes(bufferBytes)
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

        val pcmBuffer = ShortArray(framesPerChunk * 2)
        val synth = SoundscapeSynthesizer(type = type, sampleRate = sampleRate)

        try {
          while (isActive && currentType == type) {
            synth.renderStereoFrames(pcmBuffer, framesPerChunk)
            track.write(pcmBuffer, 0, pcmBuffer.size)
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

  internal class SoundscapeSynthesizer(
    private val type: AmbientSoundType,
    private val sampleRate: Int = SAMPLE_RATE,
    seed: Long = System.nanoTime(),
  ) {
    private val rng = Random(seed)
    private val pinkL = PinkNoiseFilter()
    private val pinkR = PinkNoiseFilter()

    private val biquadL = BiquadLowPass(sampleRate)
    private val biquadR = BiquadLowPass(sampleRate)
    private val subBiquadL = BiquadLowPass(sampleRate)
    private val subBiquadR = BiquadLowPass(sampleRate)

    private var brownL = 0f
    private var brownR = 0f
    private var dcPrevInL = 0f
    private var dcPrevOutL = 0f
    private var dcPrevInR = 0f
    private var dcPrevOutR = 0f

    // Rain droplet polyphony (damped resonant sine chirps)
    private val droplets = Array(6) { DampedVoice() }
    private var rainGustPhaseA = rng.nextDouble() * TWO_PI
    private var rainGustPhaseB = rng.nextDouble() * TWO_PI

    // Fireplace organic flutter & transient crackle / hollow wood pops
    private val firePops = Array(5) { FireTransientVoice() }
    private var flameFlutter = 0.65f
    private var flameTarget = 0.65f
    private var flameTimer = 0
    private var draftPhase = rng.nextDouble() * TWO_PI

    // Ocean wave swell state
    private var wavePhase = rng.nextDouble() * TWO_PI
    private var waveSpeedRadPerSample = (TWO_PI / 9.2) / sampleRate
    private var waveTargetPeriodSec = 9.2

    // Smooth fade-in ramp (~350ms)
    private var renderedFrames = 0L
    private val fadeInFrames = (sampleRate * 0.35f).toLong().coerceAtLeast(1L)

    init {
      when (type) {
        AmbientSoundType.BROWN_NOISE -> {
          biquadL.setCutoff(320f, 0.707f)
          biquadR.setCutoff(320f, 0.707f)
        }
        AmbientSoundType.RAIN -> {
          biquadL.setCutoff(1350f, 0.65f)
          biquadR.setCutoff(1350f, 0.65f)
          subBiquadL.setCutoff(240f, 0.707f)
          subBiquadR.setCutoff(240f, 0.707f)
        }
        AmbientSoundType.FIREPLACE -> {
          biquadL.setCutoff(230f, 0.72f)
          biquadR.setCutoff(230f, 0.72f)
          subBiquadL.setCutoff(95f, 0.707f)
          subBiquadR.setCutoff(95f, 0.707f)
        }
        AmbientSoundType.OCEAN_WAVES -> {
          biquadL.setCutoff(480f, 0.68f)
          biquadR.setCutoff(480f, 0.68f)
          subBiquadL.setCutoff(190f, 0.707f)
          subBiquadR.setCutoff(190f, 0.707f)
        }
        AmbientSoundType.NONE -> Unit
      }
    }

    fun renderStereoFrames(out: ShortArray, frameCount: Int) {
      val srF = sampleRate.toFloat()
      for (f in 0 until frameCount) {
        var left = 0f
        var right = 0f

        when (type) {
          AmbientSoundType.BROWN_NOISE -> {
            // Soft triangular noise -> slow leaky integrator -> 2-pole Butterworth 320 Hz lowpass
            val nL = ((rng.nextFloat() + rng.nextFloat()) - 1.0f)
            val nR = ((rng.nextFloat() + rng.nextFloat()) - 1.0f)
            val shared = (nL + nR) * 0.5f
            val inL = shared * 0.84f + nL * 0.16f
            val inR = shared * 0.84f + nR * 0.16f

            brownL = (brownL + 0.018f * inL) * 0.9965f
            brownR = (brownR + 0.018f * inR) * 0.9965f

            left = biquadL.process(brownL * 3.4f)
            right = biquadR.process(brownR * 3.4f)
          }

          AmbientSoundType.RAIN -> {
            rainGustPhaseA += (TWO_PI * 0.073) / srF
            rainGustPhaseB += (TWO_PI * 0.031) / srF
            if (rainGustPhaseA >= TWO_PI) rainGustPhaseA -= TWO_PI
            if (rainGustPhaseB >= TWO_PI) rainGustPhaseB -= TWO_PI

            val gustMod =
              (0.84 + 0.10 * sin(rainGustPhaseA) + 0.06 * cos(rainGustPhaseB)).toFloat()

            // True -3dB/oct pink noise filtered cleanly at 1350 Hz + soft low rain warmth
            val wL = rng.nextFloat() * 2f - 1f
            val wR = rng.nextFloat() * 2f - 1f
            val pL = pinkL.next(wL)
            val pR = pinkR.next(wR)
            // Blend 35% center canopy + 65% stereo air for natural room width
            val canopyMid = (pL + pR) * 0.18f
            val sheetL = biquadL.process(pL * 0.65f + canopyMid)
            val sheetR = biquadR.process(pR * 0.65f + canopyMid)
            val murmurL = subBiquadL.process(pL) * 0.45f
            val murmurR = subBiquadR.process(pR) * 0.45f

            // Spawn polyphonic resonant water droplets (~10-14 drops/sec)
            if (rng.nextFloat() < 12.5f / srF) {
              for (v in droplets) {
                if (!v.active) {
                  val pan = 0.15f + rng.nextFloat() * 0.70f
                  val baseFreq = 620f + rng.nextFloat() * 1450f
                  val decaySec = 0.016f + rng.nextFloat() * 0.028f
                  val amp = 0.065f + rng.nextFloat() * 0.095f
                  v.trigger(baseFreq, decaySec, amp, pan, sampleRate)
                  break
                }
              }
            }

            var dropL = 0f
            var dropR = 0f
            for (v in droplets) {
              if (v.active) {
                val sample = v.nextSample()
                dropL += sample * v.panL
                dropR += sample * v.panR
              }
            }

            left = (sheetL + murmurL) * gustMod * 0.92f + dropL
            right = (sheetR + murmurR) * gustMod * 0.92f + dropR
          }

          AmbientSoundType.FIREPLACE -> {
            // Organic breathing flame flutter (~2.5-5 Hz random walk + 0.19 Hz draft)
            flameTimer--
            if (flameTimer <= 0) {
              flameTimer = (sampleRate / (4 + rng.nextInt(7)))
              flameTarget = 0.45f + rng.nextFloat() * 0.55f
            }
            flameFlutter += (flameTarget - flameFlutter) * (14.0f / srF)
            draftPhase += (TWO_PI * 0.19) / srF
            if (draftPhase >= TWO_PI) draftPhase -= TWO_PI
            val draft = (0.78 + 0.22 * sin(draftPhase)).toFloat()

            val pL = pinkL.next(rng.nextFloat() * 2f - 1f)
            val pR = pinkR.next(rng.nextFloat() * 2f - 1f)
            val roarL = biquadL.process(pL) * (1.25f * flameFlutter * draft)
            val roarR = biquadR.process(pR) * (1.25f * flameFlutter * draft)
            val subCoalL = subBiquadL.process(pL) * 0.85f
            val subCoalR = subBiquadR.process(pR) * 0.85f

            // Occasional organic wood crackles & hollow log pops (~3.5 events/sec)
            if (rng.nextFloat() < 3.6f / srF) {
              for (v in firePops) {
                if (!v.active) {
                  val isHollowPop = rng.nextFloat() < 0.30f
                  val pan = 0.20f + rng.nextFloat() * 0.60f
                  if (isHollowPop) {
                    v.trigger(
                      freq = 140f + rng.nextFloat() * 140f,
                      decaySec = 0.025f + rng.nextFloat() * 0.030f,
                      amp = 0.18f + rng.nextFloat() * 0.18f,
                      noiseMix = 0.18f,
                      pan = pan,
                      sampleRate = sampleRate,
                    )
                  } else {
                    v.trigger(
                      freq = 1350f + rng.nextFloat() * 2100f,
                      decaySec = 0.004f + rng.nextFloat() * 0.009f,
                      amp = 0.14f + rng.nextFloat() * 0.22f,
                      noiseMix = 0.55f,
                      pan = pan,
                      sampleRate = sampleRate,
                    )
                  }
                  break
                }
              }
            }

            var crackleL = 0f
            var crackleR = 0f
            for (v in firePops) {
              if (v.active) {
                val s = v.nextSample(rng)
                crackleL += s * v.panL
                crackleR += s * v.panR
              }
            }

            left = roarL + subCoalL + crackleL
            right = roarR + subCoalR + crackleR
          }

          AmbientSoundType.OCEAN_WAVES -> {
            val prevPhase = wavePhase
            wavePhase += waveSpeedRadPerSample
            if (wavePhase >= TWO_PI) {
              wavePhase -= TWO_PI
              // Slight organic wave period shift each swell (7.6s to 11.4s)
              waveTargetPeriodSec = 7.6 + rng.nextDouble() * 3.8
            }
            val targetSpeed = (TWO_PI / waveTargetPeriodSec) / sampleRate
            waveSpeedRadPerSample += (targetSpeed - waveSpeedRadPerSample) * (0.25 / srF)

            // Asymmetric surf curve: smooth deep build -> brighter foam crest -> gentle wash-out
            val swellL = computeWaveEnvelope(wavePhase)
            val swellR = computeWaveEnvelope((wavePhase - 0.22).let { if (it < 0.0) it + TWO_PI else it })

            // Update resonant surf cutoff every 64 frames for ultra-smooth spectral sweep
            if ((f and 63) == 0 || prevPhase > wavePhase) {
              biquadL.setCutoff(240f + swellL * 1380f, 0.72f)
              biquadR.setCutoff(240f + swellR * 1380f, 0.72f)
            }

            val pL = pinkL.next(rng.nextFloat() * 2f - 1f)
            val pR = pinkR.next(rng.nextFloat() * 2f - 1f)
            val sharedPink = (pL + pR) * 0.5f

            val surfL = biquadL.process(sharedPink * 0.55f + pL * 0.45f) * (0.20f + 0.95f * swellL)
            val surfR = biquadR.process(sharedPink * 0.55f + pR * 0.45f) * (0.20f + 0.95f * swellR)

            val undertowL = subBiquadL.process(pL) * (0.45f + 0.40f * swellL)
            val undertowR = subBiquadR.process(pR) * (0.45f + 0.40f * swellR)

            left = surfL + undertowL
            right = surfR + undertowR
          }

          AmbientSoundType.NONE -> {
            left = 0f
            right = 0f
          }
        }

        // DC blocker removes sub-5Hz offset; soft tanh limiter preserves warmth without clipping
        val cleanL = dcBlockL(left)
        val cleanR = dcBlockR(right)

        val fade =
          if (renderedFrames < fadeInFrames) {
            renderedFrames.toFloat() / fadeInFrames.toFloat()
          } else {
            1f
          }
        renderedFrames++

        val outL = tanh((cleanL * fade * 0.85f).toDouble()).toFloat()
        val outR = tanh((cleanR * fade * 0.85f).toDouble()).toFloat()

        val idx = f * 2
        out[idx] = (outL * 29000f).toInt().coerceIn(-32767, 32767).toShort()
        out[idx + 1] = (outR * 29000f).toInt().coerceIn(-32767, 32767).toShort()
      }
    }

    private fun computeWaveEnvelope(phaseRad: Double): Float {
      // Skew phase so rise is brisker (~38%) and wash-out / recede is longer (~62%)
      val norm = (phaseRad / TWO_PI).toFloat()
      val skewed =
        if (norm < 0.38f) {
          val u = norm / 0.38f
          0.5f - 0.5f * cos(u * PI).toFloat()
        } else {
          val u = (norm - 0.38f) / 0.62f
          0.5f + 0.5f * cos(u * PI).toFloat()
        }
      return skewed * skewed
    }

    private fun dcBlockL(input: Float): Float {
      val out = input - dcPrevInL + 0.9992f * dcPrevOutL
      dcPrevInL = input
      dcPrevOutL = out
      return out
    }

    private fun dcBlockR(input: Float): Float {
      val out = input - dcPrevInR + 0.9992f * dcPrevOutR
      dcPrevInR = input
      dcPrevOutR = out
      return out
    }
  }

  /** Paul Kellet's 7-pole pink noise filter (-3 dB/octave natural acoustic spectrum). */
  private class PinkNoiseFilter {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f

    fun next(white: Float): Float {
      b0 = 0.99886f * b0 + white * 0.0555179f
      b1 = 0.99332f * b1 + white * 0.0750759f
      b2 = 0.96900f * b2 + white * 0.1538520f
      b3 = 0.86650f * b3 + white * 0.3104856f
      b4 = 0.55000f * b4 + white * 0.5329522f
      b5 = -0.7616f * b5 - white * 0.0168980f
      val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.11f
      b6 = white * 0.115926f
      return pink
    }
  }

  /** 2-pole Audio EQ Cookbook lowpass biquad for steep anti-hiss spectral shaping. */
  private class BiquadLowPass(private val sampleRate: Int) {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun setCutoff(cutoffHz: Float, q: Float) {
      val clamped = cutoffHz.coerceIn(40f, sampleRate * 0.42f)
      val w0 = (TWO_PI * clamped) / sampleRate
      val cosW0 = cos(w0)
      val alpha = sin(w0) / (2.0 * q)
      val a0 = 1.0 + alpha
      b0 = (((1.0 - cosW0) * 0.5) / a0).toFloat()
      b1 = ((1.0 - cosW0) / a0).toFloat()
      b2 = b0
      a1 = ((-2.0 * cosW0) / a0).toFloat()
      a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun process(x: Float): Float {
      val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
      x2 = x1
      x1 = x
      y2 = y1
      y1 = y
      return y
    }
  }

  /** Resonant upward-chirping water droplet ping (physical bubble acoustics). */
  private class DampedVoice {
    var active = false
    var panL = 0.7f
    var panR = 0.7f
    private var phase = 0.0
    private var freq = 1000.0
    private var freqSlidePerSample = 0.0
    private var env = 0f
    private var decayPerSample = 0.999f
    private var sampleRate = SAMPLE_RATE

    fun trigger(baseFreq: Float, decaySec: Float, amp: Float, pan: Float, sampleRate: Int) {
      this.sampleRate = sampleRate
      this.freq = baseFreq.toDouble()
      // Subtle upward bubble chirp (~+18% pitch over droplet life)
      this.freqSlidePerSample = (baseFreq * 0.18) / (decaySec * sampleRate).coerceAtLeast(1f)
      this.phase = 0.0
      this.env = amp
      this.decayPerSample = kotlin.math.exp(-1.0 / (decaySec * sampleRate)).toFloat()
      this.panL = kotlin.math.sqrt((1f - pan).toDouble()).toFloat()
      this.panR = kotlin.math.sqrt(pan.toDouble()).toFloat()
      this.active = true
    }

    fun nextSample(): Float {
      if (!active) return 0f
      val out = sin(phase).toFloat() * env
      phase += (TWO_PI * freq) / sampleRate
      if (phase >= TWO_PI) phase -= TWO_PI
      freq += freqSlidePerSample
      env *= decayPerSample
      if (env < 0.0008f) active = false
      return out
    }
  }

  /** Short organic wood crackle / hollow ember pop transient. */
  private class FireTransientVoice {
    var active = false
    var panL = 0.7f
    var panR = 0.7f
    private var phase = 0.0
    private var phaseStep = 0.0
    private var env = 0f
    private var decayPerSample = 0.998f
    private var noiseMix = 0.4f

    fun trigger(
      freq: Float,
      decaySec: Float,
      amp: Float,
      noiseMix: Float,
      pan: Float,
      sampleRate: Int,
    ) {
      this.phase = 0.0
      this.phaseStep = (TWO_PI * freq) / sampleRate
      this.env = amp
      this.decayPerSample = kotlin.math.exp(-1.0 / (decaySec * sampleRate)).toFloat()
      this.noiseMix = noiseMix
      this.panL = kotlin.math.sqrt((1f - pan).toDouble()).toFloat()
      this.panR = kotlin.math.sqrt(pan.toDouble()).toFloat()
      this.active = true
    }

    fun nextSample(rng: Random): Float {
      if (!active) return 0f
      val tone = sin(phase).toFloat()
      val burst = (rng.nextFloat() * 2f - 1f)
      val out = (tone * (1f - noiseMix) + burst * noiseMix) * env
      phase += phaseStep
      if (phase >= TWO_PI) phase -= TWO_PI
      env *= decayPerSample
      if (env < 0.001f) active = false
      return out
    }
  }

  companion object {
    internal const val SAMPLE_RATE = 44100
    private const val TWO_PI = PI * 2.0
  }
}
