package com.example.ui.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.swiss.AtlasReaderToolbar
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.cbz.CbzReaderView
import com.example.engine.epub.EpubReaderView
import com.example.engine.pdf.PdfReaderView
import com.example.engine.translate.TargetLanguageOption
import com.example.model.AmbientSoundType
import com.example.model.BookFormat
import com.example.model.HighlightPalette
import com.example.model.ReaderMode
import com.example.model.ReadingRulerStyle
import com.example.model.ReadingThemeMode
import com.example.model.SelectionAnchor
import com.example.model.SleepTimerPreset
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.navigation.LocalVolumeKeyDispatcher
import com.example.ui.reader.components.FocusAudioSheet
import com.example.ui.reader.components.IndexAndToolsSheet
import com.example.ui.reader.components.SignatureBottomDockOverlay
import com.example.ui.reader.components.ThemeBrightnessSheet
import com.example.ui.reader.components.TypographySettingsSheet
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.ReadingPalette
import com.example.ui.theme.toPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel,
  onNavigateBack: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenStats: (String) -> Unit = {},
  onOpenBookInfo: (String) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val palette = uiState.settings.themeMode.toPalette()
  val strings = LocalAppStrings.current
  val hazeState = remember { HazeState() }
  val context = LocalContext.current
  val view = LocalView.current
  @Suppress("DEPRECATION")
  val clipboardManager = LocalClipboardManager.current

  var showNoteDialog by remember { mutableStateOf(false) }
  var showQuoteDialog by remember { mutableStateOf(false) }
  var showTranslateDialog by remember { mutableStateOf(false) }
  var noteInitialColor by remember { mutableStateOf(HighlightPalette.AMBER) }
  var externalPageStepSignal by remember { mutableStateOf(0 to 0) }
  val batterySaverEnabled = uiState.settings.batterySaverModeEnabled
  val volumeKeyDispatcher = LocalVolumeKeyDispatcher.current

  // Intercept hardware volume buttons for hands-free page turns while reading
  DisposableEffect(volumeKeyDispatcher, uiState.settings.volumeKeyPageTurnEnabled, uiState.readerMode) {
    val active = uiState.settings.volumeKeyPageTurnEnabled && uiState.readerMode == ReaderMode.READ
    if (volumeKeyDispatcher != null && active) {
      volumeKeyDispatcher.onVolumeKey = { keyCode ->
        val delta =
          when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> 1
            KeyEvent.KEYCODE_VOLUME_UP -> -1
            else -> 0
          }
        if (delta != 0) {
          externalPageStepSignal = delta to (externalPageStepSignal.second + 1)
          true
        } else {
          false
        }
      }
    } else {
      volumeKeyDispatcher?.onVolumeKey = null
    }
    onDispose {
      volumeKeyDispatcher?.onVolumeKey = null
    }
  }

  // Cap display refresh rate to 60Hz when Battery-Saver Mode is active while reading
  DisposableEffect(batterySaverEnabled) {
    val activity = context as? Activity
    if (activity != null && batterySaverEnabled) {
      runCatching {
        val lp = activity.window.attributes
        val display =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
          else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
          val currentMode = display.mode
          val lowest60HzMode =
            display.supportedModes
              ?.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                  it.physicalHeight == currentMode.physicalHeight &&
                  it.refreshRate >= 59.5f
              }
              ?.minByOrNull { it.refreshRate }
          if (lowest60HzMode != null) {
            lp.preferredDisplayModeId = lowest60HzMode.modeId
          }
        }
        lp.preferredRefreshRate = 60f
        activity.window.attributes = lp
      }
    }
    onDispose {
      if (activity != null) {
        runCatching {
          val lp = activity.window.attributes
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lp.preferredDisplayModeId = 0
          }
          lp.preferredRefreshRate = 0f
          activity.window.attributes = lp
        }
      }
    }
  }

  // Apply in-app screen brightness override (or live edge-swipe preview)
  val effectiveBrightness = uiState.liveBrightnessPreview ?: uiState.settings.brightnessOverride
  LaunchedEffect(effectiveBrightness) {
    val activity = context as? Activity ?: return@LaunchedEffect
    val lp = activity.window.attributes
    lp.screenBrightness = effectiveBrightness
    activity.window.attributes = lp
  }

  // Keep screen awake if enabled in Settings
  DisposableEffect(uiState.settings.keepScreenOn) {
    val activity = context as? Activity
    if (uiState.settings.keepScreenOn) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  // Sync status bar icon contrast with active reading theme
  DisposableEffect(palette.isDark) {
    val activity = context as? Activity
    val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
    val prevLightStatus = controller?.isAppearanceLightStatusBars
    val prevLightNav = controller?.isAppearanceLightNavigationBars
    controller?.isAppearanceLightStatusBars = !palette.isDark
    controller?.isAppearanceLightNavigationBars = !palette.isDark
    onDispose {
      if (prevLightStatus != null) controller.isAppearanceLightStatusBars = prevLightStatus
      if (prevLightNav != null) controller.isAppearanceLightNavigationBars = prevLightNav
      activity?.window?.let { win ->
        val lp = win.attributes
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        win.attributes = lp
      }
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        viewModel.flushReadingStatsNow()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(uiState.toastMessage) {
    if (uiState.toastMessage != null) {
      delay(2200)
      viewModel.clearToast()
    }
  }

  BackHandler {
    when {
      showTranslateDialog -> {
        showTranslateDialog = false
        viewModel.dismissTranslation()
      }
      showQuoteDialog -> showQuoteDialog = false
      showNoteDialog -> showNoteDialog = false
      uiState.activeSheet != ReaderSheetType.NONE -> viewModel.closeSheet()
      uiState.selectedText.isNotBlank() -> viewModel.clearSelection()
      else -> {
        viewModel.flushReadingStatsNow()
        onNavigateBack()
      }
    }
  }

  val format = uiState.book?.bookFormat() ?: BookFormat.EPUB
  val locationSummary =
    when (uiState.documentState) {
      is ReaderDocumentState.EpubReady ->
        uiState.currentChapter?.title ?: "Section ${uiState.currentChapterOrPage + 1}"
      is ReaderDocumentState.PdfReady, is ReaderDocumentState.CbzReady ->
        "Page ${uiState.currentChapterOrPage + 1} / ${uiState.totalUnits}"
      else -> strings.preparingBook
    }

  Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
    // Layer 1a: Haze source backing surface (PDF or CBZ view)
    val pdfDoc = uiState.documentState as? ReaderDocumentState.PdfReady
    if (pdfDoc != null) {
      PdfReaderView(
        session = pdfDoc.session,
        currentPage = uiState.currentChapterOrPage,
        jumpToPageSignal = uiState.jumpToPdfPageSignal,
        settings = uiState.settings,
        palette = palette,
        hazeState = hazeState,
        externalPageStepSignal = externalPageStepSignal,
        onPageChanged = viewModel::onPdfPageChanged,
        onUnitViewed = viewModel::onReadingUnitViewed,
        onContentTap = viewModel::toggleControls,
        onQuickBookmark = viewModel::toggleCurrentBookmark,
        onPreviewBrightness = viewModel::previewBrightness,
        onCommitBrightness = viewModel::setBrightnessOverride,
        modifier = Modifier.fillMaxSize(),
      )
    }
    val cbzDoc = uiState.documentState as? ReaderDocumentState.CbzReady
    if (cbzDoc != null) {
      CbzReaderView(
        session = cbzDoc.session,
        currentPage = uiState.currentChapterOrPage,
        jumpToPageSignal = uiState.jumpToPdfPageSignal,
        settings = uiState.settings,
        palette = palette,
        hazeState = hazeState,
        externalPageStepSignal = externalPageStepSignal,
        onPageChanged = viewModel::onPdfPageChanged,
        onUnitViewed = viewModel::onReadingUnitViewed,
        onContentTap = viewModel::toggleControls,
        onQuickBookmark = viewModel::toggleCurrentBookmark,
        onPreviewBrightness = viewModel::previewBrightness,
        onCommitBrightness = viewModel::setBrightnessOverride,
        modifier = Modifier.fillMaxSize(),
      )
    }
    when (val doc = uiState.documentState) {
      ReaderDocumentState.Loading -> {
        Box(
          modifier = Modifier.fillMaxSize().hazeSource(state = hazeState).background(palette.background),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = palette.accent, strokeWidth = 2.5.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = strings.preparingBook,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.secondaryText,
            )
          }
        }
      }

      is ReaderDocumentState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().hazeSource(state = hazeState).background(palette.background).padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = strings.unableToOpen,
              style = MaterialTheme.typography.titleLarge,
              color = palette.text,
            )
            Text(
              text = doc.message,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.secondaryText,
              textAlign = TextAlign.Center,
            )
            val onAccent = if (palette.isDark) Color(0xFF071630) else Color.White
            Button(
              onClick = onNavigateBack,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.accent,
                  contentColor = onAccent,
                ),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(strings.returnToLibrary, color = onAccent)
            }
          }
        }
      }

      is ReaderDocumentState.EpubReady -> {
        val chapter = doc.book.chapters.getOrNull(uiState.currentChapterOrPage)
        if (chapter != null) {
          EpubReaderView(
            chapter = chapter,
            fontsDir = doc.fontsDir,
            customFonts = uiState.customFonts,
            settings = uiState.settings,
            palette = palette,
            highlights = uiState.currentChapterHighlights,
            controlsVisible = uiState.controlsVisible,
            dockExpanded = uiState.dockExpanded,
            initialScrollFraction = uiState.currentScrollFraction,
            pendingAnchor = uiState.pendingAnchor,
            jumpToPositionSignal = uiState.jumpToEpubSignal,
            hasPreviousChapter = uiState.currentChapterOrPage > 0,
            hasNextChapter = uiState.currentChapterOrPage < doc.book.chapters.lastIndex,
            clearSelectionSignal = uiState.clearSelectionSignal,
            hazeState = hazeState,
            externalPageStepSignal = externalPageStepSignal,
            onScrollFractionChanged = viewModel::onEpubScrollFractionChanged,
            onUnitViewed = viewModel::onReadingUnitViewed,
            onSelectionChanged = viewModel::onSelectionChanged,
            onContentTap = viewModel::toggleControls,
            onNavigateChapterDelta = viewModel::navigateChapterDelta,
            onQuickBookmark = viewModel::toggleCurrentBookmark,
            onPreviewBrightness = viewModel::previewBrightness,
            onCommitBrightness = viewModel::setBrightnessOverride,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      is ReaderDocumentState.PdfReady, is ReaderDocumentState.CbzReady -> Unit
    }

    // Optional Tactile Paper Texture overlay (non-touch-blocking)
    if (uiState.settings.paperTextureEnabled) {
      PaperTextureOverlay(
        palette = palette,
        modifier = Modifier.fillMaxSize().testTag("reader_paper_texture_overlay"),
      )
    }

    // Optional Warm Eye-Comfort filter overlay (non-touch-blocking)
    if (uiState.settings.eyeComfortWarmth > 0.01f) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(Color(0xFFFFA726).copy(alpha = uiState.settings.eyeComfortWarmth * 0.22f))
      )
    }

    // Optional Reading Ruler / Line Guide Overlay (pass-through canvas + draggable edge thumb)
    if (
      uiState.settings.readingRulerEnabled &&
      uiState.documentState !is ReaderDocumentState.Loading &&
      uiState.documentState !is ReaderDocumentState.Error
    ) {
      ReadingRulerOverlay(
        style = uiState.settings.readingRulerStyle,
        heightDp = uiState.settings.readingRulerHeightDp,
        dimAlpha = uiState.settings.readingRulerDimAlpha,
        palette = palette,
        onCycleStyle = {
          val styles = ReadingRulerStyle.entries
          val next = styles[(styles.indexOf(uiState.settings.readingRulerStyle) + 1) % styles.size]
          viewModel.setReadingRulerStyle(next)
        },
        modifier = Modifier.fillMaxSize(),
      )
    }

    // Distraction-free Swiss Editorial Monochrome Reader Top Toolbar
    AnimatedVisibility(
      visible = uiState.controlsVisible,
      enter = if (batterySaverEnabled) EnterTransition.None else fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically { -it / 2 },
      exit = if (batterySaverEnabled) ExitTransition.None else fadeOut(spring(stiffness = Spring.StiffnessMedium)) + slideOutVertically { -it / 2 },
      modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
    ) {
      AtlasReaderToolbar(
        title = uiState.book?.title ?: "",
        progressPercent = uiState.overallProgressPercent,
        isBookmarked = uiState.isCurrentLocationBookmarked,
        onBack = {
          viewModel.flushReadingStatsNow()
          onNavigateBack()
        },
        onToggleBookmark = viewModel::toggleCurrentBookmark,
        onOpenToc = { viewModel.openSheet(ReaderSheetType.INDEX_TOOLS, initialToolsTab = 0) },
        onOpenSearch = { viewModel.openSheet(ReaderSheetType.INDEX_TOOLS, initialToolsTab = 3) },
        onOpenTypography = { viewModel.openSheet(ReaderSheetType.TYPOGRAPHY) },
        surfaceColor = palette.dockSurface,
        contentColor = palette.text,
        dividerColor = palette.dockBorder,
      )
    }

    // Floating Liquid-Glass Text Selection Menu positioned ABOVE selected text
    AnimatedVisibility(
      visible = uiState.selectionAnchor.isActive && !showNoteDialog && !showQuoteDialog && !showTranslateDialog,
      enter = if (batterySaverEnabled) EnterTransition.None else fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.92f),
      exit = if (batterySaverEnabled) ExitTransition.None else fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(targetScale = 0.94f),
      modifier = Modifier.fillMaxSize(),
    ) {
      FloatingSelectionMenuOverlay(
        anchor = uiState.selectionAnchor,
        hazeState = hazeState,
        palette = palette,
        controlsVisible = uiState.controlsVisible,
        batterySaverEnabled = batterySaverEnabled,
        onHighlightColor = { hex -> viewModel.highlightSelection(hex) },
        onNoteClick = {
          noteInitialColor = HighlightPalette.AMBER
          showNoteDialog = true
        },
        onQuoteClick = { showQuoteDialog = true },
        onTranslateClick = {
          showTranslateDialog = true
          viewModel.translateSelection()
        },
        onBookmarkClick = viewModel::bookmarkSelection,
        onSpeakClick = viewModel::speakSelection,
        onCopyClick = {
          clipboardManager.setText(AnnotatedString(uiState.selectedText))
          viewModel.clearSelection()
          viewModel.showMessage(strings.copyAction)
        },
        onDismiss = viewModel::clearSelection,
      )
    }

    // Transient feedback toast pill
    AnimatedVisibility(
      visible = uiState.toastMessage != null,
      enter = if (batterySaverEnabled) EnterTransition.None else fadeIn(),
      exit = if (batterySaverEnabled) ExitTransition.None else fadeOut(),
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(top = 74.dp),
    ) {
      LiquidGlassCard(
        hazeState = hazeState,
        isDark = palette.isDark,
        shape = CircleShape,
        tintColor =
          if (palette.isDark) Color(0xFF1F1F28).copy(alpha = 0.85f)
          else palette.dockSurface.copy(alpha = 0.88f),
        batterySaverEnabled = batterySaverEnabled,
      ) {
        Text(
          text = uiState.toastMessage.orEmpty(),
          style = MaterialTheme.typography.labelMedium,
          color = palette.text,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }

    // Layer 2 & 3: Theme-Responsive Liquid-Glass Floating Pills
    val todayMins = uiState.settings.todayReadingSeconds / 60
    val goalSummary = strings.dailyGoalProgressLabel(todayMins, uiState.settings.dailyGoalMinutes)
    SignatureBottomDockOverlay(
      visible = uiState.controlsVisible && uiState.documentState !is ReaderDocumentState.Error,
      expanded = uiState.dockExpanded,
      hazeState = hazeState,
      palette = palette,
      bookFormat = format,
      progressPercent = uiState.overallProgressPercent,
      formattedTimeRemaining = uiState.formattedTimeRemaining,
      sessionElapsedSeconds = uiState.sessionElapsedSeconds,
      dailyGoalSummary = goalSummary,
      locationSummary = locationSummary,
      readerMode = uiState.readerMode,
      listenState = uiState.listenState,
      sleepTimer = uiState.sleepTimer,
      onToggleExpanded = viewModel::toggleDockExpanded,
      onScrubProgress = viewModel::scrubToOverallProgress,
      onSelectMode = viewModel::setReaderMode,
      onTogglePlayPauseTts = viewModel::togglePlayPauseTts,
      onSkipTtsPrevious = viewModel::skipTtsPrevious,
      onSkipTtsNext = viewModel::skipTtsNext,
      onCycleTtsSpeed = viewModel::cycleTtsSpeed,
      onOpenFocusAudioSheet = { viewModel.openSheet(ReaderSheetType.FOCUS_AUDIO) },
      onOpenThemeBrightness = { viewModel.openSheet(ReaderSheetType.THEME_BRIGHTNESS) },
      onOpenTypography = { viewModel.openSheet(ReaderSheetType.TYPOGRAPHY) },
      onOpenContents = { viewModel.openSheet(ReaderSheetType.INDEX_TOOLS, initialToolsTab = 0) },
      onOpenStats = {
        viewModel.flushReadingStatsNow()
        val id = uiState.book?.id
        if (!id.isNullOrBlank()) {
          onOpenStats(id)
        }
      },
      batterySaverEnabled = batterySaverEnabled,
      modifier = Modifier.align(Alignment.BottomCenter),
    )

    // Bottom Sheets
    when (uiState.activeSheet) {
      ReaderSheetType.THEME_BRIGHTNESS ->
        ThemeBrightnessSheet(
          settings = uiState.settings,
          palette = palette,
          bookFormat = format,
          readerMode = uiState.readerMode,
          onSelectTheme = viewModel::setThemeMode,
          onChangeBrightness = viewModel::setBrightnessOverride,
          onChangeFontSize = viewModel::setFontSize,
          onSelectLayoutMode = viewModel::setReadingLayoutMode,
          onToggleKeepScreenOn = viewModel::setKeepScreenOn,
          onToggleReadingRuler = viewModel::setReadingRulerEnabled,
          onTogglePaperTexture = viewModel::setPaperTextureEnabled,
          onToggleBatterySaver = viewModel::setBatterySaverModeEnabled,
          onTogglePdfThemeFilter = viewModel::setPdfThemeFilter,
          onOpenTypography = { viewModel.openSheet(ReaderSheetType.TYPOGRAPHY) },
          onOpenSearch = { viewModel.openSheet(ReaderSheetType.INDEX_TOOLS, initialToolsTab = 3) },
          onToggleListenMode = {
            val nextMode =
              if (uiState.readerMode == ReaderMode.LISTEN) ReaderMode.READ else ReaderMode.LISTEN
            viewModel.setReaderMode(nextMode)
          },
          onDismiss = viewModel::closeSheet,
        )

      ReaderSheetType.TYPOGRAPHY ->
        TypographySettingsSheet(
          settings = uiState.settings,
          customFonts = uiState.customFonts,
          palette = palette,
          bookFormat = format,
          onSelectLayoutMode = viewModel::setReadingLayoutMode,
          onSelectFont = viewModel::setFontOption,
          onSelectCustomFont = viewModel::setCustomFont,
          onChangeFontSize = viewModel::setFontSize,
          onChangeLineHeight = viewModel::setLineHeight,
          onChangeMargin = viewModel::setHorizontalMargin,
          onChangeVerticalMargin = viewModel::setVerticalMargin,
          onChangeParagraphSpacing = viewModel::setParagraphSpacing,
          onChangeLetterSpacing = viewModel::setLetterSpacing,
          onChangeFontWeight = viewModel::setFontWeight,
          onChangeEyeComfortWarmth = viewModel::setEyeComfortWarmth,
          onTogglePageBorder = viewModel::setShowPageBorder,
          onTogglePaperTexture = viewModel::setPaperTextureEnabled,
          onToggleKeepScreenOn = viewModel::setKeepScreenOn,
          onToggleVolumeKeyPageTurn = viewModel::setVolumeKeyPageTurnEnabled,
          onToggleBatterySaver = viewModel::setBatterySaverModeEnabled,
          onSelectAlign = viewModel::setTextAlign,
          onTogglePdfThemeFilter = viewModel::setPdfThemeFilter,
          onSelectLeftTapAction = viewModel::setLeftTapAction,
          onSelectCenterTapAction = viewModel::setCenterTapAction,
          onSelectRightTapAction = viewModel::setRightTapAction,
          onToggleEdgeBrightnessSwipe = viewModel::setEdgeBrightnessSwipeEnabled,
          onToggleDoubleTapBookmark = viewModel::setDoubleTapBookmarkEnabled,
          onToggleReadingRuler = viewModel::setReadingRulerEnabled,
          onSelectReadingRulerStyle = viewModel::setReadingRulerStyle,
          onChangeReadingRulerHeight = viewModel::setReadingRulerHeightDp,
          onChangeReadingRulerDimAlpha = viewModel::setReadingRulerDimAlpha,
          onApplyComfortPreset = viewModel::applyComfortPreset,
          onResetComfort = viewModel::resetComfortDefaults,
          onOpenThemeHub = { viewModel.openSheet(ReaderSheetType.THEME_BRIGHTNESS) },
          onOpenSettings = onOpenSettings,
          onDismiss = viewModel::closeSheet,
        )

      ReaderSheetType.INDEX_TOOLS ->
        IndexAndToolsSheet(
          initialTab = uiState.initialToolsTab,
          bookFormat = format,
          totalUnits = uiState.totalUnits,
          currentChapterOrPage = uiState.currentChapterOrPage,
          tocItems = uiState.tocItems,
          bookmarks = uiState.bookmarks,
          highlights = uiState.highlights,
          searchQuery = uiState.searchQuery,
          searchResults = uiState.searchResults,
          isSearching = uiState.isSearching,
          palette = palette,
          onSelectChapterOrPage = { idx, frac, anchor ->
            if (!format.isReflowableText) {
              viewModel.jumpToPdfPage(idx)
              viewModel.closeSheet()
            } else {
              viewModel.goToChapter(idx, frac, anchor)
            }
          },
          onDeleteBookmark = viewModel::deleteBookmark,
          onDeleteHighlight = viewModel::deleteHighlight,
          onSearchQueryChange = viewModel::runSearch,
          onOpenBookInfo = {
            viewModel.flushReadingStatsNow()
            viewModel.closeSheet()
            val id = uiState.book?.id
            if (!id.isNullOrBlank()) {
              onOpenBookInfo(id)
            }
          },
          onDismiss = viewModel::closeSheet,
        )

      ReaderSheetType.FOCUS_AUDIO ->
        FocusAudioSheet(
          settings = uiState.settings,
          palette = palette,
          readerMode = uiState.readerMode,
          sleepTimer = uiState.sleepTimer,
          activeSound = uiState.activeAmbientSound,
          focusTimer = uiState.focusTimer,
          onSelectSleepPreset = viewModel::selectSleepTimer,
          onSelectAmbientSound = viewModel::selectAmbientSound,
          onChangeAmbientVolume = viewModel::setAmbientVolume,
          onToggleFocusTimer = viewModel::toggleFocusTimer,
          onResetFocusTimer = viewModel::resetFocusTimer,
          onUpdateFocusDurations = viewModel::setFocusDurations,
          onDismiss = viewModel::closeSheet,
        )

      ReaderSheetType.NONE -> Unit
    }

    // Selection Action Dialogs
    if (showNoteDialog && uiState.selectedText.isNotBlank()) {
      AddNoteDialog(
        selectedText = uiState.selectedText,
        initialColorHex = noteInitialColor,
        palette = palette,
        onSave = { colorHex, noteText ->
          viewModel.highlightSelection(colorHex, noteText)
          showNoteDialog = false
        },
        onDismiss = { showNoteDialog = false },
      )
    }

    if (showQuoteDialog && uiState.selectedText.isNotBlank()) {
      QuoteCardDialog(
        selectedText = uiState.selectedText,
        bookTitle = uiState.book?.title.orEmpty(),
        bookAuthor = uiState.book?.author.orEmpty(),
        palette = palette,
        onCopyQuote = { formatted ->
          clipboardManager.setText(AnnotatedString(formatted))
          showQuoteDialog = false
          viewModel.clearSelection()
          viewModel.showMessage(strings.copyQuote)
        },
        onShareQuote = { formatted ->
          val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
              putExtra(Intent.EXTRA_TEXT, formatted)
              type = "text/plain"
            }
          context.startActivity(Intent.createChooser(sendIntent, strings.quoteCardTitle))
          showQuoteDialog = false
          viewModel.clearSelection()
        },
        onDismiss = { showQuoteDialog = false },
      )
    }

    if (showTranslateDialog && uiState.selectedText.isNotBlank()) {
      TranslateSelectionDialog(
        selectedText = uiState.selectedText,
        targetLanguages = uiState.supportedTranslationTargets,
        selectedTargetCode = uiState.settings.translateTargetLang,
        translationState = uiState.translationState,
        palette = palette,
        onSelectTarget = { code -> viewModel.translateSelection(code) },
        onCopyTranslation = { translated ->
          clipboardManager.setText(AnnotatedString(translated))
          viewModel.showMessage(strings.copyAction)
        },
        onSaveAsNote = {
          viewModel.saveTranslationAsNote()
          showTranslateDialog = false
        },
        onDismiss = {
          showTranslateDialog = false
          viewModel.dismissTranslation()
        },
      )
    }
  }
}

@Composable
private fun FloatingSelectionMenuOverlay(
  anchor: SelectionAnchor,
  hazeState: HazeState,
  palette: ReadingPalette,
  controlsVisible: Boolean,
  batterySaverEnabled: Boolean,
  onHighlightColor: (String) -> Unit,
  onNoteClick: () -> Unit,
  onQuoteClick: () -> Unit,
  onTranslateClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onSpeakClick: () -> Unit,
  onCopyClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val density = LocalDensity.current
  val statusBarTopPx = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }

  var menuWidthPx by remember { mutableStateOf(with(density) { 320.dp.roundToPx() }) }
  var menuHeightPx by remember { mutableStateOf(with(density) { 154.dp.roundToPx() }) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val maxPopupWidthDp = (maxWidth - 24.dp).coerceIn(240.dp, 344.dp)
    val containerWidthPx = with(density) { maxWidth.toPx() }
    val containerHeightPx = with(density) { maxHeight.toPx() }
    val edgeMarginPx = with(density) { 12.dp.toPx() }
    val gapPx = with(density) { 12.dp.toPx() }
    val headerReservePx = statusBarTopPx + with(density) { if (controlsVisible) 70.dp.toPx() else 12.dp.toPx() }
    val dockReservePx = with(density) { if (controlsVisible) 136.dp.toPx() else 24.dp.toPx() }

    val centerXPx = with(density) { ((anchor.leftDp + anchor.rightDp) / 2f).dp.toPx() }
    val topPx = with(density) { anchor.topDp.dp.toPx() }
    val bottomPx = with(density) { anchor.bottomDp.dp.toPx() }

    // Always place using physical TopLeft + absoluteOffset so both LTR (English) and RTL (Persian)
    // clamp cleanly within [edgeMarginPx, containerWidthPx - menuWidthPx - edgeMarginPx].
    val clampedLeftPx =
      (centerXPx - menuWidthPx / 2f)
        .coerceIn(edgeMarginPx, (containerWidthPx - menuWidthPx - edgeMarginPx).coerceAtLeast(edgeMarginPx))

    val aboveYPx = topPx - menuHeightPx - gapPx
    val belowYPx = bottomPx + gapPx
    val maxAllowedY = (containerHeightPx - menuHeightPx - dockReservePx).coerceAtLeast(headerReservePx)

    val chosenYPx =
      if (aboveYPx >= headerReservePx) {
        aboveYPx.coerceAtMost(maxAllowedY)
      } else {
        belowYPx.coerceIn(headerReservePx, maxAllowedY)
      }

    val isNightBw = palette.mode == ReadingThemeMode.NIGHT_BW
    val glassTint =
      when {
        isNightBw -> Color(0xFF141414).copy(alpha = 0.88f)
        palette.isDark -> palette.dockSurface.copy(alpha = 0.88f)
        else -> palette.dockSurface.copy(alpha = 0.90f)
      }
    val tileBg =
      if (isNightBw) Color.White.copy(alpha = 0.10f)
      else palette.controlSurface.copy(alpha = if (palette.isDark) 0.78f else 0.72f)
    val tileBorder =
      if (palette.isDark) Color.White.copy(alpha = if (isNightBw) 0.24f else 0.14f)
      else palette.dockBorder.copy(alpha = 0.62f)
    val outerShadowAlpha = if (palette.isDark) 0.42f else 0.16f

    LiquidGlassCard(
      hazeState = hazeState,
      isDark = palette.isDark,
      shape = RoundedCornerShape(24.dp),
      tintColor = glassTint,
      batterySaverEnabled = batterySaverEnabled,
      modifier =
        Modifier
          .align(AbsoluteAlignment.TopLeft)
          .absoluteOffset { IntOffset(clampedLeftPx.roundToInt(), chosenYPx.roundToInt()) }
          .widthIn(min = 240.dp, max = maxPopupWidthDp)
          .shadow(
            elevation = if (batterySaverEnabled) 6.dp else 14.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = Color.Black.copy(alpha = outerShadowAlpha),
            spotColor = Color.Black.copy(alpha = outerShadowAlpha),
          )
          .onSizeChanged {
            if (it.width > 0) menuWidthPx = it.width
            if (it.height > 0) menuHeightPx = it.height
          }
          .testTag("floating_selection_menu"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Row 1: Highlight Swatches + Label + Dismiss
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = strings.highlightLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = palette.secondaryText,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.width(2.dp))
            HighlightPalette.all.forEach { (name, hex) ->
              val dotColor = Color(android.graphics.Color.parseColor(hex))
              Box(
                modifier =
                  Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onHighlightColor(hex) }
                    .testTag("highlight_color_${name.lowercase()}"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(dotColor)
                      .border(
                        width = 1.dp,
                        color =
                          if (palette.isDark) Color.White.copy(alpha = 0.45f)
                          else palette.text.copy(alpha = 0.18f),
                        shape = CircleShape,
                      ),
                )
              }
            }
          }

          Surface(
            shape = CircleShape,
            color = tileBg,
            border = BorderStroke(0.7.dp, tileBorder),
            modifier =
              Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .testTag("selection_dismiss_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.done,
                tint = palette.secondaryText,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }

        HorizontalDivider(color = palette.divider.copy(alpha = 0.7f))

        // Row 2 & 3: Balanced 3x2 grid so all 6 actions fit comfortably with zero horizontal clipping
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SelectionActionChip(
              icon = Icons.Rounded.EditNote,
              label = strings.noteAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_note_button",
              modifier = Modifier.weight(1f),
              onClick = onNoteClick,
            )
            SelectionActionChip(
              icon = Icons.Rounded.FormatQuote,
              label = strings.quoteAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_quote_button",
              modifier = Modifier.weight(1f),
              onClick = onQuoteClick,
            )
            SelectionActionChip(
              icon = Icons.Rounded.Translate,
              label = strings.translateAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_translate_button",
              modifier = Modifier.weight(1f),
              onClick = onTranslateClick,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SelectionActionChip(
              icon = Icons.Outlined.BookmarkBorder,
              label = strings.bookmarkAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_bookmark_button",
              modifier = Modifier.weight(1f),
              onClick = onBookmarkClick,
            )
            SelectionActionChip(
              icon = Icons.Rounded.RecordVoiceOver,
              label = strings.speakAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_speak_button",
              modifier = Modifier.weight(1f),
              onClick = onSpeakClick,
            )
            SelectionActionChip(
              icon = Icons.Rounded.ContentCopy,
              label = strings.copyAction,
              iconTint = palette.accent,
              fg = palette.text,
              bg = tileBg,
              borderColor = tileBorder,
              testTag = "selection_copy_button",
              modifier = Modifier.weight(1f),
              onClick = onCopyClick,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SelectionActionChip(
  icon: ImageVector,
  label: String,
  iconTint: Color,
  fg: Color,
  bg: Color,
  borderColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = BorderStroke(0.7.dp, borderColor),
    modifier =
      modifier
        .heightIn(min = 38.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconTint,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AddNoteDialog(
  selectedText: String,
  initialColorHex: String,
  palette: ReadingPalette,
  onSave: (colorHex: String, noteText: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var noteText by remember { mutableStateOf("") }
  var selectedColor by remember { mutableStateOf(initialColorHex) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.dockSurface,
    titleContentColor = palette.text,
    textContentColor = palette.text,
    title = {
      Text(
        text = strings.addNoteTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.25f),
        ) {
          Text(
            text = "“$selectedText”",
            style = MaterialTheme.typography.bodySmall,
            color = palette.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HighlightPalette.all.forEach { (_, hex) ->
            val dotColor = Color(android.graphics.Color.parseColor(hex))
            val active = selectedColor == hex
            Box(
              modifier =
                Modifier
                  .size(if (active) 28.dp else 24.dp)
                  .clip(CircleShape)
                  .background(dotColor)
                  .border(
                    width = if (active) 2.5.dp else 1.dp,
                    color = if (active) palette.accent else palette.dockBorder,
                    shape = CircleShape,
                  )
                  .clickable { selectedColor = hex },
            )
          }
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          placeholder = { Text(strings.notePlaceholder, color = palette.secondaryText) },
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = palette.accent,
              unfocusedBorderColor = palette.dockBorder,
              focusedTextColor = palette.text,
              unfocusedTextColor = palette.text,
            ),
          modifier = Modifier.fillMaxWidth().testTag("note_input"),
        )
      }
    },
    confirmButton = {
      val onAccent = if (palette.isDark) Color(0xFF071630) else Color.White
      Button(
        onClick = { onSave(selectedColor, noteText) },
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = onAccent),
      ) {
        Text(strings.saveNote, color = onAccent)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.secondaryText)
      }
    },
  )
}

@Composable
private fun QuoteCardDialog(
  selectedText: String,
  bookTitle: String,
  bookAuthor: String,
  palette: ReadingPalette,
  onCopyQuote: (String) -> Unit,
  onShareQuote: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val formatted =
    buildString {
      append("“").append(selectedText.trim()).append("”")
      if (bookTitle.isNotBlank()) {
        append("\n— ").append(bookTitle)
        if (bookAuthor.isNotBlank()) append(" · ").append(bookAuthor)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.dockSurface,
    titleContentColor = palette.text,
    textContentColor = palette.text,
    title = {
      Text(
        text = strings.quoteCardTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.background,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.FormatQuote,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(26.dp),
          )
          Text(
            text = selectedText.trim(),
            fontFamily = LiterataFontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = palette.text,
          )
          if (bookTitle.isNotBlank()) {
            Text(
              text = "— $bookTitle${if (bookAuthor.isNotBlank()) " · $bookAuthor" else ""}",
              style = MaterialTheme.typography.labelMedium,
              color = palette.secondaryText,
            )
          }
        }
      }
    },
    confirmButton = {
      val onAccent = if (palette.isDark) Color(0xFF071630) else Color.White
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { onCopyQuote(formatted) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
          border = BorderStroke(1.dp, palette.dockBorder),
        ) {
          Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.text)
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.copyQuote, color = palette.text)
        }
        Button(
          onClick = { onShareQuote(formatted) },
          colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = onAccent),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = onAccent)
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.shareQuote, color = onAccent)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.done, color = palette.secondaryText)
      }
    },
  )
}

@Composable
private fun TranslateSelectionDialog(
  selectedText: String,
  targetLanguages: List<TargetLanguageOption>,
  selectedTargetCode: String,
  translationState: TranslationUiState,
  palette: ReadingPalette,
  onSelectTarget: (String) -> Unit,
  onCopyTranslation: (String) -> Unit,
  onSaveAsNote: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.dockSurface,
    titleContentColor = palette.text,
    textContentColor = palette.text,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Translate, contentDescription = null, tint = palette.accent)
        Text(
          text = strings.translateTitle,
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "“$selectedText”",
          style = MaterialTheme.typography.bodySmall,
          color = palette.secondaryText,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          targetLanguages.forEach { lang ->
            val selected = lang.code == selectedTargetCode
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) palette.accent.copy(alpha = 0.18f) else palette.controlSurface,
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelectTarget(lang.code) },
            ) {
              Text(
                text = lang.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }

        when (translationState) {
          TranslationUiState.Idle,
          is TranslationUiState.Loading -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              LinearProgressIndicator(
                color = palette.accent,
                trackColor = palette.controlSurface,
                modifier = Modifier.fillMaxWidth(),
              )
              Text(
                text = strings.translatingMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText,
              )
            }
          }

          is TranslationUiState.Error -> {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            ) {
              Text(
                text = translationState.message,
                style = MaterialTheme.typography.bodySmall,
                color = palette.text,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          is TranslationUiState.Success -> {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = palette.background,
              border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text =
                    "${translationState.outcome.sourceLanguageCode.uppercase()} → ${translationState.outcome.targetLanguageCode.uppercase()}",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.accent,
                )
                Text(
                  text = translationState.outcome.translatedText,
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.text,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      val onAccent = if (palette.isDark) Color(0xFF071630) else Color.White
      if (translationState is TranslationUiState.Success) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { onCopyTranslation(translationState.outcome.translatedText) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
            border = BorderStroke(1.dp, palette.dockBorder),
          ) {
            Text(strings.copyAction, color = palette.text)
          }
          Button(
            onClick = onSaveAsNote,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = onAccent),
          ) {
            Text(strings.saveTranslationNote, color = onAccent)
          }
        }
      } else {
        TextButton(onClick = onDismiss) {
          Text(strings.done, color = palette.accent)
        }
      }
    },
    dismissButton = {
      if (translationState is TranslationUiState.Success) {
        TextButton(onClick = onDismiss) {
          Text(strings.done, color = palette.secondaryText)
        }
      }
    },
  )
}

/**
 * Lightweight tiled organic paper-grain & page-depth overlay so both EPUB & PDF screens feel like
 * physical pressed book paper when Paper Texture is toggled on.
 */
@Composable
private fun PaperTextureOverlay(
  palette: ReadingPalette,
  modifier: Modifier = Modifier,
) {
  val isDark = palette.isDark
  val isNightBw = palette.mode == ReadingThemeMode.NIGHT_BW
  val tileBrush =
    remember(isDark, isNightBw) {
      val size = 192
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      val rng = Random(4217)
      for (y in 0 until size) {
        val horizontalFiber = if (y % 4 == 0) 5 else 0
        for (x in 0 until size) {
          val speckle = rng.nextInt(-18, 19)
          val fleck = if (rng.nextInt(95) == 0) 24 else 0
          val idx = y * size + x
          if (isDark) {
            val lum = if (isNightBw) 220 else 235
            val alpha = ((8 + speckle + fleck).coerceIn(0, 26) * (if (isNightBw) 0.55f else 0.75f)).toInt()
            pixels[idx] = android.graphics.Color.argb(alpha, lum, lum, lum)
          } else {
            val darkInk = speckle < 0
            val alpha =
              if (darkInk) {
                ((-speckle + horizontalFiber + fleck).coerceIn(0, 28) * 0.85f).toInt()
              } else {
                ((speckle + fleck).coerceIn(0, 22) * 0.65f).toInt()
              }
            pixels[idx] =
              if (darkInk) {
                android.graphics.Color.argb(alpha, 96, 82, 58)
              } else {
                android.graphics.Color.argb(alpha, 255, 252, 244)
              }
          }
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)
      ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

  val edgeTint =
    if (isDark) Color.White.copy(alpha = if (isNightBw) 0.025f else 0.035f)
    else Color(0xFF5A462B).copy(alpha = 0.065f)

  Canvas(modifier = modifier) {
    drawRect(brush = tileBrush)
    // Subtle left/right book-spine paper shading
    drawRect(
      brush =
        Brush.horizontalGradient(
          0.00f to edgeTint,
          0.07f to Color.Transparent,
          0.93f to Color.Transparent,
          1.00f to edgeTint,
        )
    )
  }
}

/**
 * Non-touch-blocking reading spotlight band / line guide overlay with a draggable side handle.
 * Leaves the entire page interactive for taps, page turns, scrolling, and text selection.
 */
@Composable
private fun ReadingRulerOverlay(
  style: ReadingRulerStyle,
  heightDp: Int,
  dimAlpha: Float,
  palette: ReadingPalette,
  onCycleStyle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val strings = LocalAppStrings.current
  var yFraction by rememberSaveable { mutableFloatStateOf(0.42f) }
  var isDragging by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier.testTag("reading_ruler_overlay")) {
    val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
    val bandHeightPx = with(density) { heightDp.dp.toPx() }
    val featherPx = with(density) { 12.dp.toPx() }
    val handleSizePx = with(density) { 48.dp.toPx() }

    val centerYPx = (yFraction * containerHeightPx).coerceIn(bandHeightPx / 2f, containerHeightPx - bandHeightPx / 2f)
    val bandTopPx = (centerYPx - bandHeightPx / 2f).coerceAtLeast(0f)
    val bandBottomPx = (centerYPx + bandHeightPx / 2f).coerceAtMost(containerHeightPx)
    val handleOffsetYPx = (centerYPx - handleSizePx / 2f).roundToInt()

    val maskColor =
      if (palette.isDark) Color.Black.copy(alpha = dimAlpha)
      else Color(0xFF1A1610).copy(alpha = dimAlpha * 0.78f)
    val tintBandColor = palette.accent.copy(alpha = (dimAlpha * 0.34f).coerceIn(0.08f, 0.28f))
    val guideBorderColor = palette.accent.copy(alpha = if (isDragging) 0.9f else 0.55f)

    Canvas(modifier = Modifier.fillMaxSize()) {
      when (style) {
        ReadingRulerStyle.SPOTLIGHT_BAND -> {
          val topMaskEnd = (bandTopPx - featherPx).coerceAtLeast(0f)
          if (topMaskEnd > 0f) {
            drawRect(color = maskColor, topLeft = Offset.Zero, size = Size(size.width, topMaskEnd))
          }
          if (bandTopPx > topMaskEnd) {
            drawRect(
              brush = Brush.verticalGradient(
                colors = listOf(maskColor, Color.Transparent),
                startY = topMaskEnd,
                endY = bandTopPx,
              ),
              topLeft = Offset(0f, topMaskEnd),
              size = Size(size.width, bandTopPx - topMaskEnd),
            )
          }

          val bottomMaskStart = (bandBottomPx + featherPx).coerceAtMost(size.height)
          if (bottomMaskStart > bandBottomPx) {
            drawRect(
              brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, maskColor),
                startY = bandBottomPx,
                endY = bottomMaskStart,
              ),
              topLeft = Offset(0f, bandBottomPx),
              size = Size(size.width, bottomMaskStart - bandBottomPx),
            )
          }
          if (bottomMaskStart < size.height) {
            drawRect(
              color = maskColor,
              topLeft = Offset(0f, bottomMaskStart),
              size = Size(size.width, size.height - bottomMaskStart),
            )
          }

          drawLine(
            color = guideBorderColor.copy(alpha = 0.35f),
            start = Offset(0f, bandTopPx),
            end = Offset(size.width, bandTopPx),
            strokeWidth = 1.dp.toPx(),
          )
          drawLine(
            color = guideBorderColor.copy(alpha = 0.45f),
            start = Offset(0f, bandBottomPx),
            end = Offset(size.width, bandBottomPx),
            strokeWidth = 1.2.dp.toPx(),
          )
        }

        ReadingRulerStyle.HIGHLIGHT_STRIP -> {
          drawRect(
            color = tintBandColor,
            topLeft = Offset(0f, bandTopPx),
            size = Size(size.width, (bandBottomPx - bandTopPx).coerceAtLeast(1f)),
          )
          drawLine(
            color = guideBorderColor.copy(alpha = 0.55f),
            start = Offset(0f, bandTopPx),
            end = Offset(size.width, bandTopPx),
            strokeWidth = 1.dp.toPx(),
          )
          drawLine(
            color = guideBorderColor.copy(alpha = 0.70f),
            start = Offset(0f, bandBottomPx),
            end = Offset(size.width, bandBottomPx),
            strokeWidth = 1.5.dp.toPx(),
          )
        }

        ReadingRulerStyle.GUIDE_BAR -> {
          val underlineY = bandBottomPx
          val barHeightPx = 3.dp.toPx()
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(Color.Transparent, palette.accent.copy(alpha = dimAlpha * 0.24f)),
              startY = (underlineY - 22.dp.toPx()).coerceAtLeast(0f),
              endY = underlineY,
            ),
            topLeft = Offset(0f, (underlineY - 22.dp.toPx()).coerceAtLeast(0f)),
            size = Size(size.width, 22.dp.toPx()),
          )
          drawRect(
            color = palette.accent.copy(alpha = (dimAlpha + 0.35f).coerceIn(0.45f, 0.95f)),
            topLeft = Offset(0f, underlineY),
            size = Size(size.width, barHeightPx),
          )
        }
      }
    }

    // Ergonomic edge drag handle (48dp x 48dp touch target, intercepts vertical drag + tap-to-cycle)
    Box(
      contentAlignment = Alignment.CenterEnd,
      modifier =
        Modifier
          .align(Alignment.TopEnd)
          .offset { IntOffset(0, handleOffsetYPx) }
          .size(48.dp)
          .pointerInput(containerHeightPx) {
            detectVerticalDragGestures(
              onDragStart = { isDragging = true },
              onDragEnd = { isDragging = false },
              onDragCancel = { isDragging = false },
              onVerticalDrag = { change, dragAmount ->
                change.consume()
                yFraction = (yFraction + dragAmount / containerHeightPx).coerceIn(0.10f, 0.90f)
              },
            )
          }
          .clickable(onClick = onCycleStyle)
          .testTag("reading_ruler_drag_handle"),
    ) {
      Surface(
        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
        color = if (isDragging) palette.accent else palette.dockSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (isDragging) palette.accent else palette.dockBorder),
        shadowElevation = if (isDragging) 8.dp else 3.dp,
        modifier = Modifier.size(width = 34.dp, height = 48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = strings.readingRulerDragHint,
            tint =
              if (isDragging) {
                if (palette.isDark) Color(0xFF071630) else Color.White
              } else {
                palette.accent
              },
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

