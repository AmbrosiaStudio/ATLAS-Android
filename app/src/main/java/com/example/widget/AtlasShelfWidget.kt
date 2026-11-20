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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private data class ShelfWidgetBookItem(
  val book: BookEntity,
  val coverBitmap: Bitmap?,
)

private data class ShelfWidgetQuote(
  val bookId: String,
  val text: String,
)

private data class AtlasShelfWidgetData(
  val recentBooks: List<ShelfWidgetBookItem>,
  val totalBooksCount: Int,
  val latestQuote: ShelfWidgetQuote?,
  val isPersian: Boolean,
  val isDark: Boolean,
)

class AtlasShelfWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val data =
      withContext(Dispatchers.IO) {
        val db = PageNestDatabase.getInstance(context)
        val dao = db.pageNestDao()
        val prefs = ReaderPreferencesDataStore(context)
        val settings = prefs.settingsFlow.first()
        val allBooks = dao.observeAllBooks().first()
        val topThree =
          allBooks.take(3).map { book ->
            ShelfWidgetBookItem(
              book = book,
              coverBitmap = book.coverPath?.let { decodeSmallCover(it) },
            )
          }
        val latestHighlight = dao.getLatestHighlight()
        val quote =
          latestHighlight?.selectedText?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            ShelfWidgetQuote(
              bookId = latestHighlight.bookId,
              text = raw.replace('\n', ' ').replace(Regex("\\s+"), " ").take(90),
            )
          }
        AtlasShelfWidgetData(
          recentBooks = topThree,
          totalBooksCount = allBooks.size,
          latestQuote = quote,
          isPersian = settings.appLanguage == AppLanguage.PERSIAN,
          isDark = settings.themeMode.isDark,
        )
      }

    provideContent {
      GlanceTheme {
        AtlasShelfWidgetContent(context = context, data = data)
      }
    }
  }

  private fun decodeSmallCover(path: String): Bitmap? =
    runCatching {
      val file = File(path)
      if (!file.exists() || file.length() == 0L) return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      val maxW = 110
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
}

@Composable
private fun AtlasShelfWidgetContent(
  context: Context,
  data: AtlasShelfWidgetData,
) {
  val bgColor = ColorProvider(if (data.isDark) Color(0xFF111111) else Color(0xFFF5F5F2))
  val cardSurface = ColorProvider(if (data.isDark) Color(0xFF1B1B1B) else Color(0xFFFFFFFF))
  val pillBg = ColorProvider(if (data.isDark) Color(0xFF242424) else Color(red = 0xEE, green = 0xEE, blue = 0xEB))
  val inkColor = ColorProvider(if (data.isDark) Color(0xFFF5F5F2) else Color(0xFF111111))
  val mutedColor = ColorProvider(if (data.isDark) Color(0xFF999999) else Color(0xFF777777))
  val onDarkCover = ColorProvider(Color(0xFFF5F5F2))
  val symbolRes = if (data.isDark) R.drawable.ic_atlas_symbol_widget_dark else R.drawable.ic_atlas_symbol_widget

  val openHomeIntent =
    Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

  Box(
    modifier =
      GlanceModifier
        .fillMaxSize()
        .background(bgColor)
        .cornerRadius(14.dp)
        .padding(12.dp),
  ) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
      // Swiss Editorial Header
      Row(
        modifier =
          GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openHomeIntent)),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Image(
          provider = ImageProvider(symbolRes),
          contentDescription = "ATLAS",
          modifier = GlanceModifier.size(14.dp),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
          text = if (data.isPersian) "قفسه اطلس" else "ATLAS SHELF",
          style =
            TextStyle(
              color = inkColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
            ),
          maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
          modifier =
            GlanceModifier
              .background(pillBg)
              .cornerRadius(4.dp)
              .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
          Text(
            text =
              if (data.isPersian) "${data.totalBooksCount} سند"
              else "${data.totalBooksCount} ${if (data.totalBooksCount == 1) "book" else "books"}",
            style =
              TextStyle(
                color = inkColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
              ),
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = GlanceModifier.height(8.dp))

      if (data.recentBooks.isEmpty()) {
        Box(
          modifier =
            GlanceModifier
              .fillMaxWidth()
              .defaultWeight()
              .background(cardSurface)
              .cornerRadius(6.dp)
              .clickable(actionStartActivity(openHomeIntent))
              .padding(12.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (data.isPersian) "قفسه شما آماده است" else "Your ATLAS shelf is ready",
              style =
                TextStyle(
                  color = inkColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                ),
              maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
              text =
                if (data.isPersian) "برای افزودن کتاب ضربه بزنید"
                else "Tap to import documents and start reading",
              style = TextStyle(color = mutedColor, fontSize = 10.sp),
              maxLines = 1,
            )
          }
        }
      } else {
        // 3-Volume Editorial Cover Dock
        Row(
          modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val monochromeCovers =
            listOf(
              Color(0xFF111111),
              Color(0xFF2B2B2B),
              Color(0xFF424242),
              Color(0xFF1F1F1F),
            )

          data.recentBooks.forEachIndexed { idx, item ->
            val bookIntent =
              Intent(context, MainActivity::class.java).apply {
                putExtra(PageNestContinueWidget.EXTRA_OPEN_BOOK_ID, item.book.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
              }
            val pct = item.book.progressPercent.roundToInt().coerceIn(0, 100)
            val fallbackCloth =
              ColorProvider(monochromeCovers[abs(item.book.title.hashCode()) % monochromeCovers.size])

            Column(
              modifier =
                GlanceModifier
                  .defaultWeight()
                  .fillMaxHeight()
                  .background(cardSurface)
                  .cornerRadius(6.dp)
                  .clickable(actionStartActivity(bookIntent))
                  .padding(6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier =
                  GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(fallbackCloth)
                    .cornerRadius(4.dp),
                contentAlignment = Alignment.Center,
              ) {
                if (item.coverBitmap != null) {
                  Image(
                    provider = ImageProvider(item.coverBitmap),
                    contentDescription = item.book.title,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(4.dp),
                  )
                } else {
                  Text(
                    text = item.book.title.take(14),
                    style =
                      TextStyle(
                        color = onDarkCover,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                      ),
                    maxLines = 2,
                    modifier = GlanceModifier.padding(4.dp),
                  )
                }
              }

              Spacer(modifier = GlanceModifier.height(4.dp))

              Text(
                text = item.book.title,
                style =
                  TextStyle(
                    color = inkColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                maxLines = 1,
              )
              Text(
                text = "$pct%",
                style =
                  TextStyle(
                    color = mutedColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                maxLines = 1,
              )
            }

            if (idx < data.recentBooks.lastIndex) {
              Spacer(modifier = GlanceModifier.width(6.dp))
            }
          }
        }

        // Bottom Editorial Highlight Quote Strip
        val quote = data.latestQuote
        if (quote != null) {
          Spacer(modifier = GlanceModifier.height(7.dp))
          val quoteIntent =
            Intent(context, MainActivity::class.java).apply {
              putExtra(PageNestContinueWidget.EXTRA_OPEN_BOOK_ID, quote.bookId)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
          Row(
            modifier =
              GlanceModifier
                .fillMaxWidth()
                .background(pillBg)
                .cornerRadius(4.dp)
                .clickable(actionStartActivity(quoteIntent))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                GlanceModifier
                  .width(2.dp)
                  .height(12.dp)
                  .background(inkColor),
            ) {}
            Spacer(modifier = GlanceModifier.width(7.dp))
            Text(
              text = "“${quote.text}”",
              style =
                TextStyle(
                  color = inkColor,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium,
                ),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

class AtlasShelfWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = AtlasShelfWidget()
}
