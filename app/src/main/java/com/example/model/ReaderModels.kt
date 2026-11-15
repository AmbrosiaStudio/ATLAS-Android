package com.example.model

enum class BookFormat(
  val extension: String,
  val isReflowableText: Boolean,
) {
  EPUB("epub", true),
  PDF("pdf", false),
  TXT("txt", true),
  MD("md", true),
  FB2("fb2", true),
  CBZ("cbz", false);

  companion object {
    fun fromString(raw: String): BookFormat =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EPUB
  }
}

enum class ReadingStatus(val label: String, val faLabel: String) {
  UNREAD("Unread", "خوانده‌نشده"),
  READING("Reading", "در حال مطالعه"),
  FINISHED("Finished", "تکمیل‌شده");

  companion object {
    fun fromString(raw: String): ReadingStatus =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNREAD
  }
}

enum class LibraryStatusFilter(val label: String, val faLabel: String) {
  ALL("All", "همه"),
  READING("Reading", "در حال مطالعه"),
  UNREAD("Unread", "خوانده‌نشده"),
  FINISHED("Finished", "تکمیل‌شده"),
  FAVORITES("Favorites", "علاقه‌مندی‌ها"),
}

enum class AppLanguage(
  val code: String,
  val nativeLabel: String,
  val isRtl: Boolean,
) {
  ENGLISH("en", "English", false),
  PERSIAN("fa", "فارسی", true),
}

enum class ReadingThemeMode(
  val label: String,
  val faLabel: String,
  val isDark: Boolean,
) {
  PAPER("Cream", "کرم", false),
  SEPIA("Sepia", "سپیا", false),
  SAGE_MIST("Sage Mist", "سبز ملایم", false),
  DARK("Dark", "تیره", true),
  AMOLED("AMOLED", "آمولد", true),
  NIGHT_BW("Night B&W", "سیاه‌وسفید", true),
}

enum class ReadingLayoutMode(
  val label: String,
  val faLabel: String,
) {
  PAGED("Book Pages", "صفحه‌بندی کتابی"),
  SCROLL("Vertical Scroll", "پیمایش عمودی"),
}

enum class ReaderFontOption(
  val displayName: String,
  val cssFamily: String,
  val fontFileName: String,
  val isSerif: Boolean,
  val supportsPersian: Boolean = false,
) {
  LITERATA("Literata", "PageNestLiterata", "literata.ttf", true, false),
  LORA("Lora", "PageNestLora", "lora.ttf", true, false),
  NEWSREADER("Newsreader", "PageNestNewsreader", "newsreader.ttf", true, false),
  VAZIRMATN("Vazirmatn (FA)", "PageNestVazirmatn", "vazirmatn.ttf", false, true),
  NOTO_NASKH("Noto Naskh (FA)", "PageNestNotoNaskh", "noto_naskh_arabic.ttf", true, true),
  PLUS_JAKARTA("Jakarta Sans", "PageNestJakarta", "plus_jakarta_sans.ttf", false, false),
  JETBRAINS_MONO("JetBrains Mono", "PageNestMono", "jetbrains_mono.ttf", false, false),
}

data class CustomFontEntry(
  val id: String,
  val displayName: String,
  val filePath: String,
)

data class TtsVoiceInfo(
  val name: String,
  val displayName: String,
  val localeLabel: String,
)

data class SelectionAnchor(
  val text: String = "",
  val leftDp: Float = 0f,
  val topDp: Float = 0f,
  val rightDp: Float = 0f,
  val bottomDp: Float = 0f,
) {
  val isActive: Boolean get() = text.isNotBlank()
}

enum class TextAlignOption(
  val label: String,
  val faLabel: String,
  val cssValue: String,
) {
  LEFT("Start", "تراز آغاز", "start"),
  JUSTIFY("Justify", "هم‌تراز", "justify"),
}

enum class LibraryLayoutMode {
  GRID,
  LIST,
}

enum class LibrarySortOrder(val label: String, val faLabel: String) {
  RECENT("Recently Opened", "اخیرترین"),
  TITLE("Title A–Z", "عنوان"),
  AUTHOR("Author A–Z", "نویسنده"),
  PROGRESS("Progress", "پیشرفت"),
}

enum class ReaderMode {
  READ,
  LISTEN,
}

enum class ComfortPreset(
  val label: String,
  val faLabel: String,
) {
  COMPACT("Compact", "فشرده"),
  BALANCED("Balanced", "متعادل"),
  RELAXED("Airy", "راحت و باز"),
  NIGHT_EASE("Night Ease", "آرامش شب"),
}

data class ReaderSettings(
  val appLanguage: AppLanguage = AppLanguage.ENGLISH,
  val themeMode: ReadingThemeMode = ReadingThemeMode.PAPER,
  val readingLayoutMode: ReadingLayoutMode = ReadingLayoutMode.PAGED,
  val fontOption: ReaderFontOption = ReaderFontOption.LITERATA,
  val customFontId: String? = null,
  val fontSizeSp: Int = 18,
  val lineHeightMultiplier: Float = 1.6f,
  val horizontalMarginDp: Int = 22,
  val verticalMarginDp: Int = 20,
  val paragraphSpacingEm: Float = 0.9f,
  val letterSpacingEm: Float = 0.0f,
  val fontWeightValue: Int = 400,
  val eyeComfortWarmth: Float = 0.0f,
  val showPageBorder: Boolean = false,
  val paperTextureEnabled: Boolean = false,
  val textAlign: TextAlignOption = TextAlignOption.LEFT,
  val brightnessOverride: Float = -1f,
  val pdfThemeFilterEnabled: Boolean = true,
  val keepScreenOn: Boolean = false,
  val batterySaverModeEnabled: Boolean = false,
  val volumeKeyPageTurnEnabled: Boolean = false,
  val restoreLastSession: Boolean = true,
  val ttsSpeechRate: Float = 1.0f,
  val ttsPitch: Float = 1.0f,
  val ttsVoiceName: String? = null,
  val translateTargetLang: String = "fa",
  val libraryLayout: LibraryLayoutMode = LibraryLayoutMode.GRID,
  val librarySortOrder: LibrarySortOrder = LibrarySortOrder.RECENT,
  val dailyGoalMinutes: Int = 20,
  val todayReadingSeconds: Int = 0,
  val lastReadingDateKey: String = "",
  val totalReadingSecondsAllTime: Long = 0L,
  val streakReminderEnabled: Boolean = false,
  val streakReminderHour: Int = 20,
  val streakReminderMinute: Int = 0,
)

data class EpubChapter(
  val index: Int,
  val id: String,
  val title: String,
  val href: String,
  val localFilePath: String,
)

data class EpubTocItem(
  val title: String,
  val chapterIndex: Int,
  val anchor: String? = null,
  val level: Int = 0,
)

data class ParsedEpubBook(
  val title: String,
  val author: String,
  val chapters: List<EpubChapter>,
  val toc: List<EpubTocItem>,
  val baseDirPath: String,
)

data class BookSearchMatch(
  val chapterOrPageIndex: Int,
  val locationLabel: String,
  val excerpt: String,
  val query: String,
)

object HighlightPalette {
  const val AMBER = "#FDE68A"
  const val LAVENDER = "#BBF7D0"
  const val SAGE = "#A7F3D0"
  const val ROSE = "#FECDD3"

  val all = listOf(
    "Amber" to AMBER,
    "Mint" to LAVENDER,
    "Sage" to SAGE,
    "Rose" to ROSE,
  )
}
