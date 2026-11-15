package com.example.engine.epub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.R
import com.example.model.BookSearchMatch
import com.example.model.EpubChapter
import com.example.model.EpubTocItem
import com.example.model.ParsedEpubBook
import com.example.model.ReaderFontOption
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.URLDecoder
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class EpubParser(private val context: Context) {

  data class EpubMetadata(
    val title: String,
    val author: String,
    val totalChapters: Int,
    val coverImageEntryPath: String?,
  )

  suspend fun ensureFontsCopied(): File =
    withContext(Dispatchers.IO) {
      val fontsDir = File(context.filesDir, "reader_fonts").apply { mkdirs() }
      val fontMap =
        mapOf(
          ReaderFontOption.LITERATA.fontFileName to R.font.literata,
          ReaderFontOption.LORA.fontFileName to R.font.lora,
          ReaderFontOption.NEWSREADER.fontFileName to R.font.newsreader,
          ReaderFontOption.VAZIRMATN.fontFileName to R.font.vazirmatn,
          ReaderFontOption.NOTO_NASKH.fontFileName to R.font.noto_naskh_arabic,
          ReaderFontOption.PLUS_JAKARTA.fontFileName to R.font.plus_jakarta_sans,
          ReaderFontOption.JETBRAINS_MONO.fontFileName to R.font.jetbrains_mono,
        )
      fontMap.forEach { (fileName, resId) ->
        val outFile = File(fontsDir, fileName)
        if (!outFile.exists() || outFile.length() == 0L) {
          context.resources.openRawResource(resId).use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
          }
        }
      }
      fontsDir
    }

  suspend fun extractMetadataAndCover(
    epubFile: File,
    bookId: String,
    fallbackTitle: String,
  ): Result<Pair<EpubMetadata, String?>> =
    withContext(Dispatchers.IO) {
      runCatching {
        ZipFile(epubFile).use { zip ->
          val opfPath = findOpfPath(zip)
            ?: error("Invalid EPUB: missing container.xml or OPF package manifest.")
          val opfEntry = zip.getEntry(opfPath)
            ?: error("Invalid EPUB: package document not found at $opfPath")
          val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
          val opfDir = opfPath.substringBeforeLast('/', "")

          val parsedOpf = parseOpf(opfXml, opfDir, fallbackTitle)
          var savedCoverPath: String? = null
          val coverZipPath = parsedOpf.coverHref?.let { resolveZipPath(opfDir, it) }
          if (coverZipPath != null) {
            val coverEntry = zip.getEntry(coverZipPath)
            if (coverEntry != null) {
              savedCoverPath = saveCompressedCover(zip.getInputStream(coverEntry).readBytes(), bookId)
            }
          }

          val metadata =
            EpubMetadata(
              title = parsedOpf.title.ifBlank { fallbackTitle },
              author = parsedOpf.author.ifBlank { "Unknown Author" },
              totalChapters = parsedOpf.spineHrefs.size.coerceAtLeast(1),
              coverImageEntryPath = coverZipPath,
            )
          metadata to savedCoverPath
        }
      }
    }

  suspend fun loadBook(epubFile: File, bookId: String, fallbackTitle: String): Result<ParsedEpubBook> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!epubFile.exists() || epubFile.length() == 0L) {
          error("Book file could not be found on device storage.")
        }
        val cacheDir = File(context.filesDir, "epub_cache/$bookId")
        val marker = File(cacheDir, ".unpacked_ok")
        if (!marker.exists()) {
          cacheDir.deleteRecursively()
          cacheDir.mkdirs()
          unpackEpubSafely(epubFile, cacheDir)
          marker.writeText("1")
        }

        val opfRelativePath = findOpfPathFromDisk(cacheDir)
          ?: error("Malformed EPUB: OPF package file could not be located.")
        val opfFile = File(cacheDir, opfRelativePath)
        if (!opfFile.exists()) error("Malformed EPUB: OPF manifest missing.")
        val opfDir = opfRelativePath.substringBeforeLast('/', "")
        val parsedOpf = parseOpf(opfFile.readText(), opfDir, fallbackTitle)

        val chapters =
          parsedOpf.spineItems.mapIndexedNotNull { idx, item ->
            val relPath = resolveZipPath(opfDir, item.href)
            val chapterFile = File(cacheDir, relPath)
            if (chapterFile.exists()) {
              EpubChapter(
                index = idx,
                id = item.id,
                title = extractHtmlTitle(chapterFile) ?: "Section ${idx + 1}",
                href = relPath,
                localFilePath = chapterFile.absolutePath,
              )
            } else {
              null
            }
          }

        if (chapters.isEmpty()) {
          error("This EPUB contains no readable chapters.")
        }

        // Re-index cleanly after filtering missing files
        val normalizedChapters = chapters.mapIndexed { idx, ch -> ch.copy(index = idx) }
        val toc =
          parseTableOfContents(cacheDir, opfDir, parsedOpf, normalizedChapters)
            .ifEmpty {
              normalizedChapters.map { ch ->
                EpubTocItem(title = ch.title, chapterIndex = ch.index)
              }
            }

        // Refine chapter titles using TOC when HTML <title> was generic
        val tocTitleByChapter = toc.distinctBy { it.chapterIndex }.associateBy { it.chapterIndex }
        val finalChapters =
          normalizedChapters.map { ch ->
            val tocTitle = tocTitleByChapter[ch.index]?.title
            if (!tocTitle.isNullOrBlank()) ch.copy(title = tocTitle) else ch
          }

        ParsedEpubBook(
          title = parsedOpf.title.ifBlank { fallbackTitle },
          author = parsedOpf.author.ifBlank { "Unknown Author" },
          chapters = finalChapters,
          toc = toc,
          baseDirPath = cacheDir.absolutePath,
        )
      }
    }

  suspend fun extractChapterPlainText(chapter: EpubChapter): String =
    withContext(Dispatchers.IO) {
      runCatching {
        val raw = File(chapter.localFilePath).readText()
        stripHtmlToReadableText(raw)
      }.getOrDefault("")
    }

  suspend fun searchInBook(
    chapters: List<EpubChapter>,
    query: String,
  ): List<BookSearchMatch> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@withContext emptyList()
      val results = mutableListOf<BookSearchMatch>()
      for (chapter in chapters) {
        val text = extractChapterPlainText(chapter)
        var searchFrom = 0
        var hitsInChapter = 0
        while (hitsInChapter < 5 && results.size < 60) {
          val idx = text.indexOf(trimmed, startIndex = searchFrom, ignoreCase = true)
          if (idx < 0) break
          val start = (idx - 42).coerceAtLeast(0)
          val end = (idx + trimmed.length + 72).coerceAtMost(text.length)
          val prefix = if (start > 0) "…" else ""
          val suffix = if (end < text.length) "…" else ""
          val excerpt = prefix + text.substring(start, end).replace('\n', ' ').trim() + suffix
          results.add(
            BookSearchMatch(
              chapterOrPageIndex = chapter.index,
              locationLabel = chapter.title,
              excerpt = excerpt,
              query = trimmed,
            )
          )
          hitsInChapter++
          searchFrom = idx + trimmed.length
        }
        if (results.size >= 60) break
      }
      results
    }

  private fun unpackEpubSafely(epubFile: File, destDir: File) {
    val canonicalDest = destDir.canonicalPath + File.separator
    ZipFile(epubFile).use { zip ->
      val entries = zip.entries()
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        val outFile = File(destDir, entry.name)
        val canonicalOut = outFile.canonicalPath
        if (!canonicalOut.startsWith(canonicalDest) && canonicalOut != destDir.canonicalPath) {
          throw SecurityException("Blocked unsafe zip entry path: ${entry.name}")
        }
        if (entry.isDirectory) {
          outFile.mkdirs()
        } else {
          outFile.parentFile?.mkdirs()
          zip.getInputStream(entry).use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
          }
        }
      }
    }
  }

  private fun saveCompressedCover(bytes: ByteArray, bookId: String): String? {
    return runCatching {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
      val maxWidth = 480
      var sampleSize = 1
      while (options.outWidth / sampleSize > maxWidth * 2) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
      val scaled =
        if (decoded.width > maxWidth) {
          val targetHeight = (decoded.height.toFloat() * maxWidth / decoded.width).toInt().coerceAtLeast(1)
          Bitmap.createScaledBitmap(decoded, maxWidth, targetHeight, true).also {
            if (it != decoded) decoded.recycle()
          }
        } else {
          decoded
        }
      val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
      val coverFile = File(coversDir, "$bookId.jpg")
      FileOutputStream(coverFile).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
      }
      scaled.recycle()
      coverFile.absolutePath
    }.getOrNull()
  }

  private fun findOpfPath(zip: ZipFile): String? {
    val containerEntry = zip.getEntry("META-INF/container.xml")
    if (containerEntry != null) {
      val xml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
      val path = parseContainerXml(xml)
      if (!path.isNullOrBlank() && zip.getEntry(path) != null) return path
    }
    val entries = zip.entries()
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement()
      if (entry.name.endsWith(".opf", ignoreCase = true)) return entry.name
    }
    return null
  }

  private fun findOpfPathFromDisk(cacheDir: File): String? {
    val containerFile = File(cacheDir, "META-INF/container.xml")
    if (containerFile.exists()) {
      val path = parseContainerXml(containerFile.readText())
      if (!path.isNullOrBlank() && File(cacheDir, path).exists()) return path
    }
    return cacheDir
      .walkTopDown()
      .firstOrNull { it.isFile && it.extension.equals("opf", ignoreCase = true) }
      ?.relativeTo(cacheDir)
      ?.path
  }

  private fun parseContainerXml(xml: String): String? {
    return runCatching {
      val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
      val parser = factory.newPullParser()
      parser.setInput(StringReader(xml))
      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':') == "rootfile") {
          val fullPath = parser.getAttributeValue(null, "full-path")
          if (!fullPath.isNullOrBlank()) return fullPath
        }
        event = parser.next()
      }
      null
    }.getOrNull()
  }

  private data class ManifestItem(
    val id: String,
    val href: String,
    val mediaType: String,
    val properties: String,
  )

  private data class OpfParseResult(
    val title: String,
    val author: String,
    val coverHref: String?,
    val ncxHref: String?,
    val navHref: String?,
    val spineHrefs: List<String>,
    val spineItems: List<ManifestItem>,
  )

  private fun parseOpf(xml: String, opfDir: String, fallbackTitle: String): OpfParseResult {
    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
    val parser = factory.newPullParser()
    parser.setInput(StringReader(xml))

    var title = ""
    var author = ""
    var coverMetaId: String? = null
    var tocAttrId: String? = null
    val manifest = LinkedHashMap<String, ManifestItem>()
    val spineIds = mutableListOf<String>()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      if (event == XmlPullParser.START_TAG) {
        val tag = parser.name.substringAfter(':').lowercase()
        when (tag) {
          "title" -> {
            val text = runCatching { parser.nextText().trim() }.getOrDefault("")
            if (title.isBlank() && text.isNotBlank()) title = text
          }
          "creator" -> {
            val text = runCatching { parser.nextText().trim() }.getOrDefault("")
            if (author.isBlank() && text.isNotBlank()) author = text
          }
          "meta" -> {
            val name = parser.getAttributeValue(null, "name")
            val content = parser.getAttributeValue(null, "content")
            if (name.equals("cover", ignoreCase = true) && !content.isNullOrBlank()) {
              coverMetaId = content
            }
          }
          "item" -> {
            val id = parser.getAttributeValue(null, "id") ?: ""
            val href = decodeHref(parser.getAttributeValue(null, "href") ?: "")
            val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
            val props = parser.getAttributeValue(null, "properties") ?: ""
            if (id.isNotBlank() && href.isNotBlank()) {
              manifest[id] = ManifestItem(id, href, mediaType, props)
            }
          }
          "spine" -> {
            tocAttrId = parser.getAttributeValue(null, "toc")
          }
          "itemref" -> {
            val idref = parser.getAttributeValue(null, "idref")
            val linear = parser.getAttributeValue(null, "linear")
            if (!idref.isNullOrBlank() && !linear.equals("no", ignoreCase = true)) {
              spineIds.add(idref)
            }
          }
        }
      }
      event = parser.next()
    }

    val spineItems =
      spineIds.mapNotNull { manifest[it] }.ifEmpty {
        manifest.values.filter {
          it.mediaType.contains("html", ignoreCase = true) ||
            it.href.endsWith(".xhtml", true) ||
            it.href.endsWith(".html", true)
        }
      }

    val coverItem =
      coverMetaId?.let { manifest[it] }
        ?: manifest.values.firstOrNull { it.properties.contains("cover-image", ignoreCase = true) }
        ?: manifest.values.firstOrNull {
          it.mediaType.startsWith("image/") && it.id.contains("cover", ignoreCase = true)
        }

    val navItem =
      manifest.values.firstOrNull { it.properties.split(" ").contains("nav") }

    val ncxItem =
      tocAttrId?.let { manifest[it] }
        ?: manifest.values.firstOrNull {
          it.mediaType.equals("application/x-dtbncx+xml", ignoreCase = true) ||
            it.href.endsWith(".ncx", ignoreCase = true)
        }

    return OpfParseResult(
      title = title.ifBlank { fallbackTitle },
      author = author,
      coverHref = coverItem?.href,
      ncxHref = ncxItem?.href,
      navHref = navItem?.href,
      spineHrefs = spineItems.map { resolveZipPath(opfDir, it.href) },
      spineItems = spineItems,
    )
  }

  private fun parseTableOfContents(
    cacheDir: File,
    opfDir: String,
    opf: OpfParseResult,
    chapters: List<EpubChapter>,
  ): List<EpubTocItem> {
    // Try EPUB 3 nav.xhtml first
    opf.navHref?.let { navHref ->
      val navPath = resolveZipPath(opfDir, navHref)
      val navFile = File(cacheDir, navPath)
      if (navFile.exists()) {
        val navDir = navPath.substringBeforeLast('/', "")
        val items = parseNavXhtml(navFile.readText(), navDir, chapters)
        if (items.isNotEmpty()) return items
      }
    }
    // Fallback to EPUB 2 toc.ncx
    opf.ncxHref?.let { ncxHref ->
      val ncxPath = resolveZipPath(opfDir, ncxHref)
      val ncxFile = File(cacheDir, ncxPath)
      if (ncxFile.exists()) {
        val ncxDir = ncxPath.substringBeforeLast('/', "")
        val items = parseNcx(ncxFile.readText(), ncxDir, chapters)
        if (items.isNotEmpty()) return items
      }
    }
    return emptyList()
  }

  private fun parseNavXhtml(
    xml: String,
    navDir: String,
    chapters: List<EpubChapter>,
  ): List<EpubTocItem> {
    return runCatching {
      val results = mutableListOf<EpubTocItem>()
      val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
      val parser = factory.newPullParser()
      parser.setInput(StringReader(xml))

      var insideTocNav = false
      var navDepth = 0
      var listDepth = 0
      var currentHref: String? = null
      val labelBuilder = StringBuilder()
      var collectingAnchor = false

      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
          XmlPullParser.START_TAG -> {
            val tag = parser.name.substringAfter(':').lowercase()
            if (tag == "nav") {
              val typeAttr =
                parser.getAttributeValue(null, "epub:type")
                  ?: parser.getAttributeValue(null, "type")
                  ?: parser.getAttributeValue(null, "role")
                  ?: ""
              if (typeAttr.contains("toc", ignoreCase = true) || results.isEmpty()) {
                insideTocNav = true
                navDepth = 1
              } else if (insideTocNav) {
                navDepth++
              }
            } else if (insideTocNav) {
              if (tag == "ol" || tag == "ul") listDepth++
              if (tag == "a") {
                currentHref = parser.getAttributeValue(null, "href")
                labelBuilder.clear()
                collectingAnchor = true
              }
            }
          }
          XmlPullParser.TEXT -> {
            if (collectingAnchor) {
              labelBuilder.append(parser.text)
            }
          }
          XmlPullParser.END_TAG -> {
            val tag = parser.name.substringAfter(':').lowercase()
            if (insideTocNav) {
              if (tag == "a" && collectingAnchor) {
                collectingAnchor = false
                val title = labelBuilder.toString().replace(Regex("\\s+"), " ").trim()
                val href = currentHref
                if (title.isNotBlank() && !href.isNullOrBlank()) {
                  matchHrefToChapter(href, navDir, chapters)?.let { (idx, anchor) ->
                    results.add(
                      EpubTocItem(
                        title = title,
                        chapterIndex = idx,
                        anchor = anchor,
                        level = (listDepth - 1).coerceIn(0, 3),
                      )
                    )
                  }
                }
              }
              if (tag == "ol" || tag == "ul") listDepth = (listDepth - 1).coerceAtLeast(0)
              if (tag == "nav") {
                navDepth--
                if (navDepth <= 0) insideTocNav = false
              }
            }
          }
        }
        event = parser.next()
      }
      results
    }.getOrDefault(emptyList())
  }

  private fun parseNcx(
    xml: String,
    ncxDir: String,
    chapters: List<EpubChapter>,
  ): List<EpubTocItem> {
    return runCatching {
      val results = mutableListOf<EpubTocItem>()
      val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
      val parser = factory.newPullParser()
      parser.setInput(StringReader(xml))

      var navPointDepth = 0
      var insideNavLabel = false
      var currentTitle = ""
      var currentSrc = ""

      var event = parser.eventType
      while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
          XmlPullParser.START_TAG -> {
            val tag = parser.name.substringAfter(':').lowercase()
            when (tag) {
              "navpoint" -> {
                navPointDepth++
                currentTitle = ""
                currentSrc = ""
              }
              "navlabel" -> insideNavLabel = true
              "text" -> {
                if (insideNavLabel) {
                  currentTitle = runCatching { parser.nextText().trim() }.getOrDefault("")
                }
              }
              "content" -> {
                currentSrc = parser.getAttributeValue(null, "src") ?: ""
                if (currentTitle.isNotBlank() && currentSrc.isNotBlank()) {
                  matchHrefToChapter(currentSrc, ncxDir, chapters)?.let { (idx, anchor) ->
                    results.add(
                      EpubTocItem(
                        title = currentTitle.replace(Regex("\\s+"), " "),
                        chapterIndex = idx,
                        anchor = anchor,
                        level = (navPointDepth - 1).coerceIn(0, 3),
                      )
                    )
                  }
                }
              }
            }
          }
          XmlPullParser.END_TAG -> {
            val tag = parser.name.substringAfter(':').lowercase()
            if (tag == "navlabel") insideNavLabel = false
            if (tag == "navpoint") navPointDepth = (navPointDepth - 1).coerceAtLeast(0)
          }
        }
        event = parser.next()
      }
      results
    }.getOrDefault(emptyList())
  }

  private fun matchHrefToChapter(
    rawHref: String,
    baseDir: String,
    chapters: List<EpubChapter>,
  ): Pair<Int, String?>? {
    val decoded = decodeHref(rawHref)
    val pathPart = decoded.substringBefore('#')
    val anchor = decoded.substringAfter('#', "").ifBlank { null }
    val resolved = resolveZipPath(baseDir, pathPart)
    val match =
      chapters.firstOrNull { it.href.equals(resolved, ignoreCase = true) }
        ?: chapters.firstOrNull {
          it.href.substringAfterLast('/') == resolved.substringAfterLast('/')
        }
    return match?.let { it.index to anchor }
  }

  private fun extractHtmlTitle(file: File): String? {
    return runCatching {
      val snippet = file.bufferedReader().use { reader ->
        val buf = CharArray(4096)
        val n = reader.read(buf)
        if (n > 0) String(buf, 0, n) else ""
      }
      val headingMatch =
        Regex("<h[12][^>]*>(.*?)</h[12]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(snippet)
          ?.groupValues
          ?.getOrNull(1)
      val titleMatch =
        Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(snippet)
          ?.groupValues
          ?.getOrNull(1)
      val raw = headingMatch ?: titleMatch ?: return@runCatching null
      val cleaned = raw.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
      cleaned.takeIf { it.isNotBlank() && it.length <= 80 }
    }.getOrNull()
  }

  private fun stripHtmlToReadableText(html: String): String {
    val noHead =
      html
        .replace(Regex("<head[^>]*>.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
    return noHead
      .replace(Regex("</(p|div|h[1-6]|li|blockquote|br)[^>]*>", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("<[^>]+>"), "")
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&mdash;", "—")
      .replace("&ndash;", "–")
      .replace(Regex("[ \\t]+"), " ")
      .replace(Regex("\\n{3,}"), "\n\n")
      .trim()
  }

  private fun decodeHref(href: String): String =
    runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)

  private fun resolveZipPath(baseDir: String, relative: String): String {
    val combined = if (baseDir.isBlank()) relative else "$baseDir/$relative"
    val parts = ArrayDeque<String>()
    for (segment in combined.split('/')) {
      when (segment) {
        "", "." -> Unit
        ".." -> if (parts.isNotEmpty()) parts.removeLast()
        else -> parts.addLast(segment)
      }
    }
    return parts.joinToString("/")
  }
}
