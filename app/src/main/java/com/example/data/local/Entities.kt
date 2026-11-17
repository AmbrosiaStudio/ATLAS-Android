package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.BookFormat
import com.example.model.ReadingStatus

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey val id: String,
  val title: String,
  val author: String,
  val format: String, // EPUB, PDF, TXT, MD, FB2, CBZ
  val filePath: String,
  val coverPath: String?,
  val fileSizeBytes: Long,
  val totalUnits: Int, // Chapter/section count or page count
  val currentChapterOrPage: Int = 0, // 0-indexed chapter or page
  val currentScrollFraction: Float = 0f, // 0f..1f within current chapter
  val progressPercent: Float = 0f, // 0f..100f overall progress
  val lastOpenedAt: Long = 0L,
  val addedAt: Long = System.currentTimeMillis(),
  val readingStatus: String = ReadingStatus.UNREAD.name,
  val isFavorite: Boolean = false,
) {
  fun bookFormat(): BookFormat = BookFormat.fromString(format)

  fun status(): ReadingStatus = ReadingStatus.fromString(readingStatus)
}

@Entity(
  tableName = "bookmarks",
  foreignKeys = [
    ForeignKey(
      entity = BookEntity::class,
      parentColumns = ["id"],
      childColumns = ["bookId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("bookId")],
)
data class BookmarkEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: String,
  val title: String,
  val chapterOrPageIndex: Int,
  val scrollFraction: Float,
  val snippet: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "highlights",
  foreignKeys = [
    ForeignKey(
      entity = BookEntity::class,
      parentColumns = ["id"],
      childColumns = ["bookId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("bookId")],
)
data class HighlightEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: String,
  val chapterOrPageIndex: Int,
  val selectedText: String,
  val note: String = "",
  val colorHex: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_reading_stats")
data class DailyReadingStatEntity(
  @PrimaryKey val dateKey: String,
  val readingSeconds: Int = 0,
  val pagesRead: Int = 0,
  val lastUpdatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "book_progress_history",
  foreignKeys = [
    ForeignKey(
      entity = BookEntity::class,
      parentColumns = ["id"],
      childColumns = ["bookId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("bookId"), Index("recordedAt")],
)
data class BookProgressPointEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: String,
  val progressPercent: Float,
  val pageOrUnitIndex: Int,
  val recordedAt: Long = System.currentTimeMillis(),
  val dateKey: String,
)

@Entity(tableName = "shelves")
data class ShelfEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "book_shelf_cross_ref",
  primaryKeys = ["bookId", "shelfId"],
  foreignKeys = [
    ForeignKey(
      entity = BookEntity::class,
      parentColumns = ["id"],
      childColumns = ["bookId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = ShelfEntity::class,
      parentColumns = ["id"],
      childColumns = ["shelfId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("bookId"), Index("shelfId")],
)
data class BookShelfCrossRef(
  val bookId: String,
  val shelfId: Long,
)

@Entity(tableName = "writer_books")
data class WriterBookEntity(
  @PrimaryKey val id: String,
  val title: String,
  val author: String,
  val synopsis: String = "",
  val isRtl: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val publishedBookId: String? = null,
  val coverThemeId: Int = 0,
  val coverImagePath: String? = null,
)

@Entity(
  tableName = "writer_chapters",
  foreignKeys = [
    ForeignKey(
      entity = WriterBookEntity::class,
      parentColumns = ["id"],
      childColumns = ["bookId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("bookId"), Index("orderIndex")],
)
data class WriterChapterEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: String,
  val orderIndex: Int,
  val title: String,
  val bodyText: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
)

