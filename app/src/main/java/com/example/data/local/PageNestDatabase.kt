package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [BookEntity::class, BookmarkEntity::class, HighlightEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class PageNestDatabase : RoomDatabase() {
  abstract fun pageNestDao(): PageNestDao

  companion object {
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
              .build()
              .also { instance = it }
        }
  }
}
