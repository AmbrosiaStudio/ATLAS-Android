package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.LibraryLayoutMode
import com.example.model.LibrarySortOrder
import com.example.model.ReaderFontOption
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TextAlignOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "pagenest_settings")

class ReaderPreferencesDataStore(private val context: Context) {
  private object Keys {
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val READING_LAYOUT_MODE = stringPreferencesKey("reading_layout_mode")
    val FONT_OPTION = stringPreferencesKey("font_option")
    val CUSTOM_FONT_ID = stringPreferencesKey("custom_font_id")
    val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
    val LINE_HEIGHT = floatPreferencesKey("line_height")
    val HORIZONTAL_MARGIN_DP = intPreferencesKey("horizontal_margin_dp")
    val VERTICAL_MARGIN_DP = intPreferencesKey("vertical_margin_dp")
    val PARAGRAPH_SPACING_EM = floatPreferencesKey("paragraph_spacing_em")
    val LETTER_SPACING_EM = floatPreferencesKey("letter_spacing_em")
    val FONT_WEIGHT_VALUE = intPreferencesKey("font_weight_value")
    val EYE_COMFORT_WARMTH = floatPreferencesKey("eye_comfort_warmth")
    val SHOW_PAGE_BORDER = booleanPreferencesKey("show_page_border")
    val PAPER_TEXTURE_ENABLED = booleanPreferencesKey("paper_texture_enabled")
    val TEXT_ALIGN = stringPreferencesKey("text_align")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val PDF_THEME_FILTER = booleanPreferencesKey("pdf_theme_filter")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val BATTERY_SAVER_MODE = booleanPreferencesKey("battery_saver_mode")
    val VOLUME_KEY_PAGE_TURN = booleanPreferencesKey("volume_key_page_turn")
    val RESTORE_LAST_SESSION = booleanPreferencesKey("restore_last_session")
    val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")
    val TRANSLATE_TARGET_LANG = stringPreferencesKey("translate_target_lang")
    val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
    val LIBRARY_SORT = stringPreferencesKey("library_sort")
    val LAST_OPENED_BOOK_ID = stringPreferencesKey("last_opened_book_id")
    val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
    val TODAY_READING_SECONDS = intPreferencesKey("today_reading_seconds")
    val LAST_READING_DATE_KEY = stringPreferencesKey("last_reading_date_key")
    val TOTAL_READING_SECONDS = longPreferencesKey("total_reading_seconds_all_time")
  }

  private fun Preferences.toSettings(): ReaderSettings =
    ReaderSettings(
      appLanguage =
        this[Keys.APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
          ?: AppLanguage.ENGLISH,
      themeMode =
        this[Keys.THEME_MODE]?.let { runCatching { ReadingThemeMode.valueOf(it) }.getOrNull() }
          ?: ReadingThemeMode.PAPER,
      readingLayoutMode =
        this[Keys.READING_LAYOUT_MODE]?.let { runCatching { ReadingLayoutMode.valueOf(it) }.getOrNull() }
          ?: ReadingLayoutMode.PAGED,
      fontOption =
        this[Keys.FONT_OPTION]?.let { runCatching { ReaderFontOption.valueOf(it) }.getOrNull() }
          ?: ReaderFontOption.LITERATA,
      customFontId = this[Keys.CUSTOM_FONT_ID]?.takeIf { it.isNotBlank() },
      fontSizeSp = (this[Keys.FONT_SIZE_SP] ?: 18).coerceIn(12, 32),
      lineHeightMultiplier = (this[Keys.LINE_HEIGHT] ?: 1.6f).coerceIn(1.15f, 2.4f),
      horizontalMarginDp = (this[Keys.HORIZONTAL_MARGIN_DP] ?: 22).coerceIn(8, 56),
      verticalMarginDp = (this[Keys.VERTICAL_MARGIN_DP] ?: 20).coerceIn(8, 60),
      paragraphSpacingEm = (this[Keys.PARAGRAPH_SPACING_EM] ?: 0.9f).coerceIn(0.4f, 2.0f),
      letterSpacingEm = (this[Keys.LETTER_SPACING_EM] ?: 0.0f).coerceIn(-0.02f, 0.12f),
      fontWeightValue = (this[Keys.FONT_WEIGHT_VALUE] ?: 400).coerceIn(300, 700),
      eyeComfortWarmth = (this[Keys.EYE_COMFORT_WARMTH] ?: 0.0f).coerceIn(0.0f, 1.0f),
      showPageBorder = this[Keys.SHOW_PAGE_BORDER] ?: false,
      paperTextureEnabled = this[Keys.PAPER_TEXTURE_ENABLED] ?: false,
      textAlign =
        this[Keys.TEXT_ALIGN]?.let { runCatching { TextAlignOption.valueOf(it) }.getOrNull() }
          ?: TextAlignOption.LEFT,
      brightnessOverride = this[Keys.BRIGHTNESS] ?: -1f,
      pdfThemeFilterEnabled = this[Keys.PDF_THEME_FILTER] ?: true,
      keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: false,
      batterySaverModeEnabled = this[Keys.BATTERY_SAVER_MODE] ?: false,
      volumeKeyPageTurnEnabled = this[Keys.VOLUME_KEY_PAGE_TURN] ?: false,
      restoreLastSession = this[Keys.RESTORE_LAST_SESSION] ?: true,
      ttsSpeechRate = (this[Keys.TTS_SPEECH_RATE] ?: 1.0f).coerceIn(0.6f, 2.0f),
      ttsPitch = (this[Keys.TTS_PITCH] ?: 1.0f).coerceIn(0.7f, 1.4f),
      ttsVoiceName = this[Keys.TTS_VOICE_NAME]?.takeIf { it.isNotBlank() },
      translateTargetLang = this[Keys.TRANSLATE_TARGET_LANG] ?: "fa",
      libraryLayout =
        this[Keys.LIBRARY_LAYOUT]?.let { runCatching { LibraryLayoutMode.valueOf(it) }.getOrNull() }
          ?: LibraryLayoutMode.GRID,
      librarySortOrder =
        this[Keys.LIBRARY_SORT]?.let { runCatching { LibrarySortOrder.valueOf(it) }.getOrNull() }
          ?: LibrarySortOrder.RECENT,
      dailyGoalMinutes = (this[Keys.DAILY_GOAL_MINUTES] ?: 20).coerceIn(5, 240),
      todayReadingSeconds = (this[Keys.TODAY_READING_SECONDS] ?: 0).coerceAtLeast(0),
      lastReadingDateKey = this[Keys.LAST_READING_DATE_KEY].orEmpty(),
      totalReadingSecondsAllTime = (this[Keys.TOTAL_READING_SECONDS] ?: 0L).coerceAtLeast(0L),
    )

  val settingsFlow: Flow<ReaderSettings> = context.readerDataStore.data.map { it.toSettings() }

  val lastOpenedBookIdFlow: Flow<String?> =
    context.readerDataStore.data.map { prefs -> prefs[Keys.LAST_OPENED_BOOK_ID] }

  suspend fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
    context.readerDataStore.edit { prefs ->
      val next = transform(prefs.toSettings())
      prefs[Keys.APP_LANGUAGE] = next.appLanguage.name
      prefs[Keys.THEME_MODE] = next.themeMode.name
      prefs[Keys.READING_LAYOUT_MODE] = next.readingLayoutMode.name
      prefs[Keys.FONT_OPTION] = next.fontOption.name
      if (next.customFontId == null) prefs.remove(Keys.CUSTOM_FONT_ID)
      else prefs[Keys.CUSTOM_FONT_ID] = next.customFontId
      prefs[Keys.FONT_SIZE_SP] = next.fontSizeSp
      prefs[Keys.LINE_HEIGHT] = next.lineHeightMultiplier
      prefs[Keys.HORIZONTAL_MARGIN_DP] = next.horizontalMarginDp
      prefs[Keys.VERTICAL_MARGIN_DP] = next.verticalMarginDp
      prefs[Keys.PARAGRAPH_SPACING_EM] = next.paragraphSpacingEm
      prefs[Keys.LETTER_SPACING_EM] = next.letterSpacingEm
      prefs[Keys.FONT_WEIGHT_VALUE] = next.fontWeightValue
      prefs[Keys.EYE_COMFORT_WARMTH] = next.eyeComfortWarmth
      prefs[Keys.SHOW_PAGE_BORDER] = next.showPageBorder
      prefs[Keys.PAPER_TEXTURE_ENABLED] = next.paperTextureEnabled
      prefs[Keys.TEXT_ALIGN] = next.textAlign.name
      prefs[Keys.BRIGHTNESS] = next.brightnessOverride
      prefs[Keys.PDF_THEME_FILTER] = next.pdfThemeFilterEnabled
      prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
      prefs[Keys.BATTERY_SAVER_MODE] = next.batterySaverModeEnabled
      prefs[Keys.VOLUME_KEY_PAGE_TURN] = next.volumeKeyPageTurnEnabled
      prefs[Keys.RESTORE_LAST_SESSION] = next.restoreLastSession
      prefs[Keys.TTS_SPEECH_RATE] = next.ttsSpeechRate
      prefs[Keys.TTS_PITCH] = next.ttsPitch
      if (next.ttsVoiceName == null) prefs.remove(Keys.TTS_VOICE_NAME)
      else prefs[Keys.TTS_VOICE_NAME] = next.ttsVoiceName
      prefs[Keys.TRANSLATE_TARGET_LANG] = next.translateTargetLang
      prefs[Keys.LIBRARY_LAYOUT] = next.libraryLayout.name
      prefs[Keys.LIBRARY_SORT] = next.librarySortOrder.name
      prefs[Keys.DAILY_GOAL_MINUTES] = next.dailyGoalMinutes
      prefs[Keys.TODAY_READING_SECONDS] = next.todayReadingSeconds
      prefs[Keys.LAST_READING_DATE_KEY] = next.lastReadingDateKey
      prefs[Keys.TOTAL_READING_SECONDS] = next.totalReadingSecondsAllTime
    }
  }

  suspend fun addReadingSeconds(deltaSeconds: Int, todayDateKey: String) {
    if (deltaSeconds <= 0) return
    context.readerDataStore.edit { prefs ->
      val previousDate = prefs[Keys.LAST_READING_DATE_KEY].orEmpty()
      val previousToday = if (previousDate == todayDateKey) (prefs[Keys.TODAY_READING_SECONDS] ?: 0) else 0
      val previousTotal = prefs[Keys.TOTAL_READING_SECONDS] ?: 0L
      prefs[Keys.LAST_READING_DATE_KEY] = todayDateKey
      prefs[Keys.TODAY_READING_SECONDS] = (previousToday + deltaSeconds).coerceAtLeast(0)
      prefs[Keys.TOTAL_READING_SECONDS] = (previousTotal + deltaSeconds).coerceAtLeast(0L)
    }
  }

  suspend fun setLastOpenedBookId(bookId: String?) {
    context.readerDataStore.edit { prefs ->
      if (bookId == null) prefs.remove(Keys.LAST_OPENED_BOOK_ID)
      else prefs[Keys.LAST_OPENED_BOOK_ID] = bookId
    }
  }
}
