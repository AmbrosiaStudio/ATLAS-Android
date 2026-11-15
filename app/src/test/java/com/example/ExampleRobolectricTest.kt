package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.PageNestDatabase
import com.example.data.repository.SampleLibrarySeeder
import com.example.engine.epub.EpubParser
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches PageNest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PageNest", appName)
  }

  @Test
  fun `epub seeder and parser extract metadata toc chapters and search results`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val parser = EpubParser(context)
    val epubFile = SampleLibrarySeeder.createSampleEpub(context)
    try {
      assertTrue(epubFile.exists() && epubFile.length() > 0)

      val metaResult = parser.extractMetadataAndCover(epubFile, "test-epub-1", "Fallback")
      assertTrue(metaResult.isSuccess)
      val (meta, coverPath) = metaResult.getOrThrow()
      assertEquals("Letters on Slow Reading", meta.title)
      assertEquals("Marcus Aurelius & Seneca", meta.author)
      assertEquals(4, meta.totalChapters)
      assertNotNull(coverPath)
      assertTrue(File(coverPath!!).exists())

      val loaded = parser.loadBook(epubFile, "test-epub-1", "Fallback").getOrThrow()
      assertEquals(4, loaded.chapters.size)
      assertEquals(4, loaded.toc.size)
      assertEquals("I. On Discursiveness in Reading", loaded.toc.first().title)

      val matches = parser.searchInBook(loaded.chapters, "solitude")
      assertTrue(matches.isNotEmpty())
      assertEquals(1, matches.first().chapterOrPageIndex)
    } finally {
      epubFile.delete()
    }
  }

  @Test
  fun `room database persists books bookmarks highlights and cascades deletes`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, PageNestDatabase::class.java).build()
    try {
      val dao = db.pageNestDao()
      val book =
        BookEntity(
          id = "b1",
          title = "Test Book",
          author = "Author",
          format = "EPUB",
          filePath = "/tmp/b1.epub",
          coverPath = null,
          fileSizeBytes = 1024L,
          totalUnits = 4,
        )
      dao.upsertBook(book)
      dao.updateReadingProgress("b1", 2, 0.45f, 61.2f, 123456L)

      val stored = dao.getBookById("b1")
      assertNotNull(stored)
      assertEquals(2, stored!!.currentChapterOrPage)
      assertEquals(0.45f, stored.currentScrollFraction, 0.001f)

      dao.insertBookmark(
        BookmarkEntity(
          bookId = "b1",
          title = "Chapter 3",
          chapterOrPageIndex = 2,
          scrollFraction = 0.45f,
          snippet = "Saved spot",
        )
      )
      dao.insertHighlight(
        HighlightEntity(
          bookId = "b1",
          chapterOrPageIndex = 2,
          selectedText = "quiet room",
          note = "Favorite passage",
          colorHex = "#FDE68A",
        )
      )

      assertEquals(1, dao.observeBookmarks("b1").first().size)
      val savedHighlights = dao.observeHighlights("b1").first()
      assertEquals(1, savedHighlights.size)
      assertEquals("Favorite passage", savedHighlights.first().note)

      dao.deleteBookById("b1")
      assertEquals(0, dao.observeBookmarks("b1").first().size)
      assertEquals(0, dao.observeHighlights("b1").first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `persian sample epub parses chapters toc and supports persian search`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val parser = EpubParser(context)
    val epubFile = SampleLibrarySeeder.createSamplePersianEpub(context)
    try {
      assertTrue(epubFile.exists() && epubFile.length() > 0)
      val (meta, coverPath) = parser.extractMetadataAndCover(epubFile, "test-fa-1", "Fallback").getOrThrow()
      assertEquals("در ستایش خواندن آرام", meta.title)
      assertEquals(2, meta.totalChapters)
      assertNotNull(coverPath)

      val loaded = parser.loadBook(epubFile, "test-fa-1", "Fallback").getOrThrow()
      assertEquals(2, loaded.chapters.size)
      assertEquals(2, loaded.toc.size)
      val matches = parser.searchInBook(loaded.chapters, "سعدی")
      assertTrue(matches.isNotEmpty())
    } finally {
      epubFile.delete()
    }
  }

  @Test
  fun `daily reading goal tracks time resets on new day and persists paged vs scroll layout`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.local.ReaderPreferencesDataStore(context)

    store.updateSettings { it.copy(dailyGoalMinutes = 35) }
    store.addReadingSeconds(deltaSeconds = 120, todayDateKey = "2026-11-14")
    store.addReadingSeconds(deltaSeconds = 60, todayDateKey = "2026-11-14")

    val sameDay = store.settingsFlow.first()
    assertEquals(35, sameDay.dailyGoalMinutes)
    assertEquals(180, sameDay.todayReadingSeconds)
    assertEquals("2026-11-14", sameDay.lastReadingDateKey)
    assertTrue(sameDay.totalReadingSecondsAllTime >= 180L)

    // Next day resets daily progress but preserves cumulative total
    val totalBeforeRollover = sameDay.totalReadingSecondsAllTime
    store.addReadingSeconds(deltaSeconds = 45, todayDateKey = "2026-11-15")
    val nextDay = store.settingsFlow.first()
    assertEquals(45, nextDay.todayReadingSeconds)
    assertEquals("2026-11-15", nextDay.lastReadingDateKey)
    assertEquals(totalBeforeRollover + 45L, nextDay.totalReadingSecondsAllTime)

    // Layout mode toggles cleanly between Book Pages and Vertical Scroll
    store.updateSettings { it.copy(readingLayoutMode = com.example.model.ReadingLayoutMode.SCROLL) }
    assertEquals(com.example.model.ReadingLayoutMode.SCROLL, store.settingsFlow.first().readingLayoutMode)
    store.updateSettings { it.copy(readingLayoutMode = com.example.model.ReadingLayoutMode.PAGED) }
    assertEquals(com.example.model.ReadingLayoutMode.PAGED, store.settingsFlow.first().readingLayoutMode)

    // Reading comfort settings, new themes, and paper texture persist cleanly
    store.updateSettings {
      it.copy(
        themeMode = com.example.model.ReadingThemeMode.NIGHT_BW,
        fontSizeSp = 22,
        lineHeightMultiplier = 1.9f,
        horizontalMarginDp = 36,
        verticalMarginDp = 32,
        paragraphSpacingEm = 1.3f,
        letterSpacingEm = 0.04f,
        fontWeightValue = 500,
        eyeComfortWarmth = 0.45f,
        showPageBorder = true,
        paperTextureEnabled = true,
      )
    }
    val comfort = store.settingsFlow.first()
    assertEquals(com.example.model.ReadingThemeMode.NIGHT_BW, comfort.themeMode)
    assertEquals(22, comfort.fontSizeSp)
    assertEquals(1.9f, comfort.lineHeightMultiplier, 0.001f)
    assertEquals(36, comfort.horizontalMarginDp)
    assertEquals(32, comfort.verticalMarginDp)
    assertEquals(1.3f, comfort.paragraphSpacingEm, 0.001f)
    assertEquals(0.04f, comfort.letterSpacingEm, 0.001f)
    assertEquals(500, comfort.fontWeightValue)
    assertEquals(0.45f, comfort.eyeComfortWarmth, 0.001f)
    assertTrue(comfort.showPageBorder)
    assertTrue(comfort.paperTextureEnabled)

    store.updateSettings {
      it.copy(
        themeMode = com.example.model.ReadingThemeMode.SAGE_MIST,
        batterySaverModeEnabled = true,
        volumeKeyPageTurnEnabled = true,
      )
    }
    val updated = store.settingsFlow.first()
    assertEquals(com.example.model.ReadingThemeMode.SAGE_MIST, updated.themeMode)
    assertTrue(updated.batterySaverModeEnabled)
    assertTrue(updated.volumeKeyPageTurnEnabled)
  }

  @Test
  fun `native epub html block parser extracts headings quotes anchors rtl and highlights`() {
    val html =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <html xmlns="http://www.w3.org/1999/xhtml">
      <body>
        <h1 id="ch1">I. On Slow Reading</h1>
        <p>First <strong>quiet</strong> sentence &mdash; with <em>italic</em> tone.</p>
        <blockquote id="quote-a">“Constant attention is key.”</blockquote>
        <ul><li>One &amp; Two</li></ul>
      </body>
      </html>
      """.trimIndent()

    val parsed = com.example.engine.epub.EpubHtmlBlockParser.parse(html, "/tmp/ch1.xhtml")
    assertEquals(4, parsed.blocks.size)
    val heading = parsed.blocks[0] as com.example.engine.epub.EpubContentBlock.Heading
    assertEquals(1, heading.level)
    assertTrue("ch1" in heading.anchors)
    val para = parsed.blocks[1] as com.example.engine.epub.EpubContentBlock.Paragraph
    assertTrue(para.text.text.contains("quiet sentence — with italic"))
    val decorated =
      com.example.engine.epub.EpubHtmlBlockParser.decorateAnnotatedText(
        base = para.text,
        highlights =
          listOf(
            HighlightEntity(
              bookId = "b",
              chapterOrPageIndex = 0,
              selectedText = "quiet sentence",
              colorHex = "#FDE68A",
            )
          ),
        activeSelectionRange = 0..5,
        selectionHighlightColor = androidx.compose.ui.graphics.Color.Green,
      )
    assertTrue(decorated.spanStyles.size >= 3)

    val faHtml =
      """
      <html dir="rtl" lang="fa"><body><h1>بخش اول</h1><p>خواندنِ عمیق&zwnj;تر</p></body></html>
      """.trimIndent()
    val faParsed = com.example.engine.epub.EpubHtmlBlockParser.parse(faHtml, "/tmp/fa.xhtml")
    assertTrue(faParsed.isRtlDocument)
    assertEquals(2, faParsed.blocks.size)
  }

  @Test
  fun `reading stats dashboard repository tracks daily time total pages read and book progress trend`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, PageNestDatabase::class.java).build()
    try {
      val dao = db.pageNestDao()
      val book =
        BookEntity(
          id = "stats-book-1",
          title = "The Enchiridion",
          author = "Epictetus",
          format = "EPUB",
          filePath = "/tmp/enchiridion.epub",
          coverPath = null,
          fileSizeBytes = 2048L,
          totalUnits = 10,
        )
      dao.upsertBook(book)

      dao.incrementDailyStats(
        dateKey = "2026-11-14",
        deltaSeconds = 900,
        deltaPages = 14,
        nowMillis = 1000L,
      )
      dao.incrementDailyStats(
        dateKey = "2026-11-15",
        deltaSeconds = 600,
        deltaPages = 9,
        nowMillis = 2000L,
      )
      dao.incrementDailyStats(
        dateKey = "2026-11-15",
        deltaSeconds = 300,
        deltaPages = 3,
        nowMillis = 3000L,
      )

      val stats = dao.observeAllDailyStats().first()
      assertEquals(2, stats.size)
      assertEquals(900, stats[0].readingSeconds)
      assertEquals(14, stats[0].pagesRead)
      assertEquals(900, stats[1].readingSeconds)
      assertEquals(12, stats[1].pagesRead)
      assertEquals(26, stats.sumOf { it.pagesRead })

      val repo =
        com.example.data.repository.ReaderRepository(
          dao = dao,
          preferences = com.example.data.local.ReaderPreferencesDataStore(context),
        )
      repo.recordBookProgressSnapshot("stats-book-1", 10f, 1, forceInitialOnly = true)
      repo.recordBookProgressSnapshot("stats-book-1", 25f, 2, forceInitialOnly = false)
      repo.recordBookProgressSnapshot("stats-book-1", 25.1f, 2, forceInitialOnly = false) // below 0.4% threshold
      repo.recordBookProgressSnapshot("stats-book-1", 48f, 4, forceInitialOnly = false)

      val history = dao.observeBookProgressHistory("stats-book-1").first()
      assertEquals(3, history.size)
      assertEquals(10f, history[0].progressPercent, 0.01f)
      assertEquals(25f, history[1].progressPercent, 0.01f)
      assertEquals(48f, history[2].progressPercent, 0.01f)

      dao.deleteBookById("stats-book-1")
      assertEquals(0, dao.observeBookProgressHistory("stats-book-1").first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `text formats converter parses txt and markdown with frontmatter chapters and toc`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val converter = com.example.engine.formats.TextFormatsConverter(context)

    val txtFile = File.createTempFile("sample_notes", ".txt", context.cacheDir)
    val mdFile = File.createTempFile("sample_essay", ".md", context.cacheDir)
    try {
      txtFile.writeText(
        """
        Chapter 1: Morning Calm
        The morning breeze carries quiet clarity across the desk.

        Chapter 2: Evening Notes
        A calm mind reads slowly and retains every nuance.
        """.trimIndent()
      )
      val txtBook =
        converter.loadBook(txtFile, com.example.model.BookFormat.TXT, "txt-test-1", "Notes").getOrThrow()
      assertEquals(2, txtBook.chapters.size)
      assertEquals("Chapter 1: Morning Calm", txtBook.toc.first().title)

      mdFile.writeText(
        """
        ---
        title: Calm Markdown Essay
        author: AmirSenpai
        ---

        # Introduction
        Welcome to **slow** reading.

        - First principle
        - Second principle

        # Deeper Practice
        > Attention is the rarest treasure.
        """.trimIndent()
      )
      val mdMeta =
        converter.extractMetadataAndCover(mdFile, com.example.model.BookFormat.MD, "md-test-1", "Fallback")
          .getOrThrow()
      assertEquals("Calm Markdown Essay", mdMeta.title)
      assertEquals("AmirSenpai", mdMeta.author)
      assertEquals(2, mdMeta.totalUnits)

      val mdLoaded =
        converter.loadBook(mdFile, com.example.model.BookFormat.MD, "md-test-1", "Fallback").getOrThrow()
      assertEquals(2, mdLoaded.chapters.size)
      assertEquals("Introduction", mdLoaded.toc[0].title)
      assertEquals("Deeper Practice", mdLoaded.toc[1].title)
    } finally {
      txtFile.delete()
      mdFile.delete()
    }
  }

  @Test
  fun `cbz engine sorts pages numerically and inspects comic archive cover`() = runTest {
    val sorted =
      listOf("page10.jpg", "page2.jpg", "page1.jpg", "cover.png").sortedWith(
        com.example.engine.cbz.CbzEngine::naturalCompare
      )
    assertEquals(listOf("cover.png", "page1.jpg", "page2.jpg", "page10.jpg"), sorted)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.engine.cbz.CbzEngine(context)
    val cbzFile = File.createTempFile("comic_test", ".cbz", context.cacheDir)
    try {
      val bmp = android.graphics.Bitmap.createBitmap(120, 180, android.graphics.Bitmap.Config.ARGB_8888)
      val pngBytes =
        java.io.ByteArrayOutputStream().also { out ->
          bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
        }.toByteArray()
      bmp.recycle()

      java.util.zip.ZipOutputStream(java.io.FileOutputStream(cbzFile)).use { zos ->
        listOf("02_panel.png", "10_end.png", "01_cover.png").forEach { name ->
          zos.putNextEntry(java.util.zip.ZipEntry(name))
          zos.write(pngBytes)
          zos.closeEntry()
        }
      }

      val (count, coverPath) = engine.inspectAndExtractCover(cbzFile, "cbz-test-1").getOrThrow()
      assertEquals(3, count)
      assertNotNull(coverPath)
      assertTrue(File(coverPath!!).exists())
    } finally {
      cbzFile.delete()
    }
  }

  @Test
  fun `room v3 manages custom shelves book metadata favorites and streak reminder delay`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, PageNestDatabase::class.java).build()
    try {
      val dao = db.pageNestDao()
      val book =
        BookEntity(
          id = "shelf-book-1",
          title = "Original Title",
          author = "Original Author",
          format = "FB2",
          filePath = "/tmp/book.fb2",
          coverPath = null,
          fileSizeBytes = 512L,
          totalUnits = 6,
        )
      dao.upsertBook(book)
      dao.updateBookMetadata(
        bookId = "shelf-book-1",
        title = "Edited Title",
        author = "Edited Author",
        readingStatus = com.example.model.ReadingStatus.READING.name,
        isFavorite = true,
        coverPath = "/tmp/custom_cover.jpg",
      )

      val updated = dao.getBookById("shelf-book-1")
      assertNotNull(updated)
      assertEquals("Edited Title", updated!!.title)
      assertEquals("Edited Author", updated.author)
      assertEquals(com.example.model.ReadingStatus.READING, updated.status())
      assertTrue(updated.isFavorite)

      val shelfId = dao.insertShelf(com.example.data.local.ShelfEntity(name = "Philosophy"))
      dao.insertBookShelfRef(com.example.data.local.BookShelfCrossRef(bookId = "shelf-book-1", shelfId = shelfId))
      assertEquals(1, dao.observeAllShelves().first().size)
      assertEquals(1, dao.observeAllBookShelfRefs().first().size)

      dao.deleteShelfById(shelfId)
      assertEquals(0, dao.observeAllShelves().first().size)
      assertEquals(0, dao.observeAllBookShelfRefs().first().size)
    } finally {
      db.close()
    }

    val delayMs = com.example.notifications.StreakReminderScheduler.computeDelayMillisUntil(20, 30)
    assertTrue(delayMs in 60_000L..86_460_000L)
  }
}
