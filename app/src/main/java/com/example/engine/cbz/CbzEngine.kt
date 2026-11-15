package com.example.engine.cbz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CbzSession(
  private val zipFile: ZipFile,
  private val entryNames: List<String>,
) : Closeable {

  val pageCount: Int get() = entryNames.size

  private val mutex = Mutex()
  private val bitmapCache =
    object : LruCache<String, Bitmap>(16) {
      override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

  suspend fun getPageAspectRatio(pageIndex: Int): Float =
    withContext(Dispatchers.IO) {
      val name = entryNames.getOrNull(pageIndex) ?: return@withContext 0.707f
      mutex.withLock {
        val entry = zipFile.getEntry(name) ?: return@withLock 0.707f
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zipFile.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth > 0 && opts.outHeight > 0) {
          opts.outWidth.toFloat() / opts.outHeight.toFloat()
        } else {
          0.707f
        }
      }
    }

  suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? =
    withContext(Dispatchers.IO) {
      val name = entryNames.getOrNull(pageIndex) ?: return@withContext null
      val safeWidth = targetWidthPx.coerceIn(360, 1600)
      val cacheKey = "${pageIndex}_$safeWidth"
      bitmapCache.get(cacheKey)?.let { cached ->
        if (!cached.isRecycled) return@withContext cached
      }

      mutex.withLock {
        bitmapCache.get(cacheKey)?.let { cached ->
          if (!cached.isRecycled) return@withLock cached
        }
        val entry = zipFile.getEntry(name) ?: return@withLock null
        val bytes = zipFile.getInputStream(entry).use { it.readBytes() }
        if (bytes.isEmpty()) return@withLock null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > safeWidth * 2) {
          sample *= 2
        }

        val opts =
          BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
          }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return@withLock null
        val finalBitmap =
          if (decoded.width > safeWidth) {
            val h = (decoded.height.toFloat() * safeWidth / decoded.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, safeWidth, h, true).also {
              if (it != decoded) decoded.recycle()
            }
          } else {
            decoded
          }
        bitmapCache.put(cacheKey, finalBitmap)
        finalBitmap
      }
    }

  override fun close() {
    bitmapCache.evictAll()
    runCatching { zipFile.close() }
  }
}

class CbzEngine(private val context: Context) {

  suspend fun inspectAndExtractCover(file: File, bookId: String): Result<Pair<Int, String?>> =
    withContext(Dispatchers.IO) {
      runCatching {
        ZipFile(file).use { zip ->
          val pages = collectSortedPageNames(zip)
          if (pages.isEmpty()) {
            error("This CBZ archive contains no readable comic images.")
          }
          val coverPath = extractCoverFromEntry(zip, pages.first(), bookId)
          pages.size to coverPath
        }
      }
    }

  suspend fun openSession(file: File): Result<CbzSession> =
    withContext(Dispatchers.IO) {
      runCatching {
        val zip = ZipFile(file)
        val pages = collectSortedPageNames(zip)
        if (pages.isEmpty()) {
          zip.close()
          error("This CBZ archive contains no readable comic images.")
        }
        CbzSession(zip, pages)
      }
    }

  private fun extractCoverFromEntry(zip: ZipFile, entryName: String, bookId: String): String? =
    runCatching {
      val entry = zip.getEntry(entryName) ?: return null
      val bytes = zip.getInputStream(entry).use { it.readBytes() }
      if (bytes.isEmpty()) return null
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
      val outFile = File(coversDir, "${bookId}_cover.jpg")
      FileOutputStream(outFile).use { fos -> scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos) }
      scaled.recycle()
      outFile.absolutePath
    }.getOrNull()

  companion object {
    private val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun collectSortedPageNames(zip: ZipFile): List<String> {
      return zip
        .entries()
        .asSequence()
        .filter { !it.isDirectory }
        .map { it.name }
        .filter { name ->
          val clean = name.substringAfterLast('/')
          !name.startsWith("__MACOSX") &&
            !clean.startsWith(".") &&
            clean.substringAfterLast('.', "").lowercase() in imageExts
        }
        .sortedWith(::naturalCompare)
        .toList()
    }

    /** Natural numeric comparison so `page2.jpg` precedes `page10.jpg`. */
    fun naturalCompare(a: String, b: String): Int {
      var ia = 0
      var ib = 0
      while (ia < a.length && ib < b.length) {
        val ca = a[ia]
        val cb = b[ib]
        if (ca.isDigit() && cb.isDigit()) {
          val startA = ia
          val startB = ib
          while (ia < a.length && a[ia].isDigit()) ia++
          while (ib < b.length && b[ib].isDigit()) ib++
          val numA = a.substring(startA, ia).trimStart('0')
          val numB = b.substring(startB, ib).trimStart('0')
          if (numA.length != numB.length) return numA.length.compareTo(numB.length)
          val cmp = numA.compareTo(numB)
          if (cmp != 0) return cmp
        } else {
          val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
          if (cmp != 0) return cmp
          ia++
          ib++
        }
      }
      return a.length.compareTo(b.length)
    }
  }
}
