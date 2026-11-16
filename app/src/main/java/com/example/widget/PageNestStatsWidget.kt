package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.local.DailyReadingStatEntity
import com.example.data.local.PageNestDatabase
import com.example.data.local.ReaderPreferencesDataStore
import com.example.model.AppLanguage
import com.example.model.StreakCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private data class StatsWidgetDayBar(
  val label: String,
  val minutes: Int,
  val goalMet: Boolean,
  val isToday: Boolean,
)

private data class StatsWidgetData(
  val todayMinutes: Int,
  val dailyGoalMinutes: Int,
  val todayPages: Int,
  val totalPagesAllTime: Int,
  val allTimeMinutes: Int,
  val streakDays: Int,
  val booksCount: Int,
  val last7Days: List<StatsWidgetDayBar>,
  val isPersian: Boolean,
)

class PageNestStatsWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val data =
      withContext(Dispatchers.IO) {
        val db = PageNestDatabase.getInstance(context)
        val dao = db.pageNestDao()
        val prefs = ReaderPreferencesDataStore(context)
        val settings = prefs.settingsFlow.first()
        val dailyStats = dao.observeAllDailyStats().first()
        val books = dao.observeAllBooks().first()

        val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val isFa = settings.appLanguage == AppLanguage.PERSIAN
        val dayLocale = if (isFa) Locale("fa") else Locale.US
        val shortDayFormatter = SimpleDateFormat("E", dayLocale)
        val todayKey = keyFormatter.format(Calendar.getInstance().time)

        val statsByDate = dailyStats.associateBy { it.dateKey }.toMutableMap()
        val dsTodaySec =
          if (settings.lastReadingDateKey == todayKey) settings.todayReadingSeconds else 0
        val existingToday = statsByDate[todayKey]
        val reconciledTodaySec = maxOf(existingToday?.readingSeconds ?: 0, dsTodaySec)
        val todayPages = existingToday?.pagesRead ?: 0
        if (reconciledTodaySec > 0 || todayPages > 0) {
          statsByDate[todayKey] =
            DailyReadingStatEntity(
              dateKey = todayKey,
              readingSeconds = reconciledTodaySec,
              pagesRead = todayPages,
              lastUpdatedAt = existingToday?.lastUpdatedAt ?: System.currentTimeMillis(),
            )
        }

        val todayMinutes =
          if (reconciledTodaySec in 1..59) 1 else reconciledTodaySec / 60
        val totalPages = statsByDate.values.sumOf { it.pagesRead }
        val allTimeSeconds =
          maxOf(
            settings.totalReadingSecondsAllTime,
            statsByDate.values.sumOf { it.readingSeconds.toLong() },
          )
        val streak = StreakCalculator.computeActiveStreak(statsByDate)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val bars =
          (0 until 7).map { idx ->
            val key = keyFormatter.format(cal.time)
            val entry = statsByDate[key]
            val mins = ((entry?.readingSeconds ?: 0) / 60)
            val label = shortDayFormatter.format(cal.time).take(2)
            val bar =
              StatsWidgetDayBar(
                label = label,
                minutes = mins,
                goalMet = (entry?.readingSeconds ?: 0) >= settings.dailyGoalMinutes * 60,
                isToday = idx == 6,
              )
            cal.add(Calendar.DAY_OF_YEAR, 1)
            bar
          }

        StatsWidgetData(
          todayMinutes = todayMinutes,
          dailyGoalMinutes = settings.dailyGoalMinutes,
          todayPages = todayPages,
          totalPagesAllTime = totalPages,
          allTimeMinutes = (allTimeSeconds / 60L).toInt(),
          streakDays = streak,
          booksCount = books.size,
          last7Days = bars,
          isPersian = isFa,
        )
      }

    provideContent {
      GlanceTheme {
        StatsWidgetContent(context = context, data = data)
      }
    }
  }
}

@Composable
private fun StatsWidgetContent(
  context: Context,
  data: StatsWidgetData,
) {
  val launchIntent =
    Intent(context, MainActivity::class.java).apply {
      putExtra(PageNestContinueWidget.EXTRA_OPEN_ROUTE, PageNestContinueWidget.ROUTE_STATS_DEEP_LINK)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

  val bgColor = ColorProvider(Color(0xFFFAF7F2))
  val cardSurface = ColorProvider(Color(0xFFFFFFFF))
  val inkColor = ColorProvider(Color(0xFF1E1C18))
  val mutedColor = ColorProvider(Color(0xFF6B655B))
  val accentColor = ColorProvider(Color(0xFF2D6A4F))
  val softGreen = ColorProvider(Color(0xFF74C69D))
  val trackColor = ColorProvider(Color(0xFFE7E2D8))
  val flameTintBg = ColorProvider(Color(0xFFFFEFE6))
  val flameInk = ColorProvider(Color(0xFFC84B11))

  val totalHours = data.allTimeMinutes / 60
  val remMins = data.allTimeMinutes % 60
  val totalTimeFormatted =
    if (totalHours > 0) {
      if (data.isPersian) "${totalHours}س ${remMins}د" else "${totalHours}h ${remMins}m"
    } else {
      if (data.isPersian) "${remMins} د" else "${remMins}m"
    }

  val maxBarMins = maxOf(data.dailyGoalMinutes, data.last7Days.maxOfOrNull { it.minutes } ?: 1, 1)

  Column(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(bgColor)
        .cornerRadius(20.dp)
        .clickable(actionStartActivity(launchIntent))
        .padding(12.dp),
  ) {
    // Header Row: Title + Streak Badge
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (data.isPersian) "آمار مطالعه پیج‌نست" else "PageNest Statistics",
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor),
        modifier = GlanceModifier.defaultWeight(),
        maxLines = 1,
      )
      Box(
        modifier =
          GlanceModifier
            .background(flameTintBg)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(R.drawable.ic_streak_flame),
            contentDescription = null,
            modifier = GlanceModifier.size(13.dp),
          )
          Spacer(modifier = GlanceModifier.width(3.dp))
          Text(
            text = if (data.isPersian) "${data.streakDays} روز" else "${data.streakDays}d",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = flameInk),
          )
        }
      }
    }

    Spacer(modifier = GlanceModifier.height(8.dp))

    // Key Metrics Row
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatMiniCell(
        label = if (data.isPersian) "امروز" else "Today",
        value =
          if (data.isPersian) "${data.todayMinutes}/${data.dailyGoalMinutes}د"
          else "${data.todayMinutes}/${data.dailyGoalMinutes}m",
        surfaceColor = cardSurface,
        inkColor = inkColor,
        mutedColor = mutedColor,
        accentColor = accentColor,
        modifier = GlanceModifier.defaultWeight(),
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      StatMiniCell(
        label = if (data.isPersian) "صفحات" else "Pages",
        value = "${data.totalPagesAllTime}",
        surfaceColor = cardSurface,
        inkColor = inkColor,
        mutedColor = mutedColor,
        accentColor = accentColor,
        modifier = GlanceModifier.defaultWeight(),
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      StatMiniCell(
        label = if (data.isPersian) "کل زمان" else "All-Time",
        value = totalTimeFormatted,
        surfaceColor = cardSurface,
        inkColor = inkColor,
        mutedColor = mutedColor,
        accentColor = accentColor,
        modifier = GlanceModifier.defaultWeight(),
      )
    }

    Spacer(modifier = GlanceModifier.height(8.dp))

    // 7-Day Activity Bar Chart
    Row(
      modifier =
        GlanceModifier
          .fillMaxWidth()
          .defaultWeight()
          .background(cardSurface)
          .cornerRadius(12.dp)
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      data.last7Days.forEach { bar ->
        val barHeightDp =
          if (bar.minutes <= 0) 3.dp
          else ((bar.minutes.toFloat() / maxBarMins.toFloat()).coerceIn(0.15f, 1f) * 26f).dp
        val barColor =
          when {
            bar.goalMet -> accentColor
            bar.minutes > 0 -> softGreen
            else -> trackColor
          }
        Column(
          modifier = GlanceModifier.defaultWeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalAlignment = Alignment.Bottom,
        ) {
          Box(
            modifier =
              GlanceModifier
                .width(12.dp)
                .height(barHeightDp)
                .background(barColor)
                .cornerRadius(4.dp),
          ) {}
          Spacer(modifier = GlanceModifier.height(3.dp))
          Text(
            text = bar.label,
            style =
              TextStyle(
                fontSize = 9.sp,
                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (bar.isToday) accentColor else mutedColor,
              ),
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun StatMiniCell(
  label: String,
  value: String,
  surfaceColor: ColorProvider,
  inkColor: ColorProvider,
  mutedColor: ColorProvider,
  accentColor: ColorProvider,
  modifier: GlanceModifier = GlanceModifier,
) {
  Column(
    modifier =
      modifier
        .background(surfaceColor)
        .cornerRadius(12.dp)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = value,
      style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor),
      maxLines = 1,
    )
    Text(
      text = label,
      style = TextStyle(fontSize = 9.sp, color = mutedColor),
      maxLines = 1,
    )
  }
}

class PageNestStatsWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = PageNestStatsWidget()
}
