package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    BookEntity::class,
    BookmarkEntity::class,
    HighlightEntity::class,
    DailyReadingStatEntity::class,
    BookProgressPointEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class PageNestDatabase : RoomDatabase() {
  abstract fun pageNestDao(): PageNestDao

  companion object {
    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_reading_stats` (
              `dateKey` TEXT NOT NULL,
              `readingSeconds` INTEGER NOT NULL,
              `pagesRead` INTEGER NOT NULL,
              `lastUpdatedAt` INTEGER NOT NULL,
              PRIMARY KEY(`dateKey`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `book_progress_history` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `bookId` TEXT NOT NULL,
              `progressPercent` REAL NOT NULL,
              `pageOrUnitIndex` INTEGER NOT NULL,
              `recordedAt` INTEGER NOT NULL,
              `dateKey` TEXT NOT NULL,
              FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_progress_history_bookId` ON `book_progress_history` (`bookId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_progress_history_recordedAt` ON `book_progress_history` (`recordedAt`)"
          )
        }
      }

    @Volatile private var instance: PageNestDatabase? = null

    fun getInstance(context: Context): PageNestDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                PageNestDatabase::class.java,
                "pagenest.db",
              )
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { instance = it }
        }
  }
}
