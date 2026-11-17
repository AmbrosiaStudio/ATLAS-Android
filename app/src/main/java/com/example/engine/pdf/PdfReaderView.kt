package com.example.engine.pdf

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TapZoneAction
import com.example.ui.reader.leftEdgeBrightnessSwipe
import com.example.ui.reader.resolveTapZoneAction
import com.example.ui.theme.ReadingPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun PdfReaderView(
  session: PdfSession,
  currentPage: Int,
  jumpToPageSignal: Pair<Int, Int>,
  settings: ReaderSettings,
  palette: ReadingPalette,
  hazeState: HazeState,
  externalPageStepSignal: Pair<Int, Int> = 0 to 0,
  onPageChanged: (Int) -> Unit,
  onUnitViewed: (String) -> Unit = {},
  onContentTap: () -> Unit,
  onQuickBookmark: () -> Unit = {},
  onPreviewBrightness: (Float?) -> Unit = {},
  onCommitBrightness: (Float) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val safeCount = session.pageCount.coerceAtLeast(1)
  val initialPage = currentPage.coerceIn(0, safeCount - 1)
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
  val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { safeCount })
  val scope = rememberCoroutineScope()

  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }
  var lastHandledStepNonce by remember { mutableIntStateOf(externalPageStepSignal.second) }

  // Sync Scroll vs Paged positions smoothly when switching layout mode
  LaunchedEffect(settings.readingLayoutMode) {
    scale = 1f
    offsetX = 0f
    offsetY = 0f
    if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
      if (pagerState.currentPage != currentPage && currentPage in 0 until safeCount) {
        pagerState.scrollToPage(currentPage)
      }
    } else {
      if (listState.firstVisibleItemIndex != currentPage && currentPage in 0 until safeCount) {
        listState.scrollToItem(currentPage)
      }
    }
  }

  // Report visible page changes cleanly for active layout mode
  val firstVisiblePage by remember {
    derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, safeCount - 1) }
  }
  LaunchedEffect(firstVisiblePage, settings.readingLayoutMode) {
    if (settings.readingLayoutMode == ReadingLayoutMode.SCROLL) {
      onPageChanged(firstVisiblePage)
      onUnitViewed("pdf_p$firstVisiblePage")
    }
  }
  LaunchedEffect(pagerState.currentPage, settings.readingLayoutMode) {
    if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
      val pageIdx = pagerState.currentPage.coerceIn(0, safeCount - 1)
      onPageChanged(pageIdx)
      onUnitViewed("pdf_p$pageIdx")
    }
  }

  // Handle explicit jumps (TOC / Bookmark / Search / Scrubber)
  LaunchedEffect(jumpToPageSignal, settings.readingLayoutMode) {
    val (targetPage, nonce) = jumpToPageSignal
    if (nonce > 0 && targetPage in 0 until safeCount) {
      if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
        pagerState.scrollToPage(targetPage)
      } else {
        listState.scrollToItem(targetPage)
      }
    }
  }

  // Handle Volume-key step turns (next/prev PDF page)
  LaunchedEffect(externalPageStepSignal) {
    val (delta, nonce) = externalPageStepSignal
    if (nonce > lastHandledStepNonce && delta != 0) {
      lastHandledStepNonce = nonce
      if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
        val target = (pagerState.currentPage + delta).coerceIn(0, safeCount - 1)
        if (target != pagerState.currentPage) {
          if (settings.batterySaverModeEnabled) {
            pagerState.scrollToPage(target)
          } else {
            pagerState.animateScrollToPage(target)
          }
        }
      } else {
        val target = (firstVisiblePage + delta).coerceIn(0, safeCount - 1)
        if (target != firstVisiblePage) {
          if (settings.batterySaverModeEnabled) {
            listState.scrollToItem(target)
          } else {
            listState.animateScrollToItem(target)
          }
        }
      }
    }
  }

  val colorFilter =
    remember(settings.themeMode, settings.pdfThemeFilterEnabled) {
      if (!settings.pdfThemeFilterEnabled) {
        null
      } else {
        when (settings.themeMode) {
          ReadingThemeMode.PAPER -> null
          ReadingThemeMode.SEPIA ->
            ColorFilter.colorMatrix(
              ColorMatrix(
                floatArrayOf(
                  0.96f, 0f, 0f, 0f, 0f,
                  0f, 0.92f, 0f, 0f, 0f,
                  0f, 0f, 0.82f, 0f, 0f,
                  0f, 0f, 0f, 1f, 0f,
                )
              )
            )
          ReadingThemeMode.SAGE_MIST ->
            ColorFilter.colorMatrix(
              ColorMatrix(
                floatArrayOf(
                  0.92f, 0f, 0f, 0f, 0f,
                  0f, 0.95f, 0f, 0f, 0f,
                  0f, 0f, 0.91f, 0f, 0f,
                  0f, 0f, 0f, 1f, 0f,
                )
              )
            )
          ReadingThemeMode.DARK ->
            ColorFilter.colorMatrix(
              ColorMatrix(
                floatArrayOf(
                  -0.86f, 0f, 0f, 0f, 232f,
                  0f, -0.84f, 0f, 0f, 234f,
                  0f, 0f, -0.86f, 0f, 228f,
                  0f, 0f, 0f, 1f, 0f,
                )
              )
            )
          ReadingThemeMode.AMOLED ->
            ColorFilter.colorMatrix(
              ColorMatrix(
                floatArrayOf(
                  -0.92f, 0f, 0f, 0f, 224f,
                  0f, -0.92f, 0f, 0f, 224f,
                  0f, 0f, -0.92f, 0f, 218f,
                  0f, 0f, 0f, 1f, 0f,
                )
              )
            )
          ReadingThemeMode.NIGHT_BW ->
            ColorFilter.colorMatrix(
              ColorMatrix(
                floatArrayOf(
                  -0.299f, -0.587f, -0.114f, 0f, 255f,
                  -0.299f, -0.587f, -0.114f, 0f, 255f,
                  -0.299f, -0.587f, -0.114f, 0f, 255f,
                  0f, 0f, 0f, 1f, 0f,
                )
              )
            )
        }
      }
    }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .hazeSource(state = hazeState)
        .background(palette.background)
        .leftEdgeBrightnessSwipe(
          enabled = settings.edgeBrightnessSwipeEnabled && scale <= 1.02f,
          initialBrightness = settings.brightnessOverride,
          onPreviewBrightness = onPreviewBrightness,
          onCommitBrightness = onCommitBrightness,
        )
        .pointerInput(
          settings.readingLayoutMode,
          safeCount,
          settings.leftTapAction,
          settings.centerTapAction,
          settings.rightTapAction,
        ) {
          detectTapGestures(
            onTap = { tapOffset ->
              if (settings.readingLayoutMode == ReadingLayoutMode.PAGED && scale <= 1.02f) {
                when (resolveTapZoneAction(tapOffset, size.width, settings)) {
                  TapZoneAction.PREV_PAGE -> {
                    if (pagerState.currentPage > 0) {
                      scope.launch {
                        if (settings.batterySaverModeEnabled) pagerState.scrollToPage(pagerState.currentPage - 1)
                        else pagerState.animateScrollToPage(pagerState.currentPage - 1)
                      }
                    } else {
                      onContentTap()
                    }
                  }
                  TapZoneAction.NEXT_PAGE -> {
                    if (pagerState.currentPage < safeCount - 1) {
                      scope.launch {
                        if (settings.batterySaverModeEnabled) pagerState.scrollToPage(pagerState.currentPage + 1)
                        else pagerState.animateScrollToPage(pagerState.currentPage + 1)
                      }
                    } else {
                      onContentTap()
                    }
                  }
                  TapZoneAction.TOGGLE_CONTROLS -> onContentTap()
                  TapZoneAction.QUICK_BOOKMARK -> onQuickBookmark()
                  TapZoneAction.NONE -> Unit
                }
              } else {
                onContentTap()
              }
            },
            onDoubleTap = {
              if (scale > 1.15f) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
              } else {
                scale = 2.1f
              }
            },
          )
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
              val event = awaitPointerEvent()
              val pressedCount = event.changes.count { it.pressed }
              if (pressedCount >= 2 || scale > 1.02f) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val newScale = (scale * zoom).coerceIn(1f, 4f)
                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                scale = newScale
                if (newScale <= 1.02f) {
                  scale = 1f
                  offsetX = 0f
                  offsetY = 0f
                } else {
                  offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                  offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                }
                if (pressedCount >= 2 || abs(pan.x) > 1f) {
                  event.changes.forEach { it.consume() }
                }
              }
            } while (event.changes.any { it.pressed })
          }
        }
  ) {
    val density = LocalDensity.current
    val horizontalPad = settings.horizontalMarginDp.coerceIn(8, 56).dp
    val extraVerticalPad = (settings.verticalMarginDp - 12).coerceIn(0, 48).dp
    val availableWidthPx = with(density) { (maxWidth - horizontalPad * 2).toPx().toInt() }
    val renderWidthPx = (availableWidthPx * 1.45f).toInt().coerceIn(720, 1600)

    if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) {
      HorizontalPager(
        state = pagerState,
        userScrollEnabled = scale <= 1.02f,
        beyondViewportPageCount = 1,
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offsetX
              translationY = offsetY
            },
      ) { pageIndex ->
        BoxWithConstraints(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(
                top = 64.dp + extraVerticalPad,
                bottom = 102.dp + extraVerticalPad,
                start = horizontalPad,
                end = horizontalPad,
              ),
          contentAlignment = Alignment.Center,
        ) {
          val maxBoxW = maxWidth
          val maxBoxH = maxHeight
          var aspectRatio by remember(pageIndex) { mutableFloatStateOf(0.707f) }
          var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

          LaunchedEffect(pageIndex, renderWidthPx) {
            aspectRatio = session.getPageAspectRatio(pageIndex).coerceIn(0.45f, 1.8f)
            bitmap = session.renderPage(pageIndex, renderWidthPx)
          }

          val boxAspect =
            (maxBoxW.value / maxBoxH.value.coerceAtLeast(1f)).coerceAtLeast(0.1f)
          val pageModifier =
            if (aspectRatio > boxAspect) {
              Modifier.fillMaxWidth().aspectRatio(aspectRatio)
            } else {
              Modifier.height(maxBoxH).width(maxBoxH * aspectRatio)
            }

          PdfPageCardContent(
            bitmap = bitmap,
            pageIndex = pageIndex,
            colorFilter = colorFilter,
            showBorder = settings.showPageBorder,
            palette = palette,
            modifier = pageModifier,
          )
        }
      }
    } else {
      LazyColumn(
        state = listState,
        userScrollEnabled = scale <= 1.02f,
        contentPadding =
          PaddingValues(
            top = 64.dp + extraVerticalPad,
            bottom = 180.dp + extraVerticalPad,
            start = horizontalPad,
            end = horizontalPad,
          ),
        verticalArrangement = Arrangement.spacedBy((8 + (settings.verticalMarginDp / 4)).dp),
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offsetX
              translationY = offsetY
            },
      ) {
        items(count = session.pageCount, key = { it }) { pageIndex ->
          PdfPageCard(
            session = session,
            pageIndex = pageIndex,
            renderWidthPx = renderWidthPx,
            colorFilter = colorFilter,
            showBorder = settings.showPageBorder,
            palette = palette,
          )
        }
      }
    }
  }
}

@Composable
private fun PdfPageCard(
  session: PdfSession,
  pageIndex: Int,
  renderWidthPx: Int,
  colorFilter: ColorFilter?,
  showBorder: Boolean,
  palette: ReadingPalette,
) {
  var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
  var aspectRatio by remember(pageIndex) { mutableFloatStateOf(0.707f) }

  LaunchedEffect(pageIndex, renderWidthPx) {
    aspectRatio = session.getPageAspectRatio(pageIndex).coerceIn(0.45f, 1.8f)
    bitmap = session.renderPage(pageIndex, renderWidthPx)
  }

  PdfPageCardContent(
    bitmap = bitmap,
    pageIndex = pageIndex,
    colorFilter = colorFilter,
    showBorder = showBorder,
    palette = palette,
    modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
  )
}

@Composable
private fun PdfPageCardContent(
  bitmap: Bitmap?,
  pageIndex: Int,
  colorFilter: ColorFilter?,
  showBorder: Boolean,
  palette: ReadingPalette,
  modifier: Modifier = Modifier,
) {
  val pageShape = RoundedCornerShape(10.dp)
  val borderModifier =
    if (showBorder) {
      Modifier.border(BorderStroke(1.2.dp, palette.accent.copy(alpha = 0.45f)), pageShape)
    } else {
      Modifier
    }
  Box(
    modifier =
      modifier
        .shadow(elevation = if (palette.isDark) 0.dp else 3.dp, shape = pageShape)
        .clip(pageShape)
        .then(borderModifier)
        .background(palette.background),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null && !bitmap.isRecycled) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "PDF Page ${pageIndex + 1}",
        contentScale = ContentScale.FillBounds,
        colorFilter = colorFilter,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      CircularProgressIndicator(
        color = palette.accent,
        strokeWidth = 2.dp,
        modifier = Modifier.padding(16.dp),
      )
    }

    Text(
      text = "${pageIndex + 1}",
      fontSize = 10.sp,
      color = palette.secondaryText.copy(alpha = 0.72f),
      modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}
