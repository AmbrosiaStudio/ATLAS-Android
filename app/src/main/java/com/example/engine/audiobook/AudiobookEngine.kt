package com.example.engine.audiobook

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

data class AudioTrackEntry(
  val index: Int,
  val title: String,
  val uriString: String,
  val localPath: String? = null,
  val startMs: Long = 0L,
  val endMs: Long = 0L,
  val durationMs: Long = 0L,
) {
  val isChapterSlice: Boolean get() = endMs > startMs && startMs >= 0L
}

data class AudiobookManifest(
  val bookId: String,
  val title: String,
  val author: String,
  val coverPath: String? = null,
  val totalDurationMs: Long = 0L,
  val tracks: List<AudioTrackEntry> = emptyList(),
  val currentTrackIndex: Int = 0,
  val currentPositionMs: Long = 0L,
  val playbackSpeed: Float = 1.0f,
  val linkedBookId: String? = null,
  val lastSyncedAt: Long = 0L,
)

data class ImportedAudiobookResult(
  val manifestFile: File,
  val manifest: AudiobookManifest,
  val totalFileSizeBytes: Long,
)

class AudiobookEngine(private val context: Context) {

  fun loadManifest(manifestFile: File): AudiobookManifest? {
    if (!manifestFile.exists()) return null
    return runCatching {
      parseManifest(manifestFile.readText(Charsets.UTF_8))
    }.getOrNull()
  }

  fun saveManifest(manifestFile: File, manifest: AudiobookManifest) {
    manifestFile.parentFile?.mkdirs()
    manifestFile.writeText(serializeManifest(manifest), Charsets.UTF_8)
  }

  fun importAudioFiles(
    bookId: String,
    uris: List<Uri>,
  ): Result<ImportedAudiobookResult> = runCatching {
    val audioDir = File(context.filesDir, "audiobooks/$bookId").apply { mkdirs() }
    val validSources =
      uris.mapNotNull { uri ->
        takePersistableReadPermissionIfPossible(uri)
        val name = resolveDisplayName(uri) ?: "track_${System.currentTimeMillis()}.mp3"
        val size = resolveFileSize(uri)
        if (!isSupportedAudioFile(name, context.contentResolver.getType(uri))) return@mapNotNull null
        DiscoveredAudioSource(
          uri = uri,
          displayName = name,
          sortKey = name,
          sizeBytes = size,
        )
      }.sortedWith { a, b -> naturalCompare(a.sortKey, b.sortKey) }

    if (validSources.isEmpty()) {
      error("No supported audio files selected (.mp3, .m4b, .m4a, .aac, .ogg, .wav, .flac).")
    }

    buildManifestFromSources(bookId, audioDir, validSources, fallbackBookTitle = null)
  }

  fun importAudioFolder(
    bookId: String,
    treeUri: Uri,
  ): Result<ImportedAudiobookResult> = runCatching {
    takePersistableReadPermissionIfPossible(treeUri)
    val folderDocId = DocumentsContract.getTreeDocumentId(treeUri)
    val folderName =
      resolveTreeRootName(treeUri, folderDocId)
        ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
        ?: "Audiobook"

    val collected = mutableListOf<DiscoveredAudioSource>()
    collectAudioFilesInTree(
      treeUri = treeUri,
      parentDocId = folderDocId,
      prefix = "",
      depth = 0,
      out = collected,
    )

    val sorted = collected.sortedWith { a, b -> naturalCompare(a.sortKey, b.sortKey) }
    if (sorted.isEmpty()) {
      error("No MP3, M4B, M4A, or OGG audio files found in this folder.")
    }

    val audioDir = File(context.filesDir, "audiobooks/$bookId").apply { mkdirs() }
    buildManifestFromSources(bookId, audioDir, sorted, fallbackBookTitle = folderName)
  }

  private fun buildManifestFromSources(
    bookId: String,
    audioDir: File,
    sources: List<DiscoveredAudioSource>,
    fallbackBookTitle: String?,
  ): ImportedAudiobookResult {
    var bookTitle: String? = fallbackBookTitle?.takeIf { it.isNotBlank() }
    var bookAuthor: String? = null
    var savedCoverPath: String? = null
    var totalBytes = 0L

    val tracks = mutableListOf<AudioTrackEntry>()
    val firstSource = sources.first()

    // Check if single-file M4B/M4A contains embedded Nero chapter atoms
    if (sources.size == 1 && isMp4AudioContainer(firstSource.displayName)) {
      val meta = inspectAudioUri(firstSource.uri)
      val chapters = extractM4bChaptersFromUri(firstSource.uri, meta.durationMs)
      val localFallbackPath = copyUriIfPermissionNotPersisted(firstSource.uri, audioDir, "001_${sanitizeFileName(firstSource.displayName)}")
      val resolvedTitle =
        meta.album?.takeIf { it.isNotBlank() }
          ?: meta.title?.takeIf { it.isNotBlank() }
          ?: firstSource.displayName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Audiobook" }
      val resolvedAuthor = meta.author?.takeIf { it.isNotBlank() } ?: "Audiobook Narrator"
      if (meta.coverBytes != null) {
        savedCoverPath = saveAudiobookCoverBytes(bookId, meta.coverBytes)
      }
      totalBytes = firstSource.sizeBytes.coerceAtLeast(localFallbackPath?.let { File(it).length() } ?: 0L)

      if (chapters.size > 1) {
        chapters.forEachIndexed { idx, ch ->
          tracks.add(
            AudioTrackEntry(
              index = idx,
              title = ch.title.ifBlank { "Chapter ${idx + 1}" },
              uriString = firstSource.uri.toString(),
              localPath = localFallbackPath,
              startMs = ch.startMs,
              endMs = ch.endMs,
              durationMs = (ch.endMs - ch.startMs).coerceAtLeast(1000L),
            )
          )
        }
      } else {
        val dur = meta.durationMs.coerceAtLeast(1000L)
        tracks.add(
          AudioTrackEntry(
            index = 0,
            title = meta.title?.takeIf { it.isNotBlank() } ?: resolvedTitle,
            uriString = firstSource.uri.toString(),
            localPath = localFallbackPath,
            startMs = 0L,
            endMs = dur,
            durationMs = dur,
          )
        )
      }
      bookTitle = resolvedTitle
      bookAuthor = resolvedAuthor
    } else {
      sources.forEachIndexed { idx, src ->
        val meta = inspectAudioUri(src.uri)
        if (bookTitle.isNullOrBlank()) {
          bookTitle = meta.album?.takeIf { it.isNotBlank() }
        }
        if (bookAuthor.isNullOrBlank()) {
          bookAuthor = meta.author?.takeIf { it.isNotBlank() }
        }
        if (savedCoverPath == null && meta.coverBytes != null) {
          savedCoverPath = saveAudiobookCoverBytes(bookId, meta.coverBytes)
        }
        val safeName = String.format("%03d_%s", idx + 1, sanitizeFileName(src.displayName))
        val localFallbackPath = copyUriIfPermissionNotPersisted(src.uri, audioDir, safeName)
        totalBytes += src.sizeBytes.coerceAtLeast(localFallbackPath?.let { File(it).length() } ?: 0L)
        val dur = meta.durationMs.coerceAtLeast(1000L)
        val trackTitle =
          meta.title?.takeIf { it.isNotBlank() }
            ?: src.displayName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Part ${idx + 1}" }
        tracks.add(
          AudioTrackEntry(
            index = idx,
            title = trackTitle,
            uriString = src.uri.toString(),
            localPath = localFallbackPath,
            startMs = 0L,
            endMs = dur,
            durationMs = dur,
          )
        )
      }
    }

    val finalTitle =
      bookTitle?.takeIf { it.isNotBlank() }
        ?: firstSource.displayName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Audiobook" }
    val finalAuthor = bookAuthor?.takeIf { it.isNotBlank() } ?: "Audiobook Narrator"
    val totalDurMs = tracks.sumOf { it.durationMs.coerceAtLeast(0L) }.coerceAtLeast(1000L)

    val manifest =
      AudiobookManifest(
        bookId = bookId,
        title = finalTitle,
        author = finalAuthor,
        coverPath = savedCoverPath,
        totalDurationMs = totalDurMs,
        tracks = tracks,
        currentTrackIndex = 0,
        currentPositionMs = 0L,
        playbackSpeed = 1.0f,
      )
    val manifestFile = File(audioDir, "manifest.json")
    saveManifest(manifestFile, manifest)
    return ImportedAudiobookResult(
      manifestFile = manifestFile,
      manifest = manifest,
      totalFileSizeBytes = totalBytes.coerceAtLeast(manifestFile.length()),
    )
  }

  private fun collectAudioFilesInTree(
    treeUri: Uri,
    parentDocId: String,
    prefix: String,
    depth: Int,
    out: MutableList<DiscoveredAudioSource>,
  ) {
    if (depth > 2) return
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    val projection =
      arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
      )
    runCatching {
      context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        while (cursor.moveToNext()) {
          val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
          val displayName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
          val mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
          val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L

          if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            collectAudioFilesInTree(
              treeUri = treeUri,
              parentDocId = docId,
              prefix = if (prefix.isBlank()) displayName else "$prefix/$displayName",
              depth = depth + 1,
              out = out,
            )
          } else if (isSupportedAudioFile(displayName, mimeType)) {
            val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val sortKey = if (prefix.isBlank()) displayName else "$prefix/$displayName"
            out.add(
              DiscoveredAudioSource(
                uri = childUri,
                displayName = displayName.ifBlank { "Track ${out.size + 1}.mp3" },
                sortKey = sortKey,
                sizeBytes = size,
              )
            )
          }
        }
      }
    }
  }

  private fun takePersistableReadPermissionIfPossible(uri: Uri) {
    runCatching {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
  }

  private fun copyUriIfPermissionNotPersisted(uri: Uri, audioDir: File, fileName: String): String? {
    val alreadyPersisted =
      context.contentResolver.persistedUriPermissions.any { perm ->
        perm.isReadPermission && (perm.uri == uri || uri.toString().startsWith(perm.uri.toString().substringBefore("/document/")))
      }
    if (alreadyPersisted) return null
    return runCatching {
      val dest = File(audioDir, fileName)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(dest).use { output -> input.copyTo(output) }
      }
      if (dest.exists() && dest.length() > 0L) dest.absolutePath else null
    }.getOrNull()
  }

  private fun inspectAudioUri(uri: Uri): AudioFileInspection {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
      val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
      val author =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
          ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
          ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
      val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val cover = runCatching { retriever.embeddedPicture }.getOrNull()
      AudioFileInspection(
        title = title,
        album = album,
        author = author,
        durationMs = durMs,
        coverBytes = cover,
      )
    } catch (_: Throwable) {
      AudioFileInspection()
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun extractM4bChaptersFromUri(uri: Uri, totalDurationMs: Long): List<Mp4Chapter> =
    runCatching {
      context.contentResolver.openInputStream(uri)?.use { input ->
        parseMp4NeroChapters(input, totalDurationMs)
      }.orEmpty()
    }.getOrDefault(emptyList())

  private fun saveAudiobookCoverBytes(bookId: String, bytes: ByteArray): String? =
    runCatching {
      if (bytes.isEmpty()) return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
      val maxWidth = 560
      var sample = 1
      while (bounds.outWidth / sample > maxWidth * 2) sample *= 2
      val decoded =
        BitmapFactory.decodeByteArray(
          bytes,
          0,
          bytes.size,
          BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
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
      val outFile = File(coversDir, "${bookId}_audio_cover.jpg")
      FileOutputStream(outFile).use { fos -> scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos) }
      scaled.recycle()
      outFile.absolutePath
    }.getOrNull()

  private fun resolveTreeRootName(treeUri: Uri, docId: String): String? =
    runCatching {
      val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
      context.contentResolver.query(
        docUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null,
      )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
      }
    }.getOrNull()

  private fun resolveDisplayName(uri: Uri): String? {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) return cursor.getString(idx)
        }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  private fun resolveFileSize(uri: Uri): Long {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
        }
      }
    }
    return 0L
  }

  private data class DiscoveredAudioSource(
    val uri: Uri,
    val displayName: String,
    val sortKey: String,
    val sizeBytes: Long,
  )

  private data class AudioFileInspection(
    val title: String? = null,
    val album: String? = null,
    val author: String? = null,
    val durationMs: Long = 0L,
    val coverBytes: ByteArray? = null,
  )

  data class Mp4Chapter(
    val title: String,
    val startMs: Long,
    val endMs: Long,
  )

  companion object {
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4b", "m4a", "aac", "ogg", "opus", "wav", "flac")
    private val MP4_AUDIO_EXTENSIONS = setOf("m4b", "m4a", "mp4")

    fun isSupportedAudioFile(fileName: String, mimeType: String?): Boolean {
      val ext = fileName.substringAfterLast('.', "").lowercase()
      if (ext in AUDIO_EXTENSIONS) return true
      val mime = mimeType.orEmpty().lowercase()
      return mime.startsWith("audio/") || mime == "application/ogg"
    }

    fun isMp4AudioContainer(fileName: String): Boolean {
      val ext = fileName.substringAfterLast('.', "").lowercase()
      return ext in MP4_AUDIO_EXTENSIONS
    }

    fun naturalCompare(a: String, b: String): Int {
      var ia = 0
      var ib = 0
      while (ia < a.length && ib < b.length) {
        val ca = a[ia]
        val cb = b[ib]
        if (ca.isDigit() && cb.isDigit()) {
          var ea = ia
          while (ea < a.length && a[ea].isDigit()) ea++
          var eb = ib
          while (eb < b.length && b[eb].isDigit()) eb++
          val na = a.substring(ia, ea).trimStart('0').ifEmpty { "0" }
          val nb = b.substring(ib, eb).trimStart('0').ifEmpty { "0" }
          if (na.length != nb.length) return na.length.compareTo(nb.length)
          val cmpNum = na.compareTo(nb)
          if (cmpNum != 0) return cmpNum
          ia = ea
          ib = eb
        } else {
          val cmpChar = ca.lowercaseChar().compareTo(cb.lowercaseChar())
          if (cmpChar != 0) return cmpChar
          ia++
          ib++
        }
      }
      return a.length.compareTo(b.length)
    }

    fun sanitizeFileName(raw: String): String =
      raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(80)

    fun serializeManifest(manifest: AudiobookManifest): String {
      val root = JSONObject()
      root.put("bookId", manifest.bookId)
      root.put("title", manifest.title)
      root.put("author", manifest.author)
      if (manifest.coverPath != null) root.put("coverPath", manifest.coverPath)
      root.put("totalDurationMs", manifest.totalDurationMs)
      root.put("currentTrackIndex", manifest.currentTrackIndex)
      root.put("currentPositionMs", manifest.currentPositionMs)
      root.put("playbackSpeed", manifest.playbackSpeed.toDouble())
      if (manifest.linkedBookId != null) root.put("linkedBookId", manifest.linkedBookId)
      root.put("lastSyncedAt", manifest.lastSyncedAt)

      val arr = JSONArray()
      manifest.tracks.forEach { t ->
        val obj = JSONObject()
        obj.put("index", t.index)
        obj.put("title", t.title)
        obj.put("uriString", t.uriString)
        if (t.localPath != null) obj.put("localPath", t.localPath)
        obj.put("startMs", t.startMs)
        obj.put("endMs", t.endMs)
        obj.put("durationMs", t.durationMs)
        arr.put(obj)
      }
      root.put("tracks", arr)
      return root.toString(2)
    }

    fun parseManifest(jsonText: String): AudiobookManifest {
      val root = JSONObject(jsonText)
      val arr = root.optJSONArray("tracks") ?: JSONArray()
      val tracks =
        (0 until arr.length()).map { i ->
          val obj = arr.getJSONObject(i)
          AudioTrackEntry(
            index = obj.optInt("index", i),
            title = obj.optString("title", "Part ${i + 1}"),
            uriString = obj.optString("uriString", ""),
            localPath = obj.optString("localPath", "").takeIf { it.isNotBlank() },
            startMs = obj.optLong("startMs", 0L),
            endMs = obj.optLong("endMs", 0L),
            durationMs = obj.optLong("durationMs", 0L),
          )
        }
      return AudiobookManifest(
        bookId = root.optString("bookId", ""),
        title = root.optString("title", "Audiobook"),
        author = root.optString("author", ""),
        coverPath = root.optString("coverPath", "").takeIf { it.isNotBlank() },
        totalDurationMs = root.optLong("totalDurationMs", tracks.sumOf { it.durationMs }),
        tracks = tracks,
        currentTrackIndex = root.optInt("currentTrackIndex", 0),
        currentPositionMs = root.optLong("currentPositionMs", 0L),
        playbackSpeed = root.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.75f, 2.5f),
        linkedBookId = root.optString("linkedBookId", "").takeIf { it.isNotBlank() },
        lastSyncedAt = root.optLong("lastSyncedAt", 0L),
      )
    }

    fun computeGlobalElapsedMs(
      manifest: AudiobookManifest,
      trackIndex: Int,
      positionInTrackMs: Long,
    ): Long {
      if (manifest.tracks.isEmpty()) return 0L
      val safeIdx = trackIndex.coerceIn(0, manifest.tracks.lastIndex)
      val currentTrack = manifest.tracks[safeIdx]
      val clampedPos = positionInTrackMs.coerceIn(0L, currentTrack.durationMs.coerceAtLeast(0L))
      var elapsed = 0L
      for (i in 0 until safeIdx) {
        elapsed += manifest.tracks[i].durationMs.coerceAtLeast(0L)
      }
      return (elapsed + clampedPos).coerceIn(0L, manifest.totalDurationMs.coerceAtLeast(1L))
    }

    fun computeOverallProgressPercent(
      manifest: AudiobookManifest,
      trackIndex: Int,
      positionInTrackMs: Long,
    ): Float {
      val total = manifest.totalDurationMs.coerceAtLeast(1L)
      val elapsed = computeGlobalElapsedMs(manifest, trackIndex, positionInTrackMs)
      return ((elapsed.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
    }

    fun resolveTrackAndPositionForPercent(
      manifest: AudiobookManifest,
      targetPercent: Float,
    ): Pair<Int, Long> {
      if (manifest.tracks.isEmpty()) return 0 to 0L
      val total = manifest.totalDurationMs.coerceAtLeast(1L)
      val targetGlobalMs = ((targetPercent.coerceIn(0f, 100f) / 100f) * total).roundToLong()
      var running = 0L
      manifest.tracks.forEachIndexed { idx, track ->
        val dur = track.durationMs.coerceAtLeast(1L)
        if (targetGlobalMs <= running + dur || idx == manifest.tracks.lastIndex) {
          val offset = (targetGlobalMs - running).coerceIn(0L, dur)
          return idx to offset
        }
        running += dur
      }
      return 0 to 0L
    }

    fun formatDurationClock(ms: Long): String {
      val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
      val hours = totalSeconds / 3600L
      val mins = (totalSeconds % 3600L) / 60L
      val secs = totalSeconds % 60L
      return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, mins, secs)
      } else {
        String.format("%02d:%02d", mins, secs)
      }
    }

    /**
     * Parses MP4/M4B Nero chapter (`chpl`) atom inside `moov/udta` cleanly with bounded memory.
     */
    fun parseMp4NeroChapters(input: InputStream, totalDurationMs: Long): List<Mp4Chapter> {
      val header = ByteArray(8)
      return scanMp4BoxesForChpl(input, Long.MAX_VALUE, header, totalDurationMs)
    }

    private fun scanMp4BoxesForChpl(
      input: InputStream,
      maxBytes: Long,
      header: ByteArray,
      totalDurationMs: Long,
    ): List<Mp4Chapter> {
      var consumed = 0L
      while (consumed + 8L <= maxBytes) {
        if (!readFully(input, header, 8)) break
        consumed += 8L
        val rawSize = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val type = String(header, 4, 4, Charsets.US_ASCII)
        var boxHeaderLen = 8L
        val boxSize: Long =
          when {
            rawSize == 1L -> {
              val extBuf = ByteArray(8)
              if (!readFully(input, extBuf, 8)) return emptyList()
              consumed += 8L
              boxHeaderLen = 16L
              ByteBuffer.wrap(extBuf).order(ByteOrder.BIG_ENDIAN).long
            }
            rawSize == 0L -> maxBytes - consumed + 8L
            else -> rawSize
          }
        if (boxSize < boxHeaderLen) break
        val payloadSize = (boxSize - boxHeaderLen).coerceAtLeast(0L)

        when (type) {
          "moov", "udta" -> {
            val found = scanMp4BoxesForChpl(input, payloadSize, header, totalDurationMs)
            if (found.isNotEmpty()) return found
            consumed += payloadSize
          }
          "chpl" -> {
            if (payloadSize in 6L..2_000_000L) {
              val bytes = ByteArray(payloadSize.toInt())
              if (readFully(input, bytes, bytes.size)) {
                val parsed = decodeChplPayload(bytes, totalDurationMs)
                if (parsed.isNotEmpty()) return parsed
              }
            } else {
              skipFully(input, payloadSize)
            }
            consumed += payloadSize
          }
          else -> {
            skipFully(input, payloadSize)
            consumed += payloadSize
          }
        }
      }
      return emptyList()
    }

    fun decodeChplPayload(bytes: ByteArray, totalDurationMs: Long): List<Mp4Chapter> {
      if (bytes.size < 8) return emptyList()
      val version = bytes[0].toInt() and 0xFF
      var pos = if (version >= 1) 8 else 4
      if (pos >= bytes.size) return emptyList()
      val count = (bytes[pos].toInt() and 0xFF).coerceAtMost(500)
      pos += 1
      val rawStarts = mutableListOf<Pair<Long, String>>()
      repeat(count) {
        if (pos + 9 > bytes.size) return@repeat
        val time100ns = ByteBuffer.wrap(bytes, pos, 8).order(ByteOrder.BIG_ENDIAN).long
        pos += 8
        val titleLen = bytes[pos].toInt() and 0xFF
        pos += 1
        if (pos + titleLen > bytes.size) return@repeat
        val title = String(bytes, pos, titleLen, Charsets.UTF_8).trim()
        pos += titleLen
        val startMs = (time100ns / 10_000L).coerceAtLeast(0L)
        rawStarts.add(startMs to title)
      }
      if (rawStarts.isEmpty()) return emptyList()
      val resolvedEndFallback =
        if (totalDurationMs > 0L) totalDurationMs
        else (rawStarts.last().first + 60_000L)
      return rawStarts.mapIndexed { idx, (startMs, title) ->
        val nextStart =
          if (idx + 1 < rawStarts.size) rawStarts[idx + 1].first.coerceAtLeast(startMs + 500L)
          else resolvedEndFallback.coerceAtLeast(startMs + 500L)
        Mp4Chapter(
          title = title.ifBlank { "Chapter ${idx + 1}" },
          startMs = startMs,
          endMs = nextStart,
        )
      }
    }

    private fun readFully(input: InputStream, buf: ByteArray, len: Int): Boolean {
      var off = 0
      while (off < len) {
        val r = input.read(buf, off, len - off)
        if (r <= 0) return false
        off += r
      }
      return true
    }

    private fun skipFully(input: InputStream, count: Long) {
      var rem = count
      val scratch = ByteArray(4096)
      while (rem > 0L) {
        val skipped = input.skip(rem)
        if (skipped > 0L) {
          rem -= skipped
        } else {
          val r = input.read(scratch, 0, minOf(scratch.size.toLong(), rem).toInt())
          if (r <= 0) break
          rem -= r
        }
      }
    }
  }
}
