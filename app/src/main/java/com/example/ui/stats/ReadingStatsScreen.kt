package com.example.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.local.BookProgressPointEntity
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReadingStatsScreen(
  viewModel: ReadingStatsViewModel,
  onNavigateBack: () -> Unit,
  onOpenBook: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val batterySaver = uiState.settings.batterySaverModeEnabled

  var showBookPicker by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 880.dp)) {
        // Editorial Clean Header matching Library & Settings
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("stats_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onBackground,
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.statsDashboardTitle,
                  fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                  fontSize = if (isFa) 21.sp else 23.sp,
                  lineHeight = if (isFa) 28.sp else 30.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = if (isFa) 0.sp else (-0.3).sp,
                  color = MaterialTheme.colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = strings.statsDashboardSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        LazyColumn(
          contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 40.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          // 1. Key Metrics Bento Grid (Today's Reading Time, Total Pages Read, All-Time & Streak)
          item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                TodayGoalStatCard(
                  todayMinutes = uiState.todayReadingMinutes,
                  goalMinutes = uiState.settings.dailyGoalMinutes,
                  progressFraction = uiState.dailyGoalProgressFraction,
                  strings = strings,
                  isFa = isFa,
                  modifier = Modifier.weight(1f).testTag("stats_today_card"),
                )
                MetricSummaryCard(
                  icon = Icons.AutoMirrored.Rounded.MenuBook,
                  title = strings.totalPagesReadLabel,
                  primaryValue = uiState.totalPagesReadAllTime.toString(),
                  secondaryValue = strings.todayPagesSub(uiState.todayPagesRead),
                  isFa = isFa,
                  modifier = Modifier.weight(1f).testTag("stats_pages_card"),
                )
              }

              AllTimeAndStreakBanner(
                allTimeSeconds = uiState.allTimeReadingSeconds,
                streakDays = uiState.activeStreakDays,
                booksCount = uiState.books.size,
                strings = strings,
                isFa = isFa,
                modifier = Modifier.fillMaxWidth().testTag("stats_streak_card"),
              )
            }
          }

          // 2. Daily Reading Time Bar Chart (7D / 14D interactive)
          item {
            DailyReadingTimeChartCard(
              entries = uiState.dailyEntries,
              windowDays = uiState.dailyWindowDays,
              dailyGoalMinutes = uiState.settings.dailyGoalMinutes,
              batterySaverEnabled = batterySaver,
              strings = strings,
              isFa = isFa,
              onSelectWindowDays = viewModel::setDailyWindowDays,
              modifier = Modifier.fillMaxWidth().testTag("stats_daily_chart"),
            )
          }

          // 3. Current Book Progress Trend Chart
          item {
            CurrentBookProgressTrendCard(
              books = uiState.books,
              selectedBook = uiState.selectedBook,
              historyPoints = uiState.bookProgressHistory,
              showBookPicker = showBookPicker,
              onToggleBookPicker = { showBookPicker = it },
              onSelectBook = { bookId ->
                viewModel.selectBook(bookId)
                showBookPicker = false
              },
              onOpenBook = onOpenBook,
              batterySaverEnabled = batterySaver,
              strings = strings,
              isFa = isFa,
              modifier = Modifier.fillMaxWidth().testTag("stats_trend_chart"),
            )
          }

          // 4. Books on Shelf Progress Breakdown
          if (uiState.books.isNotEmpty()) {
            item {
              Text(
                text = strings.readingShelfBreakdown,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
              )
            }
            items(uiState.books, key = { "stats_book_${it.id}" }) { book ->
              val isSelected = book.id == uiState.selectedBook?.id
              ShelfBookProgressRow(
                book = book,
                isSelected = isSelected,
                strings = strings,
                onClick = { viewModel.selectBook(book.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TodayGoalStatCard(
  todayMinutes: Int,
  goalMinutes: Int,
  progressFraction: Float,
  strings: AppStrings,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier =
            Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
          )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
          CircularProgressIndicator(
            progress = { progressFraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 3.5.dp,
            modifier = Modifier.fillMaxSize(),
          )
          Text(
            text = "${(progressFraction * 100f).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Text(
        text = strings.todayReadingLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = strings.minutesShort(todayMinutes),
        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = strings.dailyGoalLineLabel(goalMinutes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MetricSummaryCard(
  icon: ImageVector,
  title: String,
  primaryValue: String,
  secondaryValue: String,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = primaryValue,
        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = secondaryValue,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AllTimeAndStreakBanner(
  allTimeSeconds: Long,
  streakDays: Int,
  booksCount: Int,
  strings: AppStrings,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  val totalMins = (allTimeSeconds / 60L).toInt()
  val formattedAllTime =
    if (totalMins >= 60) strings.hoursMinutesShort(totalMins / 60, totalMins % 60)
    else strings.minutesShort(totalMins)

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.allTimeReadingLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = formattedAllTime,
          fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
          fontSize = 19.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocalFireDepartment,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = strings.activeStreakLabel(streakDays),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Text(
            text = strings.volumesCount(booksCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun DailyReadingTimeChartCard(
  entries: List<DayReadingEntry>,
  windowDays: Int,
  dailyGoalMinutes: Int,
  batterySaverEnabled: Boolean,
  strings: AppStrings,
  isFa: Boolean,
  onSelectWindowDays: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember(entries.size) { mutableIntStateOf((entries.size - 1).coerceAtLeast(0)) }
  val selectedEntry = entries.getOrNull(selectedIndex)
  val hasAnyActivity = entries.any { it.readingSeconds > 0 || it.pagesRead > 0 }

  val revealProgress = remember { Animatable(if (batterySaverEnabled) 1f else 0f) }
  LaunchedEffect(entries, windowDays, batterySaverEnabled) {
    if (batterySaverEnabled) {
      revealProgress.snapTo(1f)
    } else {
      revealProgress.snapTo(0f)
      revealProgress.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
    }
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.BarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = strings.dailyReadingTimeChartTitle,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // 7D / 14D Toggle Pill
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(7 to strings.last7DaysLabel, 14 to strings.last14DaysLabel).forEach { (days, label) ->
              val active = windowDays == days
              Surface(
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier =
                  Modifier
                    .clip(CircleShape)
                    .clickable { onSelectWindowDays(days) }
                    .testTag("stats_window_${days}d"),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                  color =
                    if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }

      if (selectedEntry != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                strings.dayBreakdownLabel(
                  selectedEntry.fullLabel,
                  selectedEntry.readingMinutes,
                  selectedEntry.pagesRead,
                ),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.dailyGoalLineLabel(dailyGoalMinutes),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      val primaryColor = MaterialTheme.colorScheme.primary
      val mutedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
      val emptyBarTrack = MaterialTheme.colorScheme.surfaceVariant
      val gridColor = MaterialTheme.colorScheme.outlineVariant
      val goalLineColor = MaterialTheme.colorScheme.tertiary

      if (entries.isNotEmpty()) {
        val maxMinutes =
          maxOf(
            entries.maxOfOrNull { it.readingMinutes } ?: 0,
            dailyGoalMinutes,
            15,
          )

        Box(modifier = Modifier.fillMaxWidth().height(166.dp)) {
          Canvas(
            modifier =
              Modifier
                .fillMaxSize()
                .pointerInput(entries.size) {
                  detectTapGestures { offset ->
                    if (entries.isNotEmpty() && size.width > 0) {
                      val slotWidth = size.width.toFloat() / entries.size.toFloat()
                      val idx = (offset.x / slotWidth).toInt().coerceIn(0, entries.lastIndex)
                      selectedIndex = idx
                    }
                  }
                }
          ) {
            val chartTop = 10.dp.toPx()
            val chartBottom = size.height - 6.dp.toPx()
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)

            // Subtle horizontal guide lines
            listOf(0f, 0.5f, 1f).forEach { ratio ->
              val y = chartBottom - chartHeight * ratio
              drawLine(
                color = gridColor.copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
              )
            }

            // Daily Goal dashed threshold line
            val goalRatio = (dailyGoalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val goalY = chartBottom - chartHeight * goalRatio
            drawLine(
              color = goalLineColor.copy(alpha = 0.85f),
              start = Offset(0f, goalY),
              end = Offset(size.width, goalY),
              strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
            )

            // Bars
            val count = entries.size
            val slotW = size.width / count.toFloat()
            val barW = (slotW * (if (count <= 7) 0.46f else 0.54f)).coerceIn(8.dp.toPx(), 30.dp.toPx())
            val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

            entries.forEachIndexed { idx, entry ->
              val centerX = slotW * idx + slotW / 2f
              val leftX = centerX - barW / 2f
              val rawMins =
                if (entry.readingSeconds in 1..59) 1f else entry.readingMinutes.toFloat()
              val barRatio = (rawMins / maxMinutes.toFloat()).coerceIn(0f, 1f) * revealProgress.value
              val barH =
                if (rawMins > 0f) (chartHeight * barRatio).coerceAtLeast(6.dp.toPx())
                else 4.dp.toPx()
              val topY = chartBottom - barH

              // Subtle background slot track when selected
              if (idx == selectedIndex) {
                drawRoundRect(
                  color = primaryColor.copy(alpha = 0.08f),
                  topLeft = Offset(centerX - slotW * 0.42f, chartTop),
                  size = Size(slotW * 0.84f, chartHeight),
                  cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )
              }

              val fill =
                when {
                  rawMins <= 0f -> emptyBarTrack
                  entry.goalMet -> primaryColor
                  else -> mutedBarColor
                }

              drawRoundRect(
                color = fill,
                topLeft = Offset(leftX, topY),
                size = Size(barW, barH),
                cornerRadius = corner,
              )
            }
          }

          if (!hasAnyActivity) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            ) {
              Text(
                text = strings.noDailyStatsYet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              )
            }
          }
        }

        // X-axis labels row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          val labelStep = if (entries.size > 10) 2 else 1
          entries.forEachIndexed { idx, entry ->
            val showText = idx % labelStep == 0 || idx == entries.lastIndex
            Text(
              text = if (showText) entry.shortLabel else "",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              fontWeight = if (idx == selectedIndex || entry.isToday) FontWeight.Bold else FontWeight.Normal,
              color =
                if (idx == selectedIndex) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 1,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CurrentBookProgressTrendCard(
  books: List<BookEntity>,
  selectedBook: BookEntity?,
  historyPoints: List<BookProgressPointEntity>,
  showBookPicker: Boolean,
  onToggleBookPicker: (Boolean) -> Unit,
  onSelectBook: (String) -> Unit,
  onOpenBook: (String) -> Unit,
  batterySaverEnabled: Boolean,
  strings: AppStrings,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  var inspectedPointIndex by remember(selectedBook?.id, historyPoints.size) {
    mutableIntStateOf((historyPoints.size - 1).coerceAtLeast(0))
  }

  val revealProgress = remember { Animatable(if (batterySaverEnabled) 1f else 0f) }
  LaunchedEffect(selectedBook?.id, historyPoints.size, batterySaverEnabled) {
    if (batterySaverEnabled) {
      revealProgress.snapTo(1f)
    } else {
      revealProgress.snapTo(0f)
      revealProgress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = strings.currentBookTrendTitle,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (books.size > 1 && selectedBook != null) {
          Box {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier =
                Modifier
                  .clip(CircleShape)
                  .clickable { onToggleBookPicker(true) }
                  .testTag("stats_book_selector"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = strings.selectBookLabel,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowDown,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
            DropdownMenu(
              expanded = showBookPicker,
              onDismissRequest = { onToggleBookPicker(false) },
              containerColor = MaterialTheme.colorScheme.surface,
            ) {
              books.forEach { b ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = b.title,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      fontWeight = if (b.id == selectedBook.id) FontWeight.SemiBold else FontWeight.Normal,
                      color =
                        if (b.id == selectedBook.id) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                  },
                  onClick = { onSelectBook(b.id) },
                )
              }
            }
          }
        }
      }

      if (selectedBook == null) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Rounded.AutoStories,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(32.dp),
            )
            Text(
              text = strings.noBooksForTrend,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        // Active Book Summary Strip
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = selectedBook.title,
                fontFamily = LiterataFontFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val unitLabel =
                if (selectedBook.bookFormat() == BookFormat.PDF) {
                  strings.pageOfTotal(selectedBook.currentChapterOrPage + 1, selectedBook.totalUnits.coerceAtLeast(1))
                } else {
                  strings.chapterOfTotal(selectedBook.currentChapterOrPage + 1, selectedBook.totalUnits.coerceAtLeast(1))
                }
              Text(
                text = "${selectedBook.progressPercent.roundToInt()}% · $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onOpenBook(selectedBook.id) },
              shape = CircleShape,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier.testTag("stats_open_book_button"),
            ) {
              Text(
                text = strings.continueThisBook,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        // Build plotted points (if only 0 or 1 DB checkpoint exists, show current book % cleanly)
        val plotPoints =
          remember(historyPoints, selectedBook.progressPercent, selectedBook.currentChapterOrPage) {
            if (historyPoints.isNotEmpty()) {
              historyPoints
            } else {
              listOf(
                BookProgressPointEntity(
                  bookId = selectedBook.id,
                  progressPercent = selectedBook.progressPercent,
                  pageOrUnitIndex = selectedBook.currentChapterOrPage,
                  recordedAt = System.currentTimeMillis(),
                  dateKey = "",
                )
              )
            }
          }

        val safeInspectIdx = inspectedPointIndex.coerceIn(0, plotPoints.lastIndex)
        val inspectedPoint = plotPoints[safeInspectIdx]
        val dateLocale = if (isFa) Locale("fa") else Locale.US
        val pointTimeText =
          remember(inspectedPoint.recordedAt, isFa) {
            SimpleDateFormat("MMM d, HH:mm", dateLocale).format(Date(inspectedPoint.recordedAt))
          }

        // Selected Checkpoint Readout
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ) {
          Text(
            text =
              strings.checkpointDetailsLabel(
                pointTimeText,
                inspectedPoint.progressPercent.roundToInt(),
                inspectedPoint.pageOrUnitIndex + 1,
              ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
          )
        }

        val lineColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val gridColor = MaterialTheme.colorScheme.outlineVariant

        Box(modifier = Modifier.fillMaxWidth().height(172.dp)) {
          Canvas(
            modifier =
              Modifier
                .fillMaxSize()
                .pointerInput(plotPoints) {
                  detectTapGestures { tapOffset ->
                    if (plotPoints.size >= 2 && size.width > 0) {
                      val padH = 12.dp.toPx()
                      val usableW = (size.width - padH * 2).coerceAtLeast(1f)
                      val ratio = ((tapOffset.x - padH) / usableW).coerceIn(0f, 1f)
                      val nearest = (ratio * (plotPoints.size - 1)).roundToInt().coerceIn(0, plotPoints.lastIndex)
                      inspectedPointIndex = nearest
                    }
                  }
                }
          ) {
            val padH = 12.dp.toPx()
            val padTop = 12.dp.toPx()
            val padBottom = 14.dp.toPx()
            val chartW = (size.width - padH * 2).coerceAtLeast(10f)
            val chartH = (size.height - padTop - padBottom).coerceAtLeast(10f)

            // Y-axis reference lines: 0%, 25%, 50%, 75%, 100%
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { pct ->
              val y = padTop + chartH * (1f - pct)
              drawLine(
                color = gridColor.copy(alpha = if (pct == 0f || pct == 1f) 0.75f else 0.45f),
                start = Offset(padH, y),
                end = Offset(padH + chartW, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect =
                  if (pct in 0.01f..0.99f) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                  else null,
              )
            }

            val coords =
              if (plotPoints.size == 1) {
                val y = padTop + chartH * (1f - (plotPoints[0].progressPercent / 100f).coerceIn(0f, 1f))
                listOf(Offset(padH + chartW * 0.5f, y))
              } else {
                plotPoints.mapIndexed { idx, pt ->
                  val x = padH + chartW * (idx.toFloat() / (plotPoints.size - 1).toFloat())
                  val y = padTop + chartH * (1f - (pt.progressPercent / 100f).coerceIn(0f, 1f))
                  Offset(x, y)
                }
              }

            if (coords.size >= 2) {
              val animCount = (coords.size * revealProgress.value).coerceAtLeast(1f)
              val visibleCount = animCount.toInt().coerceIn(2, coords.size)
              val linePath = Path()
              val fillPath = Path()

              linePath.moveTo(coords[0].x, coords[0].y)
              fillPath.moveTo(coords[0].x, padTop + chartH)
              fillPath.lineTo(coords[0].x, coords[0].y)

              for (i in 1 until visibleCount) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val ctrlX = (prev.x + curr.x) / 2f
                linePath.cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                fillPath.cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
              }

              val lastPt = coords[visibleCount - 1]
              fillPath.lineTo(lastPt.x, padTop + chartH)
              fillPath.close()

              drawPath(
                path = fillPath,
                brush =
                  Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                    startY = padTop,
                    endY = padTop + chartH,
                  ),
              )

              drawPath(
                path = linePath,
                color = lineColor,
                style =
                  Stroke(
                    width = 2.75.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                  ),
              )
            }

            // Draw nodes
            val dotStep = if (coords.size > 18) (coords.size / 12).coerceAtLeast(1) else 1
            coords.forEachIndexed { idx, offset ->
              val isInspected = idx == safeInspectIdx
              if (idx % dotStep == 0 || idx == coords.lastIndex || isInspected) {
                val outerRadius = if (isInspected) 7.dp.toPx() else 4.dp.toPx()
                val innerRadius = if (isInspected) 4.dp.toPx() else 2.2.dp.toPx()
                drawCircle(
                  color = lineColor.copy(alpha = if (isInspected) 0.25f else 0.12f),
                  radius = outerRadius + 3.dp.toPx(),
                  center = offset,
                )
                drawCircle(color = surfaceColor, radius = outerRadius, center = offset)
                drawCircle(color = lineColor, radius = innerRadius, center = offset)
              }
            }
          }

          // 0% and 100% scale tags
          Text(
            text = "100%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp),
          )
          Text(
            text = "0%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp),
          )
        }

        if (historyPoints.size <= 1) {
          Text(
            text = strings.trendEmptyHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ShelfBookProgressRow(
  book: BookEntity,
  isSelected: Boolean,
  strings: AppStrings,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color =
      if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
      else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = book.title,
            fontFamily = LiterataFontFamily,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${book.author} · ${book.format}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "${book.progressPercent.roundToInt()}%",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      LinearProgressIndicator(
        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
      )
    }
  }
}
