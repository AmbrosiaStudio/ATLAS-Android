package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BookEntity
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import kotlin.math.roundToInt

/**
 * Editorial Spotlight Card for the active book on the redesigned ATLAS Home Screen.
 * Features a subtle Cobalt atmospheric gradient wash, tall elevated cover, live progress bar,
 * Book Insights quick button, and a bold Cobalt Continue CTA.
 */
@Composable
internal fun AtlasNowReadingSpotlight(
  book: BookEntity,
  isFa: Boolean,
  eyebrowText: String,
  continueLabel: String,
  infoContentDescription: String,
  onClick: () -> Unit,
  onInfoClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val surface = MaterialTheme.colorScheme.surface
  val pct = book.progressPercent.roundToInt().coerceIn(0, 100)

  val spotlightBrush =
    Brush.linearGradient(
      colors =
        listOf(
          primaryContainer.copy(alpha = 0.58f),
          surface,
          surface,
        ),
    )

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = surface,
    border = BorderStroke(1.dp, primary.copy(alpha = 0.22f)),
    shadowElevation = 2.dp,
    modifier = modifier.clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(spotlightBrush)
          .padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Tall elevated book cover with format badge
        Box(modifier = Modifier.width(92.dp).height(138.dp)) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier = Modifier.fillMaxSize(),
          ) {
            BookCoverThumbnail(
              book = book,
              modifier = Modifier.fillMaxSize(),
            )
          }
          FormatPill(
            format = book.bookFormat(),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          // Eyebrow + Book Info quick action
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Surface(
              shape = CircleShape,
              color = primary.copy(alpha = 0.12f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(primary),
                )
                Text(
                  text = eyebrowText,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = if (isFa) 0.sp else 0.7.sp,
                  color = primary,
                  maxLines = 1,
                )
              }
            }

            IconButton(
              onClick = onInfoClick,
              modifier =
                Modifier
                  .size(34.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("continue_book_info_button"),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(32.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = infoContentDescription,
                    tint = primary,
                    modifier = Modifier.size(17.dp),
                  )
                }
              }
            }
          }

          Text(
            text = book.title,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(2.dp))

          // Progress track + percentage
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            LinearProgressIndicator(
              progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
              color = primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
            )
            Text(
              text = "$pct%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = primary,
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Primary Continue pill CTA
          Surface(
            shape = CircleShape,
            color = primary,
            modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onClick),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = continueLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Asymmetric 2-Column ATLAS Bento Strip:
 * - Left: Interactive Daily Goal Ring Card (opens Reading Stats Dashboard + inline Tune Goal button)
 * - Right: Two stacked quick-access Bento tiles for ATLAS Studio (Beta) and Free Public Catalog.
 */
@Composable
internal fun AtlasBentoDashboardStrip(
  todayMinutes: Int,
  todaySeconds: Int,
  dailyGoalMinutes: Int,
  totalReadingMinutesAllTime: Int,
  progressFraction: Float,
  goalMet: Boolean,
  onEditGoal: () -> Unit,
  onOpenStats: () -> Unit,
  onOpenStudio: () -> Unit,
  onOpenCatalog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val displayMins = if (todayMinutes == 0 && todaySeconds in 1..59) 1 else todayMinutes
  val primary = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val clampedProgress = progressFraction.coerceIn(0f, 1f)

  Row(
    modifier = modifier.fillMaxWidth().height(152.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Left Bento Tile: Daily Goal Ring + Stats trigger
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border =
        BorderStroke(
          width = 1.dp,
          color =
            if (goalMet) primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
      modifier =
        Modifier
          .weight(1.12f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(24.dp))
          .clickable(onClick = onOpenStats)
          .testTag("daily_goal_card"),
    ) {
      Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        // Top-end Edit Goal circle button
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .size(30.dp)
              .clip(CircleShape)
              .clickable(onClick = onEditGoal)
              .testTag("edit_daily_goal_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = strings.adjustGoalAction,
              tint = primary,
              modifier = Modifier.size(15.dp),
            )
          }
        }

        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Header with stats icon
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(end = 34.dp).testTag("library_stats_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.BarChart,
              contentDescription = strings.statsDashboardTitle,
              tint = primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = strings.todayGoalShort,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Center Ring + Numbers
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier.size(62.dp),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 6.5.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                drawArc(
                  color = trackColor,
                  startAngle = 0f,
                  sweepAngle = 360f,
                  useCenter = false,
                  topLeft = Offset(inset, inset),
                  size = arcSize,
                  style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                drawArc(
                  color = primary,
                  startAngle = -90f,
                  sweepAngle = (clampedProgress * 360f).coerceAtLeast(if (clampedProgress > 0f) 10f else 0f),
                  useCenter = false,
                  topLeft = Offset(inset, inset),
                  size = arcSize,
                  style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
              }
              if (goalMet) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = strings.dailyGoalComplete,
                  tint = primary,
                  modifier = Modifier.size(22.dp),
                )
              } else {
                Text(
                  text = "${(clampedProgress * 100f).roundToInt()}%",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = primary,
                )
              }
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$displayMins / ${dailyGoalMinutes}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (goalMet) strings.dailyGoalComplete
                  else strings.totalReadingTimeLabel(totalReadingMinutesAllTime),
                style = MaterialTheme.typography.labelSmall,
                color = if (goalMet) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
              )
            }
          }

          // Footer link
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = strings.openStatsAction,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = primary,
              maxLines = 1,
            )
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = null,
              tint = primary,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }

    // Right Bento Column: Stacked ATLAS Studio + Free Catalog quick tiles
    Column(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AtlasBentoQuickTile(
        icon = Icons.Rounded.EditNote,
        title = strings.studioTitle,
        subtitle = strings.studioCardSub,
        badgeText = strings.studioBetaBadge,
        accentTint = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = onOpenStudio,
        modifier = Modifier.fillMaxWidth().weight(1f).testTag("library_studio_chip"),
      )

      AtlasBentoQuickTile(
        icon = Icons.Rounded.Public,
        title = strings.catalogTitle,
        subtitle = strings.catalogCardSub,
        badgeText = null,
        accentTint = MaterialTheme.colorScheme.secondaryContainer,
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onOpenCatalog,
        modifier = Modifier.fillMaxWidth().weight(1f).testTag("library_catalog_chip"),
      )
    }
  }
}

@Composable
private fun AtlasBentoQuickTile(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badgeText: String?,
  accentTint: Color,
  iconTint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentTint,
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (badgeText != null) {
            Surface(
              shape = RoundedCornerShape(5.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
              )
            }
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Architectural section header with a Cobalt pill bar indicator and optional trailing controls.
 */
@Composable
internal fun AtlasHomeSectionHeader(
  title: String,
  countBadge: Int? = null,
  modifier: Modifier = Modifier,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f),
    ) {
      Box(
        modifier =
          Modifier
            .width(3.5.dp)
            .height(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (countBadge != null) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
        ) {
          Text(
            text = countBadge.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }
    }
    if (trailingContent != null) {
      trailingContent()
    }
  }
}

/**
 * Sleek elevated cover card for the horizontal Recently Opened shelf.
 */
@Composable
internal fun AtlasRecentShelfCard(
  book: BookEntity,
  onClick: () -> Unit,
) {
  val pct = book.progressPercent.roundToInt().coerceIn(0, 100)
  Column(
    modifier =
      Modifier
        .width(108.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(bottom = 4.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.68f)) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxSize(),
      ) {
        BookCoverThumbnail(
          book = book,
          modifier = Modifier.fillMaxSize(),
        )
      }
      Surface(
        shape = CircleShape,
        color = Color(0xCC0B101B),
        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
      ) {
        Text(
          text = "$pct%",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(7.dp))
    Text(
      text = book.title,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = book.author,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Redesigned ATLAS Empty Home State with Blue ATLAS emblem, primary Import CTA,
 * and a clean 3-tile Bento starter dock for Sample Classics, Studio, and Free Catalog.
 */
@Composable
internal fun AtlasHomeEmptyState(
  isImporting: Boolean,
  onImportClick: () -> Unit,
  onLoadSamplesClick: () -> Unit,
  onOpenStudioClick: () -> Unit,
  onOpenCatalogClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val primary = MaterialTheme.colorScheme.primary

  Column(
    modifier = modifier.padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(contentAlignment = Alignment.BottomEnd) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_library),
        contentDescription = "Minimalist book stack illustration",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxWidth(0.76f)
            .widthIn(max = 270.dp)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(26.dp)),
      )
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111832),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
        shadowElevation = 4.dp,
        modifier = Modifier.padding(10.dp).size(44.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_atlas_logo_foreground),
          contentDescription = strings.appName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
        )
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
      text = strings.emptyTitle,
      fontFamily = LiterataFontFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = strings.emptySubtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 360.dp),
    )

    Spacer(modifier = Modifier.height(22.dp))

    Button(
      onClick = onImportClick,
      enabled = !isImporting,
      shape = CircleShape,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
      modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp).testTag("empty_import_button"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = strings.importEpubOrPdf,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = strings.importFormatsHint,
      style = MaterialTheme.typography.labelSmall,
      color = primary,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Bento Starter Cards
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier =
        Modifier
          .fillMaxWidth()
          .widthIn(max = 360.dp)
          .clip(RoundedCornerShape(18.dp))
          .clickable(enabled = !isImporting, onClick = onLoadSamplesClick)
          .testTag("load_samples_button"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.AutoStories,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Text(
          text = strings.loadSamples,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = Icons.Rounded.ChevronRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AtlasBentoQuickTile(
        icon = Icons.Rounded.EditNote,
        title = strings.studioTitle,
        subtitle = strings.studioCardSub,
        badgeText = strings.studioBetaBadge,
        accentTint = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = onOpenStudioClick,
        modifier = Modifier.weight(1f).height(66.dp).testTag("empty_studio_button"),
      )
      AtlasBentoQuickTile(
        icon = Icons.Rounded.Public,
        title = strings.catalogTitle,
        subtitle = strings.catalogCardSub,
        badgeText = null,
        accentTint = MaterialTheme.colorScheme.secondaryContainer,
        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onOpenCatalogClick,
        modifier = Modifier.weight(1f).height(66.dp).testTag("empty_catalog_button"),
      )
    }
  }
}
