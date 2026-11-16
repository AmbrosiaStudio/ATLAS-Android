package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PageNestDao {
  @Query("SELECT * FROM books ORDER BY CASE WHEN lastOpenedAt > 0 THEN lastOpenedAt ELSE addedAt END DESC")
  fun observeAllBooks(): Flow<List<BookEntity>>

  @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
  fun observeBook(bookId: String): Flow<BookEntity?>

  @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
  suspend fun getBookById(bookId: String): BookEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBook(book: BookEntity)

  @Query(
    """
    UPDATE books
    SET currentChapterOrPage = :chapterOrPage,
        currentScrollFraction = :scrollFraction,
        progressPercent = :progressPercent,
        lastOpenedAt = :lastOpenedAt
    WHERE id = :bookId
    """
  )
  suspend fun updateReadingProgress(
    bookId: String,
    chapterOrPage: Int,
    scrollFraction: Float,
    progressPercent: Float,
    lastOpenedAt: Long,
  )

  @Query("UPDATE books SET totalUnits = :totalUnits WHERE id = :bookId")
  suspend fun updateTotalUnits(bookId: String, totalUnits: Int)

  @Query("DELETE FROM books WHERE id = :bookId")
  suspend fun deleteBookById(bookId: String)

  @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterOrPageIndex ASC, scrollFraction ASC")
  fun observeBookmarks(bookId: String): Flow<List<BookmarkEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: BookmarkEntity)

  @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
  suspend fun deleteBookmarkById(bookmarkId: Long)

  @Query(
    """
    DELETE FROM bookmarks
    WHERE bookId = :bookId AND chapterOrPageIndex = :chapterOrPage AND ABS(scrollFraction - :scrollFraction) < 0.05
    """
  )
  suspend fun deleteBookmarkNearLocation(bookId: String, chapterOrPage: Int, scrollFraction: Float)

  @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY chapterOrPageIndex ASC, createdAt DESC")
  fun observeHighlights(bookId: String): Flow<List<HighlightEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHighlight(highlight: HighlightEntity)

  @Query("DELETE FROM highlights WHERE id = :highlightId")
  suspend fun deleteHighlightById(highlightId: Long)

  // Daily Reading Stats
  @Query("SELECT * FROM daily_reading_stats ORDER BY dateKey ASC")
  fun observeAllDailyStats(): Flow<List<DailyReadingStatEntity>>

  @Query("SELECT * FROM daily_reading_stats WHERE dateKey = :dateKey LIMIT 1")
  suspend fun getDailyStat(dateKey: String): DailyReadingStatEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDailyStat(stat: DailyReadingStatEntity)

  @Transaction
  suspend fun incrementDailyStats(
    dateKey: String,
    deltaSeconds: Int,
    deltaPages: Int,
    nowMillis: Long,
    minSecondsFloorForToday: Int = 0,
  ) {
    val existing = getDailyStat(dateKey)
    val baseSeconds = maxOf(existing?.readingSeconds ?: 0, minSecondsFloorForToday)
    val nextSeconds = (baseSeconds + deltaSeconds.coerceAtLeast(0)).coerceAtLeast(0)
    val nextPages = ((existing?.pagesRead ?: 0) + deltaPages.coerceAtLeast(0)).coerceAtLeast(0)
    upsertDailyStat(
      DailyReadingStatEntity(
        dateKey = dateKey,
        readingSeconds = nextSeconds,
        pagesRead = nextPages,
        lastUpdatedAt = nowMillis,
      )
    )
  }

  // Book Progress Trend History
  @Query("SELECT * FROM book_progress_history WHERE bookId = :bookId ORDER BY recordedAt ASC")
  fun observeBookProgressHistory(bookId: String): Flow<List<BookProgressPointEntity>>

  @Query("SELECT * FROM book_progress_history WHERE bookId = :bookId ORDER BY recordedAt DESC LIMIT 1")
  suspend fun getLatestBookProgressPoint(bookId: String): BookProgressPointEntity?

  @Query("SELECT COUNT(*) FROM book_progress_history WHERE bookId = :bookId")
  suspend fun getBookProgressPointCount(bookId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookProgressPoint(point: BookProgressPointEntity)

  // Metadata, Status & Favorites
  @Query(
    """
    UPDATE books
    SET title = :title,
        author = :author,
        readingStatus = :readingStatus,
        isFavorite = :isFavorite,
        coverPath = :coverPath
    WHERE id = :bookId
    """
  )
  suspend fun updateBookMetadata(
    bookId: String,
    title: String,
    author: String,
    readingStatus: String,
    isFavorite: Boolean,
    coverPath: String?,
  )

  @Query("UPDATE books SET readingStatus = :readingStatus WHERE id = :bookId")
  suspend fun updateBookReadingStatus(bookId: String, readingStatus: String)

  @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
  suspend fun updateBookFavorite(bookId: String, isFavorite: Boolean)

  // Custom Shelves & Collections
  @Query("SELECT * FROM shelves ORDER BY createdAt ASC, name ASC")
  fun observeAllShelves(): Flow<List<ShelfEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertShelf(shelf: ShelfEntity): Long

  @Query("UPDATE shelves SET name = :name WHERE id = :shelfId")
  suspend fun renameShelf(shelfId: Long, name: String)

  @Query("DELETE FROM shelves WHERE id = :shelfId")
  suspend fun deleteShelfById(shelfId: Long)

  @Query("SELECT * FROM book_shelf_cross_ref")
  fun observeAllBookShelfRefs(): Flow<List<BookShelfCrossRef>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertBookShelfRef(ref: BookShelfCrossRef)

  @Query("DELETE FROM book_shelf_cross_ref WHERE bookId = :bookId AND shelfId = :shelfId")
  suspend fun deleteBookShelfRef(bookId: String, shelfId: Long)

  // PageNest Studio (Beta) — Writer Books & Chapters
  @Query("SELECT * FROM writer_books ORDER BY updatedAt DESC")
  fun observeAllWriterBooks(): Flow<List<WriterBookEntity>>

  @Query("SELECT * FROM writer_books WHERE id = :bookId LIMIT 1")
  fun observeWriterBook(bookId: String): Flow<WriterBookEntity?>

  @Query("SELECT * FROM writer_books WHERE id = :bookId LIMIT 1")
  suspend fun getWriterBook(bookId: String): WriterBookEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWriterBook(book: WriterBookEntity)

  @Query("DELETE FROM writer_books WHERE id = :bookId")
  suspend fun deleteWriterBook(bookId: String)

  @Query("SELECT * FROM writer_chapters ORDER BY bookId ASC, orderIndex ASC, id ASC")
  fun observeAllWriterChapters(): Flow<List<WriterChapterEntity>>

  @Query("SELECT * FROM writer_chapters WHERE bookId = :bookId ORDER BY orderIndex ASC, id ASC")
  fun observeWriterChapters(bookId: String): Flow<List<WriterChapterEntity>>

  @Query("SELECT * FROM writer_chapters WHERE bookId = :bookId ORDER BY orderIndex ASC, id ASC")
  suspend fun getWriterChapters(bookId: String): List<WriterChapterEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWriterChapter(chapter: WriterChapterEntity): Long

  @Query("UPDATE writer_chapters SET title = :title, bodyText = :bodyText, updatedAt = :updatedAt WHERE id = :chapterId")
  suspend fun updateWriterChapterContent(chapterId: Long, title: String, bodyText: String, updatedAt: Long)

  @Query("UPDATE writer_chapters SET orderIndex = :orderIndex WHERE id = :chapterId")
  suspend fun updateWriterChapterOrder(chapterId: Long, orderIndex: Int)

  @Query("DELETE FROM writer_chapters WHERE id = :chapterId")
  suspend fun deleteWriterChapter(chapterId: Long)

  @Query("UPDATE writer_books SET updatedAt = :updatedAt WHERE id = :bookId")
  suspend fun touchWriterBookUpdatedAt(bookId: String, updatedAt: Long)

  @Query("UPDATE writer_books SET publishedBookId = :publishedBookId, updatedAt = :updatedAt WHERE id = :bookId")
  suspend fun updateWriterBookPublishedId(bookId: String, publishedBookId: String, updatedAt: Long)
}
