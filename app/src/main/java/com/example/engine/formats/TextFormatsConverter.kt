package com.example.engine.formats

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.model.BookFormat
import com.example.model.EpubChapter
import com.example.model.EpubTocItem
import com.example.model.ParsedEpubBook
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ReflowableMetadata(
  val title: String,
  val author: String,
  val totalUnits: Int,
  val coverPath: String?,
)

class TextFormatsConverter(private val context: Context) {

  suspend fun extractMetadataAndCover(
    file: File,
    format: BookFormat,
    bookId: String,
    fallbackTitle: String,
  ): Result<ReflowableMetadata> =
    withContext(Dispatchers.IO) {
      runCatching {
        val loaded = buildAndCacheChapters(file, format, bookId, fallbackTitle)
        loaded.first
      }
    }

  suspend fun loadBook(
    file: File,
    format: BookFormat,
    bookId: String,
    fallbackTitle: String,
  ): Result<ParsedEpubBook> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cacheDir = File(context.filesDir, "epub_cache/$bookId")
        val marker = File(cacheDir, ".unpacked_v1")
        if (marker.exists()) {
          val cached = loadFromCache(cacheDir, fallbackTitle)
          if (cached != null) return@runCatching cached
        }
        buildAndCacheChapters(file, format, bookId, fallbackTitle).second
      }
    }

  private fun buildAndCacheChapters(
    file: File,
    format: BookFormat,
    bookId: String,
    fallbackTitle: String,
  ): Pair<ReflowableMetadata, ParsedEpubBook> {
    if (!file.exists() || file.length() == 0L) {
      error("Book file is empty or missing.")
    }

    val cacheDir = File(context.filesDir, "epub_cache/$bookId")
    cacheDir.deleteRecursively()
    cacheDir.mkdirs()

    return when (format) {
      BookFormat.TXT -> buildTxtBook(file, cacheDir, fallbackTitle)
      BookFormat.MD -> buildMarkdownBook(file, cacheDir, fallbackTitle)
      BookFormat.FB2 -> buildFb2Book(file, cacheDir, bookId, fallbackTitle)
      else -> error("Unsupported reflowable text format: ${format.name}")
    }
  }

  private fun buildTxtBook(
    file: File,
    cacheDir: File,
    fallbackTitle: String,
  ): Pair<ReflowableMetadata, ParsedEpubBook> {
    val raw = readTextSmart(file)
    val lines = raw.replace("\r\n", "\n").replace('\r', '\n').lines()
    val isRtl = isPredominantlyRtl(raw)

    val headingRegex =
      Regex(
        """^(chapter\s+[0-9ivxlcdm]+.*|part\s+[0-9ivxlcdm]+.*|section\s+[0-9]+.*|فصل\s+.*|بخش\s+.*)$""",
        RegexOption.IGNORE_CASE,
      )

    data class DraftChapter(val title: String, val bodyLines: MutableList<String>)

    val drafts = mutableListOf<DraftChapter>()
    var current = DraftChapter(title = fallbackTitle, bodyLines = mutableListOf())

    for (line in lines) {
      val trimmed = line.trim()
      val looksLikeHeading = headingRegex.matches(trimmed) && trimmed.length <= 90
      if (looksLikeHeading && current.bodyLines.any { it.isNotBlank() }) {
        drafts.add(current)
        current = DraftChapter(title = trimmed, bodyLines = mutableListOf())
      } else if (looksLikeHeading && current.bodyLines.none { it.isNotBlank() } && drafts.isEmpty()) {
        current = DraftChapter(title = trimmed, bodyLines = mutableListOf())
      } else {
        current.bodyLines.add(line)
        if (current.bodyLines.size >= 260 && trimmed.isBlank()) {
          drafts.add(current)
          val nextNum = drafts.size + 1
          current =
            DraftChapter(
              title = if (isRtl) "بخش $nextNum" else "Section $nextNum",
              bodyLines = mutableListOf(),
            )
        }
      }
    }
    if (current.bodyLines.any { it.isNotBlank() } || drafts.isEmpty()) {
      drafts.add(current)
    }

    val chapters = mutableListOf<EpubChapter>()
    val toc = mutableListOf<EpubTocItem>()
    drafts.forEachIndexed { idx, draft ->
      val bodyHtml = buildString {
        append("<h1>").append(escapeHtml(draft.title)).append("</h1>\n")
        val para = StringBuilder()
        fun flushPara() {
          val text = para.toString().trim()
          if (text.isNotEmpty()) {
            append("<p>").append(escapeHtml(text).replace("\n", "<br/>")).append("</p>\n")
          }
          para.clear()
        }
        for (line in draft.bodyLines) {
          if (line.isBlank()) {
            flushPara()
          } else {
            if (para.isNotEmpty()) para.append('\n')
            para.append(line.trimEnd())
          }
        }
        flushPara()
      }

      val fileName = "chapter_${idx + 1}.xhtml"
      val outFile = File(cacheDir, fileName)
      outFile.writeText(wrapXhtml(draft.title, bodyHtml, isRtl))
      chapters.add(
        EpubChapter(
          index = idx,
          id = "txt_$idx",
          title = draft.title,
          href = fileName,
          localFilePath = outFile.absolutePath,
        )
      )
      toc.add(EpubTocItem(title = draft.title, chapterIndex = idx, level = 0))
    }

    val authorLabel = if (isRtl) "متن ساده (TXT)" else "Plain Text (TXT)"
    writeManifestCache(cacheDir, fallbackTitle, authorLabel, chapters, toc)
    val parsed = ParsedEpubBook(fallbackTitle, authorLabel, chapters, toc, cacheDir.absolutePath)
    val meta = ReflowableMetadata(fallbackTitle, authorLabel, chapters.size, coverPath = null)
    return meta to parsed
  }

  private fun buildMarkdownBook(
    file: File,
    cacheDir: File,
    fallbackTitle: String,
  ): Pair<ReflowableMetadata, ParsedEpubBook> {
    var raw = readTextSmart(file).replace("\r\n", "\n").replace('\r', '\n')
    var parsedTitle = fallbackTitle
    var parsedAuthor = ""

    // Strip optional YAML frontmatter and extract title/author
    if (raw.startsWith("---\n")) {
      val endIdx = raw.indexOf("\n---\n", startIndex = 4)
      if (endIdx > 0) {
        val yaml = raw.substring(4, endIdx)
        raw = raw.substring(endIdx + 5)
        yaml.lines().forEach { line ->
          val key = line.substringBefore(':').trim().lowercase()
          val value = line.substringAfter(':', "").trim().trim('"', '\'')
          if (key == "title" && value.isNotBlank()) parsedTitle = value
          if (key == "author" && value.isNotBlank()) parsedAuthor = value
        }
      }
    }

    val isRtl = isPredominantlyRtl(raw)
    val defaultAuthor = parsedAuthor.ifBlank { if (isRtl) "سند Markdown" else "Markdown Document" }
    val lines = raw.lines()

    data class MdSection(val title: String, val lines: MutableList<String>)

    val sections = mutableListOf<MdSection>()
    var current = MdSection(title = parsedTitle, lines = mutableListOf())

    var inFence = false
    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.startsWith("```")) {
        inFence = !inFence
        current.lines.add(line)
        continue
      }
      if (!inFence && trimmed.startsWith("# ")) {
        val h1 = trimmed.removePrefix("# ").trim()
        if (sections.isEmpty() && current.lines.none { it.isNotBlank() }) {
          if (parsedTitle == fallbackTitle && h1.isNotBlank()) parsedTitle = h1
          current = MdSection(title = h1.ifBlank { parsedTitle }, lines = mutableListOf(line))
        } else {
          if (current.lines.any { it.isNotBlank() }) sections.add(current)
          current = MdSection(title = h1.ifBlank { "Section ${sections.size + 1}" }, lines = mutableListOf(line))
        }
      } else if (!inFence && trimmed.startsWith("## ") && current.lines.size > 40) {
        val h2 = trimmed.removePrefix("## ").trim()
        if (current.lines.any { it.isNotBlank() }) sections.add(current)
        current = MdSection(title = h2.ifBlank { "Section ${sections.size + 1}" }, lines = mutableListOf(line))
      } else {
        current.lines.add(line)
      }
    }
    if (current.lines.any { it.isNotBlank() } || sections.isEmpty()) {
      sections.add(current)
    }

    val chapters = mutableListOf<EpubChapter>()
    val toc = mutableListOf<EpubTocItem>()
    sections.forEachIndexed { idx, sec ->
      val bodyHtml = markdownLinesToHtml(sec.lines)
      val fileName = "md_${idx + 1}.xhtml"
      val outFile = File(cacheDir, fileName)
      outFile.writeText(wrapXhtml(sec.title, bodyHtml, isRtl))
      chapters.add(
        EpubChapter(
          index = idx,
          id = "md_$idx",
          title = sec.title,
          href = fileName,
          localFilePath = outFile.absolutePath,
        )
      )
      toc.add(EpubTocItem(title = sec.title, chapterIndex = idx, level = 0))
    }

    writeManifestCache(cacheDir, parsedTitle, defaultAuthor, chapters, toc)
    val parsed = ParsedEpubBook(parsedTitle, defaultAuthor, chapters, toc, cacheDir.absolutePath)
    val meta = ReflowableMetadata(parsedTitle, defaultAuthor, chapters.size, coverPath = null)
    return meta to parsed
  }

  private fun markdownLinesToHtml(lines: List<String>): String = buildString {
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      val trimmed = line.trim()
      when {
        trimmed.isEmpty() -> i++
        trimmed.startsWith("```") -> {
          val code = StringBuilder()
          i++
          while (i < lines.size && !lines[i].trim().startsWith("```")) {
            code.append(lines[i]).append('\n')
            i++
          }
          i++ // closing fence
          append("<pre><code>").append(escapeHtml(code.toString().trimEnd())).append("</code></pre>\n")
        }
        trimmed.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> {
          append("<hr/>\n")
          i++
        }
        trimmed.startsWith("#") && trimmed.takeWhile { it == '#' }.length in 1..6 &&
          trimmed.dropWhile { it == '#' }.firstOrNull() == ' ' -> {
          val level = trimmed.takeWhile { it == '#' }.length
          val content = trimmed.drop(level).trim()
          append("<h$level>").append(formatInlineMarkdown(content)).append("</h$level>\n")
          i++
        }
        trimmed.startsWith(">") -> {
          val quoteLines = mutableListOf<String>()
          while (i < lines.size && lines[i].trim().startsWith(">")) {
            quoteLines.add(lines[i].trim().removePrefix(">").trim())
            i++
          }
          append("<blockquote><p>").append(formatInlineMarkdown(quoteLines.joinToString(" "))).append("</p></blockquote>\n")
        }
        trimmed.matches(Regex("^[-*+]\\s+.*")) -> {
          append("<ul>\n")
          while (i < lines.size && lines[i].trim().matches(Regex("^[-*+]\\s+.*"))) {
            val itemText = lines[i].trim().replaceFirst(Regex("^[-*+]\\s+"), "")
            append("<li>").append(formatInlineMarkdown(itemText)).append("</li>\n")
            i++
          }
          append("</ul>\n")
        }
        trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
          append("<ol>\n")
          while (i < lines.size && lines[i].trim().matches(Regex("^\\d+\\.\\s+.*"))) {
            val itemText = lines[i].trim().replaceFirst(Regex("^\\d+\\.\\s+"), "")
            append("<li>").append(formatInlineMarkdown(itemText)).append("</li>\n")
            i++
          }
          append("</ol>\n")
        }
        else -> {
          val para = mutableListOf<String>()
          while (i < lines.size) {
            val cur = lines[i].trim()
            if (cur.isEmpty() || cur.startsWith("#") || cur.startsWith(">") || cur.startsWith("```") ||
              cur.matches(Regex("^[-*+]\\s+.*")) || cur.matches(Regex("^\\d+\\.\\s+.*"))
            ) {
              break
            }
            para.add(cur)
            i++
          }
          if (para.isNotEmpty()) {
            append("<p>").append(formatInlineMarkdown(para.joinToString(" "))).append("</p>\n")
          }
        }
      }
    }
  }

  private fun formatInlineMarkdown(text: String): String {
    var s = escapeHtml(text)
    s = s.replace(Regex("""`([^`]+)`""")) { "<code>${it.groupValues[1]}</code>" }
    s = s.replace(Regex("""\*\*([^*]+)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }
    s = s.replace(Regex("""__([^_]+)__""")) { "<strong>${it.groupValues[1]}</strong>" }
    s = s.replace(Regex("""\*([^*]+)\*""")) { "<em>${it.groupValues[1]}</em>" }
    s = s.replace(Regex("""\[([^\]]+)\]\(([^)]+)\)""")) { it.groupValues[1] }
    return s
  }

  private fun buildFb2Book(
    file: File,
    cacheDir: File,
    bookId: String,
    fallbackTitle: String,
  ): Pair<ReflowableMetadata, ParsedEpubBook> {
    val xmlText = readFb2XmlText(file)
    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
    val parser = factory.newPullParser()
    parser.setInput(StringReader(xmlText))

    var title = ""
    val authors = mutableListOf<String>()
    var coverBinaryId: String? = null
    val binaries = mutableMapOf<String, ByteArray>()

    data class Fb2Section(val title: String, val htmlBody: String)
    val sections = mutableListOf<Fb2Section>()

    // Track XML parsing contexts carefully
    var inTitleInfo = false
    var inAuthor = false
    var authorFirst = ""
    var authorMiddle = ""
    var authorLast = ""
    var inCoverpage = false
    var inBody = false
    var bodySectionDepth = 0
    var currentSectionTitle = StringBuilder()
    var currentSectionHtml = StringBuilder()
    var inSectionTitle = false
    var currentBinaryId: String? = null
    val binaryBase64 = StringBuilder()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG -> {
          val tag = parser.name?.lowercase().orEmpty()
          when {
            tag == "title-info" -> inTitleInfo = true
            inTitleInfo && tag == "author" -> {
              inAuthor = true
              authorFirst = ""
              authorMiddle = ""
              authorLast = ""
            }
            inTitleInfo && tag == "coverpage" -> inCoverpage = true
            inCoverpage && tag == "image" -> {
              val href =
                (0 until parser.attributeCount)
                  .firstOrNull { parser.getAttributeName(it).endsWith("href") }
                  ?.let { parser.getAttributeValue(it) }
              if (!href.isNullOrBlank()) {
                coverBinaryId = href.removePrefix("#")
              }
            }
            inTitleInfo && tag == "book-title" -> {
              val t = safeNextText(parser).trim()
              if (t.isNotBlank() && title.isBlank()) title = t
            }
            inTitleInfo && inAuthor && tag == "first-name" -> authorFirst = safeNextText(parser).trim()
            inTitleInfo && inAuthor && tag == "middle-name" -> authorMiddle = safeNextText(parser).trim()
            inTitleInfo && inAuthor && tag == "last-name" -> authorLast = safeNextText(parser).trim()
            tag == "binary" -> {
              currentBinaryId = parser.getAttributeValue(null, "id")
              binaryBase64.clear()
            }
            tag == "body" -> {
              val bodyName = parser.getAttributeValue(null, "name").orEmpty()
              // Skip "notes" body separation only if main body exists, otherwise read normally
              inBody = bodyName.lowercase() != "notes" || sections.isEmpty()
            }
            inBody && tag == "section" -> {
              bodySectionDepth++
              if (bodySectionDepth == 1) {
                currentSectionTitle.clear()
                currentSectionHtml.clear()
              }
            }
            inBody && bodySectionDepth >= 1 && tag == "title" -> {
              inSectionTitle = true
            }
            inBody && bodySectionDepth >= 1 && inSectionTitle && tag == "p" -> {
              val headingText = collectInnerPlainText(parser, "p").trim()
              if (headingText.isNotBlank()) {
                if (currentSectionTitle.isEmpty()) currentSectionTitle.append(headingText)
                val hTag = if (bodySectionDepth == 1) "h1" else "h2"
                currentSectionHtml.append("<$hTag>").append(escapeHtml(headingText)).append("</$hTag>\n")
              }
            }
            inBody && bodySectionDepth >= 1 && !inSectionTitle -> {
              when (tag) {
                "subtitle" -> {
                  val sub = collectInnerPlainText(parser, "subtitle").trim()
                  if (sub.isNotBlank()) {
                    currentSectionHtml.append("<h3>").append(escapeHtml(sub)).append("</h3>\n")
                  }
                }
                "epigraph", "cite" -> {
                  currentSectionHtml.append("<blockquote>")
                }
                "p", "v" -> {
                  val inline = collectInnerInlineHtml(parser, tag).trim()
                  if (inline.isNotBlank()) {
                    currentSectionHtml.append("<p>").append(inline).append("</p>\n")
                  }
                }
                "empty-line" -> currentSectionHtml.append("<br/>\n")
              }
            }
          }
        }

        XmlPullParser.TEXT -> {
          if (currentBinaryId != null) {
            binaryBase64.append(parser.text.orEmpty())
          }
        }

        XmlPullParser.END_TAG -> {
          val tag = parser.name?.lowercase().orEmpty()
          when {
            tag == "title-info" -> inTitleInfo = false
            inTitleInfo && tag == "author" -> {
              inAuthor = false
              val full = listOf(authorFirst, authorMiddle, authorLast).filter { it.isNotBlank() }.joinToString(" ")
              if (full.isNotBlank()) authors.add(full)
            }
            inTitleInfo && tag == "coverpage" -> inCoverpage = false
            tag == "binary" -> {
              val id = currentBinaryId
              if (!id.isNullOrBlank()) {
                runCatching {
                  val cleanB64 = binaryBase64.toString().replace("\\s+".toRegex(), "")
                  val decoded = Base64.decode(cleanB64, Base64.DEFAULT)
                  if (decoded.isNotEmpty()) binaries[id] = decoded
                }
              }
              currentBinaryId = null
              binaryBase64.clear()
            }
            tag == "body" -> inBody = false
            inBody && tag == "title" -> inSectionTitle = false
            inBody && (tag == "epigraph" || tag == "cite") -> {
              currentSectionHtml.append("</blockquote>\n")
            }
            inBody && tag == "section" -> {
              if (bodySectionDepth == 1) {
                val secHtml = currentSectionHtml.toString()
                if (secHtml.isNotBlank()) {
                  val secTitle =
                    currentSectionTitle.toString().trim().ifBlank { "Chapter ${sections.size + 1}" }
                  sections.add(Fb2Section(secTitle, secHtml))
                }
              }
              bodySectionDepth = (bodySectionDepth - 1).coerceAtLeast(0)
            }
          }
        }
      }
      event = parser.next()
    }

    val finalTitle = title.ifBlank { fallbackTitle }
    val finalAuthor = authors.joinToString(", ").ifBlank { "FictionBook (FB2)" }
    val coverBytes = coverBinaryId?.let { binaries[it] } ?: binaries.values.firstOrNull()
    val savedCoverPath = coverBytes?.let { saveCompressedCover(it, bookId) }

    val nonEmptySections =
      if (sections.isNotEmpty()) sections
      else listOf(Fb2Section(finalTitle, "<p>${escapeHtml(finalTitle)}</p>"))

    val isRtl = isPredominantlyRtl(nonEmptySections.joinToString(" ") { it.htmlBody })
    val chapters = mutableListOf<EpubChapter>()
    val toc = mutableListOf<EpubTocItem>()
    nonEmptySections.forEachIndexed { idx, sec ->
      val fileName = "fb2_${idx + 1}.xhtml"
      val outFile = File(cacheDir, fileName)
      outFile.writeText(wrapXhtml(sec.title, sec.htmlBody, isRtl))
      chapters.add(
        EpubChapter(
          index = idx,
          id = "fb2_$idx",
          title = sec.title,
          href = fileName,
          localFilePath = outFile.absolutePath,
        )
      )
      toc.add(EpubTocItem(title = sec.title, chapterIndex = idx, level = 0))
    }

    writeManifestCache(cacheDir, finalTitle, finalAuthor, chapters, toc)
    val parsed = ParsedEpubBook(finalTitle, finalAuthor, chapters, toc, cacheDir.absolutePath)
    val meta = ReflowableMetadata(finalTitle, finalAuthor, chapters.size, coverPath = savedCoverPath)
    return meta to parsed
  }

  private fun readFb2XmlText(file: File): String {
    val header = ByteArray(4)
    val count = file.inputStream().use { it.read(header) }
    if (count >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
      ZipFile(file).use { zip ->
        val fb2Entry =
          zip.entries().asSequence().firstOrNull { !it.isDirectory && it.name.lowercase().endsWith(".fb2") }
            ?: zip.entries().asSequence().firstOrNull { !it.isDirectory }
            ?: error("Compressed FB2 archive contains no readable XML entry.")
        return zip.getInputStream(fb2Entry).bufferedReader().use { it.readText() }
      }
    }
    return readTextSmart(file)
  }

  private fun collectInnerPlainText(parser: XmlPullParser, stopTag: String): String {
    val sb = StringBuilder()
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.START_TAG -> depth++
        XmlPullParser.END_TAG -> {
          depth--
          if (depth == 0 && parser.name.equals(stopTag, ignoreCase = true)) break
        }
        XmlPullParser.TEXT -> sb.append(parser.text.orEmpty())
        XmlPullParser.END_DOCUMENT -> break
      }
    }
    return sb.toString()
  }

  private fun collectInnerInlineHtml(parser: XmlPullParser, stopTag: String): String {
    val sb = StringBuilder()
    var depth = 1
    while (depth > 0) {
      when (parser.next()) {
        XmlPullParser.START_TAG -> {
          depth++
          when (parser.name?.lowercase()) {
            "emphasis" -> sb.append("<em>")
            "strong" -> sb.append("<strong>")
            "code" -> sb.append("<code>")
          }
        }
        XmlPullParser.END_TAG -> {
          depth--
          when (parser.name?.lowercase()) {
            "emphasis" -> sb.append("</em>")
            "strong" -> sb.append("</strong>")
            "code" -> sb.append("</code>")
          }
          if (depth == 0 && parser.name.equals(stopTag, ignoreCase = true)) break
        }
        XmlPullParser.TEXT -> sb.append(escapeHtml(parser.text.orEmpty()))
        XmlPullParser.END_DOCUMENT -> break
      }
    }
    return sb.toString()
  }

  private fun safeNextText(parser: XmlPullParser): String {
    return runCatching { parser.nextText() }.getOrDefault("")
  }

  private fun writeManifestCache(
    cacheDir: File,
    title: String,
    author: String,
    chapters: List<EpubChapter>,
    toc: List<EpubTocItem>,
  ) {
    val indexLines =
      mutableListOf(
        title.replace('\n', ' '),
        author.replace('\n', ' '),
      )
    chapters.forEach { ch ->
      indexLines.add("${ch.id}\t${ch.href}\t${ch.title.replace('\t', ' ').replace('\n', ' ')}")
    }
    File(cacheDir, ".manifest.txt").writeText(indexLines.joinToString("\n"))
    File(cacheDir, ".unpacked_v1").writeText("1")
  }

  private fun loadFromCache(cacheDir: File, fallbackTitle: String): ParsedEpubBook? {
    val manifest = File(cacheDir, ".manifest.txt")
    if (!manifest.exists()) return null
    val lines = manifest.readLines()
    if (lines.size < 3) return null
    val title = lines[0].ifBlank { fallbackTitle }
    val author = lines[1].ifBlank { "ATLAS" }
    val chapters = mutableListOf<EpubChapter>()
    val toc = mutableListOf<EpubTocItem>()
    lines.drop(2).forEachIndexed { idx, row ->
      val parts = row.split('\t')
      if (parts.size >= 3) {
        val href = parts[1]
        val chFile = File(cacheDir, href)
        if (!chFile.exists()) return null
        val chTitle = parts[2]
        chapters.add(EpubChapter(idx, parts[0], chTitle, href, chFile.absolutePath))
        toc.add(EpubTocItem(chTitle, idx, level = 0))
      }
    }
    if (chapters.isEmpty()) return null
    return ParsedEpubBook(title, author, chapters, toc, cacheDir.absolutePath)
  }

  private fun wrapXhtml(title: String, bodyHtml: String, isRtl: Boolean): String {
    val dirAttr = if (isRtl) " dir=\"rtl\" lang=\"fa\"" else " lang=\"en\""
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <html xmlns="http://www.w3.org/1999/xhtml"$dirAttr>
      <head><title>${escapeHtml(title)}</title></head>
      <body>
      $bodyHtml
      </body>
      </html>
    """.trimIndent()
  }

  private fun readTextSmart(file: File): String {
    val bytes = file.readBytes()
    if (bytes.size >= 2) {
      if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE"))
      }
      if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE"))
      }
    }
    val offset =
      if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3
      else 0
    return String(bytes, offset, bytes.size - offset, Charsets.UTF_8)
  }

  private fun isPredominantlyRtl(sample: String): Boolean {
    var rtl = 0
    var latin = 0
    for (ch in sample.take(2500)) {
      val dir = Character.getDirectionality(ch)
      if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
        rtl++
      } else if (ch in 'A'..'Z' || ch in 'a'..'z') {
        latin++
      }
    }
    return rtl > latin && rtl > 10
  }

  private fun escapeHtml(text: String): String =
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

  private fun saveCompressedCover(bytes: ByteArray, bookId: String): String? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      val maxWidth = 480
      var sample = 1
      while (bounds.outWidth / sample > maxWidth * 2) {
        sample *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
      val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
      val scaled =
        if (decoded.width > maxWidth) {
          val h = (decoded.height.toFloat() * maxWidth / decoded.width).toInt().coerceAtLeast(1)
          Bitmap.createScaledBitmap(decoded, maxWidth, h, true).also {
            if (it != decoded) decoded.recycle()
          }
        } else {
          decoded
        }
      val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
      val out = File(coversDir, "${bookId}_cover.jpg")
      FileOutputStream(out).use { fos -> scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos) }
      scaled.recycle()
      out.absolutePath
    }.getOrNull()
}
