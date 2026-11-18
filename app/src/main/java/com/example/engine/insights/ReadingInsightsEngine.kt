package com.example.engine.insights

import android.content.Context
import com.example.data.local.BookEntity
import com.example.data.local.BookProgressPointEntity
import com.example.data.local.DailyReadingStatEntity
import com.example.engine.audiobook.AudiobookEngine
import com.example.engine.epub.EpubParser
import com.example.engine.formats.TextFormatsConverter
import com.example.model.BookFormat
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ChapterProgressStatus {
  COMPLETED,
  IN_PROGRESS,
  UPCOMING,
}

data class ChapterInsight(
  val index: Int,
  val title: String,
  val wordCount: Int,
  val unitCount: Int,
  val progressFraction: Float,
  val status: ChapterProgressStatus,
  val estimatedTotalMinutes: Float,
  val estimatedRemainingMinutes: Float,
)

data class BookTelemetry(
  val bookId: String,
  val totalSecondsRead: Long = 0L,
  val wordsReadEstimated: Long = 0L,
  val unitsAdvanced: Int = 0,
  val lastUpdatedAt: Long = 0L,
)

data class BookReadingInsights(
  val bookId: String,
  val format: BookFormat,
  val wordsPerMinute: Int,
  val pagesPerHour: Int,
  val playbackSpeed: Float = 1.0f,
  val isMeasuredSpeed: Boolean,
  val totalBookSecondsRead: Long,
  val totalWords: Int,
  val totalEstimatedMinutes: Float,
  val remainingEstimatedMinutes: Float,
  val currentChapterRemainingMinutes: Float,
  val completedChaptersCount: Int,
  val chapters: List<ChapterInsight>,
)

class ReadingInsightsEngine(
  private val context: Context,
  private val epubParser: EpubParser,
  private val textFormatsConverter: TextFormatsConverter,
  private val audiobookEngine: AudiobookEngine,
) {
  private val _telemetryTick = MutableStateFlow(0L)
  val telemetryUpdatedTick: StateFlow<Long> = _telemetryTick.asStateFlow()

  private val telemetryLock = Any()

  fun telemetryFile(bookId: String): File {
    val dir = File(context.filesDir, "reading_insights").apply { mkdirs() }
    val safe = bookId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    return File(dir, "$safe.json")
  }

  fun loadTelemetry(bookId: String): BookTelemetry {
    val file = telemetryFile(bookId)
    if (!file.exists()) return BookTelemetry(bookId = bookId)
    return runCatching {
      val root = JSONObject(file.readText(Charsets.UTF_8))
      BookTelemetry(
        bookId = root.optString("bookId", bookId),
        totalSecondsRead = root.optLong("totalSecondsRead", 0L).coerceAtLeast(0L),
        wordsReadEstimated = root.optLong("wordsReadEstimated", 0L).coerceAtLeast(0L),
        unitsAdvanced = root.optInt("unitsAdvanced", 0).coerceAtLeast(0),
        lastUpdatedAt = root.optLong("lastUpdatedAt", 0L),
      )
    }.getOrDefault(BookTelemetry(bookId = bookId))
  }

  fun saveTelemetry(telemetry: BookTelemetry) {
    synchronized(telemetryLock) {
      val file = telemetryFile(telemetry.bookId)
      file.parentFile?.mkdirs()
      val root =
        JSONObject().apply {
          put("bookId", telemetry.bookId)
          put("totalSecondsRead", telemetry.totalSecondsRead)
          put("wordsReadEstimated", telemetry.wordsReadEstimated)
          put("unitsAdvanced", telemetry.unitsAdvanced)
          put("lastUpdatedAt", telemetry.lastUpdatedAt)
        }
      file.writeText(root.toString(), Charsets.UTF_8)
    }
    _telemetryTick.update { it + 1L }
  }

  fun recordSessionProgress(
    bookId: String,
    deltaSeconds: Int,
    deltaWords: Long = 0L,
    deltaUnits: Int = 0,
  ) {
    if (deltaSeconds <= 0 && deltaWords <= 0L && deltaUnits <= 0) return
    synchronized(telemetryLock) {
      val current = loadTelemetry(bookId)
      val next =
        current.copy(
          totalSecondsRead = current.totalSecondsRead + deltaSeconds.coerceAtLeast(0),
          wordsReadEstimated = current.wordsReadEstimated + deltaWords.coerceAtLeast(0L),
          unitsAdvanced = current.unitsAdvanced + deltaUnits.coerceAtLeast(0),
          lastUpdatedAt = System.currentTimeMillis(),
        )
      val file = telemetryFile(bookId)
      file.parentFile?.mkdirs()
      val root =
        JSONObject().apply {
          put("bookId", next.bookId)
          put("totalSecondsRead", next.totalSecondsRead)
          put("wordsReadEstimated", next.wordsReadEstimated)
          put("unitsAdvanced", next.unitsAdvanced)
          put("lastUpdatedAt", next.lastUpdatedAt)
        }
      file.writeText(root.toString(), Charsets.UTF_8)
    }
    _telemetryTick.update { it + 1L }
  }

  fun deleteBookInsights(bookId: String) {
    runCatching { telemetryFile(bookId).delete() }
  }

  suspend fun buildInsightsForBook(
    book: BookEntity,
    dailyStats: List<DailyReadingStatEntity>,
    progressHistory: List<BookProgressPointEntity>,
  ): BookReadingInsights =
    withContext(Dispatchers.IO) {
      val format = book.bookFormat()
      val telemetry = loadTelemetry(book.id)

      when {
        format == BookFormat.AUDIOBOOK -> buildAudiobookInsights(book, telemetry)
        format.isReflowableText -> buildReflowableInsights(book, format, telemetry, dailyStats, progressHistory)
        else -> buildFixedPageInsights(book, format, telemetry, dailyStats, progressHistory)
      }
    }

  private suspend fun buildReflowableInsights(
    book: BookEntity,
    format: BookFormat,
    telemetry: BookTelemetry,
    dailyStats: List<DailyReadingStatEntity>,
    progressHistory: List<BookProgressPointEntity>,
  ): BookReadingInsights {
    val file = File(book.filePath)
    val parsedBook =
      if (file.exists()) {
        if (format == BookFormat.EPUB) {
          epubParser.loadBook(file, book.id, book.title).getOrNull()
        } else {
          textFormatsConverter.loadBook(file, format, book.id, book.title).getOrNull()
        }
      } else {
        null
      }

    val chapterInputs: List<Pair<String, Int>> =
      if (parsedBook != null && parsedBook.chapters.isNotEmpty()) {
        parsedBook.chapters.map { ch ->
          val htmlFile = File(ch.localFilePath)
          val wc = if (htmlFile.exists()) countWordsInHtmlFile(htmlFile) else 320
          ch.title to wc.coerceAtLeast(1)
        }
      } else {
        val count = book.totalUnits.coerceAtLeast(1)
        (0 until count).map { idx -> "Chapter ${idx + 1}" to 850 }
      }

    val totalWords = chapterInputs.sumOf { it.second }.coerceAtLeast(1)
    val isRtlSample =
      parsedBook?.chapters?.firstOrNull()?.localFilePath?.let { path ->
        runCatching {
          val sample = File(path).readText(Charsets.UTF_8).take(1200)
          isPredominantlyRtl(sample)
        }.getOrDefault(false)
      } ?: false

    val speedResult =
      resolveTextReadingSpeed(
        telemetry = telemetry,
        totalBookWords = totalWords,
        dailyStats = dailyStats,
        progressHistory = progressHistory,
        defaultWpm = if (isRtlSample) DEFAULT_RTL_WPM else DEFAULT_LTR_WPM,
      )

    return calculateReflowableInsights(
      bookId = book.id,
      format = format,
      chaptersWordCounts = chapterInputs,
      currentChapterIndex = book.currentChapterOrPage,
      currentScrollFraction = book.currentScrollFraction,
      wordsPerMinute = speedResult.wpm,
      isMeasuredSpeed = speedResult.isMeasured,
      totalBookSecondsRead = telemetry.totalSecondsRead,
    )
  }

  private fun buildFixedPageInsights(
    book: BookEntity,
    format: BookFormat,
    telemetry: BookTelemetry,
    dailyStats: List<DailyReadingStatEntity>,
    progressHistory: List<BookProgressPointEntity>,
  ): BookReadingInsights {
    val totalPages = book.totalUnits.coerceAtLeast(1)
    val defaultMinutesPerPage = if (format == BookFormat.CBZ) 0.55f else 1.35f
    val defaultPagesPerHour = (60f / defaultMinutesPerPage).roundToInt()

    val (pagesPerHour, isMeasured) =
      resolveFixedPageSpeed(
        telemetry = telemetry,
        totalBookPages = totalPages,
        dailyStats = dailyStats,
        progressHistory = progressHistory,
        defaultPagesPerHour = defaultPagesPerHour,
      )

    return calculateFixedPageInsights(
      bookId = book.id,
      format = format,
      totalPages = totalPages,
      currentPageIndex = book.currentChapterOrPage,
      pagesPerHour = pagesPerHour,
      isMeasuredSpeed = isMeasured,
      totalBookSecondsRead = telemetry.totalSecondsRead,
    )
  }

  private fun buildAudiobookInsights(
    book: BookEntity,
    telemetry: BookTelemetry,
  ): BookReadingInsights {
    val manifest = audiobookEngine.loadManifest(File(book.filePath))
    val speed = (manifest?.playbackSpeed ?: 1.0f).coerceIn(0.75f, 2.5f)
    val tracks = manifest?.tracks.orEmpty()
    if (tracks.isEmpty()) {
      return calculateFixedPageInsights(
        bookId = book.id,
        format = BookFormat.AUDIOBOOK,
        totalPages = book.totalUnits.coerceAtLeast(1),
        currentPageIndex = book.currentChapterOrPage,
        pagesPerHour = 12,
        isMeasuredSpeed = false,
        totalBookSecondsRead = telemetry.totalSecondsRead,
      )
    }

    val currentIdx = (manifest?.currentTrackIndex ?: book.currentChapterOrPage).coerceIn(0, tracks.lastIndex)
    val currentPosMs = manifest?.currentPositionMs ?: 0L
    val chapters =
      tracks.mapIndexed { idx, track ->
        val rawMinutes = (track.durationMs.coerceAtLeast(1000L) / 60_000f) / speed
        val frac =
          when {
            idx < currentIdx -> 1f
            idx == currentIdx ->
              (currentPosMs.toFloat() / track.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            else -> 0f
          }
        val status =
          when {
            idx < currentIdx || frac >= 0.99f -> ChapterProgressStatus.COMPLETED
            idx == currentIdx -> ChapterProgressStatus.IN_PROGRESS
            else -> ChapterProgressStatus.UPCOMING
          }
        val remMins = (rawMinutes * (1f - frac)).coerceAtLeast(0f)
        ChapterInsight(
          index = idx,
          title = track.title,
          wordCount = (rawMinutes * 150f).roundToInt(),
          unitCount = 1,
          progressFraction = frac,
          status = status,
          estimatedTotalMinutes = rawMinutes,
          estimatedRemainingMinutes = remMins,
        )
      }

    val totalMins = chapters.sumOf { it.estimatedTotalMinutes.toDouble() }.toFloat()
    val remMins = chapters.sumOf { it.estimatedRemainingMinutes.toDouble() }.toFloat()
    val curRem = chapters.getOrNull(currentIdx)?.estimatedRemainingMinutes ?: 0f
    val doneCount = chapters.count { it.status == ChapterProgressStatus.COMPLETED }

    return BookReadingInsights(
      bookId = book.id,
      format = BookFormat.AUDIOBOOK,
      wordsPerMinute = (155f * speed).roundToInt(),
      pagesPerHour = 0,
      playbackSpeed = speed,
      isMeasuredSpeed = true,
      totalBookSecondsRead = telemetry.totalSecondsRead,
      totalWords = chapters.sumOf { it.wordCount },
      totalEstimatedMinutes = totalMins,
      remainingEstimatedMinutes = remMins,
      currentChapterRemainingMinutes = curRem,
      completedChaptersCount = doneCount,
      chapters = chapters,
    )
  }

  companion object {
    const val DEFAULT_LTR_WPM = 220
    const val DEFAULT_RTL_WPM = 185
    const val MIN_HUMAN_WPM = 85
    const val MAX_HUMAN_WPM = 480

    private val HTML_TAG_REGEX = Regex("<[^>]+>")
    private val HTML_ENTITY_REGEX = Regex("&[a-zA-Z0-9#]+;")
    private val WHITESPACE_REGEX = Regex("\\s+")

    data class SpeedEstimate(
      val wpm: Int,
      val isMeasured: Boolean,
    )

    fun countWordsInPlainText(text: String): Int {
      if (text.isBlank()) return 0
      var count = 0
      var inWord = false
      for (ch in text) {
        if (ch.isLetterOrDigit()) {
          if (!inWord) {
            inWord = true
            count++
          }
        } else if (ch != '\'' && ch != '\u200C' && ch != '-') {
          inWord = false
        }
      }
      return count
    }

    fun countWordsInHtmlFile(file: File): Int {
      val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return 0
      val noBodyHead = raw.substringAfter("<body", raw)
      val stripped =
        HTML_TAG_REGEX
          .replace(noBodyHead, " ")
          .let { HTML_ENTITY_REGEX.replace(it, " ") }
          .let { WHITESPACE_REGEX.replace(it, " ") }
      return countWordsInPlainText(stripped)
    }

    fun isPredominantlyRtl(sample: String): Boolean {
      var rtl = 0
      var ltr = 0
      for (c in sample) {
        when (Character.getDirectionality(c)) {
          Character.DIRECTIONALITY_RIGHT_TO_LEFT,
          Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> rtl++
          Character.DIRECTIONALITY_LEFT_TO_RIGHT -> ltr++
        }
      }
      return rtl > ltr / 3 && rtl >= 12
    }

    fun resolveTextReadingSpeed(
      telemetry: BookTelemetry,
      totalBookWords: Int,
      dailyStats: List<DailyReadingStatEntity>,
      progressHistory: List<BookProgressPointEntity>,
      defaultWpm: Int = DEFAULT_LTR_WPM,
    ): SpeedEstimate {
      // 1. Direct per-book measured telemetry (seconds + words read)
      if (telemetry.totalSecondsRead >= 25L && telemetry.wordsReadEstimated >= 60L) {
        val wpm =
          ((telemetry.wordsReadEstimated.toDouble() * 60.0) / telemetry.totalSecondsRead.toDouble())
            .roundToInt()
            .coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM)
        return SpeedEstimate(wpm = wpm, isMeasured = true)
      }

      // 2. Book progress checkpoints delta if meaningful time + progress elapsed
      if (progressHistory.size >= 2 && totalBookWords > 0) {
        val sorted = progressHistory.sortedBy { it.recordedAt }
        val first = sorted.first()
        val last = sorted.last()
        val elapsedSeconds = ((last.recordedAt - first.recordedAt) / 1000L)
        val deltaPct = last.progressPercent - first.progressPercent
        if (elapsedSeconds in 30L..86_400L && deltaPct >= 0.4f) {
          val estimatedWords = (deltaPct / 100.0) * totalBookWords.toDouble()
          if (estimatedWords >= 40.0) {
            val wpm =
              ((estimatedWords * 60.0) / elapsedSeconds.toDouble())
                .roundToInt()
                .coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM)
            return SpeedEstimate(wpm = wpm, isMeasured = true)
          }
        }
      }

      // 3. Global library pace from DailyReadingStatEntity (assuming ~260 words per screen/page flip)
      val globalSecs = dailyStats.sumOf { it.readingSeconds.toLong() }
      val globalPages = dailyStats.sumOf { it.pagesRead.toLong() }
      if (globalSecs >= 120L && globalPages >= 5L) {
        val approxWords = globalPages * 260.0
        val wpm =
          ((approxWords * 60.0) / globalSecs.toDouble())
            .roundToInt()
            .coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM)
        return SpeedEstimate(wpm = wpm, isMeasured = true)
      }

      return SpeedEstimate(wpm = defaultWpm.coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM), isMeasured = false)
    }

    fun resolveFixedPageSpeed(
      telemetry: BookTelemetry,
      totalBookPages: Int,
      dailyStats: List<DailyReadingStatEntity>,
      progressHistory: List<BookProgressPointEntity>,
      defaultPagesPerHour: Int = 45,
    ): Pair<Int, Boolean> {
      if (telemetry.totalSecondsRead >= 25L && telemetry.unitsAdvanced >= 2) {
        val pph =
          ((telemetry.unitsAdvanced.toDouble() * 3600.0) / telemetry.totalSecondsRead.toDouble())
            .roundToInt()
            .coerceIn(10, 180)
        return pph to true
      }

      if (progressHistory.size >= 2 && totalBookPages > 0) {
        val sorted = progressHistory.sortedBy { it.recordedAt }
        val elapsedSec = (sorted.last().recordedAt - sorted.first().recordedAt) / 1000L
        val deltaPct = sorted.last().progressPercent - sorted.first().progressPercent
        if (elapsedSec in 30L..86_400L && deltaPct >= 0.4f) {
          val estPages = (deltaPct / 100.0) * totalBookPages
          if (estPages >= 1.5) {
            val pph = ((estPages * 3600.0) / elapsedSec.toDouble()).roundToInt().coerceIn(10, 180)
            return pph to true
          }
        }
      }

      val globalSecs = dailyStats.sumOf { it.readingSeconds.toLong() }
      val globalPages = dailyStats.sumOf { it.pagesRead.toLong() }
      if (globalSecs >= 120L && globalPages >= 5L) {
        val pph = ((globalPages.toDouble() * 3600.0) / globalSecs.toDouble()).roundToInt().coerceIn(10, 180)
        return pph to true
      }

      return defaultPagesPerHour to false
    }

    fun calculateReflowableInsights(
      bookId: String,
      format: BookFormat,
      chaptersWordCounts: List<Pair<String, Int>>,
      currentChapterIndex: Int,
      currentScrollFraction: Float,
      wordsPerMinute: Int,
      isMeasuredSpeed: Boolean,
      totalBookSecondsRead: Long,
    ): BookReadingInsights {
      val safeWpm = wordsPerMinute.coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM)
      val safeCurrentIdx = currentChapterIndex.coerceIn(0, (chaptersWordCounts.size - 1).coerceAtLeast(0))
      val safeScroll = currentScrollFraction.coerceIn(0f, 1f)

      val chapters =
        chaptersWordCounts.mapIndexed { idx, (title, wc) ->
          val safeWords = wc.coerceAtLeast(1)
          val estTotalMins = (safeWords.toFloat() / safeWpm.toFloat()).coerceAtLeast(0.4f)
          val frac =
            when {
              idx < safeCurrentIdx -> 1f
              idx == safeCurrentIdx -> safeScroll
              else -> 0f
            }
          val status =
            when {
              idx < safeCurrentIdx || (idx == safeCurrentIdx && frac >= 0.98f) -> ChapterProgressStatus.COMPLETED
              idx == safeCurrentIdx -> ChapterProgressStatus.IN_PROGRESS
              else -> ChapterProgressStatus.UPCOMING
            }
          val remMins = (estTotalMins * (1f - frac)).coerceAtLeast(0f)
          ChapterInsight(
            index = idx,
            title = title.ifBlank { "Chapter ${idx + 1}" },
            wordCount = safeWords,
            unitCount = 1,
            progressFraction = frac,
            status = status,
            estimatedTotalMinutes = estTotalMins,
            estimatedRemainingMinutes = remMins,
          )
        }

      val totalWords = chapters.sumOf { it.wordCount }
      val totalEstMins = chapters.sumOf { it.estimatedTotalMinutes.toDouble() }.toFloat()
      val totalRemMins = chapters.sumOf { it.estimatedRemainingMinutes.toDouble() }.toFloat()
      val currentRemMins = chapters.getOrNull(safeCurrentIdx)?.estimatedRemainingMinutes ?: 0f
      val completedCount = chapters.count { it.status == ChapterProgressStatus.COMPLETED }
      val pagesPerHour = ((safeWpm * 60f) / 260f).roundToInt().coerceIn(10, 160)

      return BookReadingInsights(
        bookId = bookId,
        format = format,
        wordsPerMinute = safeWpm,
        pagesPerHour = pagesPerHour,
        playbackSpeed = 1.0f,
        isMeasuredSpeed = isMeasuredSpeed,
        totalBookSecondsRead = totalBookSecondsRead,
        totalWords = totalWords,
        totalEstimatedMinutes = totalEstMins,
        remainingEstimatedMinutes = totalRemMins,
        currentChapterRemainingMinutes = currentRemMins,
        completedChaptersCount = completedCount,
        chapters = chapters,
      )
    }

    fun calculateFixedPageInsights(
      bookId: String,
      format: BookFormat,
      totalPages: Int,
      currentPageIndex: Int,
      pagesPerHour: Int,
      isMeasuredSpeed: Boolean,
      totalBookSecondsRead: Long,
    ): BookReadingInsights {
      val safeTotal = totalPages.coerceAtLeast(1)
      val safePage = currentPageIndex.coerceIn(0, safeTotal - 1)
      val safePph = pagesPerHour.coerceIn(10, 180)
      val minutesPerPage = 60f / safePph.toFloat()

      val groupSize =
        when {
          safeTotal <= 12 -> 1
          safeTotal <= 60 -> 5
          safeTotal <= 180 -> 15
          else -> ceil(safeTotal / 12.0).toInt().coerceAtLeast(15)
        }

      val sections = mutableListOf<ChapterInsight>()
      var start = 0
      var secIdx = 0
      while (start < safeTotal) {
        val end = minOf(safeTotal - 1, start + groupSize - 1)
        val unitCount = end - start + 1
        val frac =
          when {
            safePage > end -> 1f
            safePage < start -> 0f
            else -> ((safePage - start + 1).toFloat() / unitCount.toFloat()).coerceIn(0f, 1f)
          }
        val status =
          when {
            safePage > end || frac >= 0.99f -> ChapterProgressStatus.COMPLETED
            safePage in start..end -> ChapterProgressStatus.IN_PROGRESS
            else -> ChapterProgressStatus.UPCOMING
          }
        val totalMins = unitCount * minutesPerPage
        val remMins = (totalMins * (1f - frac)).coerceAtLeast(0f)
        val label = if (start == end) "Page ${start + 1}" else "Pages ${start + 1}–${end + 1}"
        sections.add(
          ChapterInsight(
            index = start, // jump target is first page of section
            title = label,
            wordCount = unitCount * (if (format == BookFormat.CBZ) 90 else 260),
            unitCount = unitCount,
            progressFraction = frac,
            status = status,
            estimatedTotalMinutes = totalMins,
            estimatedRemainingMinutes = remMins,
          )
        )
        start = end + 1
        secIdx++
      }

      val activeSection = sections.firstOrNull { it.status == ChapterProgressStatus.IN_PROGRESS } ?: sections.lastOrNull()
      val totalEstMins = sections.sumOf { it.estimatedTotalMinutes.toDouble() }.toFloat()
      val totalRemMins = sections.sumOf { it.estimatedRemainingMinutes.toDouble() }.toFloat()
      val approxWpm = ((safePph * 260f) / 60f).roundToInt().coerceIn(MIN_HUMAN_WPM, MAX_HUMAN_WPM)

      return BookReadingInsights(
        bookId = bookId,
        format = format,
        wordsPerMinute = approxWpm,
        pagesPerHour = safePph,
        playbackSpeed = 1.0f,
        isMeasuredSpeed = isMeasuredSpeed,
        totalBookSecondsRead = totalBookSecondsRead,
        totalWords = safeTotal * (if (format == BookFormat.CBZ) 90 else 260),
        totalEstimatedMinutes = totalEstMins,
        remainingEstimatedMinutes = totalRemMins,
        currentChapterRemainingMinutes = activeSection?.estimatedRemainingMinutes ?: 0f,
        completedChaptersCount = sections.count { it.status == ChapterProgressStatus.COMPLETED },
        chapters = sections,
      )
    }

    fun formatDurationSummary(minutes: Float, isFa: Boolean): String {
      if (minutes <= 0.05f) return if (isFa) "تکمیل" else "Done"
      if (minutes < 1f) return if (isFa) "< ۱ دقیقه" else "< 1m"
      val totalMins = minutes.roundToInt().coerceAtLeast(1)
      val hours = totalMins / 60
      val mins = totalMins % 60
      return when {
        hours > 0 && mins > 0 -> if (isFa) "$hours س $mins د" else "${hours}h ${mins}m"
        hours > 0 -> if (isFa) "$hours ساعت" else "${hours}h"
        else -> if (isFa) "$mins دقیقه" else "${mins}m"
      }
    }

    fun formatShortDuration(minutes: Float, isFa: Boolean): String {
      if (minutes <= 0.05f) return if (isFa) "۰ د" else "0m"
      if (minutes < 1f) return if (isFa) "<۱ د" else "<1m"
      val totalMins = minutes.roundToInt().coerceAtLeast(1)
      val h = totalMins / 60
      val m = totalMins % 60
      return when {
        h > 0 && m > 0 -> if (isFa) "${h}س ${m}د" else "${h}h ${m}m"
        h > 0 -> if (isFa) "${h}س" else "${h}h"
        else -> if (isFa) "${m}د" else "${m}m"
      }
    }
  }
}
