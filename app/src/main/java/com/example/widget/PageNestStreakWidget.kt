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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
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

private data class StreakWidgetData(
  val streakDays: Int,
  val activeToday: Boolean,
  val isPersian: Boolean,
)

class PageNestStreakWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val data =
      withContext(Dispatchers.IO) {
        val db = PageNestDatabase.getInstance(context)
        val dao = db.pageNestDao()
        val prefs = ReaderPreferencesDataStore(context)
        val settings = prefs.settingsFlow.first()
        val dailyStats = dao.observeAllDailyStats().first()

        val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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

        val streak = StreakCalculator.computeActiveStreak(statsByDate)
        StreakWidgetData(
          streakDays = streak,
          activeToday = reconciledTodaySec >= 60 || todayPages > 0,
          isPersian = settings.appLanguage == AppLanguage.PERSIAN,
        )
      }

    provideContent {
      GlanceTheme {
        StreakWidgetContent(context = context, data = data)
      }
    }
  }
}

@Composable
private fun StreakWidgetContent(
  context: Context,
  data: StreakWidgetData,
) {
  val launchIntent =
    Intent(context, MainActivity::class.java).apply {
      putExtra(PageNestContinueWidget.EXTRA_OPEN_ROUTE, PageNestContinueWidget.ROUTE_STATS_DEEP_LINK)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

  val bgColor =
    if (data.activeToday) ColorProvider(Color(0xFFFFF4EC))
    else ColorProvider(Color(0xFFFAF7F2))
  val numberColor =
    if (data.activeToday) ColorProvider(Color(0xFFC84B11))
    else ColorProvider(Color(0xFF2D6A4F))
  val captionColor = ColorProvider(Color(0xFF6B655B))

  Column(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(bgColor)
        .cornerRadius(20.dp)
        .clickable(actionStartActivity(launchIntent))
        .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      provider = ImageProvider(R.drawable.ic_streak_flame),
      contentDescription = "Reading streak flame",
      modifier = GlanceModifier.size(28.dp),
    )
    Spacer(modifier = GlanceModifier.height(1.dp))
    Text(
      text = "${data.streakDays}",
      style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = numberColor),
      maxLines = 1,
    )
    Text(
      text =
        if (data.isPersian) "روز"
        else if (data.streakDays == 1) "DAY"
        else "DAYS",
      style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Medium, color = captionColor),
      maxLines = 1,
    )
  }
}

class PageNestStreakWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = PageNestStreakWidget()
}
