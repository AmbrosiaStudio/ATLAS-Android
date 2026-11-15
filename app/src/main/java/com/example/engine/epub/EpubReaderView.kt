package com.example.engine.epub

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.HighlightEntity
import com.example.model.CustomFontEntry
import com.example.model.EpubChapter
import com.example.model.ReaderFontOption
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.SelectionAnchor
import com.example.model.TextAlignOption
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.ReadingPalette
import com.example.ui.theme.VazirmatnFontFamily
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ActiveBlockSelection(
  val sliceId: String,
  val range: IntRange,
)

/**
 * 100% Native Jetpack Compose EPUB reader engine.
 * Eliminates Chromium WebView completely (zero renderer crashes or screen flashes) while enabling
 * real-time Haze Liquid-Glass refraction, Book Pages pagination, Vertical Scroll, and hold-to-select.
 */
@Composable
fun EpubReaderView(
  chapter: EpubChapter,
  @Suppress("UNUSED_PARAMETER") fontsDir: File?,
  customFonts: List<CustomFontEntry>,
  settings: ReaderSettings,
  palette: ReadingPalette,
  highlights: List<HighlightEntity>,
  @Suppress("UNUSED_PARAMETER") controlsVisible: Boolean,
  @Suppress("UNUSED_PARAMETER") dockExpanded: Boolean,
  initialScrollFraction: Float,
  pendingAnchor: String?,
  hasPreviousChapter: Boolean,
  hasNextChapter: Boolean,
  clearSelectionSignal: Int,
  hazeState: HazeState,
  externalPageStepSignal: Pair<Int, Int> = 0 to 0,
  onScrollFractionChanged: (Float) -> Unit,
  onUnitViewed: (String) -> Unit = {},
  onSelectionChanged: (SelectionAnchor) -> Unit,
  onContentTap: () -> Unit,
  onNavigateChapterDelta: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  val textMeasurer = rememberTextMeasurer()

  val latestScrollCb by rememberUpdatedState(onScrollFractionChanged)
  val latestUnitViewedCb by rememberUpdatedState(onUnitViewed)
  val latestSelectionCb by rememberUpdatedState(onSelectionChanged)
  val latestTapCb by rememberUpdatedState(onContentTap)
  val latestNavDeltaCb by rememberUpdatedState(onNavigateChapterDelta)

  var parsedChapter by remember(chapter.localFilePath) {
    mutableStateOf(ParsedChapterContent(isRtlDocument = false, blocks = emptyList()))
  }
  var activeSelection by remember(chapter.localFilePath) {
    mutableStateOf<ActiveBlockSelection?>(null)
  }
  var internalAnchorTarget by remember(chapter.localFilePath) { mutableStateOf<String?>(null) }
  var internalAnchorNonce by remember { mutableIntStateOf(0) }

  LaunchedEffect(chapter.localFilePath) {
    activeSelection = null
    internalAnchorTarget = null
    val loaded =
      withContext(Dispatchers.IO) {
        val raw = runCatching { File(chapter.localFilePath).readText() }.getOrDefault("")
        EpubHtmlBlockParser.parse(raw, chapter.localFilePath)
      }
    parsedChapter = loaded
  }

  LaunchedEffect(clearSelectionSignal) {
    if (clearSelectionSignal > 0) {
      activeSelection = null
    }
  }

  val activeFontFamily =
    remember(settings.fontOption, settings.customFontId, customFonts) {
      resolveReaderComposeFont(settings, customFonts)
    }

  val baseBodyStyle =
    remember(
      activeFontFamily,
      settings.fontSizeSp,
      settings.lineHeightMultiplier,
      settings.letterSpacingEm,
      settings.fontWeightValue,
      settings.textAlign,
      palette.text,
    ) {
      TextStyle(
        fontFamily = activeFontFamily,
        fontSize = settings.fontSizeSp.sp,
        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
        letterSpacing = (settings.fontSizeSp * settings.letterSpacingEm).sp,
        fontWeight = FontWeight(settings.fontWeightValue.coerceIn(100, 900)),
        textAlign = if (settings.textAlign == TextAlignOption.JUSTIFY) TextAlign.Justify else TextAlign.Start,
        color = palette.text,
      )
    }

  val statusTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val paragraphSpacingDp = (settings.fontSizeSp * settings.paragraphSpacingEm).dp

  fun clearActiveSelectionAndReport() {
    if (activeSelection != null) {
      activeSelection = null
      latestSelectionCb(SelectionAnchor())
    }
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .hazeSource(state = hazeState)
        .background(palette.background),
  ) {
    val containerWidthPx = with(density) { maxWidth.toPx() }.roundToInt()
    val containerHeightPx = with(density) { maxHeight.toPx() }.roundToInt()

    val outerMarginH = settings.horizontalMarginDp.dp
    val borderInsetH = if (settings.showPageBorder) 14.dp else 0.dp
    val borderInsetV = if (settings.showPageBorder) 12.dp else 0.dp

    if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
      val topSafeReserveDp = statusTopDp + settings.verticalMarginDp.dp.coerceAtLeast(16.dp) + 56.dp
      val bottomSafeReserveDp = navBottomDp + settings.verticalMarginDp.dp.coerceAtLeast(16.dp) + 74.dp

      val availableTextWidthPx =
        with(density) {
          (maxWidth - (outerMarginH + borderInsetH) * 2).toPx().roundToInt().coerceAtLeast(120)
        }
      val availableTextHeightPx =
        with(density) {
          (maxHeight - topSafeReserveDp - bottomSafeReserveDp - borderInsetV * 2)
            .toPx()
            .roundToInt()
            .coerceAtLeast(160)
        }

      val pages =
        remember(
          parsedChapter.blocks,
          availableTextWidthPx,
          availableTextHeightPx,
          baseBodyStyle,
          settings.paragraphSpacingEm,
          density.density,
        ) {
          val pSpacingPx = with(density) { paragraphSpacingDp.toPx() }
          val hSpacingPx = with(density) { (paragraphSpacingDp + 6.dp).toPx() }
          val bqExtraWidth = with(density) { 20.dp.toPx() }.roundToInt()
          val bqPadVPx = with(density) { 8.dp.toPx() }.roundToInt()
          val bulletW = with(density) { 24.dp.toPx() }.roundToInt()
          val imgMaxH = with(density) { 280.dp.toPx() }.roundToInt()
          val divH = with(density) { 24.dp.toPx() }.roundToInt()

          EpubPaginator.paginate(
            blocks = parsedChapter.blocks,
            textMeasurer = textMeasurer,
            contentWidthPx = availableTextWidthPx,
            contentHeightPx = availableTextHeightPx,
            baseBodyStyle = baseBodyStyle,
            paragraphSpacingPx = pSpacingPx,
            headingSpacingPx = hSpacingPx,
            blockquoteExtraWidthPx = bqExtraWidth,
            blockquotePaddingVPx = bqPadVPx,
            listItemBulletWidthPx = bulletW,
            imageMaxHeightPx = imgMaxH,
            dividerHeightPx = divH,
            densityScale = density.density,
          )
        }

      val pageCount = pages.size.coerceAtLeast(1)
      val initialPageIndex =
        remember(chapter.localFilePath, pendingAnchor, pageCount) {
          val anchorPage =
            pendingAnchor?.let { anc ->
              pages.indexOfFirst { anc in it.anchors }.takeIf { it >= 0 }
            }
          anchorPage
            ?: (initialScrollFraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
        }

      val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { pageCount })

      // Reposition cleanly if pagination count changes or anchor requested
      LaunchedEffect(pendingAnchor, internalAnchorTarget, internalAnchorNonce, pageCount) {
        val activeAnc = internalAnchorTarget ?: pendingAnchor
        if (!activeAnc.isNullOrBlank()) {
          val idx = pages.indexOfFirst { activeAnc in it.anchors }
          if (idx >= 0 && idx != pagerState.currentPage) {
            pagerState.scrollToPage(idx)
          }
          internalAnchorTarget = null
        } else {
          val target = (initialScrollFraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
          if (pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage(target)
          }
        }
      }

      LaunchedEffect(pagerState.currentPage, pageCount, chapter.localFilePath) {
        val frac =
          if (pageCount <= 1) 0f
          else pagerState.currentPage.toFloat() / (pageCount - 1).toFloat()
        latestScrollCb(frac.coerceIn(0f, 1f))
        if (parsedChapter.blocks.isNotEmpty()) {
          latestUnitViewedCb("epub_${chapter.index}_p${pagerState.currentPage}")
        }
      }

      LaunchedEffect(externalPageStepSignal, chapter.localFilePath, pageCount) {
        val (delta, nonce) = externalPageStepSignal
        if (nonce > 0 && delta != 0) {
          if (delta > 0) {
            if (pagerState.currentPage < pageCount - 1) {
              if (settings.batterySaverModeEnabled) {
                pagerState.scrollToPage(pagerState.currentPage + 1)
              } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
              }
            } else if (hasNextChapter) {
              latestNavDeltaCb(1)
            }
          } else {
            if (pagerState.currentPage > 0) {
              if (settings.batterySaverModeEnabled) {
                pagerState.scrollToPage(pagerState.currentPage - 1)
              } else {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
              }
            } else if (hasPreviousChapter) {
              latestNavDeltaCb(-1)
            }
          }
        }
      }

      val pageLayoutDirection =
        if (parsedChapter.isRtlDocument) LayoutDirection.Rtl else LayoutDirection.Ltr

      val handlePageTap: (Offset) -> Unit = { rootTapOffset ->
        if (activeSelection != null) {
          clearActiveSelectionAndReport()
        } else {
          val ratioX = rootTapOffset.x / containerWidthPx.toFloat().coerceAtLeast(1f)
          val isForwardTap =
            if (parsedChapter.isRtlDocument) ratioX < 0.22f else ratioX > 0.78f
          val isBackwardTap =
            if (parsedChapter.isRtlDocument) ratioX > 0.78f else ratioX < 0.22f

          when {
            isBackwardTap -> {
              if (pagerState.currentPage > 0) {
                scope.launch {
                  if (settings.batterySaverModeEnabled) {
                    pagerState.scrollToPage(pagerState.currentPage - 1)
                  } else {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                  }
                }
              } else if (hasPreviousChapter) {
                latestNavDeltaCb(-1)
              } else {
                latestTapCb()
              }
            }
            isForwardTap -> {
              if (pagerState.currentPage < pageCount - 1) {
                scope.launch {
                  if (settings.batterySaverModeEnabled) {
                    pagerState.scrollToPage(pagerState.currentPage + 1)
                  } else {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                  }
                }
              } else if (hasNextChapter) {
                latestNavDeltaCb(1)
              } else {
                latestTapCb()
              }
            }
            else -> latestTapCb()
          }
        }
      }

      CompositionLocalProvider(LocalLayoutDirection provides pageLayoutDirection) {
        HorizontalPager(
          state = pagerState,
          beyondViewportPageCount = 1,
          modifier = Modifier.fillMaxSize(),
        ) { pageIdx ->
          val page = pages.getOrNull(pageIdx)
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .pointerInput(chapter.localFilePath, pageIdx, parsedChapter.isRtlDocument, activeSelection != null) {
                  detectTapGestures(onTap = { tapOffset -> handlePageTap(tapOffset) })
                }
                .padding(
                  start = outerMarginH,
                  end = outerMarginH,
                  top = topSafeReserveDp,
                  bottom = bottomSafeReserveDp,
                ),
          ) {
            val frameBorderColor =
              palette.text.copy(alpha = if (palette.isDark) 0.16f else 0.14f)
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .then(
                    if (settings.showPageBorder) {
                      Modifier
                        .border(BorderStroke(1.dp, frameBorderColor), RoundedCornerShape(14.dp))
                        .padding(horizontal = borderInsetH, vertical = borderInsetV)
                    } else {
                      Modifier
                    }
                  ),
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
              ) {
                page?.items?.forEachIndexed { idx, item ->
                  val isLastItem = idx == page.items.lastIndex
                  val itemBottomPad =
                    when (item.block) {
                      is EpubContentBlock.Heading -> paragraphSpacingDp + 6.dp
                      is EpubContentBlock.ListItem -> (paragraphSpacingDp * 0.65f) + 2.dp
                      else -> paragraphSpacingDp
                    }
                  EpubBlockView(
                    sliceId = item.sliceId,
                    block = item.block,
                    baseBodyStyle = baseBodyStyle,
                    palette = palette,
                    highlights = highlights,
                    activeSelection = activeSelection,
                    pageWidthPx = containerWidthPx,
                    onTapBlock = { rootX -> handlePageTap(Offset(rootX, containerHeightPx / 2f)) },
                    onAnchorClick = { anc ->
                      internalAnchorTarget = anc
                      internalAnchorNonce++
                    },
                    onUpdateSelection = { sel, anchor ->
                      activeSelection = sel
                      latestSelectionCb(anchor)
                    },
                    onClearSelection = ::clearActiveSelectionAndReport,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isLastItem) 0.dp else itemBottomPad),
                  )
                }

                // Boundary chapter controls on first/last page
                if (pageIdx == pageCount - 1 && (hasPreviousChapter || hasNextChapter)) {
                  Spacer(modifier = Modifier.weight(1f, fill = true))
                  ChapterBoundaryControls(
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    palette = palette,
                    prevLabel = strings.previousChapter,
                    nextLabel = strings.nextChapter,
                    onNavigateDelta = { delta -> latestNavDeltaCb(delta) },
                    modifier = Modifier.padding(top = 8.dp),
                  )
                }
              }

              // Quiet footer page badge
              Text(
                text = "${pageIdx + 1} / $pageCount",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText.copy(alpha = 0.72f),
                modifier = Alignment.BottomCenter.let { align ->
                  Modifier.align(align).padding(bottom = 2.dp)
                },
              )
            }
          }
        }
      }
    } else {
      // SCROLL MODE: Vertical Scroll with LazyColumn
      val blocks = parsedChapter.blocks
      val listState = rememberLazyListState()

      // Restore initial scroll fraction or jump to pending anchor
      LaunchedEffect(chapter.localFilePath, blocks.size) {
        if (blocks.isNotEmpty() && pendingAnchor == null && initialScrollFraction > 0.01f) {
          val targetIdx = (initialScrollFraction * (blocks.size - 1)).roundToInt().coerceIn(0, blocks.lastIndex)
          listState.scrollToItem(targetIdx)
        }
      }

      LaunchedEffect(pendingAnchor, internalAnchorTarget, internalAnchorNonce, blocks.size) {
        val anc = internalAnchorTarget ?: pendingAnchor
        if (!anc.isNullOrBlank() && blocks.isNotEmpty()) {
          val idx = blocks.indexOfFirst { anc in it.anchors }
          if (idx >= 0) {
            if (settings.batterySaverModeEnabled) {
              listState.scrollToItem(idx)
            } else {
              listState.animateScrollToItem(idx)
            }
          }
          internalAnchorTarget = null
        }
      }

      LaunchedEffect(externalPageStepSignal, chapter.localFilePath) {
        val (delta, nonce) = externalPageStepSignal
        if (nonce > 0 && delta != 0) {
          val stepPx = containerHeightPx * 0.78f
          if (delta > 0) {
            if (!listState.canScrollForward && hasNextChapter) {
              latestNavDeltaCb(1)
            } else if (settings.batterySaverModeEnabled) {
              listState.scrollBy(stepPx)
            } else {
              listState.animateScrollBy(stepPx)
            }
          } else {
            if (!listState.canScrollBackward && hasPreviousChapter) {
              latestNavDeltaCb(-1)
            } else if (settings.batterySaverModeEnabled) {
              listState.scrollBy(-stepPx)
            } else {
              listState.animateScrollBy(-stepPx)
            }
          }
        }
      }

      val scrollFraction by remember(blocks.size) {
        derivedStateOf {
          if (blocks.size <= 1) 0f
          else (listState.firstVisibleItemIndex.toFloat() / (blocks.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
      }
      LaunchedEffect(scrollFraction, chapter.localFilePath) {
        if (blocks.isNotEmpty()) {
          latestScrollCb(scrollFraction)
          val virtualStep = (scrollFraction * 8f).roundToInt()
          latestUnitViewedCb("epub_${chapter.index}_s$virtualStep")
        }
      }

      val scrollLayoutDirection =
        if (parsedChapter.isRtlDocument) LayoutDirection.Rtl else LayoutDirection.Ltr
      val topContentPad = statusTopDp + settings.verticalMarginDp.dp.coerceAtLeast(16.dp) + 64.dp
      val bottomContentPad = navBottomDp + settings.verticalMarginDp.dp.coerceAtLeast(16.dp) + 96.dp
      val frameBorderColor = palette.text.copy(alpha = if (palette.isDark) 0.16f else 0.14f)

      CompositionLocalProvider(LocalLayoutDirection provides scrollLayoutDirection) {
        LazyColumn(
          state = listState,
          contentPadding =
            PaddingValues(
              start = outerMarginH,
              end = outerMarginH,
              top = topContentPad,
              bottom = bottomContentPad,
            ),
          modifier =
            Modifier
              .fillMaxSize()
              .pointerInput(chapter.localFilePath, activeSelection != null) {
                detectTapGestures(
                  onTap = {
                    if (activeSelection != null) {
                      clearActiveSelectionAndReport()
                    } else {
                      latestTapCb()
                    }
                  }
                )
              },
        ) {
          itemsIndexed(
            items = blocks,
            key = { idx, blk -> "${chapter.index}_${blk.id}_$idx" },
          ) { _, block ->
            val bottomGap =
              when (block) {
                is EpubContentBlock.Heading -> paragraphSpacingDp + 6.dp
                is EpubContentBlock.ListItem -> (paragraphSpacingDp * 0.65f) + 2.dp
                else -> paragraphSpacingDp
              }
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .then(
                    if (settings.showPageBorder) {
                      Modifier
                        .border(BorderStroke(0.8.dp, frameBorderColor), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                    } else {
                      Modifier
                    }
                  )
                  .padding(bottom = bottomGap),
            ) {
              EpubBlockView(
                sliceId = block.id,
                block = block,
                baseBodyStyle = baseBodyStyle,
                palette = palette,
                highlights = highlights,
                activeSelection = activeSelection,
                pageWidthPx = containerWidthPx,
                onTapBlock = {
                  if (activeSelection != null) {
                    clearActiveSelectionAndReport()
                  } else {
                    latestTapCb()
                  }
                },
                onAnchorClick = { anc ->
                  internalAnchorTarget = anc
                  internalAnchorNonce++
                },
                onUpdateSelection = { sel, anchor ->
                  activeSelection = sel
                  latestSelectionCb(anchor)
                },
                onClearSelection = ::clearActiveSelectionAndReport,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (hasPreviousChapter || hasNextChapter) {
            item(key = "chapter_nav_footer_${chapter.index}") {
              ChapterBoundaryControls(
                hasPreviousChapter = hasPreviousChapter,
                hasNextChapter = hasNextChapter,
                palette = palette,
                prevLabel = strings.previousChapter,
                nextLabel = strings.nextChapter,
                onNavigateDelta = { delta -> latestNavDeltaCb(delta) },
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EpubBlockView(
  sliceId: String,
  block: EpubContentBlock,
  baseBodyStyle: TextStyle,
  palette: ReadingPalette,
  highlights: List<HighlightEntity>,
  activeSelection: ActiveBlockSelection?,
  pageWidthPx: Int,
  onTapBlock: (Float) -> Unit,
  onAnchorClick: (String) -> Unit,
  onUpdateSelection: (ActiveBlockSelection, SelectionAnchor) -> Unit,
  onClearSelection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (block) {
    is EpubContentBlock.Divider -> {
      HorizontalDivider(
        color = palette.text.copy(alpha = 0.16f),
        thickness = 1.dp,
        modifier = modifier.padding(vertical = 8.dp),
      )
    }

    is EpubContentBlock.Image -> {
      AsyncImage(
        model = File(block.localFilePath),
        contentDescription = block.altText,
        contentScale = ContentScale.Fit,
        modifier =
          modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .clip(RoundedCornerShape(12.dp)),
      )
    }

    is EpubContentBlock.Heading -> {
      val headingStyle = EpubPaginator.headingStyleForLevel(baseBodyStyle, block.level, block.isRtl)
      SelectableEpubText(
        sliceId = sliceId,
        rawText = block.text,
        style = headingStyle,
        palette = palette,
        highlights = highlights,
        activeSelection = activeSelection,
        fallbackWidthPx = pageWidthPx,
        onTapRootX = onTapBlock,
        onAnchorClick = onAnchorClick,
        onUpdateSelection = onUpdateSelection,
        onClearSelection = onClearSelection,
        modifier = modifier,
      )
    }

    is EpubContentBlock.Paragraph -> {
      val pStyle =
        baseBodyStyle.copy(
          textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
        )
      SelectableEpubText(
        sliceId = sliceId,
        rawText = block.text,
        style = pStyle,
        palette = palette,
        highlights = highlights,
        activeSelection = activeSelection,
        fallbackWidthPx = pageWidthPx,
        onTapRootX = onTapBlock,
        onAnchorClick = onAnchorClick,
        onUpdateSelection = onUpdateSelection,
        onClearSelection = onClearSelection,
        modifier = modifier,
      )
    }

    is EpubContentBlock.Blockquote -> {
      val qStyle =
        baseBodyStyle.copy(
          color = palette.secondaryText,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
          textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
        )
      Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(
          modifier =
            Modifier
              .width(3.dp)
              .fillMaxHeight()
              .clip(RoundedCornerShape(2.dp))
              .background(palette.accent.copy(alpha = 0.72f)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        SelectableEpubText(
          sliceId = sliceId,
          rawText = block.text,
          style = qStyle,
          palette = palette,
          highlights = highlights,
          activeSelection = activeSelection,
          fallbackWidthPx = pageWidthPx,
          onTapRootX = onTapBlock,
          onAnchorClick = onAnchorClick,
          onUpdateSelection = onUpdateSelection,
          onClearSelection = onClearSelection,
          modifier = Modifier.weight(1f).padding(vertical = 2.dp),
        )
      }
    }

    is EpubContentBlock.ListItem -> {
      val lStyle =
        baseBodyStyle.copy(
          textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
        )
      Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
          text = block.bullet,
          style = lStyle.copy(fontWeight = FontWeight.SemiBold, color = palette.accent),
          modifier = Modifier.width(24.dp),
        )
        SelectableEpubText(
          sliceId = sliceId,
          rawText = block.text,
          style = lStyle,
          palette = palette,
          highlights = highlights,
          activeSelection = activeSelection,
          fallbackWidthPx = pageWidthPx,
          onTapRootX = onTapBlock,
          onAnchorClick = onAnchorClick,
          onUpdateSelection = onUpdateSelection,
          onClearSelection = onClearSelection,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SelectableEpubText(
  sliceId: String,
  rawText: AnnotatedString,
  style: TextStyle,
  palette: ReadingPalette,
  highlights: List<HighlightEntity>,
  activeSelection: ActiveBlockSelection?,
  fallbackWidthPx: Int,
  onTapRootX: (Float) -> Unit,
  onAnchorClick: (String) -> Unit,
  onUpdateSelection: (ActiveBlockSelection, SelectionAnchor) -> Unit,
  onClearSelection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val currentRange = activeSelection?.takeIf { it.sliceId == sliceId }?.range

  val decorated =
    remember(rawText, highlights, currentRange, palette.accent) {
      EpubHtmlBlockParser.decorateAnnotatedText(
        base = rawText,
        highlights = highlights,
        activeSelectionRange = currentRange,
        selectionHighlightColor = palette.accent,
      )
    }

  var layoutResult by remember(sliceId, rawText.text) { mutableStateOf<TextLayoutResult?>(null) }
  var layoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var dragAnchorRange by remember { mutableStateOf<IntRange?>(null) }
  var liveDragRange by remember { mutableStateOf<IntRange?>(null) }

  val latestSelectionCb by rememberUpdatedState(onUpdateSelection)
  val latestClearCb by rememberUpdatedState(onClearSelection)
  val latestTapRootX by rememberUpdatedState(onTapRootX)
  val latestAnchorClick by rememberUpdatedState(onAnchorClick)

  Text(
    text = decorated,
    style = style,
    onTextLayout = { layoutResult = it },
    modifier =
      modifier
        .onGloballyPositioned { layoutCoords = it }
        .pointerInput(sliceId, decorated.text) {
          detectTapGestures(
            onTap = { localPos ->
              val layout = layoutResult
              val coords = layoutCoords
              if (currentRange != null || activeSelection != null) {
                latestClearCb()
                return@detectTapGestures
              }
              if (layout != null && decorated.isNotEmpty()) {
                val offsetIdx = layout.getOffsetForPosition(localPos).coerceIn(0, decorated.length - 1)
                val link =
                  decorated
                    .getStringAnnotations(tag = "ANCHOR", start = offsetIdx, end = offsetIdx + 1)
                    .firstOrNull()
                if (link != null) {
                  latestAnchorClick(link.item)
                  return@detectTapGestures
                }
              }
              val rootX = coords?.localToRoot(localPos)?.x ?: (fallbackWidthPx * 0.5f)
              latestTapRootX(rootX)
            }
          )
        }
        .pointerInput(sliceId, decorated.text, palette.accent) {
          detectDragGesturesAfterLongPress(
            onDragStart = { startOffset ->
              val layout = layoutResult ?: return@detectDragGesturesAfterLongPress
              val coords = layoutCoords ?: return@detectDragGesturesAfterLongPress
              val textStr = decorated.text
              if (textStr.isBlank()) return@detectDragGesturesAfterLongPress

              val charIdx = layout.getOffsetForPosition(startOffset).coerceIn(0, textStr.lastIndex)
              val initialRange = findWordRangeAt(textStr, charIdx)
              dragAnchorRange = initialRange
              liveDragRange = initialRange
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)

              val selAnchor =
                buildSelectionAnchor(
                  text = textStr,
                  range = initialRange,
                  layout = layout,
                  coords = coords,
                  density = density,
                )
              latestSelectionCb(ActiveBlockSelection(sliceId, initialRange), selAnchor)
            },
            onDrag = { change, _ ->
              change.consume()
              val layout = layoutResult ?: return@detectDragGesturesAfterLongPress
              val coords = layoutCoords ?: return@detectDragGesturesAfterLongPress
              val anchor = dragAnchorRange ?: return@detectDragGesturesAfterLongPress
              val textStr = decorated.text
              if (textStr.isBlank()) return@detectDragGesturesAfterLongPress

              val charIdx = layout.getOffsetForPosition(change.position).coerceIn(0, textStr.lastIndex)
              val curWord = findWordRangeAt(textStr, charIdx)
              val combined = min(anchor.first, curWord.first)..max(anchor.last, curWord.last)
              if (combined != liveDragRange) {
                liveDragRange = combined
                val selAnchor =
                  buildSelectionAnchor(
                    text = textStr,
                    range = combined,
                    layout = layout,
                    coords = coords,
                    density = density,
                  )
                latestSelectionCb(ActiveBlockSelection(sliceId, combined), selAnchor)
              }
            },
            onDragEnd = {
              val layout = layoutResult
              val coords = layoutCoords
              val finalRange = liveDragRange
              val textStr = decorated.text
              if (layout != null && coords != null && finalRange != null && textStr.isNotBlank()) {
                val selAnchor =
                  buildSelectionAnchor(
                    text = textStr,
                    range = finalRange,
                    layout = layout,
                    coords = coords,
                    density = density,
                  )
                latestSelectionCb(ActiveBlockSelection(sliceId, finalRange), selAnchor)
              }
              dragAnchorRange = null
              liveDragRange = null
            },
            onDragCancel = {
              dragAnchorRange = null
              liveDragRange = null
            },
          )
        },
  )
}

@Composable
private fun ChapterBoundaryControls(
  hasPreviousChapter: Boolean,
  hasNextChapter: Boolean,
  palette: ReadingPalette,
  prevLabel: String,
  nextLabel: String,
  onNavigateDelta: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (hasPreviousChapter) {
      OutlinedButton(
        onClick = { onNavigateDelta(-1) },
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(prevLabel, style = MaterialTheme.typography.labelLarge)
      }
    }
    if (hasNextChapter) {
      val onAccent = if (palette.isDark) Color(0xFF0A2114) else Color.White
      Button(
        onClick = { onNavigateDelta(1) },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = onAccent,
          ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(nextLabel, style = MaterialTheme.typography.labelLarge, color = onAccent)
      }
    }
  }
}

private fun resolveReaderComposeFont(
  settings: ReaderSettings,
  customFonts: List<CustomFontEntry>,
): FontFamily {
  val custom = settings.customFontId?.let { id -> customFonts.firstOrNull { it.id == id } }
  if (custom != null) {
    val file = File(custom.filePath)
    if (file.exists()) {
      val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
      if (tf != null) return FontFamily(tf)
    }
  }
  return when (settings.fontOption) {
    ReaderFontOption.LITERATA -> LiterataFontFamily
    ReaderFontOption.LORA -> LoraFontFamily
    ReaderFontOption.NEWSREADER -> NewsreaderFontFamily
    ReaderFontOption.VAZIRMATN -> VazirmatnFontFamily
    ReaderFontOption.NOTO_NASKH -> NotoNaskhFontFamily
    ReaderFontOption.PLUS_JAKARTA -> PlusJakartaSansFamily
    ReaderFontOption.JETBRAINS_MONO -> JetBrainsMonoFontFamily
  }
}

private fun isWordChar(ch: Char): Boolean {
  return ch.isLetterOrDigit() || ch == '\'' || ch == '’' || ch == '\u200C' || ch == '-'
}

private fun findWordRangeAt(text: String, charIndex: Int): IntRange {
  if (text.isEmpty()) return 0..0
  var center = charIndex.coerceIn(0, text.lastIndex)
  if (!isWordChar(text[center])) {
    // Search nearby for closest word character
    var offset = 1
    while (offset <= 8) {
      val right = center + offset
      val left = center - offset
      if (right < text.length && isWordChar(text[right])) {
        center = right
        break
      }
      if (left >= 0 && isWordChar(text[left])) {
        center = left
        break
      }
      offset++
    }
  }
  if (!isWordChar(text[center])) {
    val start = (center - 6).coerceAtLeast(0)
    val end = (center + 6).coerceAtMost(text.length)
    return start..end
  }
  var start = center
  while (start > 0 && isWordChar(text[start - 1])) start--
  var end = center + 1
  while (end < text.length && isWordChar(text[end])) end++
  return start..end
}

private fun buildSelectionAnchor(
  text: String,
  range: IntRange,
  layout: TextLayoutResult,
  coords: LayoutCoordinates,
  density: Density,
): SelectionAnchor {
  val safeStart = range.first.coerceIn(0, text.length)
  val safeEnd = range.last.coerceIn(safeStart, text.length)
  val selected = text.substring(safeStart, safeEnd).trim()
  if (selected.isBlank() || safeEnd <= safeStart) return SelectionAnchor()

  val startLine = layout.getLineForOffset(safeStart)
  val endLine = layout.getLineForOffset((safeEnd - 1).coerceAtLeast(safeStart))

  val startBox = layout.getBoundingBox(safeStart)
  val endBox = layout.getBoundingBox((safeEnd - 1).coerceAtLeast(safeStart))

  val localLeft: Float
  val localRight: Float
  if (startLine == endLine) {
    localLeft = min(startBox.left, endBox.left)
    localRight = max(startBox.right, endBox.right)
  } else {
    var minX = Float.MAX_VALUE
    var maxX = 0f
    for (line in startLine..endLine) {
      minX = min(minX, layout.getLineLeft(line))
      maxX = max(maxX, layout.getLineRight(line))
    }
    localLeft = if (minX == Float.MAX_VALUE) 0f else minX
    localRight = if (maxX <= localLeft) layout.size.width.toFloat() else maxX
  }
  val localTop = layout.getLineTop(startLine)
  val localBottom = layout.getLineBottom(endLine)

  val topLeftRoot = coords.localToRoot(Offset(localLeft, localTop))
  val bottomRightRoot = coords.localToRoot(Offset(localRight, localBottom))
  val pxToDp = density.density.coerceAtLeast(1f)

  return SelectionAnchor(
    text = selected,
    leftDp = topLeftRoot.x / pxToDp,
    topDp = topLeftRoot.y / pxToDp,
    rightDp = bottomRightRoot.x / pxToDp,
    bottomDp = bottomRightRoot.y / pxToDp,
  )
}
