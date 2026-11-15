package com.example.engine.epub

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.example.data.local.HighlightEntity
import java.io.File
import java.net.URLDecoder

data class ParsedChapterContent(
  val isRtlDocument: Boolean,
  val blocks: List<EpubContentBlock>,
)

sealed interface EpubContentBlock {
  val id: String
  val anchors: Set<String>

  data class Heading(
    override val id: String,
    val level: Int,
    val text: AnnotatedString,
    val isRtl: Boolean,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock

  data class Paragraph(
    override val id: String,
    val text: AnnotatedString,
    val isRtl: Boolean,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock

  data class Blockquote(
    override val id: String,
    val text: AnnotatedString,
    val isRtl: Boolean,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock

  data class ListItem(
    override val id: String,
    val bullet: String,
    val text: AnnotatedString,
    val isRtl: Boolean,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock

  data class Image(
    override val id: String,
    val localFilePath: String,
    val altText: String,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock

  data class Divider(
    override val id: String,
    override val anchors: Set<String> = emptySet(),
  ) : EpubContentBlock
}

/**
 * Pure-Kotlin XHTML/HTML5 block & inline-span parser for EPUB chapters.
 * Converts chapter markup directly into Compose-ready semantic blocks without Chromium WebView.
 */
object EpubHtmlBlockParser {

  private data class ActiveStyleFrame(
    val tag: String,
    val spanStyle: SpanStyle?,
    val linkAnchor: String? = null,
  )

  private data class InlineRun(
    val text: String,
    val styles: List<SpanStyle>,
    val anchorTarget: String? = null,
  )

  private val stripNonContentRegex =
    Regex(
      "<(head|script|style|noscript|title|svg)[^>]*>.*?</\\1>",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

  private val commentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
  private val xmlDeclRegex = Regex("<\\?.*?\\?>|<![^>]*>", RegexOption.DOT_MATCHES_ALL)
  private val bodyExtractRegex =
    Regex("<body([^>]*)>(.*)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  private val htmlAttrRegex =
    Regex("<html([^>]*)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  private val tokenRegex = Regex("(<[^>]+>)|([^<]+)", RegexOption.DOT_MATCHES_ALL)
  private val tagNameRegex = Regex("^</?\\s*([a-zA-Z0-9:_-]+)")
  private val attrRegex =
    Regex("""([a-zA-Z0-9:_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")
  private val numericEntityRegex = Regex("&#(x?[0-9a-fA-F]+);")

  fun parse(
    rawHtml: String,
    chapterFilePath: String,
  ): ParsedChapterContent {
    val htmlAttrText = htmlAttrRegex.find(rawHtml)?.groupValues?.getOrNull(1).orEmpty()
    val bodyMatch = bodyExtractRegex.find(rawHtml)
    val bodyAttrText = bodyMatch?.groupValues?.getOrNull(1).orEmpty()
    val isRtlDoc =
      containsRtlDirOrLang(htmlAttrText) || containsRtlDirOrLang(bodyAttrText)

    val cleanedBody =
      (bodyMatch?.groupValues?.getOrNull(2) ?: rawHtml)
        .replace(commentRegex, "")
        .replace(xmlDeclRegex, "")
        .replace(stripNonContentRegex, "")

    val chapterDir = File(chapterFilePath).parentFile
    val blocks = mutableListOf<EpubContentBlock>()
    val styleStack = ArrayDeque<ActiveStyleFrame>()
    var listCounterStack = ArrayDeque<Pair<Boolean, Int>>() // isOrdered to nextIndex
    var currentBlockTag = "p"
    var currentHeadingLevel = 1
    var currentInsideBlockquote = false
    var currentAnchors = linkedSetOf<String>()
    var currentRuns = mutableListOf<InlineRun>()
    var pendingOrderedBullet: String? = null
    var blockIndex = 0
    var inPre = false

    fun flushCurrentBlock() {
      if (currentRuns.isEmpty()) return
      val built = buildBlockAnnotatedString(currentRuns, preserveWhitespace = inPre)
      val trimmedText = built.text.trim()
      if (trimmedText.isNotEmpty()) {
        val id = "blk_${blockIndex++}"
        val isRtl = isRtlDoc || detectTextRtl(trimmedText)
        val anchorsCopy = currentAnchors.toSet()
        val block =
          when {
            currentBlockTag in listOf("h1", "h2", "h3", "h4", "h5", "h6") ->
              EpubContentBlock.Heading(
                id = id,
                level = currentHeadingLevel.coerceIn(1, 4),
                text = built,
                isRtl = isRtl,
                anchors = anchorsCopy,
              )
            currentInsideBlockquote ->
              EpubContentBlock.Blockquote(
                id = id,
                text = built,
                isRtl = isRtl,
                anchors = anchorsCopy,
              )
            currentBlockTag == "li" ->
              EpubContentBlock.ListItem(
                id = id,
                bullet = pendingOrderedBullet ?: "•",
                text = built,
                isRtl = isRtl,
                anchors = anchorsCopy,
              )
            else ->
              EpubContentBlock.Paragraph(
                id = id,
                text = built,
                isRtl = isRtl,
                anchors = anchorsCopy,
              )
          }
        blocks.add(block)
        currentAnchors = linkedSetOf()
      }
      currentRuns = mutableListOf()
      pendingOrderedBullet = null
    }

    for (match in tokenRegex.findAll(cleanedBody)) {
      val tagToken = match.groups[1]?.value
      val textToken = match.groups[2]?.value

      if (textToken != null) {
        val decoded = decodeHtmlEntities(textToken)
        if (decoded.isNotEmpty()) {
          val activeStyles = styleStack.mapNotNull { it.spanStyle }
          val activeAnchor = styleStack.lastOrNull { it.linkAnchor != null }?.linkAnchor
          currentRuns.add(InlineRun(decoded, activeStyles, activeAnchor))
        }
        continue
      }

      if (tagToken != null) {
        val isClosing = tagToken.startsWith("</")
        val isSelfClosing = tagToken.endsWith("/>")
        val rawTagName = tagNameRegex.find(tagToken)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
        val tagName = rawTagName.substringAfter(':')
        if (tagName.isBlank()) continue

        val attrs = if (!isClosing) parseAttributes(tagToken) else emptyMap()
        if (!isClosing) {
          attrs["id"]?.takeIf { it.isNotBlank() }?.let { currentAnchors.add(it) }
          attrs["name"]?.takeIf { it.isNotBlank() }?.let { currentAnchors.add(it) }
        }

        when {
          tagName == "br" -> {
            if (!isClosing) {
              currentRuns.add(InlineRun("\n", emptyList()))
            }
          }
          tagName == "hr" -> {
            flushCurrentBlock()
            blocks.add(
              EpubContentBlock.Divider(
                id = "blk_${blockIndex++}",
                anchors = currentAnchors.toSet(),
              )
            )
            currentAnchors = linkedSetOf()
          }
          tagName == "img" || tagName == "image" -> {
            val src = attrs["src"] ?: attrs["xlink:href"] ?: attrs["href"] ?: ""
            val resolvedFile = resolveLocalImageFile(chapterDir, src)
            if (resolvedFile != null) {
              flushCurrentBlock()
              blocks.add(
                EpubContentBlock.Image(
                  id = "blk_${blockIndex++}",
                  localFilePath = resolvedFile.absolutePath,
                  altText = attrs["alt"].orEmpty().ifBlank { "Illustration" },
                  anchors = currentAnchors.toSet(),
                )
              )
              currentAnchors = linkedSetOf()
            }
          }
          tagName in setOf("h1", "h2", "h3", "h4", "h5", "h6") -> {
            if (!isClosing) {
              flushCurrentBlock()
              currentBlockTag = tagName
              currentHeadingLevel = tagName.drop(1).toIntOrNull() ?: 2
            } else {
              flushCurrentBlock()
              currentBlockTag = "p"
            }
          }
          tagName == "blockquote" -> {
            if (!isClosing) {
              flushCurrentBlock()
              currentInsideBlockquote = true
              currentBlockTag = "blockquote"
            } else {
              flushCurrentBlock()
              currentInsideBlockquote = false
              currentBlockTag = "p"
            }
          }
          tagName == "ol" -> {
            if (!isClosing) {
              flushCurrentBlock()
              listCounterStack.addLast(true to 1)
            } else {
              flushCurrentBlock()
              if (listCounterStack.isNotEmpty()) listCounterStack.removeLast()
            }
          }
          tagName == "ul" -> {
            if (!isClosing) {
              flushCurrentBlock()
              listCounterStack.addLast(false to 1)
            } else {
              flushCurrentBlock()
              if (listCounterStack.isNotEmpty()) listCounterStack.removeLast()
            }
          }
          tagName == "li" -> {
            if (!isClosing) {
              flushCurrentBlock()
              currentBlockTag = "li"
              val topList = listCounterStack.lastOrNull()
              if (topList != null && topList.first) {
                pendingOrderedBullet = "${topList.second}."
                listCounterStack.removeLast()
                listCounterStack.addLast(true to (topList.second + 1))
              } else {
                pendingOrderedBullet = "•"
              }
            } else {
              flushCurrentBlock()
              currentBlockTag = "p"
            }
          }
          tagName == "pre" -> {
            if (!isClosing) {
              flushCurrentBlock()
              inPre = true
              styleStack.addLast(
                ActiveStyleFrame(
                  tag = tagName,
                  spanStyle = SpanStyle(fontFamily = FontFamily.Monospace),
                )
              )
            } else {
              flushCurrentBlock()
              inPre = false
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName in setOf("p", "div", "section", "article", "header", "footer", "figure", "figcaption") -> {
            if (!isClosing) {
              if (currentRuns.any { it.text.isNotBlank() }) {
                flushCurrentBlock()
              }
              if (!currentInsideBlockquote) {
                currentBlockTag = "p"
              }
            } else {
              flushCurrentBlock()
              if (!currentInsideBlockquote) {
                currentBlockTag = "p"
              }
            }
          }
          tagName in setOf("b", "strong") -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(ActiveStyleFrame(tagName, SpanStyle(fontWeight = FontWeight.Bold)))
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName in setOf("i", "em", "cite", "dfn") -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(ActiveStyleFrame(tagName, SpanStyle(fontStyle = FontStyle.Italic)))
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName in setOf("u", "ins") -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(
                ActiveStyleFrame(tagName, SpanStyle(textDecoration = TextDecoration.Underline))
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName in setOf("s", "del", "strike") -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(
                ActiveStyleFrame(tagName, SpanStyle(textDecoration = TextDecoration.LineThrough))
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName in setOf("code", "kbd", "tt") -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(
                ActiveStyleFrame(tagName, SpanStyle(fontFamily = FontFamily.Monospace))
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName == "sup" -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(
                ActiveStyleFrame(
                  tagName,
                  SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em),
                )
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName == "sub" -> {
            if (!isClosing && !isSelfClosing) {
              styleStack.addLast(
                ActiveStyleFrame(
                  tagName,
                  SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em),
                )
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
          tagName == "a" -> {
            val href = attrs["href"]
            if (!isClosing && !isSelfClosing) {
              val anchorOnly = href?.substringAfter('#', "")?.takeIf { it.isNotBlank() && !href.startsWith("http") }
              styleStack.addLast(
                ActiveStyleFrame(
                  tag = tagName,
                  spanStyle = if (href != null) SpanStyle(textDecoration = TextDecoration.Underline) else null,
                  linkAnchor = anchorOnly,
                )
              )
            } else if (isClosing) {
              popMatchingTag(styleStack, tagName)
            }
          }
        }
      }
    }

    flushCurrentBlock()

    val effectiveBlocks =
      blocks.ifEmpty {
        listOf(
          EpubContentBlock.Paragraph(
            id = "blk_empty",
            text = AnnotatedString(""),
            isRtl = isRtlDoc,
          )
        )
      }

    return ParsedChapterContent(
      isRtlDocument = isRtlDoc || effectiveBlocks.any {
        when (it) {
          is EpubContentBlock.Heading -> it.isRtl
          is EpubContentBlock.Paragraph -> it.isRtl
          is EpubContentBlock.Blockquote -> it.isRtl
          is EpubContentBlock.ListItem -> it.isRtl
          else -> false
        }
      },
      blocks = effectiveBlocks,
    )
  }

  /**
   * Overlays persisted highlights and the active user selection range onto a block's AnnotatedString.
   */
  fun decorateAnnotatedText(
    base: AnnotatedString,
    highlights: List<HighlightEntity>,
    activeSelectionRange: IntRange?,
    selectionHighlightColor: Color,
  ): AnnotatedString {
    if (highlights.isEmpty() && activeSelectionRange == null) return base
    val plain = base.text
    if (plain.isEmpty()) return base

    return buildAnnotatedString {
      append(base)
      for (hl in highlights) {
        val needle = hl.selectedText.trim()
        if (needle.length < 2) continue
        val bgColor = parseHighlightHex(hl.colorHex).copy(alpha = 0.48f)
        var startIdx = 0
        while (startIdx < plain.length) {
          val foundIdx = plain.indexOf(needle, startIndex = startIdx, ignoreCase = false)
          if (foundIdx < 0) break
          val endIdx = (foundIdx + needle.length).coerceAtMost(plain.length)
          addStyle(
            style = SpanStyle(background = bgColor, color = Color(0xFF1A1C1A)),
            start = foundIdx,
            end = endIdx,
          )
          startIdx = endIdx
        }
      }

      if (activeSelectionRange != null) {
        val s = activeSelectionRange.first.coerceIn(0, plain.length)
        val e = activeSelectionRange.last.coerceIn(s, plain.length)
        if (e > s) {
          addStyle(
            style = SpanStyle(background = selectionHighlightColor.copy(alpha = 0.30f)),
            start = s,
            end = e,
          )
        }
      }
    }
  }

  private fun parseHighlightHex(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
      .getOrDefault(Color(0xFFFDE68A))
  }

  private fun buildBlockAnnotatedString(
    runs: List<InlineRun>,
    preserveWhitespace: Boolean,
  ): AnnotatedString {
    val rawBuilder = buildAnnotatedString {
      var lastEndedWithSpace = true
      runs.forEach { run ->
        val normalized =
          if (preserveWhitespace) {
            run.text
          } else {
            normalizeRunText(run.text, leadingSpaceCollapsed = lastEndedWithSpace)
          }
        if (normalized.isEmpty()) return@forEach
        val start = length
        append(normalized)
        val end = length
        run.styles.forEach { style ->
          addStyle(style, start, end)
        }
        if (run.anchorTarget != null) {
          addStringAnnotation(tag = "ANCHOR", annotation = run.anchorTarget, start = start, end = end)
        }
        lastEndedWithSpace = normalized.last().isWhitespace()
      }
    }

    // Trim trailing whitespace while keeping spans aligned cleanly
    val rawText = rawBuilder.text
    val trimStart = rawText.indexOfFirst { !it.isWhitespace() }
    if (trimStart < 0) return AnnotatedString("")
    val trimEnd = rawText.indexOfLast { !it.isWhitespace() } + 1
    return rawBuilder.subSequence(trimStart, trimEnd)
  }

  private fun normalizeRunText(input: String, leadingSpaceCollapsed: Boolean): String {
    if (input == "\n") return "\n"
    val sb = StringBuilder(input.length)
    var prevWasSpace = leadingSpaceCollapsed
    for (ch in input) {
      when {
        ch == '\n' -> {
          // Collapse intra-line newlines to single space unless explicit <br> was emitted alone
          if (!prevWasSpace) {
            sb.append(' ')
            prevWasSpace = true
          }
        }
        ch == ' ' || ch == '\t' || ch == '\r' -> {
          if (!prevWasSpace) {
            sb.append(' ')
            prevWasSpace = true
          }
        }
        else -> {
          sb.append(ch)
          prevWasSpace = false
        }
      }
    }
    return sb.toString()
  }

  private fun popMatchingTag(stack: ArrayDeque<ActiveStyleFrame>, tagName: String) {
    for (idx in stack.indices.reversed()) {
      if (stack[idx].tag == tagName) {
        stack.removeAt(idx)
        return
      }
    }
  }

  private fun parseAttributes(tagToken: String): Map<String, String> {
    val map = LinkedHashMap<String, String>()
    for (m in attrRegex.findAll(tagToken)) {
      val key = m.groupValues[1].lowercase()
      val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
      map[key] = decodeHtmlEntities(value)
    }
    return map
  }

  private fun containsRtlDirOrLang(attrs: String): Boolean {
    val lower = attrs.lowercase()
    return lower.contains("""dir="rtl"""") ||
      lower.contains("""dir='rtl'""") ||
      lower.contains("""lang="fa"""") ||
      lower.contains("""lang="ar"""") ||
      lower.contains("""lang="he"""") ||
      lower.contains("""lang="ur"""")
  }

  fun detectTextRtl(text: String): Boolean {
    for (ch in text) {
      val code = ch.code
      if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF || code in 0x0590..0x05FF) {
        return true
      }
      if (ch in 'A'..'Z' || ch in 'a'..'z') {
        return false
      }
    }
    return false
  }

  private fun resolveLocalImageFile(chapterDir: File?, rawSrc: String): File? {
    if (chapterDir == null || rawSrc.isBlank()) return null
    val cleaned =
      runCatching { URLDecoder.decode(rawSrc.substringBefore('#').substringBefore('?'), "UTF-8") }
        .getOrDefault(rawSrc)
        .trim()
    if (cleaned.startsWith("http://", true) || cleaned.startsWith("https://", true) || cleaned.startsWith("data:", true)) {
      return null
    }
    val candidate = File(chapterDir, cleaned).canonicalFile
    if (candidate.exists() && candidate.isFile) return candidate
    // Fallback: relative to EPUB root parent if needed
    val fileName = File(cleaned).name
    val fallback = chapterDir.parentFile?.walkTopDown()?.firstOrNull { it.isFile && it.name == fileName }
    return fallback
  }

  fun decodeHtmlEntities(input: String): String {
    if (!input.contains('&')) return input
    val named =
      input
        .replace("&nbsp;", "\u00A0")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace("&ldquo;", "“")
        .replace("&rdquo;", "”")
        .replace("&lsquo;", "‘")
        .replace("&rsquo;", "’")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&zwnj;", "\u200C")
        .replace("&zwj;", "\u200D")
    return numericEntityRegex.replace(named) { m ->
      val rawNum = m.groupValues[1]
      val code =
        if (rawNum.startsWith("x", ignoreCase = true)) {
          rawNum.drop(1).toIntOrNull(16)
        } else {
          rawNum.toIntOrNull(10)
        }
      if (code != null && code in 1..0x10FFFF) {
        String(Character.toChars(code))
      } else {
        m.value
      }
    }
  }
}
