package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ReaderRepository
import com.example.engine.fonts.FontManager
import com.example.engine.tts.ListenState
import com.example.engine.tts.ReadAloudController
import com.example.model.AppLanguage
import com.example.model.CustomFontEntry
import com.example.model.ReaderFontOption
import com.example.model.ReaderSettings
import com.example.config.MonetizationConfig
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TapZoneAction
import com.example.model.TipPromptMode
import com.example.notifications.StreakReminderScheduler
import com.example.notifications.StreakReminderWorker
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageStats(
  val booksSizeBytes: Long = 0L,
  val epubCacheBytes: Long = 0L,
)

data class SettingsUiState(
  val settings: ReaderSettings = ReaderSettings(),
  val customFonts: List<CustomFontEntry> = emptyList(),
  val ttsState: ListenState = ListenState(),
  val storageStats: StorageStats = StorageStats(),
  val message: String? = null,
)

class SettingsViewModel(
  private val context: Context,
  private val readerRepository: ReaderRepository,
  private val fontManager: FontManager,
) : ViewModel() {

  private val readAloud = ReadAloudController(context)
  private val storageStats = MutableStateFlow(StorageStats())
  private val message = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SettingsUiState> =
    combine(
      readerRepository.settingsFlow,
      fontManager.customFontsFlow,
      readAloud.state,
      storageStats,
      message,
    ) { settings, customFonts, tts, storage, msg ->
      SettingsUiState(
        settings = settings,
        customFonts = customFonts,
        ttsState = tts,
        storageStats = storage,
        message = msg,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = SettingsUiState(),
    )

  init {
    refreshStorageStats()
    viewModelScope.launch {
      combine(readerRepository.settingsFlow, readAloud.state) { s, tts ->
        Triple(s.ttsSpeechRate, s.ttsPitch, s.ttsVoiceName) to tts.isReady
      }.collect { (cfg, ready) ->
        if (ready) {
          readAloud.applyConfig(cfg.first, cfg.second, cfg.third)
        }
      }
    }
  }

  fun setAppLanguage(language: AppLanguage) {
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        val nextFont =
          if (language == AppLanguage.PERSIAN && !current.fontOption.supportsPersian) {
            ReaderFontOption.VAZIRMATN
          } else if (language == AppLanguage.ENGLISH && current.fontOption.supportsPersian) {
            ReaderFontOption.LITERATA
          } else {
            current.fontOption
          }
        current.copy(appLanguage = language, fontOption = nextFont)
      }
      com.example.widget.WidgetUpdater.requestAllUpdates(context)
    }
  }

  fun setThemeMode(mode: ReadingThemeMode) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(themeMode = mode) }
      com.example.widget.WidgetUpdater.requestAllUpdates(context)
    }
  }

  fun setReadingLayoutMode(mode: ReadingLayoutMode) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(readingLayoutMode = mode) } }
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

  fun setRestoreLastSession(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(restoreLastSession = enabled) } }
  }

  fun setPdfThemeFilter(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(pdfThemeFilterEnabled = enabled) } }
  }

  fun setPaperTextureEnabled(enabled: Boolean) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(paperTextureEnabled = enabled) } }
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

  fun setStreakReminderEnabled(enabled: Boolean) {
    viewModelScope.launch {
      val current = uiState.value.settings
      readerRepository.updateSettings { it.copy(streakReminderEnabled = enabled) }
      if (enabled) {
        StreakReminderScheduler.scheduleDaily(
          context = context,
          hour = current.streakReminderHour,
          minute = current.streakReminderMinute,
        )
        val formattedTime = String.format("%02d:%02d", current.streakReminderHour, current.streakReminderMinute)
        message.value =
          if (current.appLanguage == AppLanguage.PERSIAN) {
            "یادآور روزانه برای ساعت $formattedTime تنظیم شد."
          } else {
            "Daily streak reminder scheduled for $formattedTime."
          }
      } else {
        StreakReminderScheduler.cancel(context)
        message.value =
          if (current.appLanguage == AppLanguage.PERSIAN) {
            "یادآور روزانه غیرفعال شد."
          } else {
            "Daily streak reminder turned off."
          }
      }
    }
  }

  fun setStreakReminderTime(hour: Int, minute: Int) {
    val safeH = hour.coerceIn(0, 23)
    val safeM = minute.coerceIn(0, 59)
    viewModelScope.launch {
      val current = uiState.value.settings
      readerRepository.updateSettings {
        it.copy(streakReminderHour = safeH, streakReminderMinute = safeM)
      }
      if (current.streakReminderEnabled) {
        StreakReminderScheduler.scheduleDaily(context, safeH, safeM)
      }
      val formattedTime = String.format("%02d:%02d", safeH, safeM)
      message.value =
        if (current.appLanguage == AppLanguage.PERSIAN) {
          "ساعت یادآور به $formattedTime تغییر یافت."
        } else {
          "Reminder time updated to $formattedTime."
        }
    }
  }

  fun sendTestStreakReminder() {
    val current = uiState.value.settings
    if (!StreakReminderWorker.hasNotificationPermission(context)) {
      message.value =
        if (current.appLanguage == AppLanguage.PERSIAN) {
          "برای دریافت یادآورهای روزانه، مجوز اعلان را فعال کنید."
        } else {
          "Allow notification permission to receive daily streak reminders."
        }
      return
    }
    StreakReminderScheduler.triggerTestNow(context)
    message.value =
      if (current.appLanguage == AppLanguage.PERSIAN) {
        "اعلان آزمایشی در حال ارسال است…"
      } else {
        "Sending test streak reminder notification…"
      }
  }

  fun setTipPromptMode(mode: TipPromptMode) {
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        val nextThreshold =
          if (mode == TipPromptMode.OFF) current.nextTipPromptAtReadingSeconds
          else {
            current.totalReadingSecondsAllTime +
              MonetizationConfig.computeNextPromptDelaySeconds(
                isRandomMode = mode == TipPromptMode.RANDOM_USAGE,
                fixedMinutes = current.tipFixedMinutes,
              )
          }
        current.copy(
          tipPromptMode = mode,
          nextTipPromptAtReadingSeconds = nextThreshold,
        )
      }
    }
  }

  fun setTipFixedMinutes(minutes: Int) {
    val safe = minutes.coerceIn(10, 120)
    viewModelScope.launch {
      readerRepository.updateSettings { current ->
        current.copy(
          tipFixedMinutes = safe,
          nextTipPromptAtReadingSeconds = current.totalReadingSecondsAllTime + (safe * 60L),
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
      message.value =
        if (isFa) "از حمایت ارزشمند شما سپاسگزاریم!"
        else "Thank you so much for supporting ATLAS!"
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
      message.value =
        if (isFa) "نسخه اطلس Pro فعال شد — سپاس از حمایت شما!"
        else "ATLAS Pro activated — thank you for your support!"
    }
    return true
  }

  fun selectBuiltInFont(option: ReaderFontOption) {
    viewModelScope.launch {
      readerRepository.updateSettings { it.copy(fontOption = option, customFontId = null) }
    }
  }

  fun selectCustomFont(fontId: String) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(customFontId = fontId) } }
  }

  fun importCustomFont(uri: Uri) {
    viewModelScope.launch {
      fontManager
        .importFontFromUri(uri)
        .onSuccess { entry ->
          readerRepository.updateSettings { it.copy(customFontId = entry.id) }
          message.value = "Imported font “${entry.displayName}”"
        }
        .onFailure { err ->
          message.value = err.message ?: "Failed to import font."
        }
    }
  }

  fun deleteCustomFont(fontId: String) {
    viewModelScope.launch {
      if (uiState.value.settings.customFontId == fontId) {
        readerRepository.updateSettings { it.copy(customFontId = null) }
      }
      fontManager.deleteCustomFont(fontId)
      message.value = "Removed custom font"
    }
  }

  fun setTtsRate(rate: Float) {
    val safe = (Math.round(rate * 20f) / 20f).coerceIn(0.6f, 2.0f)
    viewModelScope.launch { readerRepository.updateSettings { it.copy(ttsSpeechRate = safe) } }
  }

  fun setTtsPitch(pitch: Float) {
    val safe = (Math.round(pitch * 20f) / 20f).coerceIn(0.7f, 1.4f)
    viewModelScope.launch { readerRepository.updateSettings { it.copy(ttsPitch = safe) } }
  }

  fun setTtsVoice(voiceName: String?) {
    viewModelScope.launch { readerRepository.updateSettings { it.copy(ttsVoiceName = voiceName) } }
  }

  fun testTtsVoice() {
    if (uiState.value.ttsState.isPlaying) {
      readAloud.pause()
    } else {
      val sample =
        if (uiState.value.settings.appLanguage == AppLanguage.PERSIAN) {
          "به اطلس خوش آمدید. خواندن آرام و تایپوگرافی دقیق، هر فصل را لذت‌بخش می‌کند."
        } else {
          "Welcome to ATLAS. Crisp typography and calm voices make every chapter effortless."
        }
      readAloud.speakSample(sample)
    }
  }

  fun clearEpubCache() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        File(context.filesDir, "epub_cache").deleteRecursively()
      }
      refreshStorageStats()
      message.value = "Cleared unpacked EPUB cache"
    }
  }

  fun refreshStorageStats() {
    viewModelScope.launch {
      val stats =
        withContext(Dispatchers.IO) {
          StorageStats(
            booksSizeBytes = dirSize(File(context.filesDir, "books")),
            epubCacheBytes = dirSize(File(context.filesDir, "epub_cache")),
          )
        }
      storageStats.value = stats
    }
  }

  fun clearMessage() {
    message.update { null }
  }

  private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
  }

  override fun onCleared() {
    super.onCleared()
    readAloud.release()
  }

  class Factory(
    private val context: Context,
    private val readerRepository: ReaderRepository,
    private val fontManager: FontManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return SettingsViewModel(context, readerRepository, fontManager) as T
    }
  }
}
