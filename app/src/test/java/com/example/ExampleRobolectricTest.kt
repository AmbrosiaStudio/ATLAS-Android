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

  @Test
  fun `monetization config computes random and fixed tip delays and persists pro entitlement`() = runTest {
    val randomLow = com.example.config.MonetizationConfig.computeNextPromptDelaySeconds(true, 30, 0.0)
    val randomHigh = com.example.config.MonetizationConfig.computeNextPromptDelaySeconds(true, 30, 0.99)
    assertTrue(randomLow >= com.example.config.MonetizationConfig.RANDOM_INTERVAL_MIN_MINUTES * 60L)
    assertTrue(randomHigh <= com.example.config.MonetizationConfig.RANDOM_INTERVAL_MAX_MINUTES * 60L)

    val fixed = com.example.config.MonetizationConfig.computeNextPromptDelaySeconds(false, 45)
    assertEquals(45 * 60L, fixed)

    assertTrue(com.example.config.MonetizationConfig.isValidSupporterCode("pagenest-pro"))
    assertTrue(!com.example.config.MonetizationConfig.isValidSupporterCode("INVALID-KEY"))
    assertTrue(com.example.config.MonetizationConfig.activeIranTipLinks().isNotEmpty())
    assertTrue(com.example.config.MonetizationConfig.activeGlobalTipLinks().isNotEmpty())

    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.local.ReaderPreferencesDataStore(context)
    store.updateSettings {
      it.copy(
        isProUnlocked = true,
        proSupporterNote = "PAGENEST-PRO",
        tipPromptMode = com.example.model.TipPromptMode.FIXED_USAGE,
        tipFixedMinutes = 40,
        hasSupportedViaTipJar = true,
      )
    }
    val loaded = store.settingsFlow.first()
    assertTrue(loaded.isProUnlocked)
    assertEquals("PAGENEST-PRO", loaded.proSupporterNote)
    assertEquals(com.example.model.TipPromptMode.FIXED_USAGE, loaded.tipPromptMode)
    assertEquals(40, loaded.tipFixedMinutes)
    assertTrue(loaded.hasSupportedViaTipJar)
  }

  @Test
  fun `room v4 writer studio persists manuscripts chapters and cascades deletes`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, PageNestDatabase::class.java).build()
    try {
      val dao = db.pageNestDao()
      val book =
        com.example.data.local.WriterBookEntity(
          id = "manuscript-1",
          title = "Quiet Harbor",
          author = "AmirSenpai",
          synopsis = "Short essays on stillness.",
          isRtl = false,
          createdAt = 1000L,
          updatedAt = 2000L,
        )
      dao.upsertWriterBook(book)
      val ch1Id =
        dao.insertWriterChapter(
          com.example.data.local.WriterChapterEntity(
            bookId = "manuscript-1",
            orderIndex = 0,
            title = "Chapter 1",
            bodyText = "Silence at dawn.",
            updatedAt = 2000L,
          )
        )
      val ch2Id =
        dao.insertWriterChapter(
          com.example.data.local.WriterChapterEntity(
            bookId = "manuscript-1",
            orderIndex = 1,
            title = "Chapter 2",
            bodyText = "Lantern by the shore.",
            updatedAt = 2500L,
          )
        )

      assertEquals(1, dao.observeAllWriterBooks().first().size)
      val chapters = dao.getWriterChapters("manuscript-1")
      assertEquals(2, chapters.size)
      assertEquals("Chapter 1", chapters[0].title)
      assertEquals("Chapter 2", chapters[1].title)

      dao.updateWriterChapterContent(ch1Id, "Prologue", "Updated prose here.", 3000L)
      assertEquals("Prologue", dao.getWriterChapters("manuscript-1").first().title)

      dao.updateWriterBookPublishedId("manuscript-1", "lib-book-99", 4000L)
      assertEquals("lib-book-99", dao.getWriterBook("manuscript-1")?.publishedBookId)

      dao.deleteWriterChapter(ch2Id)
      assertEquals(1, dao.getWriterChapters("manuscript-1").size)

      dao.deleteWriterBook("manuscript-1")
      assertEquals(0, dao.observeAllWriterBooks().first().size)
      assertEquals(0, dao.getWriterChapters("manuscript-1").size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `writer epub exporter builds valid ltr and rtl epubs parsed cleanly by epub parser`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val parser = EpubParser(context)

    val ltrBook =
      com.example.data.local.WriterBookEntity(
        id = "studio-ltr",
        title = "Studio Craft",
        author = "AmirSenpai",
        synopsis = "Notes on writing in peace.",
        isRtl = false,
      )
    val ltrChapters =
      listOf(
        com.example.data.local.WriterChapterEntity(
          id = 1L,
          bookId = "studio-ltr",
          orderIndex = 0,
          title = "Opening Pages",
          bodyText = "## First Beat\n> Read slowly and write honestly.\n- Draft calmly with **clarity**.",
        ),
        com.example.data.local.WriterChapterEntity(
          id = 2L,
          bookId = "studio-ltr",
          orderIndex = 1,
          title = "Second Movement",
          bodyText = "Every quiet morning builds another page.",
        ),
      )

    val ltrEpub = com.example.engine.writer.WriterEpubExporter.buildEpubFile(context, ltrBook, ltrChapters)
    try {
      assertTrue(ltrEpub.exists() && ltrEpub.length() > 0)
      val (meta, coverPath) = parser.extractMetadataAndCover(ltrEpub, "studio-ltr-test", "Fallback").getOrThrow()
      assertEquals("Studio Craft", meta.title)
      assertEquals("AmirSenpai", meta.author)
      assertEquals(2, meta.totalChapters)
      assertNotNull(coverPath)
      assertTrue(File(coverPath!!).exists())

      val loaded = parser.loadBook(ltrEpub, "studio-ltr-test", "Fallback").getOrThrow()
      assertEquals(2, loaded.chapters.size)
      assertEquals("Opening Pages", loaded.toc[0].title)
      assertEquals("Second Movement", loaded.toc[1].title)

      val mdExport = com.example.engine.writer.WriterEpubExporter.buildMarkdownExport(ltrBook, ltrChapters)
      assertTrue(mdExport.contains("# Studio Craft"))
      assertTrue(mdExport.contains("## Opening Pages"))
      assertTrue(com.example.engine.writer.WriterEpubExporter.countWords(ltrChapters[0].bodyText) >= 10)
    } finally {
      ltrEpub.delete()
    }

    val rtlBook =
      com.example.data.local.WriterBookEntity(
        id = "studio-rtl",
        title = "دفترچه آرامش",
        author = "نویسنده",
        synopsis = "",
        isRtl = true,
      )
    val rtlChapters =
      listOf(
        com.example.data.local.WriterChapterEntity(
          id = 3L,
          bookId = "studio-rtl",
          orderIndex = 0,
          title = "فصل نخست",
          bodyText = "نوشتن در سکوت صبحگاهی آرامش‌بخش است.",
        )
      )
    val rtlEpub = com.example.engine.writer.WriterEpubExporter.buildEpubFile(context, rtlBook, rtlChapters)
    try {
      val loadedRtl = parser.loadBook(rtlEpub, "studio-rtl-test", "Fallback").getOrThrow()
      assertEquals(1, loadedRtl.chapters.size)
      assertEquals("فصل نخست", loadedRtl.toc.first().title)
    } finally {
      rtlEpub.delete()
    }
  }

  @Test
  fun `streak calculator computes active yesterday preserved and broken streaks accurately`() {
    val cal = java.util.Calendar.getInstance()
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val today = fmt.format(cal.time)
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val yesterday = fmt.format(cal.time)
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val twoDaysAgo = fmt.format(cal.time)
    cal.add(java.util.Calendar.DAY_OF_YEAR, -2)
    val fourDaysAgo = fmt.format(cal.time)

    val unbrokenMap =
      mapOf(
        today to com.example.data.local.DailyReadingStatEntity(today, 120, 2, 1L),
        yesterday to com.example.data.local.DailyReadingStatEntity(yesterday, 90, 1, 1L),
        twoDaysAgo to com.example.data.local.DailyReadingStatEntity(twoDaysAgo, 60, 0, 1L),
        fourDaysAgo to com.example.data.local.DailyReadingStatEntity(fourDaysAgo, 300, 5, 1L),
      )
    assertEquals(3, com.example.model.StreakCalculator.computeActiveStreak(unbrokenMap))

    val preservedYesterdayMap =
      mapOf(
        yesterday to com.example.data.local.DailyReadingStatEntity(yesterday, 0, 3, 1L),
        twoDaysAgo to com.example.data.local.DailyReadingStatEntity(twoDaysAgo, 180, 2, 1L),
      )
    assertEquals(2, com.example.model.StreakCalculator.computeActiveStreak(preservedYesterdayMap))

    val brokenMap =
      mapOf(
        fourDaysAgo to com.example.data.local.DailyReadingStatEntity(fourDaysAgo, 300, 5, 1L),
      )
    assertEquals(0, com.example.model.StreakCalculator.computeActiveStreak(brokenMap))
  }

  @Test
  fun `v3_2_0 tap zone settings ambient focus prefs and tap zone resolver persist accurately`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.local.ReaderPreferencesDataStore(context)

    store.updateSettings {
      it.copy(
        leftTapAction = com.example.model.TapZoneAction.QUICK_BOOKMARK,
        centerTapAction = com.example.model.TapZoneAction.TOGGLE_CONTROLS,
        rightTapAction = com.example.model.TapZoneAction.NEXT_PAGE,
        edgeBrightnessSwipeEnabled = false,
        doubleTapBookmarkEnabled = true,
        ambientVolume = 0.72f,
        focusWorkMinutes = 45,
        focusBreakMinutes = 10,
      )
    }

    val loaded = store.settingsFlow.first()
    assertEquals(com.example.model.TapZoneAction.QUICK_BOOKMARK, loaded.leftTapAction)
    assertEquals(com.example.model.TapZoneAction.TOGGLE_CONTROLS, loaded.centerTapAction)
    assertEquals(com.example.model.TapZoneAction.NEXT_PAGE, loaded.rightTapAction)
    assertTrue(!loaded.edgeBrightnessSwipeEnabled)
    assertTrue(loaded.doubleTapBookmarkEnabled)
    assertEquals(0.72f, loaded.ambientVolume, 0.001f)
    assertEquals(45, loaded.focusWorkMinutes)
    assertEquals(10, loaded.focusBreakMinutes)

    assertEquals(
      com.example.model.TapZoneAction.QUICK_BOOKMARK,
      com.example.ui.reader.resolveTapZoneAction(
        tapOffset = androidx.compose.ui.geometry.Offset(100f, 100f),
        containerWidthPx = 1000,
        settings = loaded,
      ),
    )
    assertEquals(
      com.example.model.TapZoneAction.TOGGLE_CONTROLS,
      com.example.ui.reader.resolveTapZoneAction(
        tapOffset = androidx.compose.ui.geometry.Offset(500f, 100f),
        containerWidthPx = 1000,
        settings = loaded,
      ),
    )
    assertEquals(
      com.example.model.TapZoneAction.NEXT_PAGE,
      com.example.ui.reader.resolveTapZoneAction(
        tapOffset = androidx.compose.ui.geometry.Offset(900f, 100f),
        containerWidthPx = 1000,
        settings = loaded,
      ),
    )
  }

  @Test
  fun `opds catalog parser parses gutendex json and atom opds xml cleanly`() {
    val sampleJson =
      """
      {
        "count": 2,
        "results": [
          {
            "id": 1342,
            "title": "Pride and Prejudice",
            "authors": [{"name": "Austen, Jane"}],
            "languages": ["en"],
            "summaries": ["A classic manners novel."],
            "formats": {
              "image/jpeg": "https://example.org/1342.jpg",
              "application/epub+zip": "https://example.org/1342.epub"
            }
          },
          {
            "id": 9999,
            "title": "No Epub Entry",
            "authors": [{"name": "Anon"}],
            "languages": ["en"],
            "formats": {"text/plain": "https://example.org/9999.txt"}
          }
        ]
      }
      """.trimIndent()

    val gutendexItems = com.example.data.catalog.OpdsCatalogRepository.parseGutendexJson(sampleJson)
    assertEquals(1, gutendexItems.size)
    assertEquals("Pride and Prejudice", gutendexItems[0].title)
    assertEquals("Austen, Jane", gutendexItems[0].author)
    assertEquals("https://example.org/1342.epub", gutendexItems[0].epubDownloadUrl)
    assertEquals("https://example.org/1342.jpg", gutendexItems[0].coverUrl)

    val sampleOpdsXml =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dcterms="http://purl.org/dc/terms/">
        <entry>
          <id>urn:uuid:se-1</id>
          <title>Meditations</title>
          <author><name>Marcus Aurelius</name></author>
          <dcterms:language>en</dcterms:language>
          <summary>Stoic personal notes.</summary>
          <link rel="http://opds-spec.org/image/thumbnail" type="image/jpeg" href="/covers/meditations.jpg"/>
          <link rel="http://opds-spec.org/acquisition" type="application/epub+zip" href="/ebooks/meditations.epub"/>
        </entry>
      </feed>
      """.trimIndent()

    val opdsItems =
      com.example.data.catalog.OpdsCatalogRepository.parseOpdsAtomXml(
        xmlText = sampleOpdsXml,
        baseUrl = "https://standardebooks.org/feeds/opds/all",
        source = com.example.data.catalog.CatalogSource.STANDARD_EBOOKS,
      )
    assertEquals(1, opdsItems.size)
    assertEquals("Meditations", opdsItems[0].title)
    assertEquals("Marcus Aurelius", opdsItems[0].author)
    assertEquals("https://standardebooks.org/ebooks/meditations.epub", opdsItems[0].epubDownloadUrl)
    assertEquals("https://standardebooks.org/covers/meditations.jpg", opdsItems[0].coverUrl)
  }

  @Test
  fun `studio v5 supports cover theme persistence and chapter reordering in epub export`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, PageNestDatabase::class.java).build()
    try {
      val dao = db.pageNestDao()
      val libraryRepo =
        com.example.data.repository.LibraryRepository(
          context = context,
          dao = dao,
          epubParser = EpubParser(context),
          pdfEngine = com.example.engine.pdf.PdfEngine(context),
        )
      val writerRepo = com.example.data.repository.WriterRepository(context, dao, libraryRepo)
      val book = writerRepo.createManuscript("Cover Craft", "Amir", "Notes", isRtl = false)
      val ch2 = writerRepo.addChapter(book.id, "Second")
      val initialChapters = dao.getWriterChapters(book.id)
      assertEquals(2, initialChapters.size)

      // Move chapter 2 earlier -> should become index 0
      val moved = writerRepo.moveChapter(book.id, ch2.id, -1)
      assertTrue(moved)
      val reordered = dao.getWriterChapters(book.id)
      assertEquals("Second", reordered[0].title)
      assertEquals(0, reordered[0].orderIndex)
      assertEquals(1, reordered[1].orderIndex)

      // Persist cover theme 4 (Obsidian Gold)
      writerRepo.updateBookCover(book.id, coverThemeId = 4)
      val updatedBook = dao.getWriterBook(book.id)!!
      assertEquals(4, updatedBook.coverThemeId)

      val exported = com.example.engine.writer.WriterEpubExporter.buildEpubFile(context, updatedBook, reordered)
      try {
        val loaded = EpubParser(context).loadBook(exported, "v5-cover-test", "Fallback").getOrThrow()
        assertEquals("Second", loaded.toc.first().title)
      } finally {
        exported.delete()
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `ambient soundscape synthesizer generates non-clipping stereo audio for all sound types`() {
    val frames = 4096
    val pcm = ShortArray(frames * 2)
    val types =
      listOf(
        com.example.model.AmbientSoundType.RAIN,
        com.example.model.AmbientSoundType.FIREPLACE,
        com.example.model.AmbientSoundType.BROWN_NOISE,
        com.example.model.AmbientSoundType.OCEAN_WAVES,
      )
    types.forEach { soundType ->
      val synth =
        com.example.engine.audio.AmbientSoundPlayer.SoundscapeSynthesizer(
          type = soundType,
          sampleRate = 44100,
          seed = 42L,
        )
      // Render 4 chunks (~370ms) so fade-in settles and transient voices fire
      repeat(4) { synth.renderStereoFrames(pcm, frames) }

      var sumSqL = 0.0
      var sumSqR = 0.0
      var diffSq = 0.0
      var maxAbs = 0
      for (i in 0 until frames) {
        val l = pcm[i * 2].toInt()
        val r = pcm[i * 2 + 1].toInt()
        val absMax = maxOf(kotlin.math.abs(l), kotlin.math.abs(r))
        if (absMax > maxAbs) maxAbs = absMax
        sumSqL += l.toDouble() * l.toDouble()
        sumSqR += r.toDouble() * r.toDouble()
        val d = (l - r).toDouble()
        diffSq += d * d
      }
      val rmsL = kotlin.math.sqrt(sumSqL / frames)
      val rmsR = kotlin.math.sqrt(sumSqR / frames)
      val stereoDiffRms = kotlin.math.sqrt(diffSq / frames)

      assertTrue("$soundType should produce audible signal (rmsL=$rmsL)", rmsL > 120.0)
      assertTrue("$soundType should produce audible right channel (rmsR=$rmsR)", rmsR > 120.0)
      assertTrue("$soundType should have true stereo decorrelation (diff=$stereoDiffRms)", stereoDiffRms > 40.0)
      assertTrue("$soundType should never clip (maxAbs=$maxAbs)", maxAbs <= 32767)
    }
  }
}
