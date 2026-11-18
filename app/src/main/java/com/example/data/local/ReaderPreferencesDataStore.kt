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
import com.example.config.MonetizationConfig
import com.example.model.AppLanguage
import com.example.model.LibraryLayoutMode
import com.example.model.LibrarySortOrder
import com.example.model.ReaderFontOption
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingRulerStyle
import com.example.model.ReadingThemeMode
import com.example.model.TapZoneAction
import com.example.model.TextAlignOption
import com.example.model.TipPromptMode
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
    val STREAK_REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
    val STREAK_REMINDER_HOUR = intPreferencesKey("streak_reminder_hour")
    val STREAK_REMINDER_MINUTE = intPreferencesKey("streak_reminder_minute")
    val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
    val PRO_SUPPORTER_NOTE = stringPreferencesKey("pro_supporter_note")
    val TIP_PROMPT_MODE = stringPreferencesKey("tip_prompt_mode")
    val TIP_FIXED_MINUTES = intPreferencesKey("tip_fixed_minutes")
    val NEXT_TIP_PROMPT_AT_READING_SEC = longPreferencesKey("next_tip_prompt_at_reading_sec")
    val LAST_TIP_PROMPT_SHOWN_AT = longPreferencesKey("last_tip_prompt_shown_at")
    val HAS_SUPPORTED_VIA_TIP_JAR = booleanPreferencesKey("has_supported_via_tip_jar")
    val LEFT_TAP_ACTION = stringPreferencesKey("left_tap_action")
    val CENTER_TAP_ACTION = stringPreferencesKey("center_tap_action")
    val RIGHT_TAP_ACTION = stringPreferencesKey("right_tap_action")
    val EDGE_BRIGHTNESS_SWIPE = booleanPreferencesKey("edge_brightness_swipe")
    val DOUBLE_TAP_BOOKMARK = booleanPreferencesKey("double_tap_bookmark")
    val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
    val FOCUS_WORK_MINUTES = intPreferencesKey("focus_work_minutes")
    val FOCUS_BREAK_MINUTES = intPreferencesKey("focus_break_minutes")
    val READING_RULER_ENABLED = booleanPreferencesKey("reading_ruler_enabled")
    val READING_RULER_STYLE = stringPreferencesKey("reading_ruler_style")
    val READING_RULER_HEIGHT_DP = intPreferencesKey("reading_ruler_height_dp")
    val READING_RULER_DIM_ALPHA = floatPreferencesKey("reading_ruler_dim_alpha")
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
      streakReminderEnabled = this[Keys.STREAK_REMINDER_ENABLED] ?: false,
      streakReminderHour = (this[Keys.STREAK_REMINDER_HOUR] ?: 20).coerceIn(0, 23),
      streakReminderMinute = (this[Keys.STREAK_REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
      isProUnlocked = this[Keys.IS_PRO_UNLOCKED] ?: false,
      proSupporterNote = this[Keys.PRO_SUPPORTER_NOTE].orEmpty(),
      tipPromptMode =
        this[Keys.TIP_PROMPT_MODE]?.let { runCatching { TipPromptMode.valueOf(it) }.getOrNull() }
          ?: TipPromptMode.RANDOM_USAGE,
      tipFixedMinutes = (this[Keys.TIP_FIXED_MINUTES] ?: MonetizationConfig.DEFAULT_FIXED_INTERVAL_MINUTES).coerceIn(10, 240),
      nextTipPromptAtReadingSeconds =
        this[Keys.NEXT_TIP_PROMPT_AT_READING_SEC]
          ?: (MonetizationConfig.FIRST_PROMPT_AFTER_READING_MINUTES * 60L),
      lastTipPromptShownAtMillis = this[Keys.LAST_TIP_PROMPT_SHOWN_AT] ?: 0L,
      hasSupportedViaTipJar = this[Keys.HAS_SUPPORTED_VIA_TIP_JAR] ?: false,
      leftTapAction =
        this[Keys.LEFT_TAP_ACTION]?.let { runCatching { TapZoneAction.valueOf(it) }.getOrNull() }
          ?: TapZoneAction.PREV_PAGE,
      centerTapAction =
        this[Keys.CENTER_TAP_ACTION]?.let { runCatching { TapZoneAction.valueOf(it) }.getOrNull() }
          ?: TapZoneAction.TOGGLE_CONTROLS,
      rightTapAction =
        this[Keys.RIGHT_TAP_ACTION]?.let { runCatching { TapZoneAction.valueOf(it) }.getOrNull() }
          ?: TapZoneAction.NEXT_PAGE,
      edgeBrightnessSwipeEnabled = this[Keys.EDGE_BRIGHTNESS_SWIPE] ?: true,
      doubleTapBookmarkEnabled = this[Keys.DOUBLE_TAP_BOOKMARK] ?: false,
      ambientVolume = (this[Keys.AMBIENT_VOLUME] ?: 0.45f).coerceIn(0.05f, 1.0f),
      focusWorkMinutes = (this[Keys.FOCUS_WORK_MINUTES] ?: 25).coerceIn(5, 90),
      focusBreakMinutes = (this[Keys.FOCUS_BREAK_MINUTES] ?: 5).coerceIn(1, 30),
      readingRulerEnabled = this[Keys.READING_RULER_ENABLED] ?: false,
      readingRulerStyle =
        this[Keys.READING_RULER_STYLE]?.let { runCatching { ReadingRulerStyle.valueOf(it) }.getOrNull() }
          ?: ReadingRulerStyle.SPOTLIGHT_BAND,
      readingRulerHeightDp = (this[Keys.READING_RULER_HEIGHT_DP] ?: 56).coerceIn(32, 140),
      readingRulerDimAlpha = (this[Keys.READING_RULER_DIM_ALPHA] ?: 0.45f).coerceIn(0.15f, 0.75f),
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
      prefs[Keys.STREAK_REMINDER_ENABLED] = next.streakReminderEnabled
      prefs[Keys.STREAK_REMINDER_HOUR] = next.streakReminderHour
      prefs[Keys.STREAK_REMINDER_MINUTE] = next.streakReminderMinute
      prefs[Keys.IS_PRO_UNLOCKED] = next.isProUnlocked
      prefs[Keys.PRO_SUPPORTER_NOTE] = next.proSupporterNote
      prefs[Keys.TIP_PROMPT_MODE] = next.tipPromptMode.name
      prefs[Keys.TIP_FIXED_MINUTES] = next.tipFixedMinutes
      prefs[Keys.NEXT_TIP_PROMPT_AT_READING_SEC] = next.nextTipPromptAtReadingSeconds
      prefs[Keys.LAST_TIP_PROMPT_SHOWN_AT] = next.lastTipPromptShownAtMillis
      prefs[Keys.HAS_SUPPORTED_VIA_TIP_JAR] = next.hasSupportedViaTipJar
      prefs[Keys.LEFT_TAP_ACTION] = next.leftTapAction.name
      prefs[Keys.CENTER_TAP_ACTION] = next.centerTapAction.name
      prefs[Keys.RIGHT_TAP_ACTION] = next.rightTapAction.name
      prefs[Keys.EDGE_BRIGHTNESS_SWIPE] = next.edgeBrightnessSwipeEnabled
      prefs[Keys.DOUBLE_TAP_BOOKMARK] = next.doubleTapBookmarkEnabled
      prefs[Keys.AMBIENT_VOLUME] = next.ambientVolume
      prefs[Keys.FOCUS_WORK_MINUTES] = next.focusWorkMinutes
      prefs[Keys.FOCUS_BREAK_MINUTES] = next.focusBreakMinutes
      prefs[Keys.READING_RULER_ENABLED] = next.readingRulerEnabled
      prefs[Keys.READING_RULER_STYLE] = next.readingRulerStyle.name
      prefs[Keys.READING_RULER_HEIGHT_DP] = next.readingRulerHeightDp
      prefs[Keys.READING_RULER_DIM_ALPHA] = next.readingRulerDimAlpha
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
