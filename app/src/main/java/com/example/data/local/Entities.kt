package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.BookFormat

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey val id: String,
  val title: String,
  val author: String,
  val format: String, // "EPUB" or "PDF"
  val filePath: String,
  val coverPath: String?,
  val fileSizeBytes: Long,
  val totalUnits: Int, // Chapter count for EPUB, page count for PDF
  val currentChapterOrPage: Int = 0, // 0-indexed chapter for EPUB, 0-indexed page for PDF
  val currentScrollFraction: Float = 0f, // 0f..1f within current chapter for EPUB
  val progressPercent: Float = 0f, // 0f..100f overall progress
  val lastOpenedAt: Long = 0L,
  val addedAt: Long = System.currentTimeMillis(),
) {
  fun bookFormat(): BookFormat =
    if (format.equals("PDF", ignoreCase = true)) BookFormat.PDF else BookFormat.EPUB
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
