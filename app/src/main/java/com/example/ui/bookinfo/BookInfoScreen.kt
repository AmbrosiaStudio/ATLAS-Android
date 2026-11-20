package com.example.ui.bookinfo

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.BookEntity
import com.example.data.local.ShelfEntity
import com.example.engine.insights.BookReadingInsights
import com.example.engine.insights.ChapterInsight
import com.example.engine.insights.ChapterProgressStatus
import com.example.engine.insights.ReadingInsightsEngine
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.ReadingStatus
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BookInfoScreen(
  viewModel: BookInfoViewModel,
  onNavigateBack: () -> Unit,
  onOpenBook: (String) -> Unit,
  onOpenBookAtChapter: (String, Int) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val batterySaver = uiState.settings.batterySaverModeEnabled

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 880.dp)) {
        // Editorial Clean Header
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
                modifier = Modifier.minimumInteractiveComponentSize().testTag("book_info_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = strings.returnToLibrary,
                  tint = MaterialTheme.colorScheme.onBackground,
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = strings.bookInfoTitle,
                fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                fontSize = if (isFa) 21.sp else 22.sp,
                lineHeight = if (isFa) 28.sp else 29.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = if (isFa) 0.sp else (-0.3).sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (uiState.book != null) {
                IconButton(
                  onClick = viewModel::toggleFavorite,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("book_info_favorite_button"),
                ) {
                  Icon(
                    imageVector =
                      if (uiState.book?.isFavorite == true) Icons.Rounded.Favorite
                      else Icons.Rounded.FavoriteBorder,
                    contentDescription =
                      if (uiState.book?.isFavorite == true) strings.removeFavoriteAction
                      else strings.addFavoriteAction,
                    tint =
                      if (uiState.book?.isFavorite == true) Color(0xFFE11D48)
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        val book = uiState.book
        when {
          uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
          }
          book == null -> {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = strings.unableToOpen,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
          else -> {
            val insights = uiState.insights
            LazyColumn(
              contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 40.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
            ) {
              // 1. Book Hero & Metadata Card
              item {
                BookHeroSectionCard(
                  book = book,
                  bookmarksCount = uiState.bookmarksCount,
                  highlightsCount = uiState.highlightsCount,
                  shelves = uiState.shelves,
                  assignedShelfIds = uiState.assignedShelfIds,
                  strings = strings,
                  isFa = isFa,
                  onOpenBook = { onOpenBook(book.id) },
                  onSelectStatus = viewModel::setReadingStatus,
                  onToggleShelf = viewModel::toggleShelf,
                )
              }

              // 2. Reading Insights Section Header + Speed & ETA Bento Cards
              item {
                Column(
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("reading_insights_section"),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = strings.readingInsightsTitle,
                        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                      )
                      Text(
                        text = strings.readingInsightsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    if (insights != null) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Surface(
                        shape = CircleShape,
                        color =
                          if (insights.isMeasuredSpeed) MaterialTheme.colorScheme.primaryContainer
                          else MaterialTheme.colorScheme.surfaceVariant,
                      ) {
                        Text(
                          text =
                            if (insights.isMeasuredSpeed) strings.measuredPaceBadge
                            else strings.baselinePaceBadge,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.SemiBold,
                          color =
                            if (insights.isMeasuredSpeed) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                      }
                    }
                  }

                  if (insights == null && uiState.isCalculatingInsights) {
                    Surface(
                      shape = RoundedCornerShape(20.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Box(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        CircularProgressIndicator(
                          color = MaterialTheme.colorScheme.primary,
                          strokeWidth = 2.5.dp,
                          modifier = Modifier.size(28.dp),
                        )
                      }
                    }
                  } else if (insights != null) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      ReadingSpeedBentoCard(
                        insights = insights,
                        strings = strings,
                        isFa = isFa,
                        modifier = Modifier.weight(1f).testTag("book_info_speed_card"),
                      )
                      TimeRemainingBentoCard(
                        insights = insights,
                        strings = strings,
                        isFa = isFa,
                        modifier = Modifier.weight(1f).testTag("book_info_eta_card"),
                      )
                    }
                  }
                }
              }

              // 3. Interactive Chapter Progress & Time Chart
              if (insights != null) {
                item {
                  if (insights.chapters.isEmpty()) {
                    Surface(
                      shape = RoundedCornerShape(20.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = strings.noChaptersInsightsMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                      )
                    }
                  } else {
                    ChapterProgressChartCard(
                      book = book,
                      insights = insights,
                      batterySaverEnabled = batterySaver,
                      strings = strings,
                      isFa = isFa,
                      onOpenChapter = { chapterIndex ->
                        onOpenBookAtChapter(book.id, chapterIndex)
                      },
                      modifier = Modifier.fillMaxWidth().testTag("book_info_progress_chart"),
                    )
                  }
                }

                // 4. Time Remaining per Chapter List
                if (insights.chapters.isNotEmpty()) {
                  item {
                    Text(
                      text = strings.chapterBreakdownTitle,
                      fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                      fontSize = 17.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onBackground,
                      modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
                    )
                  }

                  items(
                    items = insights.chapters,
                    key = { "chapter_row_${it.index}_${it.title}" },
                  ) { chapter ->
                    val isCurrent = chapter.index == book.currentChapterOrPage ||
                      chapter.status == ChapterProgressStatus.IN_PROGRESS
                    ChapterInsightRowCard(
                      chapter = chapter,
                      isReflowable = book.bookFormat().isReflowableText,
                      isCurrent = isCurrent,
                      strings = strings,
                      isFa = isFa,
                      onClick = { onOpenBookAtChapter(book.id, chapter.index) },
                      modifier = Modifier.fillMaxWidth().testTag("chapter_insight_row_${chapter.index}"),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookHeroSectionCard(
  book: BookEntity,
  bookmarksCount: Int,
  highlightsCount: Int,
  shelves: List<ShelfEntity>,
  assignedShelfIds: Set<Long>,
  strings: AppStrings,
  isFa: Boolean,
  onOpenBook: () -> Unit,
  onSelectStatus: (ReadingStatus) -> Unit,
  onToggleShelf: (Long) -> Unit,
) {
  val format = book.bookFormat()
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
      ) {
        BookInfoCoverThumbnail(
          book = book,
          modifier = Modifier.width(90.dp).height(132.dp),
        )

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = if (format == BookFormat.AUDIOBOOK) "AUDIO" else format.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Text(
              text = formatFileSize(book.fileSizeBytes),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Text(
            text = book.title,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )

          Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(2.dp))

          // Bookmarks & Highlights counters
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = strings.bookmarksCountLabel(bookmarksCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = strings.highlightsCountLabel(highlightsCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Overall Progress Bar
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val unitText =
            if (!format.isReflowableText) {
              strings.pageOfTotal(book.currentChapterOrPage + 1, book.totalUnits.coerceAtLeast(1))
            } else {
              strings.chapterOfTotal(book.currentChapterOrPage + 1, book.totalUnits.coerceAtLeast(1))
            }
          Text(
            text = unitText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${book.progressPercent.roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        LinearProgressIndicator(
          progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
        )
      }

      // Primary Open Button + Reading Status Row
      Button(
        onClick = onOpenBook,
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("book_info_read_button"),
      ) {
        Icon(
          imageVector =
            if (format == BookFormat.AUDIOBOOK) Icons.Rounded.Headphones
            else Icons.Rounded.AutoStories,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (book.progressPercent > 0.5f) strings.continueReading
            else strings.continueThisBook,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      // Status Selector Chips
      val currentStatus = book.status()
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ReadingStatus.entries.forEach { st ->
          val selected = st == currentStatus
          FilterChip(
            selected = selected,
            onClick = { onSelectStatus(st) },
            label = {
              Text(
                text = if (isFa) st.faLabel else st.label,
                style = MaterialTheme.typography.labelSmall,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          )
        }
      }

      // Shelf Tags if user has shelves
      if (shelves.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          shelves.forEach { shelf ->
            val assigned = shelf.id in assignedShelfIds
            FilterChip(
              selected = assigned,
              onClick = { onToggleShelf(shelf.id) },
              label = {
                Text(
                  text = shelf.name,
                  style = MaterialTheme.typography.labelSmall,
                )
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReadingSpeedBentoCard(
  insights: BookReadingInsights,
  strings: AppStrings,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  val primaryValue =
    when {
      insights.format == BookFormat.AUDIOBOOK -> strings.speedMultiplierLabel(insights.playbackSpeed)
      insights.format.isReflowableText -> strings.wpmValueLabel(insights.wordsPerMinute)
      else -> strings.pagesPerHourLabel(insights.pagesPerHour)
    }

  val readMins = (insights.totalBookSecondsRead / 60f)
  val formattedTimeRead =
    if (insights.totalBookSecondsRead in 1L..59L) {
      if (isFa) "${insights.totalBookSecondsRead} ثانیه" else "${insights.totalBookSecondsRead}s"
    } else if (insights.totalBookSecondsRead <= 0L) {
      if (isFa) "۰ د" else "0m"
    } else {
      ReadingInsightsEngine.formatDurationSummary(readMins, isFa)
    }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
          imageVector = Icons.Rounded.Speed,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = strings.readingSpeedLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = primaryValue,
        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = strings.timeReadLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = formattedTimeRead,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = strings.totalWordsLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = formatWordCountCompact(insights.totalWords),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun TimeRemainingBentoCard(
  insights: BookReadingInsights,
  strings: AppStrings,
  isFa: Boolean,
  modifier: Modifier = Modifier,
) {
  val bookRemText = ReadingInsightsEngine.formatDurationSummary(insights.remainingEstimatedMinutes, isFa)
  val chapterRemText = ReadingInsightsEngine.formatDurationSummary(insights.currentChapterRemainingMinutes, isFa)

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
      Text(
        text = strings.timeRemainingBookLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = bookRemText,
        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = strings.timeRemainingChapterLabel,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Text(
          text = chapterRemText,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun ChapterProgressChartCard(
  book: BookEntity,
  insights: BookReadingInsights,
  batterySaverEnabled: Boolean,
  strings: AppStrings,
  isFa: Boolean,
  onOpenChapter: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val chapters = insights.chapters
  val initialActiveIdx =
    remember(book.id, chapters.size, book.currentChapterOrPage) {
      chapters.indexOfFirst { it.status == ChapterProgressStatus.IN_PROGRESS }
        .takeIf { it >= 0 }
        ?: chapters.indexOfFirst { it.index >= book.currentChapterOrPage }.takeIf { it >= 0 }
        ?: 0
    }
  var inspectedListIdx by remember(book.id, chapters.size) { mutableIntStateOf(initialActiveIdx) }
  val safeIdx = inspectedListIdx.coerceIn(0, chapters.lastIndex)
  val inspectedChapter = chapters[safeIdx]

  val revealProgress = remember { Animatable(if (batterySaverEnabled) 1f else 0f) }
  LaunchedEffect(book.id, chapters.size, batterySaverEnabled) {
    if (batterySaverEnabled) {
      revealProgress.snapTo(1f)
    } else {
      revealProgress.snapTo(0f)
      revealProgress.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
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
            text = strings.chapterProgressChartTitle,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ) {
          Text(
            text = strings.completedChaptersSummary(insights.completedChaptersCount, chapters.size),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // Active/Selected Chapter Readout + Jump Button
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = inspectedChapter.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val pct = (inspectedChapter.progressFraction * 100f).roundToInt()
            val remStr = ReadingInsightsEngine.formatDurationSummary(inspectedChapter.estimatedRemainingMinutes, isFa)
            val details =
              if (book.bookFormat().isReflowableText) {
                "$pct% · ${strings.wordsCountLabel(inspectedChapter.wordCount)} · $remStr"
              } else {
                "$pct% · $remStr"
              }
            Text(
              text = details,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable { onOpenChapter(inspectedChapter.index) }
                .testTag("book_info_jump_selected_chapter"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = strings.openAtChapterAction,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = strings.openAtChapterAction,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
              )
            }
          }
        }
      }

      val maxChapterMins = chapters.maxOf { it.estimatedTotalMinutes }.coerceAtLeast(1f)
      val primaryColor = MaterialTheme.colorScheme.primary
      val activeFillColor = MaterialTheme.colorScheme.tertiary
      val trackColor = MaterialTheme.colorScheme.surfaceVariant
      val gridColor = MaterialTheme.colorScheme.outlineVariant

      val useScrollableWidth = chapters.size > 10
      val chartScroll = rememberScrollState()

      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val barContainerWidth =
          if (useScrollableWidth) (chapters.size * 38).dp.coerceAtLeast(maxWidth)
          else maxWidth

        Column(
          modifier =
            if (useScrollableWidth) Modifier.horizontalScroll(chartScroll).width(barContainerWidth)
            else Modifier.fillMaxWidth()
        ) {
          Canvas(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(156.dp)
                .pointerInput(chapters.size) {
                  detectTapGestures { offset ->
                    if (chapters.isNotEmpty() && size.width > 0) {
                      val slotW = size.width.toFloat() / chapters.size.toFloat()
                      val idx = (offset.x / slotW).toInt().coerceIn(0, chapters.lastIndex)
                      inspectedListIdx = idx
                    }
                  }
                }
          ) {
            val topPad = 10.dp.toPx()
            val bottomPad = 6.dp.toPx()
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(16f)

            // Reference guide lines (0%, 50%, 100% of max chapter duration)
            listOf(0f, 0.5f, 1f).forEach { r ->
              val y = topPad + chartH * (1f - r)
              drawLine(
                color = gridColor.copy(alpha = if (r == 0f) 0.8f else 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (r > 0f) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null,
              )
            }

            val count = chapters.size
            val slotW = size.width / count.toFloat()
            val barW = (slotW * 0.54f).coerceIn(10.dp.toPx(), 34.dp.toPx())
            val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())

            chapters.forEachIndexed { idx, ch ->
              val centerX = slotW * idx + slotW / 2f
              val leftX = centerX - barW / 2f
              val lengthRatio = (ch.estimatedTotalMinutes / maxChapterMins).coerceIn(0.20f, 1.0f)
              val barTotalH = (chartH * lengthRatio * revealProgress.value).coerceAtLeast(14.dp.toPx())
              val barTopY = (topPad + chartH) - barTotalH

              // Highlight slot behind selected chapter
              if (idx == safeIdx) {
                drawRoundRect(
                  color = primaryColor.copy(alpha = 0.09f),
                  topLeft = Offset(centerX - slotW * 0.43f, topPad),
                  size = Size(slotW * 0.86f, chartH),
                  cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )
              }

              // Track outline/fill representing total estimated chapter duration
              drawRoundRect(
                color = trackColor,
                topLeft = Offset(leftX, barTopY),
                size = Size(barW, barTotalH),
                cornerRadius = corner,
              )
              if (idx == safeIdx) {
                drawRoundRect(
                  color = primaryColor.copy(alpha = 0.55f),
                  topLeft = Offset(leftX, barTopY),
                  size = Size(barW, barTotalH),
                  cornerRadius = corner,
                  style = Stroke(width = 1.6.dp.toPx()),
                )
              }

              // Progress fill from bottom
              val progFrac =
                when (ch.status) {
                  ChapterProgressStatus.COMPLETED -> 1f
                  ChapterProgressStatus.IN_PROGRESS -> ch.progressFraction.coerceIn(0.12f, 1f)
                  ChapterProgressStatus.UPCOMING -> 0f
                }
              if (progFrac > 0f) {
                val fillH = (barTotalH * progFrac).coerceAtLeast(6.dp.toPx())
                val fillTopY = (topPad + chartH) - fillH
                val fillColor =
                  if (ch.status == ChapterProgressStatus.COMPLETED) primaryColor
                  else activeFillColor
                drawRoundRect(
                  color = fillColor,
                  topLeft = Offset(leftX, fillTopY),
                  size = Size(barW, fillH),
                  cornerRadius = corner,
                )
              }
            }
          }

          // Chapter number x-axis labels
          Row(modifier = Modifier.fillMaxWidth()) {
            chapters.forEachIndexed { idx, _ ->
              val selected = idx == safeIdx
              Text(
                text = "${idx + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Legend + Hint Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ChartLegendDot(color = MaterialTheme.colorScheme.primary, label = strings.chapterDoneLabel)
          ChartLegendDot(color = MaterialTheme.colorScheme.tertiary, label = strings.chapterInProgressLabel)
          ChartLegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = strings.chapterUpcomingLabel)
        }
        Text(
          text = strings.chapterChartHint,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun ChapterInsightRowCard(
  chapter: ChapterInsight,
  isReflowable: Boolean,
  isCurrent: Boolean,
  strings: AppStrings,
  isFa: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color =
      if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
      else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        when (chapter.status) {
          ChapterProgressStatus.COMPLETED -> {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = strings.chapterDoneLabel,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          ChapterProgressStatus.IN_PROGRESS -> {
            Box(
              modifier =
                Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
              )
            }
          }
          ChapterProgressStatus.UPCOMING -> {
            Box(
              modifier =
                Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "${chapter.index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = chapter.title,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          val pct = (chapter.progressFraction * 100f).roundToInt()
          val meta =
            if (isReflowable) {
              "$pct% · ${strings.wordsCountLabel(chapter.wordCount)}"
            } else {
              "$pct%"
            }
          Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }

        Surface(
          shape = CircleShape,
          color =
            when (chapter.status) {
              ChapterProgressStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
              ChapterProgressStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
              ChapterProgressStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
          Text(
            text =
              if (chapter.status == ChapterProgressStatus.COMPLETED) strings.chapterDoneLabel
              else ReadingInsightsEngine.formatShortDuration(chapter.estimatedRemainingMinutes, isFa),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color =
              when (chapter.status) {
                ChapterProgressStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                ChapterProgressStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimary
                ChapterProgressStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
              },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = strings.openAtChapterAction,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }

      LinearProgressIndicator(
        progress = { chapter.progressFraction.coerceIn(0f, 1f) },
        color =
          if (chapter.status == ChapterProgressStatus.IN_PROGRESS) MaterialTheme.colorScheme.tertiary
          else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
      )
    }
  }
}

@Composable
private fun BookInfoCoverThumbnail(
  book: BookEntity,
  modifier: Modifier = Modifier,
) {
  val coverFile = book.coverPath?.let { File(it) }?.takeIf { it.exists() }
  val shape = RoundedCornerShape(14.dp)

  if (coverFile != null) {
    AsyncImage(
      model = coverFile,
      contentDescription = "${book.title} cover",
      contentScale = ContentScale.Crop,
      modifier = modifier.clip(shape),
    )
  } else {
    val clothColors =
      listOf(
        Color(0xFF111111),
        Color(0xFF242424),
        Color(0xFF363636),
        Color(0xFF1C1C1C),
      )
    val clothColor = clothColors[abs(book.title.hashCode()) % clothColors.size]
    Box(
      modifier =
        modifier
          .clip(shape)
          .background(clothColor)
          .padding(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxHeight()
            .width(2.dp)
            .background(Color.White.copy(alpha = 0.16f)),
      )
      Column(
        modifier = Modifier.fillMaxSize().padding(start = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = book.title,
          fontFamily = LiterataFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFF6F2E9),
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 15.sp,
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFF6F2E9).copy(alpha = 0.75f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0L) return "0 KB"
  val kb = bytes / 1024.0
  return if (kb >= 1024.0) String.format("%.1f MB", kb / 1024.0)
  else String.format("%.0f KB", kb.coerceAtLeast(1.0))
}

private fun formatWordCountCompact(words: Int): String {
  if (words < 1000) return words.toString()
  val k = words / 1000.0
  return String.format("%.1fk", k)
}
