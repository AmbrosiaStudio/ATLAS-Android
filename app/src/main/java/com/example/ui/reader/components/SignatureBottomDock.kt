package com.example.ui.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.tts.ListenState
import com.example.model.BookFormat
import com.example.model.ReaderMode
import com.example.model.ReadingThemeMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.ReadingPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt

/**
 * Theme-responsive Liquid-Glass floating bottom interface matching the minimalist reader aesthetic:
 * - Subtle edge dissolve gradient so text fades smoothly under the dock without a heavy frosted slab.
 * - Floating clear specular Liquid-Glass pills (Time pill, Read | Listen segmented pill,
 *   Theme/Brightness circle, and TT Typography & Comfort circle) adapting to every reading theme.
 */
@Composable
fun SignatureBottomDockOverlay(
  visible: Boolean,
  expanded: Boolean,
  hazeState: HazeState,
  palette: ReadingPalette,
  bookFormat: BookFormat,
  progressPercent: Float,
  formattedTimeRemaining: String,
  sessionElapsedSeconds: Int,
  dailyGoalSummary: String,
  locationSummary: String,
  readerMode: ReaderMode,
  listenState: ListenState,
  onToggleExpanded: () -> Unit,
  onScrubProgress: (Float) -> Unit,
  onSelectMode: (ReaderMode) -> Unit,
  onTogglePlayPauseTts: () -> Unit,
  onCycleTtsSpeed: () -> Unit,
  onOpenThemeBrightness: () -> Unit,
  onOpenTypography: () -> Unit,
  onOpenContents: () -> Unit,
  onOpenStats: () -> Unit = {},
  batterySaverEnabled: Boolean = false,
  modifier: Modifier = Modifier,
) {
  var showScrubber by remember { mutableStateOf(false) }
  val strings = LocalAppStrings.current

  AnimatedVisibility(
    visible = visible,
    enter = if (batterySaverEnabled) EnterTransition.None else fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
    exit = if (batterySaverEnabled) ExitTransition.None else fadeOut(spring(stiffness = Spring.StiffnessMedium)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.BottomCenter,
    ) {
      // Smooth lightweight bottom dissolve (clear edge transition, NOT a heavy frosted fog block)
      val fadeHeight =
        when {
          !expanded -> 76.dp
          showScrubber || readerMode == ReaderMode.LISTEN -> 182.dp
          else -> 110.dp
        }
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(fadeHeight)
            .background(
              brush =
                Brush.verticalGradient(
                  0.00f to palette.background.copy(alpha = 0f),
                  0.40f to palette.background.copy(alpha = 0.20f),
                  0.72f to palette.background.copy(alpha = 0.62f),
                  1.00f to palette.background.copy(alpha = 0.92f),
                )
            )
      )

      // Theme-responsive clear Liquid-Glass visual tokens matching Top Bar
      val isNightBw = palette.mode == ReadingThemeMode.NIGHT_BW
      val glassTint =
        when {
          isNightBw -> Color(0xFF141414).copy(alpha = 0.76f)
          palette.isDark -> palette.dockSurface.copy(alpha = 0.72f)
          else -> palette.dockSurface.copy(alpha = 0.72f)
        }
      val glassFg = palette.text
      val glassMutedFg = palette.secondaryText
      val activeSegmentBg =
        when {
          isNightBw -> Color.White
          palette.isDark -> Color.White.copy(alpha = 0.16f)
          else -> Color.White.copy(alpha = 0.90f)
        }
      val activeSegmentFg =
        when {
          isNightBw -> Color.Black
          palette.isDark -> palette.text
          else -> palette.text
        }
      val activeSegmentBorder =
        if (palette.isDark) Color.White.copy(alpha = if (isNightBw) 0f else 0.24f)
        else palette.dockBorder.copy(alpha = 0.45f)

      val glassRim =
        Brush.verticalGradient(
          colors =
            if (palette.isDark) {
              listOf(
                Color.White.copy(alpha = if (isNightBw) 0.42f else 0.34f),
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.08f),
              )
            } else {
              listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.45f),
                palette.dockBorder.copy(alpha = 0.50f),
              )
            }
        )

      Column(
        modifier =
          Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Optional expanded progress scrubber / TTS Listen mini-bar floating above pills
        AnimatedVisibility(
          visible = expanded && (showScrubber || readerMode == ReaderMode.LISTEN),
          enter = if (batterySaverEnabled) EnterTransition.None else expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
          exit = if (batterySaverEnabled) ExitTransition.None else shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
        ) {
          LiquidGlassPillContainer(
            tintColor = glassTint,
            isDark = palette.isDark,
            rimBrush = glassRim,
            hazeState = hazeState,
            batterySaverEnabled = batterySaverEnabled,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
          ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
              if (readerMode == ReaderMode.LISTEN) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    val ttsPlayBg = if (isNightBw) Color.White else palette.accent
                    val ttsPlayFg = if (palette.isDark) Color(0xFF0A140E) else Color.White
                    Surface(
                      shape = CircleShape,
                      color = ttsPlayBg,
                      modifier =
                        Modifier
                          .size(34.dp)
                          .clip(CircleShape)
                          .clickable(onClick = onTogglePlayPauseTts)
                          .testTag("tts_play_pause_button"),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector =
                            if (listenState.isPlaying) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow,
                          contentDescription =
                            if (listenState.isPlaying) "Pause" else "Play",
                          tint = if (isNightBw) Color.Black else ttsPlayFg,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text =
                          if (!listenState.isReady) "Preparing voice…"
                          else if (listenState.isPlaying) strings.listenTab
                          else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = glassFg,
                      )
                      if (listenState.totalChunks > 0) {
                        Text(
                          text =
                            "${listenState.currentChunkIndex + 1} / ${listenState.totalChunks}",
                          style = MaterialTheme.typography.labelSmall,
                          color = glassMutedFg,
                        )
                      }
                    }
                  }

                  Surface(
                    shape = CircleShape,
                    color = palette.controlSurface.copy(alpha = 0.65f),
                    border = BorderStroke(0.75.dp, palette.dockBorder.copy(alpha = 0.6f)),
                    modifier =
                      Modifier
                        .clip(CircleShape)
                        .clickable { onCycleTtsSpeed() }
                        .testTag("tts_speed_button"),
                  ) {
                    Text(
                      text = "${listenState.speechRate}x",
                      style = MaterialTheme.typography.labelMedium,
                      color = glassFg,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                  }
                }
              }

              if (showScrubber) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = locationSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = glassMutedFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "${progressPercent.roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = glassFg,
                  )
                }
                val sessionMins = sessionElapsedSeconds / 60
                val sessionSecs = sessionElapsedSeconds % 60
                Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text =
                      "$dailyGoalSummary · ${String.format("%d:%02d", sessionMins, sessionSecs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = CircleShape,
                    color = palette.accent.copy(alpha = 0.14f),
                    border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.35f)),
                    modifier =
                      Modifier
                        .clip(CircleShape)
                        .clickable { onOpenStats() }
                        .testTag("reader_stats_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(13.dp),
                      )
                      Text(
                        text = strings.openStatsAction,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }
                var isDragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(progressPercent) }
                Slider(
                  value = (if (isDragging) dragValue else progressPercent).coerceIn(0f, 100f),
                  onValueChange = {
                    isDragging = true
                    dragValue = it
                  },
                  onValueChangeFinished = {
                    isDragging = false
                    onScrubProgress(dragValue)
                  },
                  valueRange = 0f..100f,
                  colors =
                    SliderDefaults.colors(
                      thumbColor = palette.accent,
                      activeTrackColor = palette.accent,
                      inactiveTrackColor = palette.text.copy(alpha = 0.16f),
                    ),
                  modifier = Modifier.fillMaxWidth().height(28.dp).testTag("reader_progress_slider"),
                )
              }
            }
          }
        }

        // Main Liquid Glass Pill Row (4 items): Time · Read|Listen · Theme · TT
        AnimatedVisibility(
          visible = expanded,
          enter = if (batterySaverEnabled) EnterTransition.None else expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
          exit = if (batterySaverEnabled) ExitTransition.None else shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          ) {
            // 1. Time / Progress Pill — tap to toggle scrubber
            LiquidGlassPillContainer(
              tintColor = glassTint,
              isDark = palette.isDark,
              rimBrush = glassRim,
              hazeState = hazeState,
              batterySaverEnabled = batterySaverEnabled,
              shape = CircleShape,
              modifier =
                Modifier
                  .height(44.dp)
                  .clip(CircleShape)
                  .clickable { showScrubber = !showScrubber }
                  .testTag("dock_time_pill"),
            ) {
              Row(
                modifier = Modifier.height(44.dp).padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Schedule,
                  contentDescription = "Reading progress and scrubber",
                  tint = glassFg,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = formattedTimeRemaining,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = glassFg,
                  maxLines = 1,
                )
              }
            }

            // 2. Read | Listen Liquid-Glass Segmented Pill
            LiquidGlassPillContainer(
              tintColor = glassTint,
              isDark = palette.isDark,
              rimBrush = glassRim,
              hazeState = hazeState,
              batterySaverEnabled = batterySaverEnabled,
              shape = CircleShape,
              modifier = Modifier.height(44.dp),
            ) {
              Row(
                modifier = Modifier.height(44.dp).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                LiquidModeChip(
                  label = strings.readTab,
                  selected = readerMode == ReaderMode.READ,
                  activeBg = activeSegmentBg,
                  activeFg = activeSegmentFg,
                  activeBorder = activeSegmentBorder,
                  inactiveFg = glassMutedFg,
                  testTag = "mode_read_button",
                  onClick = { onSelectMode(ReaderMode.READ) },
                )
                LiquidModeChip(
                  label = strings.listenTab,
                  selected = readerMode == ReaderMode.LISTEN,
                  enabled = bookFormat == BookFormat.EPUB,
                  activeBg = activeSegmentBg,
                  activeFg = activeSegmentFg,
                  activeBorder = activeSegmentBorder,
                  inactiveFg = glassMutedFg,
                  testTag = "mode_listen_button",
                  onClick = { onSelectMode(ReaderMode.LISTEN) },
                )
              }
            }

            // 3. Brightness & Theme Circle Button
            LiquidGlassPillContainer(
              tintColor = glassTint,
              isDark = palette.isDark,
              rimBrush = glassRim,
              hazeState = hazeState,
              batterySaverEnabled = batterySaverEnabled,
              shape = CircleShape,
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onOpenThemeBrightness)
                  .testTag("dock_theme_button"),
            ) {
              Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Outlined.WbSunny,
                  contentDescription = strings.readingTheme,
                  tint = glassFg,
                  modifier = Modifier.size(19.dp),
                )
              }
            }

            // 4. Typography & Reading Comfort (TT) Circle Button
            LiquidGlassPillContainer(
              tintColor = glassTint,
              isDark = palette.isDark,
              rimBrush = glassRim,
              hazeState = hazeState,
              batterySaverEnabled = batterySaverEnabled,
              shape = CircleShape,
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onOpenTypography)
                  .testTag("dock_typography_button"),
            ) {
              Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = "TT",
                  fontFamily = LiterataFontFamily,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = glassFg,
                  letterSpacing = (-0.5).sp,
                )
              }
            }
          }
        }

        // Collapsed compact indicator + Expand/Collapse Chevron
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(top = 2.dp),
        ) {
          if (!expanded) {
            LiquidGlassPillContainer(
              tintColor = glassTint,
              isDark = palette.isDark,
              rimBrush = glassRim,
              hazeState = hazeState,
              batterySaverEnabled = batterySaverEnabled,
              shape = CircleShape,
              modifier =
                Modifier
                  .padding(end = 6.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onOpenContents)
                  .testTag("dock_contents_button"),
            ) {
              Text(
                text = "${progressPercent.roundToInt()}% · ${strings.contentsTab}",
                style = MaterialTheme.typography.labelSmall,
                color = glassFg,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }

          IconButton(
            onClick = onToggleExpanded,
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .size(36.dp)
                .testTag("dock_expand_toggle"),
          ) {
            Icon(
              imageVector =
                if (expanded) Icons.Rounded.KeyboardArrowDown
                else Icons.Rounded.KeyboardArrowUp,
              contentDescription = if (expanded) "Collapse controls" else "Expand controls",
              tint = palette.text.copy(alpha = 0.78f),
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiquidGlassPillContainer(
  tintColor: Color,
  isDark: Boolean,
  rimBrush: Brush,
  hazeState: HazeState,
  shape: Shape,
  modifier: Modifier = Modifier,
  batterySaverEnabled: Boolean = false,
  content: @Composable () -> Unit,
) {
  val specularSheen =
    if (isDark) {
      Brush.verticalGradient(
        colors =
          listOf(
            Color.White.copy(alpha = 0.14f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.12f),
          )
      )
    } else {
      Brush.verticalGradient(
        colors =
          listOf(
            Color.White.copy(alpha = 0.48f),
            Color.White.copy(alpha = 0.08f),
            Color.Black.copy(alpha = 0.03f),
          )
      )
    }

  Box(
    modifier =
      modifier
        .then(
          if (batterySaverEnabled) {
            Modifier
          } else {
            Modifier.shadow(
              elevation = if (isDark) 6.dp else 8.dp,
              shape = shape,
              ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.14f),
              spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.14f),
            )
          }
        )
        .clip(shape)
        .then(
          if (batterySaverEnabled) {
            Modifier.background(tintColor.copy(alpha = 0.94f))
          } else {
            Modifier
              .hazeEffect(
                state = hazeState,
                style =
                  HazeStyle(
                    backgroundColor = tintColor,
                    tint = HazeTint(tintColor),
                    blurRadius = 8.dp,
                  ),
              )
              .background(specularSheen)
          }
        )
        .border(BorderStroke(0.95.dp, rimBrush), shape)
  ) {
    content()
  }
}

@Composable
private fun LiquidModeChip(
  label: String,
  selected: Boolean,
  activeBg: Color,
  activeFg: Color,
  activeBorder: Color,
  inactiveFg: Color,
  testTag: String,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .height(36.dp)
        .clip(CircleShape)
        .background(if (selected) activeBg else Color.Transparent)
        .then(
          if (selected && activeBorder.alpha > 0.01f) {
            Modifier.border(BorderStroke(0.6.dp, activeBorder), CircleShape)
          } else {
            Modifier
          }
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 13.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      color =
        when {
          !enabled -> inactiveFg.copy(alpha = 0.4f)
          selected -> activeFg
          else -> inactiveFg
        },
    )
  }
}
