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
    ShelfEntity::class,
    BookShelfCrossRef::class,
    WriterBookEntity::class,
    WriterChapterEntity::class,
  ],
  version = 5,
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

    val MIGRATION_2_3: Migration =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE `books` ADD COLUMN `readingStatus` TEXT NOT NULL DEFAULT 'UNREAD'")
          db.execSQL("ALTER TABLE `books` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
          db.execSQL(
            "UPDATE `books` SET `readingStatus` = CASE WHEN `progressPercent` >= 98.0 THEN 'FINISHED' WHEN `progressPercent` >= 1.0 OR `lastOpenedAt` > 0 THEN 'READING' ELSE 'UNREAD' END"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shelves` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `book_shelf_cross_ref` (
              `bookId` TEXT NOT NULL,
              `shelfId` INTEGER NOT NULL,
              PRIMARY KEY(`bookId`, `shelfId`),
              FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
              FOREIGN KEY(`shelfId`) REFERENCES `shelves`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_shelf_cross_ref_bookId` ON `book_shelf_cross_ref` (`bookId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_shelf_cross_ref_shelfId` ON `book_shelf_cross_ref` (`shelfId`)"
          )
        }
      }

    val MIGRATION_3_4: Migration =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `writer_books` (
              `id` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `author` TEXT NOT NULL,
              `synopsis` TEXT NOT NULL,
              `isRtl` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              `publishedBookId` TEXT,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `writer_chapters` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `bookId` TEXT NOT NULL,
              `orderIndex` INTEGER NOT NULL,
              `title` TEXT NOT NULL,
              `bodyText` TEXT NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              FOREIGN KEY(`bookId`) REFERENCES `writer_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_writer_chapters_bookId` ON `writer_chapters` (`bookId`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_writer_chapters_orderIndex` ON `writer_chapters` (`orderIndex`)"
          )
        }
      }

    val MIGRATION_4_5: Migration =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE `writer_books` ADD COLUMN `coverThemeId` INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE `writer_books` ADD COLUMN `coverImagePath` TEXT")
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
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
              .build()
              .also { instance = it }
        }
  }
}
