package com.example.engine.writer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.local.WriterBookEntity
import com.example.data.local.WriterChapterEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Packages manuscripts authored in PageNest Studio (Beta) into standards-compliant EPUB 3.0 books
 * with auto-generated editorial cover art, TOC, RTL/LTR metadata, and clean XHTML formatting.
 */
object WriterEpubExporter {

  fun countWords(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }
  }

  fun buildMarkdownExport(book: WriterBookEntity, chapters: List<WriterChapterEntity>): String =
    buildString {
      append("# ").append(book.title.trim()).append('\n')
      if (book.author.isNotBlank()) {
        append("*by ").append(book.author.trim()).append("*\n")
      }
      if (book.synopsis.isNotBlank()) {
        append("\n> ").append(book.synopsis.trim().replace("\n", "\n> ")).append('\n')
      }
      append('\n')
      chapters.sortedBy { it.orderIndex }.forEach { ch ->
        append("---\n\n")
        append("## ").append(ch.title.trim()).append("\n\n")
        append(ch.bodyText.trim()).append("\n\n")
      }
    }

  suspend fun buildEpubFile(
    context: Context,
    book: WriterBookEntity,
    chapters: List<WriterChapterEntity>,
  ): File =
    withContext(Dispatchers.IO) {
      val orderedChapters =
        chapters.sortedBy { it.orderIndex }.ifEmpty {
          listOf(
            WriterChapterEntity(
              bookId = book.id,
              orderIndex = 0,
              title = if (book.isRtl) "فصل اول" else "Chapter 1",
              bodyText = "",
            )
          )
        }

      val exportDir = File(context.cacheDir, "studio_export").apply { mkdirs() }
      val epubFile = File(exportDir, "studio_${book.id}.epub")
      val coverBytes = renderStudioCoverBytes(book)
      val safeTitle = escapeXml(book.title.ifBlank { "Untitled Draft" })
      val safeAuthor = escapeXml(book.author.ifBlank { "PageNest Author" })
      val langCode = if (book.isRtl) "fa" else "en"
      val spineDirAttr = if (book.isRtl) """ page-progression-direction="rtl"""" else ""

      ZipOutputStream(FileOutputStream(epubFile)).use { zip ->
        // 1. Stored mimetype entry (required first entry per EPUB spec)
        val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
        val mimeEntry =
          ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            crc = CRC32().apply { update(mimetypeBytes) }.value
          }
        zip.putNextEntry(mimeEntry)
        zip.write(mimetypeBytes)
        zip.closeEntry()

        // 2. container.xml
        writeUtf8Entry(
          zip,
          "META-INF/container.xml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            <rootfiles>
              <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            </rootfiles>
          </container>
          """.trimIndent().trimStart(),
        )

        // 3. Cover JPEG
        zip.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
        zip.write(coverBytes)
        zip.closeEntry()

        // 4. content.opf
        val manifestItems =
          orderedChapters.indices.joinToString("\n      ") { idx ->
            """<item id="ch${idx + 1}" href="chapter_${idx + 1}.xhtml" media-type="application/xhtml+xml"/>"""
          }
        val spineRefs =
          orderedChapters.indices.joinToString("\n      ") { idx ->
            """<itemref idref="ch${idx + 1}"/>"""
          }
        val descriptionMeta =
          if (book.synopsis.isNotBlank()) {
            "\n      <dc:description>${escapeXml(book.synopsis.trim())}</dc:description>"
          } else {
            ""
          }

        writeUtf8Entry(
          zip,
          "OEBPS/content.opf",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="$langCode" unique-identifier="StudioBookId">
            <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
              <dc:identifier id="StudioBookId">urn:uuid:pagenest-studio-${escapeXml(book.id)}</dc:identifier>
              <dc:title>$safeTitle</dc:title>
              <dc:creator>$safeAuthor</dc:creator>
              <dc:language>$langCode</dc:language>$descriptionMeta
              <meta name="cover" content="cover-img"/>
            </metadata>
            <manifest>
              <item id="cover-img" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>
              <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
              $manifestItems
            </manifest>
            <spine$spineDirAttr>
              $spineRefs
            </spine>
          </package>
          """.trimIndent().trimStart(),
        )

        // 5. nav.xhtml
        val navDirAttr = if (book.isRtl) """ dir="rtl" lang="fa"""" else """ lang="en""""
        val tocTitle = if (book.isRtl) "فهرست مطالب" else "Contents"
        val navLinks =
          orderedChapters.mapIndexed { idx, ch ->
            val chTitle = escapeXml(ch.title.ifBlank { "Chapter ${idx + 1}" })
            """<li><a href="chapter_${idx + 1}.xhtml">$chTitle</a></li>"""
          }.joinToString("\n        ")

        writeUtf8Entry(
          zip,
          "OEBPS/nav.xhtml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"$navDirAttr>
          <head><title>$tocTitle</title></head>
          <body>
            <nav epub:type="toc" id="toc">
              <h1>$tocTitle</h1>
              <ol>
                $navLinks
              </ol>
            </nav>
          </body>
          </html>
          """.trimIndent().trimStart(),
        )

        // 6. Chapter XHTML files
        orderedChapters.forEachIndexed { idx, ch ->
          val xhtml = chapterToXhtml(ch, idx, book.isRtl)
          writeUtf8Entry(zip, "OEBPS/chapter_${idx + 1}.xhtml", xhtml)
        }
      }
      epubFile
    }

  private fun chapterToXhtml(
    chapter: WriterChapterEntity,
    index: Int,
    bookIsRtl: Boolean,
  ): String {
    val chTitle = chapter.title.trim().ifBlank { if (bookIsRtl) "فصل ${index + 1}" else "Chapter ${index + 1}" }
    val dirAttr = if (bookIsRtl || hasRtlChars(chapter.bodyText)) """ dir="rtl" lang="fa"""" else """ lang="en""""
    val bodyDir = if (bookIsRtl || hasRtlChars(chapter.bodyText)) """ dir="rtl"""" else ""
    val bodyHtml = markdownBodyToHtml(chTitle, chapter.bodyText)

    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <html xmlns="http://www.w3.org/1999/xhtml"$dirAttr>
      <head><title>${escapeXml(chTitle)}</title></head>
      <body$bodyDir>
        $bodyHtml
      </body>
      </html>
    """.trimIndent().trimStart()
  }

  private fun markdownBodyToHtml(chapterTitle: String, rawBody: String): String {
    val sb = StringBuilder()
    sb.append("<h1>").append(escapeXml(chapterTitle)).append("</h1>\n")
    val lines = rawBody.replace("\r\n", "\n").replace('\r', '\n').lines()
    val para = StringBuilder()
    var inList = false

    fun flushPara() {
      val content = para.toString().trim()
      if (content.isNotEmpty()) {
        sb.append("<p>").append(formatInlineMarkdown(content)).append("</p>\n")
      }
      para.clear()
    }

    fun closeListIfNeeded() {
      if (inList) {
        sb.append("</ul>\n")
        inList = false
      }
    }

    for (rawLine in lines) {
      val line = rawLine.trimEnd()
      val trimmed = line.trim()
      when {
        trimmed.isBlank() -> {
          flushPara()
          closeListIfNeeded()
        }
        trimmed == "---" || trimmed == "***" -> {
          flushPara()
          closeListIfNeeded()
          sb.append("<hr/>\n")
        }
        trimmed.startsWith("### ") -> {
          flushPara()
          closeListIfNeeded()
          sb.append("<h3>").append(formatInlineMarkdown(trimmed.drop(4).trim())).append("</h3>\n")
        }
        trimmed.startsWith("## ") -> {
          flushPara()
          closeListIfNeeded()
          sb.append("<h2>").append(formatInlineMarkdown(trimmed.drop(3).trim())).append("</h2>\n")
        }
        trimmed.startsWith("# ") -> {
          flushPara()
          closeListIfNeeded()
          sb.append("<h2>").append(formatInlineMarkdown(trimmed.drop(2).trim())).append("</h2>\n")
        }
        trimmed.startsWith("> ") -> {
          flushPara()
          closeListIfNeeded()
          sb.append("<blockquote>").append(formatInlineMarkdown(trimmed.drop(2).trim())).append("</blockquote>\n")
        }
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
          flushPara()
          if (!inList) {
            sb.append("<ul>\n")
            inList = true
          }
          sb.append("<li>").append(formatInlineMarkdown(trimmed.drop(2).trim())).append("</li>\n")
        }
        else -> {
          closeListIfNeeded()
          if (para.isNotEmpty()) para.append("<br/>")
          para.append(trimmed)
        }
      }
    }
    flushPara()
    closeListIfNeeded()
    return sb.toString()
  }

  private fun formatInlineMarkdown(raw: String): String {
    var escaped = escapeXml(raw)
    escaped = escaped.replace(Regex("""\*\*(.+?)\*\*""")) { m -> "<strong>${m.groupValues[1]}</strong>" }
    escaped = escaped.replace(Regex("""\*(.+?)\*""")) { m -> "<em>${m.groupValues[1]}</em>" }
    return escaped
  }

  private fun hasRtlChars(text: String): Boolean =
    text.any { ch ->
      val code = ch.code
      (code in 0x0600..0x06FF) || (code in 0x0750..0x077F) || (code in 0xFB50..0xFDFF) || (code in 0xFE70..0xFEFF)
    }

  private fun renderStudioCoverBytes(book: WriterBookEntity): ByteArray {
    val palettes =
      listOf(
        Triple("#1F3A2F", "#12221B", "#7BC6A0"), // Deep Botanical Green
        Triple("#2C2242", "#171126", "#B7A6EB"), // Midnight Violet
        Triple("#4D2B22", "#281510", "#E8AE98"), // Warm Terracotta
        Triple("#1F3340", "#111D25", "#8AC0DB"), // Slate Ocean
      )
    val palette = palettes[abs(book.id.hashCode()) % palettes.size]
    val bmp = Bitmap.createBitmap(420, 620, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint =
      Paint().apply {
        shader =
          LinearGradient(
            0f,
            0f,
            420f,
            620f,
            intArrayOf(Color.parseColor(palette.first), Color.parseColor(palette.second)),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, 420f, 620f, bgPaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor(palette.third)
        alpha = 140
      }
    canvas.drawRect(26f, 26f, 394f, 594f, borderPaint)

    val badgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(palette.third)
        textSize = 14f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    canvas.drawText("PAGENEST STUDIO", 48f, 76f, badgePaint)

    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAF8F4")
        textSize = 36f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      }
    val titleText = book.title.trim().ifBlank { "Untitled Book" }
    val alignment =
      if (book.isRtl || hasRtlChars(titleText)) Layout.Alignment.ALIGN_OPPOSITE
      else Layout.Alignment.ALIGN_NORMAL
    val titleLayout =
      StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, 324)
        .setAlignment(alignment)
        .setMaxLines(4)
        .setLineSpacing(4f, 1.05f)
        .build()

    canvas.save()
    canvas.translate(48f, 180f)
    titleLayout.draw(canvas)
    canvas.restore()

    val authorPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(palette.third)
        textSize = 19f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      }
    val authorText = book.author.trim().ifBlank { "PageNest Writer" }
    val authorLayout =
      StaticLayout.Builder.obtain(authorText, 0, authorText.length, authorPaint, 324)
        .setAlignment(alignment)
        .setMaxLines(2)
        .build()
    canvas.save()
    canvas.translate(48f, 515f)
    authorLayout.draw(canvas)
    canvas.restore()

    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    bmp.recycle()
    return baos.toByteArray()
  }

  private fun writeUtf8Entry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
  }

  private fun escapeXml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
}
