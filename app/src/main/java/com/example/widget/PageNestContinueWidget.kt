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
import androidx.glance.appwidget.updateAll
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

    suspend fun requestUpdate(context: Context) {
      runCatching {
        PageNestContinueWidget().updateAll(context.applicationContext)
      }
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

  val bgColor = ColorProvider(Color(0xFFFAF7F2))
  val cardSurface = ColorProvider(Color(0xFFFFFFFF))
  val inkColor = ColorProvider(Color(0xFF1E1C18))
  val mutedColor = ColorProvider(Color(0xFF6B655B))
  val accentColor = ColorProvider(Color(0xFF2D6A4F))
  val accentTint = ColorProvider(Color(0xFFD8EBE0))

  Box(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(bgColor)
        .cornerRadius(20.dp)
        .clickable(actionStartActivity(launchIntent))
        .padding(12.dp),
  ) {
    if (book == null) {
      Column(
        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (data.isPersian) "پیج‌نست" else "PageNest",
          style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
          text =
            if (data.isPersian) "کتابی برای ادامه مطالعه اضافه کنید"
            else "Tap to import a book and start reading",
          style = TextStyle(fontSize = 12.sp, color = mutedColor),
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
                .cornerRadius(10.dp),
          )
        } else {
          Box(
            modifier =
              GlanceModifier
                .width(56.dp)
                .fillMaxHeight()
                .background(accentColor)
                .cornerRadius(10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              provider = ImageProvider(R.mipmap.ic_launcher),
              contentDescription = book.title,
              modifier = GlanceModifier.size(30.dp),
            )
          }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

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
                  .background(accentTint)
                  .cornerRadius(6.dp)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = book.bookFormat().name,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor),
              )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
              text =
                if (data.isPersian) "امروز ${data.todayMinutes}/${data.dailyGoalMinutes} د"
                else "Today ${data.todayMinutes}/${data.dailyGoalMinutes}m",
              style = TextStyle(fontSize = 10.sp, color = mutedColor),
              maxLines = 1,
            )
          }

          Spacer(modifier = GlanceModifier.height(4.dp))

          Text(
            text = book.title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = inkColor),
            maxLines = 1,
          )
          Text(
            text = book.author,
            style = TextStyle(fontSize = 11.sp, color = mutedColor),
            maxLines = 1,
          )

          Spacer(modifier = GlanceModifier.height(8.dp))

          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LinearProgressIndicator(
              progress = (book.progressPercent / 100f).coerceIn(0f, 1f),
              color = accentColor,
              backgroundColor = cardSurface,
              modifier = GlanceModifier.defaultWeight().height(6.dp).cornerRadius(4.dp),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
              text = "${book.progressPercent.roundToInt()}%",
              style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor),
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
