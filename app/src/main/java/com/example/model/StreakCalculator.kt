package com.example.model

import com.example.data.local.DailyReadingStatEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object StreakCalculator {
  fun computeActiveStreak(
    statsByDate: Map<String, DailyReadingStatEntity>,
    referenceTimeMillis: Long = System.currentTimeMillis(),
  ): Int {
    val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
    val todayKey = keyFormatter.format(cal.time)
    val todayActive =
      statsByDate[todayKey]?.let { it.readingSeconds >= 60 || it.pagesRead > 0 } == true

    if (!todayActive) {
      // Allow streak to remain intact today if yesterday met the daily activity threshold
      cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    var streak = 0
    for (i in 0 until 365) {
      val key = keyFormatter.format(cal.time)
      val stat = statsByDate[key]
      if (stat != null && (stat.readingSeconds >= 60 || stat.pagesRead > 0)) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
      } else {
        break
      }
    }
    return streak
  }
}
