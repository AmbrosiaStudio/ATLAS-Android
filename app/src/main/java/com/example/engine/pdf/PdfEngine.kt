package com.example.engine.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.example.model.BookSearchMatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Inflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PdfSession(
  private val fileDescriptor: ParcelFileDescriptor,
  private val renderer: PdfRenderer,
) {
  val pageCount: Int = renderer.pageCount
  private val mutex = Mutex()

  // Keep at most 6 rendered high-DPI pages in memory to support large PDFs safely
  private val cache =
    object : LruCache<Int, Bitmap>(6) {
      override fun entryRemoved(
        evicted: Boolean,
        key: Int,
        oldValue: Bitmap,
        newValue: Bitmap?,
      ) {
        if (evicted && !oldValue.isRecycled) {
          oldValue.recycle()
        }
      }
    }

  suspend fun getPageAspectRatio(pageIndex: Int): Float =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        if (pageIndex !in 0 until pageCount) return@withLock 0.707f
        renderer.openPage(pageIndex).use { page ->
          val w = page.width.coerceAtLeast(1)
          val h = page.height.coerceAtLeast(1)
          w.toFloat() / h.toFloat()
        }
      }
    }

  suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? =
    withContext(Dispatchers.IO) {
      if (pageIndex !in 0 until pageCount) return@withContext null
      cache.get(pageIndex)?.takeIf { !it.isRecycled }?.let { return@withContext it }

      mutex.withLock {
        cache.get(pageIndex)?.takeIf { !it.isRecycled }?.let { return@withLock it }
        renderer.openPage(pageIndex).use { page ->
          val safeWidth = targetWidthPx.coerceIn(480, 1800)
          val scale = safeWidth.toFloat() / page.width.coerceAtLeast(1)
          val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
          val bitmap = Bitmap.createBitmap(safeWidth, targetHeight, Bitmap.Config.ARGB_8888)
          bitmap.eraseColor(Color.WHITE)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          cache.put(pageIndex, bitmap)
          bitmap
        }
      }
    }

  fun close() {
    cache.evictAll()
    runCatching { renderer.close() }
    runCatching { fileDescriptor.close() }
  }
}

class PdfEngine(private val context: Context) {

  suspend fun openSession(pdfFile: File): Result<PdfSession> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
          error("PDF file is missing or empty.")
        }
        val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        if (renderer.pageCount <= 0) {
          renderer.close()
          fd.close()
          error("This PDF contains zero readable pages.")
        }
        PdfSession(fd, renderer)
      }
    }

  suspend fun inspectAndExtractCover(
    pdfFile: File,
    bookId: String,
  ): Result<Pair<Int, String?>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        try {
          val pageCount = renderer.pageCount
          if (pageCount <= 0) error("PDF has no pages.")
          val coverPath =
            renderer.openPage(0).use { page ->
              val width = 460
              val height = (page.height.toFloat() * width / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
              val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
              bmp.eraseColor(Color.WHITE)
              page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
              val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
              val outFile = File(coversDir, "$bookId.jpg")
              FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 86, out)
              }
              bmp.recycle()
              outFile.absolutePath
            }
          pageCount to coverPath
        } finally {
          runCatching { renderer.close() }
          runCatching { fd.close() }
        }
      }
    }

  /**
   * Lightweight streaming text scanner for PDFs with uncompressed or FlateDecode text streams.
   * Note: scanned image-only PDFs or subset-cid-encoded streams may yield partial or empty matches.
   */
  suspend fun searchTextInPdf(pdfFile: File, query: String): List<BookSearchMatch> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2 || !pdfFile.exists()) return@withContext emptyList()
      val matches = mutableListOf<BookSearchMatch>()
      runCatching {
        val bytes = pdfFile.readBytes()
        val rawText = String(bytes, Charsets.ISO_8859_1)
        val streamRegex = Regex("stream\\r?\\n(.*?)\\r?\\nendstream", RegexOption.DOT_MATCHES_ALL)
        var pageEstimate = 0
        for (match in streamRegex.findAll(rawText)) {
          val chunkStart = match.range.first
          val streamBytes =
            bytes.copyOfRange(
              (chunkStart + rawText.substring(chunkStart).indexOf('\n') + 1).coerceAtMost(bytes.size),
              (match.range.last - "endstream".length).coerceAtLeast(chunkStart),
            )
          val decodedStream = tryInflateOrRaw(streamBytes)
          if (decodedStream.contains("BT") && decodedStream.contains("ET")) {
            val extracted = extractPdfTextOperators(decodedStream)
            if (extracted.isNotBlank()) {
              val hit = extracted.indexOf(trimmed, ignoreCase = true)
              if (hit >= 0) {
                val start = (hit - 36).coerceAtLeast(0)
                val end = (hit + trimmed.length + 64).coerceAtMost(extracted.length)
                matches.add(
                  BookSearchMatch(
                    chapterOrPageIndex = pageEstimate,
                    locationLabel = "Page ${pageEstimate + 1}",
                    excerpt = extracted.substring(start, end).trim(),
                    query = trimmed,
                  )
                )
                if (matches.size >= 40) break
              }
              pageEstimate++
            }
          }
        }
      }
      matches
    }

  private fun tryInflateOrRaw(data: ByteArray): String {
    val inflated =
      runCatching {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var guard = 0
        while (!inflater.finished() && guard < 256) {
          val count = inflater.inflate(buf)
          if (count == 0) break
          out.write(buf, 0, count)
          guard++
        }
        inflater.end()
        out.toString(Charsets.ISO_8859_1.name())
      }.getOrNull()
    return inflated ?: String(data, Charsets.ISO_8859_1)
  }

  private fun extractPdfTextOperators(content: String): String {
    val sb = StringBuilder()
    val parenRegex = Regex("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)")
    for (m in parenRegex.findAll(content)) {
      val clean =
        m.groupValues[1]
          .replace("\\(", "(")
          .replace("\\)", ")")
          .replace("\\n", " ")
          .replace("\\r", " ")
      if (clean.any { it.isLetterOrDigit() }) {
        sb.append(clean).append(' ')
      }
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
  }
}
