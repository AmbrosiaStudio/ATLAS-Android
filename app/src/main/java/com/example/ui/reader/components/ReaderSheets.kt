package com.example.ui.reader.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.model.AmbientSoundType
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.BookSearchMatch
import com.example.model.ComfortPreset
import com.example.model.CustomFontEntry
import com.example.model.EpubTocItem
import com.example.model.ReaderFontOption
import com.example.model.ReaderMode
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingRulerStyle
import com.example.model.ReadingThemeMode
import com.example.model.SleepTimerPreset
import com.example.model.TapZoneAction
import com.example.model.TextAlignOption
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.reader.FocusTimerState
import com.example.ui.reader.SleepTimerState
import com.example.ui.theme.AtlasGroupedCard
import com.example.ui.theme.AtlasHairlineDivider
import com.example.ui.theme.AtlasOnAccentDark
import com.example.ui.theme.AtlasRadioDot
import com.example.ui.theme.AtlasSegmentedControl
import com.example.ui.theme.AtlasSliderRow
import com.example.ui.theme.AtlasToggleRow
import com.example.ui.theme.AtlasVerticalCapsuleSlider
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.ReadingPalette
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.toPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeBrightnessSheet(
  settings: ReaderSettings,
  palette: ReadingPalette,
  bookFormat: BookFormat,
  readerMode: ReaderMode = ReaderMode.READ,
  onSelectTheme: (ReadingThemeMode) -> Unit,
  onChangeBrightness: (Float) -> Unit,
  onChangeFontSize: (Int) -> Unit = {},
  onSelectLayoutMode: (ReadingLayoutMode) -> Unit = {},
  onToggleKeepScreenOn: (Boolean) -> Unit = {},
  onToggleReadingRuler: (Boolean) -> Unit = {},
  onTogglePaperTexture: (Boolean) -> Unit,
  onToggleBatterySaver: (Boolean) -> Unit,
  onTogglePdfThemeFilter: (Boolean) -> Unit,
  onOpenTypography: () -> Unit = {},
  onOpenSearch: () -> Unit = {},
  onToggleListenMode: () -> Unit = {},
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val strings = LocalAppStrings.current
  val isFa = settings.appLanguage == AppLanguage.PERSIAN
  val tileSurface = palette.controlSurface.copy(alpha = if (palette.isDark) 0.85f else 0.78f)
  val cardSurface = if (palette.isDark) palette.controlSurface.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.75f)
  val isAutoBrightness = settings.brightnessOverride < 0f
  val displayBrightness = if (isAutoBrightness) 0.65f else settings.brightnessOverride

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.dockSurface,
    contentColor = palette.text,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(max = 640.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Minimalist centered header
      Text(
        text = strings.readerSettingsTitle,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = palette.secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )

      // Row 1: Circular Theme Swatch Pill Bar + Circular Voice Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = tileSurface,
            border = BorderStroke(1.dp, palette.dockBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 10.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ReadingThemeMode.entries.forEach { mode ->
                val modePalette = mode.toPalette()
                val selected = settings.themeMode == mode
                Box(
                  modifier =
                    Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(modePalette.background)
                      .border(
                        width = if (selected) 2.5.dp else 1.2.dp,
                        color = if (selected) palette.accent else modePalette.dockBorder,
                        shape = CircleShape,
                      )
                      .clickable { onSelectTheme(mode) }
                      .testTag("theme_swatch_${mode.name.lowercase()}"),
                  contentAlignment = Alignment.Center,
                ) {
                  if (selected) {
                    Box(
                      modifier =
                        Modifier
                          .size(16.dp)
                          .clip(CircleShape)
                          .background(palette.accent),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = if (isFa) mode.faLabel else mode.label,
                        tint = if (palette.isDark) AtlasOnAccentDark else Color.White,
                        modifier = Modifier.size(11.dp),
                      )
                    }
                  } else {
                    Text(
                      text = "A",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = modePalette.text.copy(alpha = 0.72f),
                    )
                  }
                }
              }
            }
          }
          Text(
            text = "${strings.readingTheme} · ${if (isFa) settings.themeMode.faLabel else settings.themeMode.label}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Circular Voice / Listen quick toggle
        val listenActive = readerMode == ReaderMode.LISTEN
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = if (listenActive) palette.accent else tileSurface,
            border =
              BorderStroke(
                width = 1.dp,
                color = if (listenActive) palette.accent else palette.dockBorder,
              ),
            modifier =
              Modifier
                .size(54.dp)
                .clip(CircleShape)
                .clickable(enabled = bookFormat.isReflowableText) {
                  onToggleListenMode()
                  onDismiss()
                }
                .testTag("control_hub_voice_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = strings.voiceQuickLabel,
                tint =
                  when {
                    !bookFormat.isReflowableText -> palette.secondaryText.copy(alpha = 0.4f)
                    listenActive -> if (palette.isDark) AtlasOnAccentDark else Color.White
                    else -> palette.text
                  },
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Text(
            text = strings.voiceQuickLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (listenActive) palette.accent else palette.secondaryText,
            maxLines = 1,
          )
        }
      }

      // Row 2: Bento Control Center Grid (Left: Large Text Customize Tile + 2 Quick Circles;
      // Right: Search & Flow Squircles + Dual Vertical Capsule Scrubbers for Text Size & Brightness)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Left Column
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Large "Aa · Text Customize" Bento Tile -> opens Advanced Typography Studio
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = tileSurface,
            border = BorderStroke(1.dp, palette.dockBorder),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onOpenTypography() }
                .testTag("control_hub_text_customize"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Aa",
                  fontFamily = settings.fontOption.toComposeFontFamily(),
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = palette.text,
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = palette.secondaryText,
                  modifier = Modifier.size(18.dp),
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = strings.textCustomizeTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.text,
                  maxLines = 1,
                )
                Text(
                  text = strings.textCustomizeSub,
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.secondaryText,
                  maxLines = 1,
                )
              }
            }
          }

          Text(
            text = strings.advancedLabel,
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )

          // Dual Circular Quick Toggles: Keep Screen Awake & Reading Ruler
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ControlHubCircleToggle(
              icon = if (settings.keepScreenOn) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
              label = strings.awakeQuickLabel,
              active = settings.keepScreenOn,
              palette = palette,
              tileSurface = tileSurface,
              testTag = "control_hub_awake_toggle",
              onClick = { onToggleKeepScreenOn(!settings.keepScreenOn) },
              modifier = Modifier.weight(1f),
            )
            ControlHubCircleToggle(
              icon = Icons.Rounded.HorizontalRule,
              label = strings.rulerQuickLabel,
              active = settings.readingRulerEnabled,
              palette = palette,
              tileSurface = tileSurface,
              testTag = "control_hub_ruler_toggle",
              onClick = { onToggleReadingRuler(!settings.readingRulerEnabled) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Right Column
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Top two quick action squircles: Search + Flow (Paged / Scroll)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ControlHubSquircleAction(
              icon = Icons.Rounded.Search,
              label = strings.searchTab,
              active = false,
              palette = palette,
              tileSurface = tileSurface,
              testTag = "control_hub_search_button",
              onClick = onOpenSearch,
              modifier = Modifier.weight(1f),
            )
            val isScroll = settings.readingLayoutMode == ReadingLayoutMode.SCROLL
            ControlHubSquircleAction(
              icon = if (isScroll) Icons.Rounded.SwapVert else Icons.AutoMirrored.Rounded.MenuBook,
              label = strings.transitionsLabel,
              active = isScroll,
              palette = palette,
              tileSurface = tileSurface,
              testTag = "control_hub_flow_button",
              onClick = {
                val next =
                  if (isScroll) ReadingLayoutMode.PAGED else ReadingLayoutMode.SCROLL
                onSelectLayoutMode(next)
              },
              modifier = Modifier.weight(1f),
            )
          }

          // Side-by-side Vision-Inspired Vertical Capsule Scrubbers (Text Size & Brightness)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AtlasVerticalCapsuleSlider(
              value = settings.fontSizeSp.toFloat(),
              valueRange = 12f..32f,
              onValueChange = { onChangeFontSize(it.roundToInt()) },
              label = strings.textSize,
              valueBadge = "${settings.fontSizeSp}",
              accentColor = palette.accent,
              trackColor = tileSurface,
              fillColor = palette.accent.copy(alpha = if (palette.isDark) 0.34f else 0.20f),
              borderColor = palette.dockBorder,
              iconColor = palette.text,
              labelColor = palette.secondaryText,
              glyphText = "Tt",
              height = 132.dp,
              modifier = Modifier.weight(1f).testTag("hub_text_size_scrubber"),
            )
            AtlasVerticalCapsuleSlider(
              value = displayBrightness,
              valueRange = 0.05f..1.0f,
              onValueChange = { onChangeBrightness(it.coerceIn(0.05f, 1f)) },
              label = strings.brightness,
              valueBadge = if (isAutoBrightness) "Auto" else "${(displayBrightness * 100).roundToInt()}%",
              accentColor = palette.accent,
              trackColor = tileSurface,
              fillColor = palette.accent.copy(alpha = if (palette.isDark) 0.34f else 0.20f),
              borderColor = palette.dockBorder,
              iconColor = palette.text,
              labelColor = palette.secondaryText,
              icon = Icons.Outlined.WbSunny,
              height = 132.dp,
              modifier = Modifier.weight(1f).testTag("brightness_slider"),
            )
          }
        }
      }

      // Brightness Auto/Manual quick bar + Display Options Card
      AtlasGroupedCard(
        containerColor = cardSurface,
        borderColor = palette.dockBorder,
        contentPadding = 14.dp,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.brightness,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = palette.text,
          )
          Surface(
            shape = CircleShape,
            color = if (isAutoBrightness) palette.accent.copy(alpha = 0.16f) else tileSurface,
            border = BorderStroke(1.dp, if (isAutoBrightness) palette.accent else palette.dockBorder),
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable { onChangeBrightness(if (isAutoBrightness) 0.65f else -1f) }
                .testTag("brightness_auto_toggle"),
          ) {
            Text(
              text = if (isAutoBrightness) strings.systemAuto else strings.manual,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (isAutoBrightness) palette.accent else palette.secondaryText,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        AtlasHairlineDivider(color = palette.divider)

        AtlasToggleRow(
          title = strings.paperStyleToggle,
          subtitle = strings.paperStyleSub,
          checked = settings.paperTextureEnabled,
          onCheckedChange = onTogglePaperTexture,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("theme_paper_texture_switch"),
        )

        AtlasHairlineDivider(color = palette.divider)

        AtlasToggleRow(
          title = strings.batterySaverToggle,
          subtitle = strings.batterySaverSub,
          checked = settings.batterySaverModeEnabled,
          onCheckedChange = onToggleBatterySaver,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("theme_battery_saver_switch"),
        )

        if (!bookFormat.isReflowableText) {
          AtlasHairlineDivider(color = palette.divider)
          AtlasToggleRow(
            title = strings.adaptPdfColors,
            subtitle = strings.adaptPdfColorsSub,
            checked = settings.pdfThemeFilterEnabled,
            onCheckedChange = onTogglePdfThemeFilter,
            activeColor = palette.accent,
            textColor = palette.text,
            secondaryColor = palette.secondaryText,
            inactiveTrackColor = palette.controlSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun ControlHubCircleToggle(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean,
  palette: ReadingPalette,
  tileSurface: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = if (active) palette.accent.copy(alpha = 0.20f) else tileSurface,
      border = BorderStroke(1.2.dp, if (active) palette.accent else palette.dockBorder),
      modifier =
        Modifier
          .size(54.dp)
          .clip(CircleShape)
          .clickable(onClick = onClick)
          .testTag(testTag),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (active) palette.accent else palette.text,
          modifier = Modifier.size(21.dp),
        )
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) palette.accent else palette.secondaryText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ControlHubSquircleAction(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean,
  palette: ReadingPalette,
  tileSurface: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = if (active) palette.accent.copy(alpha = 0.18f) else tileSurface,
      border = BorderStroke(1.dp, if (active) palette.accent else palette.dockBorder),
      modifier =
        Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(18.dp))
          .clickable(onClick = onClick)
          .testTag(testTag),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (active) palette.accent else palette.text,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) palette.accent else palette.secondaryText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypographySettingsSheet(
  settings: ReaderSettings,
  customFonts: List<CustomFontEntry>,
  palette: ReadingPalette,
  bookFormat: BookFormat,
  onSelectLayoutMode: (ReadingLayoutMode) -> Unit,
  onSelectFont: (ReaderFontOption) -> Unit,
  onSelectCustomFont: (String) -> Unit,
  onChangeFontSize: (Int) -> Unit,
  onChangeLineHeight: (Float) -> Unit,
  onChangeMargin: (Int) -> Unit,
  onChangeVerticalMargin: (Int) -> Unit,
  onChangeParagraphSpacing: (Float) -> Unit,
  onChangeLetterSpacing: (Float) -> Unit,
  onChangeFontWeight: (Int) -> Unit,
  onChangeEyeComfortWarmth: (Float) -> Unit,
  onTogglePageBorder: (Boolean) -> Unit,
  onTogglePaperTexture: (Boolean) -> Unit,
  onToggleKeepScreenOn: (Boolean) -> Unit,
  onToggleVolumeKeyPageTurn: (Boolean) -> Unit,
  onToggleBatterySaver: (Boolean) -> Unit,
  onSelectAlign: (TextAlignOption) -> Unit,
  onTogglePdfThemeFilter: (Boolean) -> Unit,
  onSelectLeftTapAction: (TapZoneAction) -> Unit = {},
  onSelectCenterTapAction: (TapZoneAction) -> Unit = {},
  onSelectRightTapAction: (TapZoneAction) -> Unit = {},
  onToggleEdgeBrightnessSwipe: (Boolean) -> Unit = {},
  onToggleDoubleTapBookmark: (Boolean) -> Unit = {},
  onToggleReadingRuler: (Boolean) -> Unit = {},
  onSelectReadingRulerStyle: (ReadingRulerStyle) -> Unit = {},
  onChangeReadingRulerHeight: (Int) -> Unit = {},
  onChangeReadingRulerDimAlpha: (Float) -> Unit = {},
  onApplyComfortPreset: (ComfortPreset) -> Unit,
  onResetComfort: () -> Unit,
  onOpenThemeHub: () -> Unit = {},
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val strings = LocalAppStrings.current
  val isFa = settings.appLanguage == AppLanguage.PERSIAN
  val cardSurface = if (palette.isDark) palette.controlSurface.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.80f)
  val tileSurface = palette.controlSurface.copy(alpha = if (palette.isDark) 0.85f else 0.72f)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.dockSurface,
    contentColor = palette.text,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(max = 640.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Studio Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (bookFormat.isReflowableText) strings.typographyAndLayout else strings.pageDisplay,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = palette.text,
        )
        Surface(
          shape = CircleShape,
          color = tileSurface,
          border = BorderStroke(1.dp, palette.dockBorder),
          modifier =
            Modifier
              .clip(CircleShape)
              .clickable(onClick = onResetComfort)
              .testTag("sheet_reset_comfort"),
        ) {
          Text(
            text = strings.resetDefaults,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      // 1. Live Typography Preview Card + "Text & Background" Shortcut (Vision-inspired)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardSurface,
        border = BorderStroke(1.dp, palette.dockBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Live Preview Window styled with current reading theme & typography
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(palette.background)
                .then(
                  if (settings.eyeComfortWarmth > 0.01f) {
                    Modifier.background(Color(0xFFFFA726).copy(alpha = settings.eyeComfortWarmth * 0.16f))
                  } else {
                    Modifier
                  }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = strings.previewLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText,
              )
              val previewWeight =
                when (settings.fontWeightValue) {
                  500 -> FontWeight.Medium
                  600 -> FontWeight.SemiBold
                  else -> FontWeight.Normal
                }
              Text(
                text = strings.typographyPreviewSample,
                fontFamily = settings.fontOption.toComposeFontFamily(),
                fontSize = settings.fontSizeSp.coerceIn(13, 24).sp,
                lineHeight = (settings.fontSizeSp.coerceIn(13, 24) * settings.lineHeightMultiplier).sp,
                letterSpacing = (settings.letterSpacingEm * 12f).sp,
                fontWeight = previewWeight,
                textAlign =
                  if (settings.textAlign == TextAlignOption.JUSTIFY) TextAlign.Justify
                  else TextAlign.Start,
                color = palette.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          HorizontalDivider(thickness = 0.8.dp, color = palette.divider)

          // Text & Background quick jump row -> Theme Control Center
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenThemeHub)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("typo_jump_theme_hub"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(palette.accent.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.textAndBackgroundTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
              )
              Text(
                text = strings.textAndBackgroundSub,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = null,
              tint = palette.secondaryText,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      // 2. Select Font — Horizontal Visual "Aa" Cards (Vision-inspired)
      if (bookFormat.isReflowableText) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.selectFontLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = palette.secondaryText,
            )
            Text(
              text = strings.manageFontsTts,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = palette.accent,
              modifier =
                Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    onDismiss()
                    onOpenSettings()
                  }
                  .padding(horizontal = 6.dp, vertical = 4.dp)
                  .testTag("manage_fonts_shortcut"),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ReaderFontOption.entries.forEach { option ->
              val selected = settings.customFontId == null && settings.fontOption == option
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) cardSurface else tileSurface,
                border =
                  BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else palette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .width(92.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectFont(option) }
                    .testTag("font_option_${option.name.lowercase()}"),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = if (option.supportsPersian) "اب" else "Aa",
                    fontFamily = option.toComposeFontFamily(),
                    fontSize = 22.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.text,
                  )
                  Text(
                    text = option.displayName.substringBefore(" ("),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) palette.accent else palette.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
            customFonts.forEach { cf ->
              val selected = settings.customFontId == cf.id
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) cardSurface else tileSurface,
                border =
                  BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else palette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .width(92.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectCustomFont(cf.id) }
                    .testTag("custom_font_${cf.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = "Aa",
                    fontSize = 22.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.text,
                  )
                  Text(
                    text = cf.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) palette.accent else palette.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      // 3. Comfort Presets & Page Flow Segmented Control
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.comfortPresets,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = palette.secondaryText,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ComfortPreset.entries.forEach { preset ->
            Surface(
              shape = CircleShape,
              color = tileSurface,
              border = BorderStroke(1.dp, palette.dockBorder),
              modifier =
                Modifier
                  .clip(CircleShape)
                  .clickable { onApplyComfortPreset(preset) }
                  .testTag("sheet_preset_${preset.name.lowercase()}"),
            ) {
              Text(
                text = if (isFa) preset.faLabel else preset.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              )
            }
          }
        }

        AtlasSegmentedControl(
          options = ReadingLayoutMode.entries.map { if (isFa) it.faLabel else it.label },
          selectedIndex = ReadingLayoutMode.entries.indexOf(settings.readingLayoutMode).coerceAtLeast(0),
          onSelect = { idx -> onSelectLayoutMode(ReadingLayoutMode.entries[idx]) },
          trackColor = tileSurface,
          activeSurfaceColor = cardSurface,
          activeTextColor = palette.accent,
          inactiveTextColor = palette.secondaryText,
          borderColor = palette.dockBorder,
        )
      }

      // 4. Grouped Typography Controls Card
      if (!bookFormat.isReflowableText) {
        AtlasGroupedCard(
          containerColor = cardSurface,
          borderColor = palette.dockBorder,
          contentPadding = 14.dp,
        ) {
          Text(
            text =
              if (isFa) "برای بزرگ‌نمایی روان صفحات، با دو انگشت زوم کنید یا دو بار ضربه بزنید."
              else "Pinch with two fingers or double-tap any page to zoom in and pan smoothly.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
          AtlasSliderRow(
            label = strings.margins,
            valueText = "${settings.horizontalMarginDp} dp",
            value = settings.horizontalMarginDp.toFloat(),
            valueRange = 8f..56f,
            onValueChange = { onChangeMargin(it.roundToInt()) },
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.verticalMargins,
            valueText = "${settings.verticalMarginDp} dp",
            value = settings.verticalMarginDp.toFloat(),
            valueRange = 8f..60f,
            onValueChange = { onChangeVerticalMargin(it.roundToInt()) },
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasHairlineDivider(color = palette.divider)
          AtlasToggleRow(
            title = strings.adaptPdfColors,
            checked = settings.pdfThemeFilterEnabled,
            onCheckedChange = onTogglePdfThemeFilter,
            activeColor = palette.accent,
            textColor = palette.text,
            secondaryColor = palette.secondaryText,
            inactiveTrackColor = palette.controlSurface,
          )
        }
      } else {
        AtlasGroupedCard(
          containerColor = cardSurface,
          borderColor = palette.dockBorder,
          contentPadding = 14.dp,
        ) {
          // Tt Font Size with quick - / + stepper + slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Tt",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
              )
              Text(
                text = strings.textSize,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = tileSurface,
                border = BorderStroke(1.dp, palette.dockBorder),
                modifier =
                  Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onChangeFontSize((settings.fontSizeSp - 1).coerceAtLeast(12)) },
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.Remove, contentDescription = "Decrease font size", tint = palette.text, modifier = Modifier.size(16.dp))
                }
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.accent.copy(alpha = 0.14f),
              ) {
                Text(
                  text = "${settings.fontSizeSp}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.accent,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Surface(
                shape = CircleShape,
                color = tileSurface,
                border = BorderStroke(1.dp, palette.dockBorder),
                modifier =
                  Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onChangeFontSize((settings.fontSizeSp + 1).coerceAtMost(32)) },
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.Add, contentDescription = "Increase font size", tint = palette.text, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
          Slider(
            value = settings.fontSizeSp.toFloat(),
            onValueChange = { onChangeFontSize(it.roundToInt()) },
            valueRange = 12f..32f,
            colors =
              SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.controlSurface,
              ),
            modifier = Modifier.fillMaxWidth().testTag("font_size_slider"),
          )

          AtlasHairlineDivider(color = palette.divider)

          // A Font Weight row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "A",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
              )
              Text(
                text = strings.fontWeightLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(
                400 to strings.weightRegular,
                500 to strings.weightMedium,
                600 to strings.weightBold,
              ).forEach { (weight, lbl) ->
                val selected = settings.fontWeightValue == weight
                Surface(
                  shape = RoundedCornerShape(11.dp),
                  color = if (selected) palette.accent.copy(alpha = 0.16f) else tileSurface,
                  border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(11.dp))
                      .clickable { onChangeFontWeight(weight) }
                      .testTag("weight_$weight"),
                ) {
                  Text(
                    text = lbl,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.text,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
              }
            }
          }

          AtlasHairlineDivider(color = palette.divider)

          AtlasSliderRow(
            label = strings.lineSpacing,
            valueText = String.format("%.2f×", settings.lineHeightMultiplier),
            value = settings.lineHeightMultiplier,
            valueRange = 1.15f..2.4f,
            onValueChange = onChangeLineHeight,
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.margins,
            valueText = "${settings.horizontalMarginDp} dp",
            value = settings.horizontalMarginDp.toFloat(),
            valueRange = 8f..56f,
            onValueChange = { onChangeMargin(it.roundToInt()) },
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.verticalMargins,
            valueText = "${settings.verticalMarginDp} dp",
            value = settings.verticalMarginDp.toFloat(),
            valueRange = 8f..60f,
            onValueChange = { onChangeVerticalMargin(it.roundToInt()) },
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.paragraphSpacing,
            valueText = String.format("%.2f em", settings.paragraphSpacingEm),
            value = settings.paragraphSpacingEm,
            valueRange = 0.4f..2.0f,
            onValueChange = onChangeParagraphSpacing,
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.letterSpacing,
            valueText = String.format("%+.2f em", settings.letterSpacingEm),
            value = settings.letterSpacingEm,
            valueRange = -0.02f..0.12f,
            onValueChange = onChangeLetterSpacing,
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
        }

        // 5. Visual 2-Column Text Alignment Cards (Vision-inspired with custom AtlasRadioDot)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.alignment,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            TextAlignOption.entries.forEach { align ->
              val selected = settings.textAlign == align
              val alignLabel = if (isFa) align.faLabel else align.label
              val alignDesc =
                if (align == TextAlignOption.LEFT) strings.alignDefaultDesc else strings.alignJustifyDesc
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) cardSurface else tileSurface,
                border =
                  BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else palette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectAlign(align) }
                    .testTag("align_${align.name.lowercase()}"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = alignLabel,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) palette.accent else palette.text,
                    )
                    Icon(
                      imageVector =
                        if (align == TextAlignOption.LEFT) Icons.AutoMirrored.Rounded.FormatAlignLeft
                        else Icons.Rounded.FormatAlignJustify,
                      contentDescription = alignLabel,
                      tint = if (selected) palette.accent else palette.secondaryText,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  Text(
                    text = alignDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                  ) {
                    AtlasRadioDot(
                      selected = selected,
                      activeColor = palette.accent,
                      inactiveBorderColor = palette.secondaryText.copy(alpha = 0.4f),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 6. Reading Comfort & Eye Warmth Grouped Card
      AtlasGroupedCard(
        containerColor = cardSurface,
        borderColor = palette.dockBorder,
        contentPadding = 14.dp,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Rounded.Tune, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
          Text(
            text = strings.readingComfort,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.text,
          )
        }
        AtlasSliderRow(
          label = strings.eyeComfortWarmth,
          valueText = "${(settings.eyeComfortWarmth * 100f).roundToInt()}%",
          value = settings.eyeComfortWarmth,
          valueRange = 0f..1f,
          onValueChange = onChangeEyeComfortWarmth,
          accentColor = palette.accent,
          textColor = palette.text,
          trackColor = palette.controlSurface,
        )
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.pageBorderToggle,
          checked = settings.showPageBorder,
          onCheckedChange = onTogglePageBorder,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
        )
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.paperStyleToggle,
          subtitle = strings.paperStyleSub,
          checked = settings.paperTextureEnabled,
          onCheckedChange = onTogglePaperTexture,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("sheet_paper_texture_switch"),
        )
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.keepScreenAwake,
          checked = settings.keepScreenOn,
          onCheckedChange = onToggleKeepScreenOn,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
        )
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.volumeKeyPageTurnToggle,
          subtitle = strings.volumeKeyPageTurnSub,
          checked = settings.volumeKeyPageTurnEnabled,
          onCheckedChange = onToggleVolumeKeyPageTurn,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("sheet_volume_key_switch"),
        )
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.batterySaverToggle,
          subtitle = strings.batterySaverSub,
          checked = settings.batterySaverModeEnabled,
          onCheckedChange = onToggleBatterySaver,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("sheet_battery_saver_switch"),
        )
      }

      // 7. Tap Zones & Gestures Grouped Card
      AtlasGroupedCard(
        containerColor = cardSurface,
        borderColor = palette.dockBorder,
        contentPadding = 14.dp,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
          Text(
            text = strings.tapZonesSection,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.text,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TapZoneDropdownTile(
            zoneLabel = strings.leftTapZoneLabel,
            selectedAction = settings.leftTapAction,
            isFa = isFa,
            palette = palette,
            onSelect = onSelectLeftTapAction,
            modifier = Modifier.weight(1f).testTag("tap_zone_left_selector"),
          )
          TapZoneDropdownTile(
            zoneLabel = strings.centerTapZoneLabel,
            selectedAction = settings.centerTapAction,
            isFa = isFa,
            palette = palette,
            onSelect = onSelectCenterTapAction,
            modifier = Modifier.weight(1f).testTag("tap_zone_center_selector"),
          )
          TapZoneDropdownTile(
            zoneLabel = strings.rightTapZoneLabel,
            selectedAction = settings.rightTapAction,
            isFa = isFa,
            palette = palette,
            onSelect = onSelectRightTapAction,
            modifier = Modifier.weight(1f).testTag("tap_zone_right_selector"),
          )
        }
        AtlasHairlineDivider(color = palette.divider)
        AtlasToggleRow(
          title = strings.edgeBrightnessSwipeToggle,
          subtitle = strings.edgeBrightnessSwipeSub,
          checked = settings.edgeBrightnessSwipeEnabled,
          onCheckedChange = onToggleEdgeBrightnessSwipe,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("sheet_edge_brightness_switch"),
        )
        if (bookFormat.isReflowableText) {
          AtlasHairlineDivider(color = palette.divider)
          AtlasToggleRow(
            title = strings.doubleTapBookmarkToggle,
            subtitle = strings.doubleTapBookmarkSub,
            checked = settings.doubleTapBookmarkEnabled,
            onCheckedChange = onToggleDoubleTapBookmark,
            activeColor = palette.accent,
            textColor = palette.text,
            secondaryColor = palette.secondaryText,
            inactiveTrackColor = palette.controlSurface,
            modifier = Modifier.testTag("sheet_double_tap_bookmark_switch"),
          )
        }
      }

      // 8. Reading Ruler / Line Guide Grouped Card
      AtlasGroupedCard(
        containerColor = cardSurface,
        borderColor = palette.dockBorder,
        contentPadding = 14.dp,
      ) {
        AtlasToggleRow(
          title = strings.readingRulerSection,
          subtitle = strings.readingRulerSub,
          checked = settings.readingRulerEnabled,
          onCheckedChange = onToggleReadingRuler,
          icon = Icons.Rounded.HorizontalRule,
          activeColor = palette.accent,
          textColor = palette.text,
          secondaryColor = palette.secondaryText,
          inactiveTrackColor = palette.controlSurface,
          modifier = Modifier.testTag("sheet_reading_ruler_switch"),
        )
        if (settings.readingRulerEnabled) {
          AtlasHairlineDivider(color = palette.divider)
          Text(
            text = strings.readingRulerStyleLabel,
            style = MaterialTheme.typography.labelLarge,
            color = palette.secondaryText,
          )
          AtlasSegmentedControl(
            options = ReadingRulerStyle.entries.map { if (isFa) it.faLabel else it.label },
            selectedIndex = ReadingRulerStyle.entries.indexOf(settings.readingRulerStyle).coerceAtLeast(0),
            onSelect = { idx -> onSelectReadingRulerStyle(ReadingRulerStyle.entries[idx]) },
            trackColor = tileSurface,
            activeSurfaceColor = cardSurface,
            activeTextColor = palette.accent,
            inactiveTextColor = palette.secondaryText,
            borderColor = palette.dockBorder,
          )
          AtlasSliderRow(
            label = strings.readingRulerHeightLabel,
            valueText = "${settings.readingRulerHeightDp} dp",
            value = settings.readingRulerHeightDp.toFloat(),
            valueRange = 32f..140f,
            onValueChange = { onChangeReadingRulerHeight(it.roundToInt()) },
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
          AtlasSliderRow(
            label = strings.readingRulerOpacityLabel,
            valueText = "${(settings.readingRulerDimAlpha * 100f).roundToInt()}%",
            value = settings.readingRulerDimAlpha,
            valueRange = 0.15f..0.75f,
            onValueChange = onChangeReadingRulerDimAlpha,
            accentColor = palette.accent,
            textColor = palette.text,
            trackColor = palette.controlSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun TapZoneDropdownTile(
  zoneLabel: String,
  selectedAction: TapZoneAction,
  isFa: Boolean,
  palette: ReadingPalette,
  onSelect: (TapZoneAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = palette.controlSurface.copy(alpha = 0.72f),
      border = BorderStroke(1.dp, palette.dockBorder),
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { expanded = true },
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = zoneLabel,
          style = MaterialTheme.typography.labelSmall,
          color = palette.secondaryText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (isFa) selectedAction.faLabel else selectedAction.label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = palette.accent,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = palette.dockSurface,
    ) {
      TapZoneAction.entries.forEach { action ->
        DropdownMenuItem(
          text = {
            Text(
              text = if (isFa) action.faLabel else action.label,
              color = if (action == selectedAction) palette.accent else palette.text,
            )
          },
          trailingIcon = {
            if (action == selectedAction) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
            }
          },
          onClick = {
            expanded = false
            onSelect(action)
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexAndToolsSheet(
  initialTab: Int,
  bookFormat: BookFormat,
  totalUnits: Int,
  currentChapterOrPage: Int,
  tocItems: List<EpubTocItem>,
  bookmarks: List<BookmarkEntity>,
  highlights: List<HighlightEntity>,
  searchQuery: String,
  searchResults: List<BookSearchMatch>,
  isSearching: Boolean,
  palette: ReadingPalette,
  onSelectChapterOrPage: (Int, Float, String?) -> Unit,
  onDeleteBookmark: (Long) -> Unit,
  onDeleteHighlight: (Long) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onOpenBookInfo: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val strings = LocalAppStrings.current
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
  val tabs = listOf(strings.contentsTab, strings.bookmarksTab, strings.highlightsTab, strings.searchTab)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.dockSurface,
    contentColor = palette.text,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 340.dp, max = 580.dp)
          .padding(bottom = 16.dp)
    ) {
      if (onOpenBookInfo != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = palette.accent.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.28f)),
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable(onClick = onOpenBookInfo)
              .testTag("reader_book_info_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = palette.accent,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = strings.bookInfoMenuAction,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = palette.accent,
              modifier = Modifier.weight(1f),
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = strings.bookInfoMenuAction,
              tint = palette.accent,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = palette.accent,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = palette.accent,
          )
        },
        divider = { HorizontalDivider(color = palette.divider) },
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                color = if (selectedTab == index) palette.accent else palette.secondaryText,
              )
            },
          )
        }
      }

      when (selectedTab) {
        0 -> {
          // Table of Contents or Fixed Pages
          if (bookFormat.isReflowableText) {
            if (tocItems.isEmpty()) {
              EmptyTabMessage("No table of contents entries in this book.", palette)
            } else {
              LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                items(tocItems) { item ->
                  val isCurrent = item.chapterIndex == currentChapterOrPage
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color =
                      if (isCurrent) palette.accent.copy(alpha = 0.12f) else Color.Transparent,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectChapterOrPage(item.chapterIndex, 0f, item.anchor) },
                  ) {
                    Row(
                      modifier =
                        Modifier.padding(
                          start = (12 + item.level * 16).dp,
                          end = 12.dp,
                          top = 12.dp,
                          bottom = 12.dp,
                        ),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) palette.accent else palette.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                      )
                      Text(
                        text = "${item.chapterIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText,
                      )
                    }
                  }
                }
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              items(totalUnits) { pageIdx ->
                val isCurrent = pageIdx == currentChapterOrPage
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color =
                    if (isCurrent) palette.accent.copy(alpha = 0.12f) else Color.Transparent,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onSelectChapterOrPage(pageIdx, 0f, null) },
                ) {
                  Text(
                    text = "Page ${pageIdx + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) palette.accent else palette.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  )
                }
              }
            }
          }
        }
        1 -> {
          // Bookmarks
          if (bookmarks.isEmpty()) {
            EmptyTabMessage(strings.noBookmarks, palette)
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(bookmarks, key = { it.id }) { bm ->
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = palette.controlSurface.copy(alpha = 0.55f),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { onSelectChapterOrPage(bm.chapterOrPageIndex, bm.scrollFraction, null) },
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Bookmark,
                      contentDescription = null,
                      tint = palette.accent,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = bm.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                      )
                      if (bm.snippet.isNotBlank()) {
                        Text(
                          text = bm.snippet,
                          style = MaterialTheme.typography.bodySmall,
                          color = palette.secondaryText,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                      Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete bookmark",
                        tint = palette.secondaryText,
                      )
                    }
                  }
                }
              }
            }
          }
        }
        2 -> {
          // Highlights
          if (highlights.isEmpty()) {
            EmptyTabMessage(strings.noHighlights, palette)
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(highlights, key = { it.id }) { hl ->
                val dotColor = runCatching { Color(android.graphics.Color.parseColor(hl.colorHex)) }.getOrDefault(palette.accent)
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = palette.controlSurface.copy(alpha = 0.55f),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { onSelectChapterOrPage(hl.chapterOrPageIndex, 0f, null) },
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .padding(top = 5.dp)
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "“${hl.selectedText}”",
                        fontFamily = LiterataFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = palette.text,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (hl.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = palette.accent.copy(alpha = 0.12f),
                        ) {
                          Text(
                            text = hl.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.text,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "Section ${hl.chapterOrPageIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText,
                      )
                    }
                    IconButton(onClick = { onDeleteHighlight(hl.id) }) {
                      Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete highlight",
                        tint = palette.secondaryText,
                      )
                    }
                  }
                }
              }
            }
          }
        }
        3 -> {
          // Search inside book
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChange,
              placeholder = { Text(strings.searchInBookHint, color = palette.secondaryText) },
              leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondaryText)
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = palette.secondaryText)
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = palette.accent,
                  unfocusedBorderColor = palette.dockBorder,
                  focusedTextColor = palette.text,
                  unfocusedTextColor = palette.text,
                ),
              modifier = Modifier.fillMaxWidth().testTag("book_search_input"),
            )

            if (isSearching) {
              Spacer(modifier = Modifier.height(10.dp))
              LinearProgressIndicator(
                color = palette.accent,
                trackColor = palette.controlSurface,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (searchQuery.trim().length < 2) {
              EmptyTabMessage(strings.searchPrompt, palette)
            } else if (!isSearching && searchResults.isEmpty()) {
              EmptyTabMessage(strings.noMatches, palette)
            } else {
              LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { hit ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.controlSurface.copy(alpha = 0.55f),
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectChapterOrPage(hit.chapterOrPageIndex, 0f, null) },
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text(
                        text = hit.locationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent,
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = hit.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
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
}

@Composable
private fun LabeledSliderRow(
  label: String,
  valueLabel: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  palette: ReadingPalette,
  testTag: String,
  onValueChange: (Float) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelLarge, color = palette.secondaryText)
      Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = palette.accent)
    }
    Slider(
      value = value.coerceIn(valueRange.start, valueRange.endInclusive),
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors =
        SliderDefaults.colors(
          thumbColor = palette.accent,
          activeTrackColor = palette.accent,
          inactiveTrackColor = palette.controlSurface,
        ),
      modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
  }
}

@Composable
private fun EmptyTabMessage(message: String, palette: ReadingPalette) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.secondaryText,
    )
  }
}

private fun ReaderFontOption.toComposeFontFamily(): FontFamily =
  when (this) {
    ReaderFontOption.LITERATA -> LiterataFontFamily
    ReaderFontOption.LORA -> LoraFontFamily
    ReaderFontOption.NEWSREADER -> NewsreaderFontFamily
    ReaderFontOption.VAZIRMATN -> VazirmatnFontFamily
    ReaderFontOption.NOTO_NASKH -> NotoNaskhFontFamily
    ReaderFontOption.PLUS_JAKARTA -> PlusJakartaSansFamily
    ReaderFontOption.JETBRAINS_MONO -> JetBrainsMonoFontFamily
  }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FocusAudioSheet(
  settings: ReaderSettings,
  palette: ReadingPalette,
  readerMode: ReaderMode,
  sleepTimer: SleepTimerState,
  activeSound: AmbientSoundType,
  focusTimer: FocusTimerState,
  onSelectSleepPreset: (SleepTimerPreset) -> Unit,
  onSelectAmbientSound: (AmbientSoundType) -> Unit,
  onChangeAmbientVolume: (Float) -> Unit,
  onToggleFocusTimer: () -> Unit,
  onResetFocusTimer: () -> Unit,
  onUpdateFocusDurations: (Int, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val strings = LocalAppStrings.current
  val isFa = settings.appLanguage == AppLanguage.PERSIAN

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.dockSurface,
    contentColor = palette.text,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.focusAndAudioTitle,
          style = MaterialTheme.typography.titleMedium,
          color = palette.text,
        )
        if (sleepTimer.isFadingOut) {
          Text(
            text = strings.fadeOutActiveLabel,
            style = MaterialTheme.typography.labelSmall,
            color = palette.accent,
          )
        }
      }

      // Section 1: Sleep Timer & Gentle Fade-Out (Listen Mode)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Text(
              text = strings.sleepTimerLabel,
              style = MaterialTheme.typography.titleSmall,
              color = palette.text,
            )
          }
          if (sleepTimer.preset != SleepTimerPreset.OFF) {
            val badgeText =
              if (sleepTimer.preset == SleepTimerPreset.END_OF_CHAPTER) {
                if (isFa) sleepTimer.preset.faLabel else sleepTimer.preset.label
              } else {
                String.format("%02d:%02d", sleepTimer.remainingSeconds / 60, sleepTimer.remainingSeconds % 60)
              }
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = palette.accent,
            )
          }
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SleepTimerPreset.entries.forEach { preset ->
            val selected = sleepTimer.preset == preset
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (selected) palette.accent.copy(alpha = 0.16f)
                else palette.controlSurface.copy(alpha = 0.72f),
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectSleepPreset(preset) }
                  .testTag("sleep_preset_${preset.name.lowercase()}"),
            ) {
              Text(
                text = if (isFa) preset.faLabel else preset.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        if (readerMode == ReaderMode.LISTEN) {
          Text(
            text = strings.lockScreenControlsHint,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
      }

      HorizontalDivider(color = palette.divider)

      // Section 2: Ambient Reading Soundscapes
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
          Text(
            text = strings.ambientSoundscapesLabel,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AmbientSoundType.entries.forEach { sound ->
            val selected = activeSound == sound
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (selected) palette.accent.copy(alpha = 0.16f)
                else palette.controlSurface.copy(alpha = 0.72f),
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectAmbientSound(sound) }
                  .testTag("ambient_sound_${sound.name.lowercase()}"),
            ) {
              Text(
                text = if (isFa) sound.faLabel else sound.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        if (activeSound != AmbientSoundType.NONE) {
          LabeledSliderRow(
            label = strings.ambientVolumeLabel,
            valueLabel = "${(settings.ambientVolume * 100f).roundToInt()}%",
            value = settings.ambientVolume,
            valueRange = 0.05f..1.0f,
            palette = palette,
            testTag = "ambient_volume_slider",
            onValueChange = onChangeAmbientVolume,
          )
        }
      }

      HorizontalDivider(color = palette.divider)

      // Section 3: Reading Focus Timer (Pomodoro)
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Text(
              text = strings.focusTimerLabel,
              style = MaterialTheme.typography.titleSmall,
              color = palette.text,
            )
          }
          if (focusTimer.completedCycles > 0) {
            Text(
              text = strings.focusCyclesLabel(focusTimer.completedCycles),
              style = MaterialTheme.typography.labelSmall,
              color = palette.accent,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = palette.controlSurface.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, palette.dockBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = if (focusTimer.isBreakPhase) strings.breakPhaseLabel else strings.focusPhaseLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent,
              )
              Text(
                text = String.format("%02d:%02d", focusTimer.remainingSeconds / 60, focusTimer.remainingSeconds % 60),
                fontFamily = LiterataFontFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = onToggleFocusTimer,
                shape = RoundedCornerShape(12.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) AtlasOnAccentDark else Color.White,
                  ),
                modifier = Modifier.testTag("focus_timer_toggle_button"),
              ) {
                Icon(
                  imageVector = if (focusTimer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (focusTimer.isRunning) strings.focusPauseAction else strings.focusStartAction)
              }
              OutlinedButton(
                onClick = onResetFocusTimer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.dockBorder),
                modifier = Modifier.testTag("focus_timer_reset_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = strings.focusResetAction,
                  tint = palette.text,
                  modifier = Modifier.size(17.dp),
                )
              }
            }
          }
        }

        // Preset Work/Break ratios
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(15 to 3, 25 to 5, 45 to 10, 60 to 15).forEach { (w, b) ->
            val selected = focusTimer.workMinutes == w && focusTimer.breakMinutes == b
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (selected) palette.accent.copy(alpha = 0.16f)
                else palette.controlSurface.copy(alpha = 0.65f),
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onUpdateFocusDurations(w, b) },
            ) {
              Text(
                text = "${w}/${b}m",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(vertical = 8.dp).wrapContentWidth(),
              )
            }
          }
        }
      }
    }
  }
}
