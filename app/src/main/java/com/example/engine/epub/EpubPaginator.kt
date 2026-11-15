package com.example.engine.epub

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class EpubPageItem(
  val sliceId: String,
  val block: EpubContentBlock,
  val anchors: Set<String>,
)

data class EpubPage(
  val pageIndex: Int,
  val items: List<EpubPageItem>,
  val anchors: Set<String>,
)

/**
 * Dynamically paginates semantic EPUB blocks into screen-fitted pages using Jetpack Compose's
 * TextMeasurer so every page fills naturally with zero vertical scrolling and zero cut-off lines.
 */
object EpubPaginator {

  fun paginate(
    blocks: List<EpubContentBlock>,
    textMeasurer: TextMeasurer,
    contentWidthPx: Int,
    contentHeightPx: Int,
    baseBodyStyle: TextStyle,
    paragraphSpacingPx: Float,
    headingSpacingPx: Float,
    blockquoteExtraWidthPx: Int,
    blockquotePaddingVPx: Int,
    listItemBulletWidthPx: Int,
    imageMaxHeightPx: Int,
    dividerHeightPx: Int,
    densityScale: Float,
  ): List<EpubPage> {
    if (blocks.isEmpty() || contentWidthPx <= 64 || contentHeightPx <= 96) {
      return listOf(
        EpubPage(
          pageIndex = 0,
          items =
            blocks.map { EpubPageItem(sliceId = it.id, block = it, anchors = it.anchors) },
          anchors = blocks.flatMapTo(linkedSetOf()) { it.anchors },
        )
      )
    }

    val pages = mutableListOf<EpubPage>()
    var currentItems = mutableListOf<EpubPageItem>()
    var currentAnchors = linkedSetOf<String>()
    var usedHeightPx = 0f

    fun flushPage() {
      if (currentItems.isNotEmpty()) {
        pages.add(
          EpubPage(
            pageIndex = pages.size,
            items = currentItems,
            anchors = currentAnchors,
          )
        )
        currentItems = mutableListOf()
        currentAnchors = linkedSetOf()
        usedHeightPx = 0f
      }
    }

    for (block in blocks) {
      when (block) {
        is EpubContentBlock.Divider -> {
          val h = dividerHeightPx.toFloat() + paragraphSpacingPx
          if (currentItems.isNotEmpty() && usedHeightPx + h > contentHeightPx) {
            flushPage()
          }
          currentItems.add(EpubPageItem(block.id, block, block.anchors))
          currentAnchors.addAll(block.anchors)
          usedHeightPx += h
        }

        is EpubContentBlock.Image -> {
          val h = imageMaxHeightPx.coerceAtMost((contentHeightPx * 0.72f).roundToInt()).toFloat() + paragraphSpacingPx
          if (currentItems.isNotEmpty() && usedHeightPx + h > contentHeightPx) {
            flushPage()
          }
          currentItems.add(EpubPageItem(block.id, block, block.anchors))
          currentAnchors.addAll(block.anchors)
          usedHeightPx += h
        }

        is EpubContentBlock.Heading -> {
          val headingStyle =
            headingStyleForLevel(baseBodyStyle, block.level, block.isRtl)
          val layout =
            textMeasurer.measure(
              text = block.text,
              style = headingStyle,
              constraints = Constraints(maxWidth = contentWidthPx),
            )
          val neededH = layout.size.height.toFloat() + headingSpacingPx
          if (currentItems.isNotEmpty() && usedHeightPx + neededH > contentHeightPx) {
            flushPage()
          }
          currentItems.add(EpubPageItem(block.id, block, block.anchors))
          currentAnchors.addAll(block.anchors)
          usedHeightPx += neededH
        }

        is EpubContentBlock.Paragraph -> {
          val pStyle =
            baseBodyStyle.copy(
              textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
            )
          paginateSplittableText(
            blockId = block.id,
            fullText = block.text,
            anchors = block.anchors,
            textStyle = pStyle,
            measureWidthPx = contentWidthPx,
            extraVerticalPx = 0f,
            spacingAfterPx = paragraphSpacingPx,
            textMeasurer = textMeasurer,
            contentHeightPx = contentHeightPx,
            currentPageEmpty = { currentItems.isEmpty() },
            remainingHeightPx = { (contentHeightPx - usedHeightPx).coerceAtLeast(0f) },
            onAddSlice = { sliceId, sliceAnnotated, sliceAnchors, sliceHeightPx ->
              val sliceBlock = block.copy(id = sliceId, text = sliceAnnotated, anchors = sliceAnchors)
              currentItems.add(EpubPageItem(sliceId, sliceBlock, sliceAnchors))
              currentAnchors.addAll(sliceAnchors)
              usedHeightPx += sliceHeightPx
            },
            onFlushPage = ::flushPage,
          )
        }

        is EpubContentBlock.Blockquote -> {
          val qStyle =
            baseBodyStyle.copy(
              textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
            )
          val innerWidth = (contentWidthPx - blockquoteExtraWidthPx).coerceAtLeast(80)
          paginateSplittableText(
            blockId = block.id,
            fullText = block.text,
            anchors = block.anchors,
            textStyle = qStyle,
            measureWidthPx = innerWidth,
            extraVerticalPx = blockquotePaddingVPx.toFloat(),
            spacingAfterPx = paragraphSpacingPx,
            textMeasurer = textMeasurer,
            contentHeightPx = contentHeightPx,
            currentPageEmpty = { currentItems.isEmpty() },
            remainingHeightPx = { (contentHeightPx - usedHeightPx).coerceAtLeast(0f) },
            onAddSlice = { sliceId, sliceAnnotated, sliceAnchors, sliceHeightPx ->
              val sliceBlock = block.copy(id = sliceId, text = sliceAnnotated, anchors = sliceAnchors)
              currentItems.add(EpubPageItem(sliceId, sliceBlock, sliceAnchors))
              currentAnchors.addAll(sliceAnchors)
              usedHeightPx += sliceHeightPx
            },
            onFlushPage = ::flushPage,
          )
        }

        is EpubContentBlock.ListItem -> {
          val lStyle =
            baseBodyStyle.copy(
              textDirection = if (block.isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
            )
          val innerWidth = (contentWidthPx - listItemBulletWidthPx).coerceAtLeast(80)
          paginateSplittableText(
            blockId = block.id,
            fullText = block.text,
            anchors = block.anchors,
            textStyle = lStyle,
            measureWidthPx = innerWidth,
            extraVerticalPx = 0f,
            spacingAfterPx = (paragraphSpacingPx * 0.65f) + (2f * densityScale),
            textMeasurer = textMeasurer,
            contentHeightPx = contentHeightPx,
            currentPageEmpty = { currentItems.isEmpty() },
            remainingHeightPx = { (contentHeightPx - usedHeightPx).coerceAtLeast(0f) },
            onAddSlice = { sliceId, sliceAnnotated, sliceAnchors, sliceHeightPx ->
              val sliceBlock = block.copy(id = sliceId, text = sliceAnnotated, anchors = sliceAnchors)
              currentItems.add(EpubPageItem(sliceId, sliceBlock, sliceAnchors))
              currentAnchors.addAll(sliceAnchors)
              usedHeightPx += sliceHeightPx
            },
            onFlushPage = ::flushPage,
          )
        }
      }
    }

    flushPage()
    if (pages.isEmpty()) {
      pages.add(EpubPage(0, emptyList(), emptySet()))
    }
    return pages
  }

  private fun paginateSplittableText(
    blockId: String,
    fullText: AnnotatedString,
    anchors: Set<String>,
    textStyle: TextStyle,
    measureWidthPx: Int,
    extraVerticalPx: Float,
    spacingAfterPx: Float,
    textMeasurer: TextMeasurer,
    contentHeightPx: Int,
    currentPageEmpty: () -> Boolean,
    remainingHeightPx: () -> Float,
    onAddSlice: (String, AnnotatedString, Set<String>, Float) -> Unit,
    onFlushPage: () -> Unit,
  ) {
    var pendingText = fullText
    var pendingAnchors = anchors
    var sliceCounter = 0

    while (pendingText.text.isNotEmpty() && sliceCounter < 64) {
      val layout =
        textMeasurer.measure(
          text = pendingText,
          style = textStyle,
          constraints = Constraints(maxWidth = measureWidthPx),
        )
      val totalBlockH = layout.size.height.toFloat() + extraVerticalPx
      val avail = remainingHeightPx()

      if (totalBlockH + spacingAfterPx <= avail) {
        onAddSlice(
          if (sliceCounter == 0) blockId else "${blockId}_s$sliceCounter",
          pendingText,
          pendingAnchors,
          totalBlockH + spacingAfterPx,
        )
        break
      }

      // Check how many lines fit cleanly inside remaining height
      val usableH = avail - extraVerticalPx
      var lastFittingLine = -1
      for (line in 0 until layout.lineCount) {
        if (layout.getLineBottom(line) <= usableH) {
          lastFittingLine = line
        } else {
          break
        }
      }

      // Avoid 1-line orphans at bottom of a non-empty page
      val minLinesToSplit = if (currentPageEmpty()) 1 else 2
      if (lastFittingLine + 1 < minLinesToSplit || lastFittingLine >= layout.lineCount - 1) {
        if (!currentPageEmpty()) {
          onFlushPage()
          continue
        }
        // Page is empty and even 1+ lines exceed contentHeightPx -> take as many lines as fit (min 1)
        val forcedLine = lastFittingLine.coerceAtLeast(0).coerceAtMost(layout.lineCount - 1)
        val rawEnd = layout.getLineEnd(forcedLine, visibleEnd = false).coerceAtLeast(1)
        if (rawEnd >= pendingText.length) {
          onAddSlice(
            if (sliceCounter == 0) blockId else "${blockId}_s$sliceCounter",
            pendingText,
            pendingAnchors,
            totalBlockH + spacingAfterPx,
          )
          break
        }
        val sliceText = trimEndAnnotated(pendingText.subSequence(0, rawEnd))
        val sliceH = layout.getLineBottom(forcedLine) + extraVerticalPx + spacingAfterPx
        if (sliceText.isNotEmpty()) {
          onAddSlice(
            if (sliceCounter == 0) blockId else "${blockId}_s$sliceCounter",
            sliceText,
            pendingAnchors,
            sliceH,
          )
          pendingAnchors = emptySet()
          sliceCounter++
        }
        onFlushPage()
        pendingText = trimStartAnnotated(pendingText.subSequence(rawEnd, pendingText.length))
        continue
      }

      val splitEnd = layout.getLineEnd(lastFittingLine, visibleEnd = false)
      if (splitEnd <= 0 || splitEnd >= pendingText.length) {
        onAddSlice(
          if (sliceCounter == 0) blockId else "${blockId}_s$sliceCounter",
          pendingText,
          pendingAnchors,
          totalBlockH + spacingAfterPx,
        )
        break
      }

      val sliceText = trimEndAnnotated(pendingText.subSequence(0, splitEnd))
      val sliceH = layout.getLineBottom(lastFittingLine) + extraVerticalPx + spacingAfterPx
      if (sliceText.isNotEmpty()) {
        onAddSlice(
          if (sliceCounter == 0) blockId else "${blockId}_s$sliceCounter",
          sliceText,
          pendingAnchors,
          sliceH,
        )
        pendingAnchors = emptySet()
        sliceCounter++
      }
      onFlushPage()
      pendingText = trimStartAnnotated(pendingText.subSequence(splitEnd, pendingText.length))
    }
  }

  private fun trimStartAnnotated(src: AnnotatedString): AnnotatedString {
    val first = src.text.indexOfFirst { !it.isWhitespace() }
    if (first < 0) return AnnotatedString("")
    return if (first == 0) src else src.subSequence(first, src.length)
  }

  private fun trimEndAnnotated(src: AnnotatedString): AnnotatedString {
    val last = src.text.indexOfLast { !it.isWhitespace() }
    if (last < 0) return AnnotatedString("")
    return if (last == src.length - 1) src else src.subSequence(0, last + 1)
  }

  fun headingStyleForLevel(
    baseBodyStyle: TextStyle,
    level: Int,
    isRtl: Boolean,
  ): TextStyle {
    val bodySp = baseBodyStyle.fontSize.value
    val scale =
      when (level) {
        1 -> 1.48f
        2 -> 1.28f
        3 -> 1.15f
        else -> 1.06f
      }
    return baseBodyStyle.copy(
      fontSize = (bodySp * scale).sp,
      lineHeight = (bodySp * scale * 1.28f).sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = if (isRtl) 0.sp else (-0.015).em,
      textAlign = TextAlign.Start,
      textDirection = if (isRtl) TextDirection.Rtl else TextDirection.ContentOrLtr,
    )
  }
}
