package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.example.data.local.BookEntity
import com.example.data.local.PageNestDatabase
import com.example.data.local.ReaderPreferencesDataStore
import com.example.model.AppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private data class WidgetBookData(
  val book: BookEntity?,
  val coverBitmap: Bitmap?,
  val todayMinutes: Int,
  val dailyGoalMinutes: Int,
  val isPersian: Boolean,
  val isDark: Boolean,
)

class PageNestContinueWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val widgetData =
      withContext(Dispatchers.IO) {
        val db = PageNestDatabase.getInstance(context)
        val dao = db.pageNestDao()
        val prefs = ReaderPreferencesDataStore(context)
        val settings = prefs.settingsFlow.first()
        val lastBookId = prefs.lastOpenedBookIdFlow.first()
        val allBooks = dao.observeAllBooks().first()
        val activeBook =
          lastBookId?.let { idVal -> allBooks.firstOrNull { it.id == idVal } }
            ?: allBooks.firstOrNull()

        val coverBitmap =
          activeBook?.coverPath?.let { path -> decodeDownsampledWidgetCover(path) }

        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayDbSec = dao.getDailyStat(todayKey)?.readingSeconds ?: 0
        val todayPrefSec = if (settings.lastReadingDateKey == todayKey) settings.todayReadingSeconds else 0
        val todayMins = maxOf(todayDbSec, todayPrefSec) / 60

        WidgetBookData(
          book = activeBook,
          coverBitmap = coverBitmap,
          todayMinutes = todayMins,
          dailyGoalMinutes = settings.dailyGoalMinutes,
          isPersian = settings.appLanguage == AppLanguage.PERSIAN,
          isDark = settings.themeMode.isDark,
        )
      }

    provideContent {
      GlanceTheme {
        ContinueReadingWidgetContent(context = context, data = widgetData)
      }
    }
  }

  private fun decodeDownsampledWidgetCover(path: String): Bitmap? =
    runCatching {
      val file = File(path)
      if (!file.exists() || file.length() == 0L) return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      val maxW = 140
      var sample = 1
      while (bounds.outWidth / sample > maxW * 2) {
        sample *= 2
      }
      val opts =
        BitmapFactory.Options().apply {
          inSampleSize = sample
          inPreferredConfig = Bitmap.Config.RGB_565
        }
      BitmapFactory.decodeFile(file.absolutePath, opts)
    }.getOrNull()

  companion object {
    const val EXTRA_OPEN_BOOK_ID = "extra_open_book_id"
    const val EXTRA_OPEN_ROUTE = "extra_open_route"
    const val ROUTE_STATS_DEEP_LINK = "stats"

    suspend fun requestUpdate(context: Context) {
      WidgetUpdater.requestAllUpdates(context)
    }
  }
}

@Composable
private fun ContinueReadingWidgetContent(
  context: Context,
  data: WidgetBookData,
) {
  val book = data.book
  val launchIntent =
    Intent(context, MainActivity::class.java).apply {
      if (book != null) {
        putExtra(PageNestContinueWidget.EXTRA_OPEN_BOOK_ID, book.id)
      }
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

  val bgColor = ColorProvider(if (data.isDark) Color(0xFF111111) else Color(0xFFF5F5F2))
  val mutedSurface = ColorProvider(if (data.isDark) Color(0xFF242424) else Color(red = 0xEE, green = 0xEE, blue = 0xEB))
  val trackColor = ColorProvider(if (data.isDark) Color(0xFF303030) else Color(0xFFE6E6E2))
  val inkColor = ColorProvider(if (data.isDark) Color(0xFFF5F5F2) else Color(0xFF111111))
  val secondaryColor = ColorProvider(if (data.isDark) Color(0xFF999999) else Color(0xFF777777))
  val coverFallbackBg = ColorProvider(if (data.isDark) Color(0xFF1B1B1B) else Color(0xFF111111))
  val onCoverInk = ColorProvider(Color(0xFFF5F5F2))
  val symbolRes = if (data.isDark) R.drawable.ic_atlas_symbol_widget_dark else R.drawable.ic_atlas_symbol_widget

  Box(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(bgColor)
        .cornerRadius(14.dp)
        .clickable(actionStartActivity(launchIntent))
        .padding(14.dp),
  ) {
    if (book == null) {
      Column(
        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(symbolRes),
            contentDescription = "ATLAS",
            modifier = GlanceModifier.size(16.dp),
          )
          Spacer(modifier = GlanceModifier.width(6.dp))
          Text(
            text = if (data.isPersian) "اطلس" else "ATLAS",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = inkColor),
          )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
          text =
            if (data.isPersian) "سندی برای ادامه مطالعه اضافه کنید"
            else "Tap to import a document and start reading",
          style = TextStyle(fontSize = 11.sp, color = secondaryColor),
          maxLines = 2,
        )
      }
    } else {
      Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (data.coverBitmap != null && !data.coverBitmap.isRecycled) {
          Image(
            provider = ImageProvider(data.coverBitmap),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier =
              GlanceModifier
                .width(56.dp)
                .fillMaxHeight()
                .cornerRadius(4.dp),
          )
        } else {
          Column(
            modifier =
              GlanceModifier
                .width(56.dp)
                .fillMaxHeight()
                .background(coverFallbackBg)
                .cornerRadius(4.dp)
                .padding(7.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Image(
              provider = ImageProvider(R.drawable.ic_atlas_symbol_widget_dark),
              contentDescription = book.title,
              modifier = GlanceModifier.size(16.dp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
              text = book.title.take(12),
              style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Medium, color = onCoverInk),
              maxLines = 2,
            )
          }
        }

        Spacer(modifier = GlanceModifier.width(14.dp))

        Column(
          modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                GlanceModifier
                  .background(mutedSurface)
                  .cornerRadius(4.dp)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = book.bookFormat().name,
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = inkColor),
              )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
              text =
                if (data.isPersian) "امروز ${data.todayMinutes}/${data.dailyGoalMinutes} د"
                else "Today ${data.todayMinutes}/${data.dailyGoalMinutes}m",
              style = TextStyle(fontSize = 10.sp, color = secondaryColor),
              maxLines = 1,
            )
          }

          Spacer(modifier = GlanceModifier.height(6.dp))

          Text(
            text = book.title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = inkColor),
            maxLines = 1,
          )
          Text(
            text = book.author,
            style = TextStyle(fontSize = 11.sp, color = secondaryColor),
            maxLines = 1,
          )

          Spacer(modifier = GlanceModifier.height(10.dp))

          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LinearProgressIndicator(
              progress = (book.progressPercent / 100f).coerceIn(0f, 1f),
              color = inkColor,
              backgroundColor = trackColor,
              modifier = GlanceModifier.defaultWeight().height(3.dp).cornerRadius(2.dp),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
              text = "${book.progressPercent.roundToInt()}%",
              style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inkColor),
            )
          }
        }
      }
    }
  }
}

class PageNestContinueWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = PageNestContinueWidget()
}
