package com.example.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.model.AppLanguage

@Immutable
interface AppStrings {
  val appName: String
  val librarySubtitle: String
  val volumesCount: (Int) -> String
  val importLabel: String
  val importEpubOrPdf: String
  val loadSamples: String
  val emptyTitle: String
  val emptySubtitle: String
  val searchLibraryHint: String
  val continueReading: String
  val resumeSession: String
  val recentlyOpened: String
  val allBooks: String
  val matchingBooks: String
  val noMatchingBooks: (String) -> String
  val removeBookTitle: String
  val removeBookBody: (String) -> String
  val remove: String
  val cancel: String
  val settingsAndAbout: String
  // Shelves, Filters & Book Metadata Editor
  val importFormatsHint: String
  val newShelfAction: String
  val createShelfTitle: String
  val shelfNameHint: String
  val createShelfButton: String
  val deleteShelfTitle: String
  val deleteShelfBody: (String) -> String
  val noBooksInFilter: String
  val assignToShelves: String
  val noShelvesYet: String
  val readingStatusLabel: String
  val favoriteLabel: String
  val addFavoriteAction: String
  val removeFavoriteAction: String
  val editBookAction: String
  val editBookTitle: String
  val bookTitleLabel: String
  val bookAuthorLabel: String
  val customCoverLabel: String
  val changeCoverAction: String
  val removeCoverAction: String
  val saveChangesAction: String
  // Daily Reading Goal
  val dailyGoalTitle: String
  val dailyGoalComplete: String
  val dailyGoalProgressLabel: (Int, Int) -> String
  val totalReadingTimeLabel: (Int) -> String
  val adjustGoalAction: String
  val setDailyGoalTitle: String
  val dailyTargetMinutesLabel: (Int) -> String
  val saveGoal: String
  // Reading Statistics Dashboard
  val statsDashboardTitle: String
  val statsDashboardSubtitle: String
  val openStatsAction: String
  val readingStatsMenu: String
  val todayReadingLabel: String
  val totalPagesReadLabel: String
  val todayPagesSub: (Int) -> String
  val allTimeReadingLabel: String
  val activeStreakLabel: (Int) -> String
  val dailyReadingTimeChartTitle: String
  val last7DaysLabel: String
  val last14DaysLabel: String
  val dailyGoalLineLabel: (Int) -> String
  val dayBreakdownLabel: (String, Int, Int) -> String
  val noDailyStatsYet: String
  val currentBookTrendTitle: String
  val selectBookLabel: String
  val noBooksForTrend: String
  val trendEmptyHint: String
  val checkpointDetailsLabel: (String, Int, Int) -> String
  val continueThisBook: String
  val readingShelfBreakdown: String
  val minutesShort: (Int) -> String
  val hoursMinutesShort: (Int, Int) -> String
  val pagesCountLabel: (Int) -> String
  val chapterOfTotal: (Int, Int) -> String
  val pageOfTotal: (Int, Int) -> String
  // Reader
  val readTab: String
  val listenTab: String
  val preparingBook: String
  val unableToOpen: String
  val returnToLibrary: String
  val readingTheme: String
  val brightness: String
  val systemAuto: String
  val manual: String
  val adaptPdfColors: String
  val adaptPdfColorsSub: String
  val typographyAndLayout: String
  val pageDisplay: String
  val readingModeSection: String
  val typeface: String
  val manageFontsTts: String
  val textSize: String
  val lineSpacing: String
  val margins: String
  val verticalMargins: String
  val paragraphSpacing: String
  val letterSpacing: String
  val fontWeightLabel: String
  val weightRegular: String
  val weightMedium: String
  val weightBold: String
  val eyeComfortWarmth: String
  val pageBorderToggle: String
  val paperStyleToggle: String
  val paperStyleSub: String
  val readingComfort: String
  val comfortPresets: String
  val resetDefaults: String
  val openFullTypography: String
  val alignment: String
  val contentsTab: String
  val bookmarksTab: String
  val highlightsTab: String
  val searchTab: String
  val searchInBookHint: String
  val noBookmarks: String
  val noHighlights: String
  val searchPrompt: String
  val noMatches: String
  // Selection menu
  val highlightLabel: String
  val noteAction: String
  val quoteAction: String
  val translateAction: String
  val bookmarkAction: String
  val speakAction: String
  val copyAction: String
  val addNoteTitle: String
  val notePlaceholder: String
  val saveNote: String
  val quoteCardTitle: String
  val shareQuote: String
  val copyQuote: String
  val translateTitle: String
  val translatingMessage: String
  val saveTranslationNote: String
  // Settings & Reminders
  val settingsTitle: String
  val settingsSubtitle: String
  val appLanguageSection: String
  val readingDefaultsSection: String
  val defaultTheme: String
  val keepScreenAwake: String
  val keepScreenAwakeSub: String
  val batterySaverToggle: String
  val batterySaverSub: String
  val volumeKeyPageTurnToggle: String
  val volumeKeyPageTurnSub: String
  val resumeLastBook: String
  val resumeLastBookSub: String
  val remindersSection: String
  val streakReminderToggle: String
  val streakReminderSub: String
  val reminderTimeLabel: String
  val setReminderTimeTitle: String
  val testReminderAction: String
  val reminderPermissionNeeded: String
  val widgetSectionTitle: String
  val widgetSectionSub: String
  val fontManagementSection: String
  val readerTypefaces: String
  val readerTypefacesSub: String
  val ttsSection: String
  val voiceEngine: String
  val speechSpeed: String
  val voicePitch: String
  val previewVoice: String
  val stopPreview: String
  val versionSection: String
  val changelog: String
  val localStorage: String
  val clearCache: String
  val aboutMeSection: String
  val craftedBy: String
  val aboutBio: String
  val done: String
  // Tip Jar & PageNest Pro
  val supportAndProSection: String
  val tipJarTitle: String
  val tipJarSubtitle: String
  val tipJarMenu: String
  val tipJarRecommendTitle: String
  val tipJarRecommendBody: (Int) -> String
  val tipJarIranSection: String
  val tipJarGlobalSection: String
  val tipJarAlreadySupported: String
  val tipJarRemindLater: String
  val tipJarScheduleLabel: String
  val tipJarScheduleSub: String
  val tipJarFixedMinutesLabel: (Int) -> String
  val proTitle: String
  val proBadge: String
  val proMenu: String
  val proActiveTitle: String
  val proActiveSub: String
  val proHeroTitle: String
  val proHeroSub: String
  val proBenefitsTitle: String
  val proBenefitNoPrompts: String
  val proBenefitBadge: String
  val proBenefitSupportDev: String
  val proBenefitStoreReady: String
  val proUpgradeIranAction: String
  val proUpgradeGlobalAction: String
  val proRedeemCodeLabel: String
  val proRedeemCodeHint: String
  val proRedeemAction: String
  val proInvalidCodeMessage: String
  val proUnlockedMessage: String
  val openLinkAction: String
  // PageNest Studio (Beta)
  val studioTitle: String
  val studioBetaBadge: String
  val studioSubtitle: String
  val studioMenuAction: String
  val newBookAction: String
  val manuscriptTitleLabel: String
  val authorNameLabel: String
  val synopsisLabel: String
  val writingDirectionLabel: String
  val ltrLabel: String
  val rtlLabel: String
  val addChapterAction: String
  val publishToLibraryAction: String
  val publishingLabel: String
  val publishedSuccessLabel: String
  val openPublishedBookAction: String
  val shareMarkdownAction: String
  val savedLabel: String
  val wordCountLabel: (Int) -> String
  val chaptersCountLabel: (Int) -> String
  val deleteManuscriptTitle: String
  val deleteChapterTitle: String
  val chapterTitleLabel: String
  val renameChapterTitle: String
  val editBookDetailsTitle: String
  val writeHereHint: String
  val studioEmptyTitle: String
  val studioEmptySubtitle: String
  // Additional Home Widgets
  val statsWidgetTitle: String
  val statsWidgetSub: String
  val streakWidgetTitle: String
  val streakWidgetSub: String
  val pinWidgetAction: String
  // v3.2.0 Sleep Timer, Media Controls, Gestures, Soundscapes & Focus, Catalog, Cover Studio
  val focusAndAudioTitle: String
  val sleepTimerLabel: String
  val fadeOutActiveLabel: String
  val lockScreenControlsHint: String
  val tapZonesSection: String
  val leftTapZoneLabel: String
  val centerTapZoneLabel: String
  val rightTapZoneLabel: String
  val edgeBrightnessSwipeToggle: String
  val edgeBrightnessSwipeSub: String
  val doubleTapBookmarkToggle: String
  val doubleTapBookmarkSub: String
  val ambientSoundscapesLabel: String
  val ambientVolumeLabel: String
  val focusTimerLabel: String
  val focusPhaseLabel: String
  val breakPhaseLabel: String
  val focusStartAction: String
  val focusPauseAction: String
  val focusResetAction: String
  val focusCyclesLabel: (Int) -> String
  val focusWorkMinutesLabel: (Int) -> String
  val focusBreakMinutesLabel: (Int) -> String
  val catalogTitle: String
  val catalogMenuAction: String
  val catalogSubtitle: String
  val catalogSearchHint: String
  val catalogAllLanguages: String
  val catalogEnglishOnly: String
  val catalogPersianOnly: String
  val catalogImportAction: String
  val catalogImportedLabel: String
  val catalogDownloadingLabel: String
  val catalogNoResults: String
  val catalogCustomFeedHint: String
  val catalogLoadFeedAction: String
  val coverStudioAction: String
  val coverStudioTitle: String
  val coverThemeLabel: String
  val reorderChaptersAction: String
  val moveUpAction: String
  val moveDownAction: String
}

object EnglishStrings : AppStrings {
  override val appName = "PageNest"
  override val librarySubtitle = "Your calm, local reading shelf"
  override val volumesCount: (Int) -> String = { n -> "$n ${if (n == 1) "volume" else "volumes"}" }
  override val importLabel = "Import"
  override val importEpubOrPdf = "Import Book"
  override val loadSamples = "Load Sample Classics (EN & FA)"
  override val emptyTitle = "Your reading shelf awaits"
  override val emptySubtitle = "Import EPUB, PDF, TXT, Markdown, FB2, or CBZ files from your device. Books are copied into PageNest private storage so they stay available offline."
  override val searchLibraryHint = "Search title or author"
  override val continueReading = "Continue Reading"
  override val resumeSession = "Resume Session"
  override val recentlyOpened = "Recently Opened"
  override val allBooks = "All Books"
  override val matchingBooks = "Matching Books"
  override val noMatchingBooks: (String) -> String = { q -> "No books match “$q”." }
  override val removeBookTitle = "Remove book?"
  override val removeBookBody: (String) -> String = { t -> "“$t” and its saved bookmarks and highlights will be removed from PageNest storage." }
  override val remove = "Remove"
  override val cancel = "Cancel"
  override val settingsAndAbout = "Settings, Fonts & About"
  override val importFormatsHint = "Supports EPUB · PDF · TXT · MD · FB2 · CBZ"
  override val newShelfAction = "New Shelf"
  override val createShelfTitle = "Create Custom Shelf"
  override val shelfNameHint = "e.g. Philosophy, Comics, Weekend Reads"
  override val createShelfButton = "Create"
  override val deleteShelfTitle = "Delete shelf?"
  override val deleteShelfBody: (String) -> String = { name -> "Remove shelf “$name”? Your books will stay safely in All Books." }
  override val noBooksInFilter = "No books in this view yet. Try switching filters or editing a book's status or shelf."
  override val assignToShelves = "Shelves & Collections"
  override val noShelvesYet = "No custom shelves yet. Create one from the shelf filter bar."
  override val readingStatusLabel = "Reading Status"
  override val favoriteLabel = "Favorite"
  override val addFavoriteAction = "Add to Favorites"
  override val removeFavoriteAction = "Remove from Favorites"
  override val editBookAction = "Edit Metadata & Cover"
  override val editBookTitle = "Edit Book & Cover"
  override val bookTitleLabel = "Title"
  override val bookAuthorLabel = "Author"
  override val customCoverLabel = "Book Cover"
  override val changeCoverAction = "Choose Cover Photo"
  override val removeCoverAction = "Reset Cover"
  override val saveChangesAction = "Save Changes"
  override val dailyGoalTitle = "Daily Reading Goal"
  override val dailyGoalComplete = "Goal reached for today"
  override val dailyGoalProgressLabel: (Int, Int) -> String = { mins, goal -> "$mins / $goal min today" }
  override val totalReadingTimeLabel: (Int) -> String = { totalMins -> "Total read: $totalMins min" }
  override val adjustGoalAction = "Edit Goal"
  override val setDailyGoalTitle = "Daily Reading Target"
  override val dailyTargetMinutesLabel: (Int) -> String = { mins -> "$mins minutes / day" }
  override val saveGoal = "Save Target"
  override val statsDashboardTitle = "Reading Statistics"
  override val statsDashboardSubtitle = "Daily reading time, pages read & book trends"
  override val openStatsAction = "Stats"
  override val readingStatsMenu = "Reading Statistics"
  override val todayReadingLabel = "Today's Reading"
  override val totalPagesReadLabel = "Total Pages Read"
  override val todayPagesSub: (Int) -> String = { pages -> "+$pages today" }
  override val allTimeReadingLabel = "All-Time Reading"
  override val activeStreakLabel: (Int) -> String = { days -> "$days-day streak" }
  override val dailyReadingTimeChartTitle = "Daily Reading Time"
  override val last7DaysLabel = "7 Days"
  override val last14DaysLabel = "14 Days"
  override val dailyGoalLineLabel: (Int) -> String = { mins -> "Goal: ${mins}m" }
  override val dayBreakdownLabel: (String, Int, Int) -> String = { day, mins, pages -> "$day · $mins min · $pages pages" }
  override val noDailyStatsYet = "Open a book to start tracking daily reading time and pages read."
  override val currentBookTrendTitle = "Current Book Progress Trend"
  override val selectBookLabel = "Book"
  override val noBooksForTrend = "Import or load a book to see its reading progress trend."
  override val trendEmptyHint = "Turn a few pages to build your progress curve over time."
  override val checkpointDetailsLabel: (String, Int, Int) -> String = { date, pct, unit -> "$date · $pct% (Section $unit)" }
  override val continueThisBook = "Open Book"
  override val readingShelfBreakdown = "Books on Shelf"
  override val minutesShort: (Int) -> String = { mins -> "${mins}m" }
  override val hoursMinutesShort: (Int, Int) -> String = { h, m -> "${h}h ${m}m" }
  override val pagesCountLabel: (Int) -> String = { p -> "$p pages" }
  override val chapterOfTotal: (Int, Int) -> String = { cur, tot -> "Ch. $cur / $tot" }
  override val pageOfTotal: (Int, Int) -> String = { cur, tot -> "Pg. $cur / $tot" }
  override val readTab = "Read"
  override val listenTab = "Listen"
  override val preparingBook = "Preparing your book…"
  override val unableToOpen = "Unable to open book"
  override val returnToLibrary = "Return to Library"
  override val readingTheme = "Reading Theme"
  override val brightness = "Brightness"
  override val systemAuto = "System Auto"
  override val manual = "Manual"
  override val adaptPdfColors = "Adapt page colors to theme"
  override val adaptPdfColorsSub = "Applies warm sepia or inverted dark ink for night reading."
  override val typographyAndLayout = "Typography & Layout"
  override val pageDisplay = "Page Display"
  override val readingModeSection = "Page Flow"
  override val typeface = "Typeface"
  override val manageFontsTts = "Manage Fonts & TTS →"
  override val textSize = "Text Size"
  override val lineSpacing = "Line Spacing"
  override val margins = "Side Margin Borders"
  override val verticalMargins = "Top & Bottom Borders"
  override val paragraphSpacing = "Paragraph Spacing"
  override val letterSpacing = "Letter Spacing"
  override val fontWeightLabel = "Text Weight"
  override val weightRegular = "Regular"
  override val weightMedium = "Medium"
  override val weightBold = "Bold"
  override val eyeComfortWarmth = "Eye-Comfort Warmth"
  override val pageBorderToggle = "Page Frame Border"
  override val paperStyleToggle = "Paper Texture"
  override val paperStyleSub = "Adds fine tactile paper grain and subtle page depth"
  override val readingComfort = "Reading Comfort"
  override val comfortPresets = "Comfort Presets"
  override val resetDefaults = "Reset"
  override val openFullTypography = "All Fonts & Layout →"
  override val alignment = "Alignment"
  override val contentsTab = "Contents"
  override val bookmarksTab = "Bookmarks"
  override val highlightsTab = "Highlights"
  override val searchTab = "Search"
  override val searchInBookHint = "Search inside book…"
  override val noBookmarks = "No bookmarks yet. Tap the bookmark icon in the header to save your spot."
  override val noHighlights = "No highlights yet. Hold any text on the page to highlight, add notes, quote, or translate."
  override val searchPrompt = "Type at least 2 characters to search across chapters or pages."
  override val noMatches = "No matching passages found."
  override val highlightLabel = "Highlight"
  override val noteAction = "Note"
  override val quoteAction = "Quote"
  override val translateAction = "Translate"
  override val bookmarkAction = "Bookmark"
  override val speakAction = "Speak"
  override val copyAction = "Copy"
  override val addNoteTitle = "Add Marginalia Note"
  override val notePlaceholder = "Write your reflection on this passage…"
  override val saveNote = "Save Highlight & Note"
  override val quoteCardTitle = "Share Literary Quote"
  override val shareQuote = "Share"
  override val copyQuote = "Copy Quote"
  override val translateTitle = "Instant Translation"
  override val translatingMessage = "Translating on device…"
  override val saveTranslationNote = "Save as Note"
  override val settingsTitle = "Settings"
  override val settingsSubtitle = "Language, reminders, fonts, TTS & creator"
  override val appLanguageSection = "App Language"
  override val readingDefaultsSection = "Reading Defaults"
  override val defaultTheme = "Default Theme"
  override val keepScreenAwake = "Keep screen awake"
  override val keepScreenAwakeSub = "Prevent screen sleep while reading a book"
  override val batterySaverToggle = "Battery-Saver Mode"
  override val batterySaverSub = "Caps display at 60Hz, simplifies transitions, and reduces blur shaders while reading"
  override val volumeKeyPageTurnToggle = "Volume key page turn"
  override val volumeKeyPageTurnSub = "Use Volume Down / Up buttons to turn pages while reading"
  override val resumeLastBook = "Resume last book on launch"
  override val resumeLastBookSub = "Reopen your active reading session automatically"
  override val remindersSection = "Daily Streak Reminders & Widget"
  override val streakReminderToggle = "Daily streak reminder"
  override val streakReminderSub = "Gentle local notification if today's reading goal is still unfinished"
  override val reminderTimeLabel = "Reminder time"
  override val setReminderTimeTitle = "Daily Reminder Time"
  override val testReminderAction = "Send Test"
  override val reminderPermissionNeeded = "Allow notification permission to receive daily streak reminders."
  override val widgetSectionTitle = "Continue Reading Home Widget"
  override val widgetSectionSub = "Long-press your home screen → Widgets → PageNest to pin your active book & progress."
  override val fontManagementSection = "Font Management"
  override val readerTypefaces = "Reader Typefaces"
  override val readerTypefacesSub = "Choose Persian/Latin fonts or import .ttf / .otf files"
  override val ttsSection = "Text-to-Speech (Listen Mode)"
  override val voiceEngine = "Voice Engine"
  override val speechSpeed = "Speech Speed"
  override val voicePitch = "Voice Pitch"
  override val previewVoice = "Preview Voice"
  override val stopPreview = "Stop Preview"
  override val versionSection = "Version Management & Storage"
  override val changelog = "Changelog"
  override val localStorage = "Local Storage"
  override val clearCache = "Clear Cache"
  override val aboutMeSection = "About Me"
  override val craftedBy = "Crafted by AmirSenpai"
  override val aboutBio = "PageNest is a calm, local sanctuary for EPUB, PDF, TXT, Markdown, FB2 & CBZ reading in English and Persian — with zero accounts or tracking."
  override val done = "Done"
  override val supportAndProSection = "Support & PageNest Pro"
  override val tipJarTitle = "Tip Jar"
  override val tipJarSubtitle = "Support calm, ad-free indie development"
  override val tipJarMenu = "Tip Jar (Support)"
  override val tipJarRecommendTitle = "Enjoying your reading sanctuary?"
  override val tipJarRecommendBody: (Int) -> String = { mins ->
    if (mins > 0) "You've spent $mins minutes reading in PageNest! If it helps your focus, consider leaving a small tip or upgrading to Pro."
    else "If PageNest makes your daily reading calmer, consider supporting indie development or upgrading to Pro."
  }
  override val tipJarIranSection = "Inside Iran (Rial / Local Links)"
  override val tipJarGlobalSection = "Outside Iran (International Links)"
  override val tipJarAlreadySupported = "I Already Supported"
  override val tipJarRemindLater = "Remind Later"
  override val tipJarScheduleLabel = "Tip Jar Recommendation Timing"
  override val tipJarScheduleSub = "Suggest support gently based on reading time or turn off"
  override val tipJarFixedMinutesLabel: (Int) -> String = { mins -> "Every $mins min of reading" }
  override val proTitle = "PageNest Pro"
  override val proBadge = "PRO"
  override val proMenu = "PageNest Pro"
  override val proActiveTitle = "PageNest Pro Active"
  override val proActiveSub = "Thank you for supporting PageNest! All recommendation prompts are disabled."
  override val proHeroTitle = "Support PageNest & Unlock Pro"
  override val proHeroSub = "Enjoy a distraction-free reading sanctuary forever with zero prompts and direct supporter perks."
  override val proBenefitsTitle = "Included with PageNest Pro"
  override val proBenefitNoPrompts = "Zero tip prompts or upgrade banners across the app"
  override val proBenefitBadge = "Exclusive PRO Supporter emblem in your Library header"
  override val proBenefitSupportDev = "Directly funds Persian RTL typography, format engines & updates"
  override val proBenefitStoreReady = "Works offline forever — activate via Myket/store link or supporter key"
  override val proUpgradeIranAction = "Upgrade via Myket / Iran Link"
  override val proUpgradeGlobalAction = "Upgrade Outside Iran"
  override val proRedeemCodeLabel = "Have a Supporter Key?"
  override val proRedeemCodeHint = "Enter code (e.g. from receipt)"
  override val proRedeemAction = "Activate"
  override val proInvalidCodeMessage = "Supporter key not recognized. Check your receipt or MonetizationConfig.kt."
  override val proUnlockedMessage = "PageNest Pro activated — thank you for your support!"
  override val openLinkAction = "Open"
  override val studioTitle = "PageNest Studio"
  override val studioBetaBadge = "BETA"
  override val studioSubtitle = "Craft multi-chapter books offline and publish directly to your shelf"
  override val studioMenuAction = "PageNest Studio (Beta)"
  override val newBookAction = "New Book"
  override val manuscriptTitleLabel = "Book Title"
  override val authorNameLabel = "Pen Name / Author"
  override val synopsisLabel = "Synopsis (Optional)"
  override val writingDirectionLabel = "Writing Direction"
  override val ltrLabel = "Left to Right (EN)"
  override val rtlLabel = "Right to Left (FA/AR)"
  override val addChapterAction = "Chapter"
  override val publishToLibraryAction = "Publish to Library"
  override val publishingLabel = "Publishing…"
  override val publishedSuccessLabel = "Published to your PageNest library!"
  override val openPublishedBookAction = "Read Book"
  override val shareMarkdownAction = "Export Markdown"
  override val savedLabel = "Saved"
  override val wordCountLabel: (Int) -> String = { words -> "$words words" }
  override val chaptersCountLabel: (Int) -> String = { ch -> "$ch ${if (ch == 1) "chapter" else "chapters"}" }
  override val deleteManuscriptTitle = "Delete manuscript?"
  override val deleteChapterTitle = "Delete chapter?"
  override val chapterTitleLabel = "Chapter Title"
  override val renameChapterTitle = "Rename Chapter"
  override val editBookDetailsTitle = "Manuscript Details"
  override val writeHereHint = "Begin your story… Use # Heading, > Quote, **bold**, or *italic*."
  override val studioEmptyTitle = "Start writing your book"
  override val studioEmptySubtitle = "Draft chapters in a calm distraction-free studio with live autosave, Markdown export, and one-tap EPUB publishing to your library."
  override val statsWidgetTitle = "Reading Statistics Widget (4×2)"
  override val statsWidgetSub = "Displays daily minutes vs goal, total pages read, streak & 7-day trend bars."
  override val streakWidgetTitle = "Streak Flame Widget (1×1)"
  override val streakWidgetSub = "Compact 1×1 home badge showing the flame emblem and your active streak days."
  override val pinWidgetAction = "Pin Widget"
  override val focusAndAudioTitle = "Audio, Sleep & Focus"
  override val sleepTimerLabel = "Sleep Timer"
  override val fadeOutActiveLabel = "Gentle fade-out active"
  override val lockScreenControlsHint = "Lock-screen & Bluetooth earbud controls enabled while playing."
  override val tapZonesSection = "Tap Zones & Gestures"
  override val leftTapZoneLabel = "Left Zone"
  override val centerTapZoneLabel = "Center Zone"
  override val rightTapZoneLabel = "Right Zone"
  override val edgeBrightnessSwipeToggle = "Left-Edge Brightness Swipe"
  override val edgeBrightnessSwipeSub = "Slide vertically along the left edge to adjust screen brightness"
  override val doubleTapBookmarkToggle = "Double-Tap Quick Bookmark"
  override val doubleTapBookmarkSub = "Double-tap any text page to toggle a bookmark at your current spot"
  override val ambientSoundscapesLabel = "Ambient Soundscapes"
  override val ambientVolumeLabel = "Ambient Volume"
  override val focusTimerLabel = "Reading Focus Timer (Pomodoro)"
  override val focusPhaseLabel = "Focus Session"
  override val breakPhaseLabel = "Short Break"
  override val focusStartAction = "Start"
  override val focusPauseAction = "Pause"
  override val focusResetAction = "Reset"
  override val focusCyclesLabel: (Int) -> String = { c -> "$c cycles done" }
  override val focusWorkMinutesLabel: (Int) -> String = { m -> "Focus: ${m}m" }
  override val focusBreakMinutesLabel: (Int) -> String = { m -> "Break: ${m}m" }
  override val catalogTitle = "Free Books Catalog"
  override val catalogMenuAction = "Browse Free Books (OPDS)"
  override val catalogSubtitle = "Project Gutenberg, Standard Ebooks & custom OPDS feeds"
  override val catalogSearchHint = "Search public-domain classics or authors…"
  override val catalogAllLanguages = "All"
  override val catalogEnglishOnly = "English"
  override val catalogPersianOnly = "Persian"
  override val catalogImportAction = "Download EPUB"
  override val catalogImportedLabel = "Added to Library"
  override val catalogDownloadingLabel = "Downloading…"
  override val catalogNoResults = "No books found for this query. Try another keyword or source."
  override val catalogCustomFeedHint = "https://example.org/opds.xml"
  override val catalogLoadFeedAction = "Load Feed"
  override val coverStudioAction = "Cover Studio"
  override val coverStudioTitle = "Studio Cover Designer"
  override val coverThemeLabel = "Cover Theme"
  override val reorderChaptersAction = "Reorder Chapters"
  override val moveUpAction = "Move Earlier"
  override val moveDownAction = "Move Later"
}

object PersianStrings : AppStrings {
  override val appName = "پیج‌نست"
  override val librarySubtitle = "قفسه آرام مطالعه"
  override val volumesCount: (Int) -> String = { n -> "$n کتاب" }
  override val importLabel = "افزودن"
  override val importEpubOrPdf = "افزودن کتاب"
  override val loadSamples = "بارگذاری کتاب‌های نمونه (فارسی و انگلیسی)"
  override val emptyTitle = "قفسه مطالعه شما آماده است"
  override val emptySubtitle = "فایل‌های EPUB، PDF، TXT، Markdown، FB2 یا CBZ را از دستگاه خود وارد کنید. کتاب‌ها در حافظه امن برنامه ذخیره می‌شوند تا همیشه آفلاین در دسترس باشند."
  override val searchLibraryHint = "جستجوی عنوان یا نویسنده"
  override val continueReading = "ادامه مطالعه"
  override val resumeSession = "ادامه آخرین جلسه"
  override val recentlyOpened = "اخیراً خوانده‌شده"
  override val allBooks = "همه کتاب‌ها"
  override val matchingBooks = "کتاب‌های یافت‌شده"
  override val noMatchingBooks: (String) -> String = { q -> "کتابی با عبارت «$q» یافت نشد." }
  override val removeBookTitle = "حذف کتاب؟"
  override val removeBookBody: (String) -> String = { t -> "کتاب «$t» به همراه نشانک‌ها و هایلایت‌های آن از حافظه برنامه حذف می‌شود." }
  override val remove = "حذف"
  override val cancel = "انصراف"
  override val settingsAndAbout = "تنظیمات، فونت‌ها و درباره سازنده"
  override val importFormatsHint = "پشتیبانی از EPUB · PDF · TXT · MD · FB2 · CBZ"
  override val newShelfAction = "قفسه جدید"
  override val createShelfTitle = "ساخت قفسه سفارشی"
  override val shelfNameHint = "مثلاً: فلسفه، کمیک، آخر هفته"
  override val createShelfButton = "ساختن"
  override val deleteShelfTitle = "حذف قفسه؟"
  override val deleteShelfBody: (String) -> String = { name -> "قفسه «$name» حذف شود؟ کتاب‌های داخل آن در همه کتاب‌ها باقی می‌مانند." }
  override val noBooksInFilter = "کتابی در این فیلتر یافت نشد. فیلتر را تغییر دهید یا وضعیت و قفسه کتاب را ویرایش کنید."
  override val assignToShelves = "قفسه‌ها و مجموعه‌ها"
  override val noShelvesYet = "هنوز قفسه سفارشی نساخته‌اید. از نوار بالای کتابخانه یک قفسه بسازید."
  override val readingStatusLabel = "وضعیت مطالعه"
  override val favoriteLabel = "برگزیده"
  override val addFavoriteAction = "افزودن به علاقه‌مندی‌ها"
  override val removeFavoriteAction = "حذف از علاقه‌مندی‌ها"
  override val editBookAction = "ویرایش مشخصات و جلد"
  override val editBookTitle = "ویرایش اطلاعات و جلد کتاب"
  override val bookTitleLabel = "عنوان کتاب"
  override val bookAuthorLabel = "نویسنده"
  override val customCoverLabel = "تصویر جلد"
  override val changeCoverAction = "انتخاب تصویر جلد"
  override val removeCoverAction = "حذف جلد سفارشی"
  override val saveChangesAction = "ذخیره تغییرات"
  override val dailyGoalTitle = "هدف مطالعه روزانه"
  override val dailyGoalComplete = "هدف امروز تکمیل شد"
  override val dailyGoalProgressLabel: (Int, Int) -> String = { mins, goal -> "$mins از $goal دقیقه امروز" }
  override val totalReadingTimeLabel: (Int) -> String = { totalMins -> "مجموع مطالعه: $totalMins دقیقه" }
  override val adjustGoalAction = "تنظیم هدف"
  override val setDailyGoalTitle = "تعیین هدف روزانه"
  override val dailyTargetMinutesLabel: (Int) -> String = { mins -> "$mins دقیقه در روز" }
  override val saveGoal = "ذخیره هدف"
  override val statsDashboardTitle = "آمار و روند مطالعه"
  override val statsDashboardSubtitle = "زمان روزانه، صفحات خوانده‌شده و نمودار پیشرفت کتاب"
  override val openStatsAction = "آمار"
  override val readingStatsMenu = "داشبورد آمار مطالعه"
  override val todayReadingLabel = "مطالعه امروز"
  override val totalPagesReadLabel = "کل صفحات خوانده‌شده"
  override val todayPagesSub: (Int) -> String = { pages -> "+$pages صفحه امروز" }
  override val allTimeReadingLabel = "مجموع زمان مطالعه"
  override val activeStreakLabel: (Int) -> String = { days -> "رشته $days روزه" }
  override val dailyReadingTimeChartTitle = "زمان مطالعه روزانه"
  override val last7DaysLabel = "۷ روز"
  override val last14DaysLabel = "۱۴ روز"
  override val dailyGoalLineLabel: (Int) -> String = { mins -> "هدف: $mins د" }
  override val dayBreakdownLabel: (String, Int, Int) -> String = { day, mins, pages -> "$day · $mins دقیقه · $pages صفحه" }
  override val noDailyStatsYet = "برای ثبت زمان مطالعه روزانه و صفحات، یک کتاب را باز کنید."
  override val currentBookTrendTitle = "روند پیشرفت کتاب فعلی"
  override val selectBookLabel = "کتاب"
  override val noBooksForTrend = "برای مشاهده نمودار پیشرفت، کتابی به کتابخانه اضافه کنید."
  override val trendEmptyHint = "چند صفحه بخوانید تا منحنی روند پیشرفت کتاب رسم شود."
  override val checkpointDetailsLabel: (String, Int, Int) -> String = { date, pct, unit -> "$date · $pct٪ (بخش $unit)" }
  override val continueThisBook = "ادامه کتاب"
  override val readingShelfBreakdown = "پیشرفت کتاب‌های قفسه"
  override val minutesShort: (Int) -> String = { mins -> "$mins د" }
  override val hoursMinutesShort: (Int, Int) -> String = { h, m -> "$h س $m د" }
  override val pagesCountLabel: (Int) -> String = { p -> "$p صفحه" }
  override val chapterOfTotal: (Int, Int) -> String = { cur, tot -> "فصل $cur از $tot" }
  override val pageOfTotal: (Int, Int) -> String = { cur, tot -> "صفحه $cur از $tot" }
  override val readTab = "مطالعه"
  override val listenTab = "شنیدن"
  override val preparingBook = "در حال آماده‌سازی کتاب…"
  override val unableToOpen = "امکان باز کردن کتاب وجود ندارد"
  override val returnToLibrary = "بازگشت به کتابخانه"
  override val readingTheme = "پوسته مطالعه"
  override val brightness = "روشنایی صفحه"
  override val systemAuto = "خودکار سیستم"
  override val manual = "دستی"
  override val adaptPdfColors = "تطبیق رنگ صفحات با پوسته"
  override val adaptPdfColorsSub = "اعمال فیلتر سپیا یا حالت شب روی صفحات PDF و کمیک برای مطالعه راحت‌تر."
  override val typographyAndLayout = "تایپوگرافی و چیدمان"
  override val pageDisplay = "نمایش صفحه"
  override val readingModeSection = "شیوه ورق‌زدن"
  override val typeface = "قلم متن"
  override val manageFontsTts = "مدیریت فونت و گفتار ←"
  override val textSize = "اندازه قلم"
  override val lineSpacing = "فاصله خطوط"
  override val margins = "حاشیه چپ و راست"
  override val verticalMargins = "حاشیه بالا و پایین"
  override val paragraphSpacing = "فاصله پاراگراف‌ها"
  override val letterSpacing = "فاصله حروف"
  override val fontWeightLabel = "ضخامت متن"
  override val weightRegular = "معمولی"
  override val weightMedium = "متوسط"
  override val weightBold = "ضخیم"
  override val eyeComfortWarmth = "نور گرم محافظ چشم"
  override val pageBorderToggle = "کادر حاشیه صفحه"
  override val paperStyleToggle = "بافت کاغذی صفحه"
  override val paperStyleSub = "افزودن بافت طبیعی کاغذ و عمق ملایم هنگام مطالعه"
  override val readingComfort = "راحتی مطالعه"
  override val comfortPresets = "حالت‌های آماده"
  override val resetDefaults = "بازنشانی"
  override val openFullTypography = "همه فونت‌ها و چیدمان ←"
  override val alignment = "ترازبندی"
  override val contentsTab = "فهرست"
  override val bookmarksTab = "نشانک‌ها"
  override val highlightsTab = "هایلایت‌ها"
  override val searchTab = "جستجو"
  override val searchInBookHint = "جستجو در متن کتاب…"
  override val noBookmarks = "هنوز نشانکی ثبت نشده است. از دکمه نشانک در نوار بالا استفاده کنید."
  override val noHighlights = "هنوز هایلایتی ندارید. برای هایلایت، یادداشت، نقل‌قول یا ترجمه، روی متن نگه دارید."
  override val searchPrompt = "برای جستجو در فصل‌ها یا صفحات، حداقل ۲ حرف وارد کنید."
  override val noMatches = "عبارت مورد نظر یافت نشد."
  override val highlightLabel = "هایلایت"
  override val noteAction = "یادداشت"
  override val quoteAction = "نقل‌قول"
  override val translateAction = "ترجمه"
  override val bookmarkAction = "نشانک"
  override val speakAction = "خوانش"
  override val copyAction = "کپی"
  override val addNoteTitle = "افزودن یادداشت حاشیه"
  override val notePlaceholder = "یادداشت یا برداشت خود را درباره این بخش بنویسید…"
  override val saveNote = "ذخیره هایلایت و یادداشت"
  override val quoteCardTitle = "اشتراک‌گذاری نقل‌قول"
  override val shareQuote = "اشتراک‌گذاری"
  override val copyQuote = "کپی نقل‌قول"
  override val translateTitle = "ترجمه فوری"
  override val translatingMessage = "در حال ترجمه روی دستگاه…"
  override val saveTranslationNote = "ذخیره به عنوان یادداشت"
  override val settingsTitle = "تنظیمات"
  override val settingsSubtitle = "زبان، یادآور، فونت، گفتار و درباره سازنده"
  override val appLanguageSection = "زبان برنامه"
  override val readingDefaultsSection = "پیش‌فرض‌های مطالعه"
  override val defaultTheme = "پوسته پیش‌فرض"
  override val keepScreenAwake = "روشن ماندن صفحه"
  override val keepScreenAwakeSub = "جلوگیری از خاموش شدن نمایشگر هنگام مطالعه"
  override val batterySaverToggle = "حالت صرفه‌جویی باتری"
  override val batterySaverSub = "محدودسازی نرخ نوسازی به ۶۰ هرتز، ساده‌سازی انیمیشن‌ها و کاهش افکت بلور هنگام مطالعه"
  override val volumeKeyPageTurnToggle = "ورق‌زدن با دکمه‌های صدا"
  override val volumeKeyPageTurnSub = "استفاده از دکمه‌های کاهش و افزایش صدا برای رفتن به صفحه بعد یا قبل هنگام مطالعه"
  override val resumeLastBook = "باز کردن آخرین کتاب در شروع"
  override val resumeLastBookSub = "ادامه خودکار آخرین جلسه مطالعه پس از ورود"
  override val remindersSection = "یادآور مطالعه روزانه و ویجت"
  override val streakReminderToggle = "یادآور روزانه رشته مطالعه"
  override val streakReminderSub = "ارسال اعلان محلی ملایم در صورت کامل نشدن هدف مطالعه امروز"
  override val reminderTimeLabel = "ساعت یادآور"
  override val setReminderTimeTitle = "زمان یادآور روزانه"
  override val testReminderAction = "ارسال آزمایشی"
  override val reminderPermissionNeeded = "برای دریافت یادآورهای روزانه، مجوز اعلان را فعال کنید."
  override val widgetSectionTitle = "ویجت ادامه مطالعه در صفحه اصلی"
  override val widgetSectionSub = "صفحه اصلی گوشی را لمس طولانی کنید ← ویجت‌ها ← پیج‌نست تا آخرین کتاب روی صفحه قرار گیرد."
  override val fontManagementSection = "مدیریت فونت‌ها"
  override val readerTypefaces = "فونت‌های مطالعه"
  override val readerTypefacesSub = "انتخاب فونت فارسی/لاتین یا افزودن فایل .ttf و .otf"
  override val ttsSection = "خوانش صوتی متن (TTS)"
  override val voiceEngine = "موتور صدا"
  override val speechSpeed = "سرعت خوانش"
  override val voicePitch = "زیر و بمی صدا"
  override val previewVoice = "پخش نمونه صدا"
  override val stopPreview = "توقف پخش"
  override val versionSection = "مدیریت نسخه و حافظه"
  override val changelog = "تغییرات نسخه"
  override val localStorage = "حافظه داخلی"
  override val clearCache = "پاکسازی کش"
  override val aboutMeSection = "درباره من"
  override val craftedBy = "ساخته شده توسط AmirSenpai"
  override val aboutBio = "پیج‌نست یک کتابخوان آرام و محلی برای فایل‌های EPUB، PDF، TXT، Markdown، FB2 و CBZ با پشتیبانی کامل از فارسی و انگلیسی است."
  override val done = "بستن"
  override val supportAndProSection = "حمایت و نسخه حرفه‌ای (Pro)"
  override val tipJarTitle = "صندوق حمایت (Tip Jar)"
  override val tipJarSubtitle = "حمایت داوطلبانه از توسعه پیج‌نست"
  override val tipJarMenu = "حمایت از سازنده (Tip Jar)"
  override val tipJarRecommendTitle = "از مطالعه در پیج‌نست لذت می‌برید؟"
  override val tipJarRecommendBody: (Int) -> String = { mins ->
    if (mins > 0) "تاکنون $mins دقیقه در پیج‌نست مطالعه کرده‌اید! اگر این برنامه برایتان مفید بوده، می‌توانید با یک حمایت کوچک یا ارتقا به نسخه Pro از ادامه توسعه حمایت کنید."
    else "اگر پیج‌نست فضای مطالعه‌تان را آرام‌تر کرده، می‌توانید از توسعه مستقل آن حمایت کنید."
  }
  override val tipJarIranSection = "داخل ایران (درگاه ریالی و لینک‌های داخلی)"
  override val tipJarGlobalSection = "خارج از ایران (لینک‌های بین‌المللی)"
  override val tipJarAlreadySupported = "قبلاً حمایت کرده‌ام"
  override val tipJarRemindLater = "یادآوری بعدی"
  override val tipJarScheduleLabel = "زمان‌بندی پیشنهاد حمایت"
  override val tipJarScheduleSub = "پیشنهاد محترمانه بر اساس زمان مطالعه تصادفی یا ثابت"
  override val tipJarFixedMinutesLabel: (Int) -> String = { mins -> "هر $mins دقیقه مطالعه" }
  override val proTitle = "نسخه حرفه‌ای پیج‌نست (Pro)"
  override val proBadge = "PRO"
  override val proMenu = "پیج‌نست Pro"
  override val proActiveTitle = "نسخه پیج‌نست Pro فعال است"
  override val proActiveSub = "از حمایت ارزشمند شما سپاسگزاریم! تمام پیشنهادها و پیام‌های تبلیغاتی غیرفعال هستند."
  override val proHeroTitle = "حمایت از پیج‌نست و فعال‌سازی Pro"
  override val proHeroSub = "تجربه‌ای کاملاً آرام و بدون هیچ پیام پیشنهادی همراه با نشان ویژه حامیان."
  override val proBenefitsTitle = "مزایای نسخه حرفه‌ای"
  override val proBenefitNoPrompts = "حذف کامل تمام پیام‌های پیشنهاد حمایت در برنامه"
  override val proBenefitBadge = "نمایش نشان اختصاصی PRO در بخش کتابخانه"
  override val proBenefitSupportDev = "پشتیبانی مستقیم از توسعه فونت فارسی، موتورهای جدید و آپدیت‌ها"
  override val proBenefitStoreReady = "عملکرد کاملاً آفلاین — فعال‌سازی از طریق مایکت، لینک حمایت یا کد حامی"
  override val proUpgradeIranAction = "ارتقا از مایکت / لینک داخل ایران"
  override val proUpgradeGlobalAction = "ارتقا از خارج از ایران"
  override val proRedeemCodeLabel = "کد فعال‌سازی حامیان دارید؟"
  override val proRedeemCodeHint = "کد دریافتی را وارد کنید"
  override val proRedeemAction = "فعال‌سازی"
  override val proInvalidCodeMessage = "کد واردشده معتبر نیست. کد رسید یا تنظیمات فایل MonetizationConfig را بررسی کنید."
  override val proUnlockedMessage = "نسخه پیج‌نست Pro فعال شد — سپاس از حمایت شما!"
  override val openLinkAction = "باز کردن"
  override val studioTitle = "استودیو نویسندگی پیج‌نست"
  override val studioBetaBadge = "آزمایشی"
  override val studioSubtitle = "کتاب‌های چندفصلی بنویسید و با یک لمس در کتابخانه خود منتشر کنید"
  override val studioMenuAction = "استودیو نویسندگی (بتا)"
  override val newBookAction = "کتاب جدید"
  override val manuscriptTitleLabel = "عنوان کتاب"
  override val authorNameLabel = "نام نویسنده / تخلص"
  override val synopsisLabel = "چکیده یا مقدمه (اختیاری)"
  override val writingDirectionLabel = "جهت نگارش"
  override val ltrLabel = "چپ به راست (انگلیسی)"
  override val rtlLabel = "راست به چپ (فارسی)"
  override val addChapterAction = "فصل جدید"
  override val publishToLibraryAction = "انتشار در کتابخانه"
  override val publishingLabel = "در حال انتشار…"
  override val publishedSuccessLabel = "کتاب با موفقیت در کتابخانه پیج‌نست منتشر شد!"
  override val openPublishedBookAction = "مطالعه کتاب"
  override val shareMarkdownAction = "خروجی مارک‌داون"
  override val savedLabel = "ذخیره شد"
  override val wordCountLabel: (Int) -> String = { words -> "$words کلمه" }
  override val chaptersCountLabel: (Int) -> String = { ch -> "$ch فصل" }
  override val deleteManuscriptTitle = "حذف دست‌نوشته؟"
  override val deleteChapterTitle = "حذف فصل؟"
  override val chapterTitleLabel = "عنوان فصل"
  override val renameChapterTitle = "تغییر نام فصل"
  override val editBookDetailsTitle = "مشخصات کتاب"
  override val writeHereHint = "داستان یا فصل خود را بنویسید… از # تیتر، > نقل‌قول و **ضخیم** پشتیبانی می‌شود."
  override val studioEmptyTitle = "نوشتن کتاب خود را آغاز کنید"
  override val studioEmptySubtitle = "در محیطی آرام فصل‌به‌فصل بنویسید، به صورت خودکار ذخیره کنید و کتاب را به شکل EPUB وارد قفسه کتابخانه خود کنید."
  override val statsWidgetTitle = "ویجت آمار مطالعه (۴×۲)"
  override val statsWidgetSub = "نمایش زمان مطالعه امروز، کل صفحات خوانده‌شده، رشته روزانه و نمودار ۷ روز اخیر."
  override val streakWidgetTitle = "ویجت شعله رشته مطالعه (۱×۱)"
  override val streakWidgetSub = "نشان کوچک ۱×۱ صفحه اصلی با آیکون شعله و عدد روزهای متوالی مطالعه."
  override val pinWidgetAction = "افزودن ویجت"
  override val focusAndAudioTitle = "صدا، تایمر خواب و تمرکز"
  override val sleepTimerLabel = "تایمر خواب"
  override val fadeOutActiveLabel = "کاهش ملایم صدا فعال است"
  override val lockScreenControlsHint = "کنترل پخش در صفحه قفل و دکمه‌های هندزفری بلوتوث هنگام پخش فعال است."
  override val tapZonesSection = "مناطق لمس و ژست‌های حرکتی"
  override val leftTapZoneLabel = "بخش چپ"
  override val centerTapZoneLabel = "بخش میانی"
  override val rightTapZoneLabel = "بخش راست"
  override val edgeBrightnessSwipeToggle = "تنظیم روشنایی با کشیدن لبه چپ"
  override val edgeBrightnessSwipeSub = "کشیدن عمودی روی لبه چپ صفحه برای تغییر سریع روشنایی مطالعه"
  override val doubleTapBookmarkToggle = "نشانک سریع با دو ضربه"
  override val doubleTapBookmarkSub = "دو بار ضربه روی صفحه متنی برای ثبت یا حذف سریع نشانک"
  override val ambientSoundscapesLabel = "صداهای آرامش‌بخش محیطی"
  override val ambientVolumeLabel = "شدت صدای محیطی"
  override val focusTimerLabel = "تایمر تمرکز مطالعه (پومودورو)"
  override val focusPhaseLabel = "دوره تمرکز"
  override val breakPhaseLabel = "استراحت کوتاه"
  override val focusStartAction = "شروع"
  override val focusPauseAction = "مکث"
  override val focusResetAction = "بازنشانی"
  override val focusCyclesLabel: (Int) -> String = { c -> "$c دوره کامل‌شده" }
  override val focusWorkMinutesLabel: (Int) -> String = { m -> "تمرکز: $m د" }
  override val focusBreakMinutesLabel: (Int) -> String = { m -> "استراحت: $m د" }
  override val catalogTitle = "کتابخانه رایگان و آزاد (OPDS)"
  override val catalogMenuAction = "مرور کتاب‌های رایگان (OPDS)"
  override val catalogSubtitle = "پروژه گوتنبرگ، Standard Ebooks و فیدهای سفارشی OPDS"
  override val catalogSearchHint = "جستجوی عنوان یا نویسنده آثار کلاسیک…"
  override val catalogAllLanguages = "همه"
  override val catalogEnglishOnly = "انگلیسی"
  override val catalogPersianOnly = "فارسی"
  override val catalogImportAction = "دانلود EPUB"
  override val catalogImportedLabel = "به کتابخانه افزوده شد"
  override val catalogDownloadingLabel = "در حال دانلود…"
  override val catalogNoResults = "کتابی یافت نشد. کلیدواژه یا منبع دیگری را امتحان کنید."
  override val catalogCustomFeedHint = "https://example.org/opds.xml"
  override val catalogLoadFeedAction = "بارگذاری فید"
  override val coverStudioAction = "استودیو طراحی جلد"
  override val coverStudioTitle = "طراحی جلد کتاب"
  override val coverThemeLabel = "تم رنگی جلد"
  override val reorderChaptersAction = "مرتب‌سازی و چیدمان فصل‌ها"
  override val moveUpAction = "انتقال به قبل"
  override val moveDownAction = "انتقال به بعد"
}

fun AppLanguage.strings(): AppStrings =
  when (this) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.PERSIAN -> PersianStrings
  }

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
