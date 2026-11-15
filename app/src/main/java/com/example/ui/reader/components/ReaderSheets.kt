package com.example.ui.reader.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.HighlightEntity
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.BookSearchMatch
import com.example.model.ComfortPreset
import com.example.model.CustomFontEntry
import com.example.model.EpubTocItem
import com.example.model.ReaderFontOption
import com.example.model.ReaderSettings
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TextAlignOption
import com.example.ui.i18n.LocalAppStrings
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
  onSelectTheme: (ReadingThemeMode) -> Unit,
  onChangeBrightness: (Float) -> Unit,
  onTogglePaperTexture: (Boolean) -> Unit,
  onToggleBatterySaver: (Boolean) -> Unit,
  onTogglePdfThemeFilter: (Boolean) -> Unit,
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
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = strings.readingTheme,
        style = MaterialTheme.typography.titleMedium,
        color = palette.text,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReadingThemeMode.entries.chunked(3).forEach { rowModes ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            rowModes.forEach { mode ->
              val modePalette = mode.toPalette()
              val selected = settings.themeMode == mode
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = modePalette.background,
                border =
                  BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else modePalette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectTheme(mode) }
                    .testTag("theme_swatch_${mode.name.lowercase()}"),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = "Aa",
                    fontFamily = LiterataFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = modePalette.text,
                  )
                  Text(
                    text = if (isFa) mode.faLabel else mode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = modePalette.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.brightness,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          val isAuto = settings.brightnessOverride < 0f
          FilterChip(
            selected = isAuto,
            onClick = {
              onChangeBrightness(if (isAuto) 0.65f else -1f)
            },
            label = {
              Text(
                text = if (isAuto) strings.systemAuto else strings.manual,
                color = if (isAuto) palette.accent else palette.secondaryText,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.accent.copy(alpha = 0.16f),
                selectedLabelColor = palette.accent,
                labelColor = palette.secondaryText,
              ),
          )
        }

        val displayValue =
          if (settings.brightnessOverride < 0f) 0.65f else settings.brightnessOverride
        Slider(
          value = displayValue,
          onValueChange = { onChangeBrightness(it.coerceIn(0.05f, 1f)) },
          valueRange = 0.05f..1.0f,
          colors =
            SliderDefaults.colors(
              thumbColor = palette.accent,
              activeTrackColor = palette.accent,
              inactiveTrackColor = palette.controlSurface,
            ),
          modifier = Modifier.fillMaxWidth().testTag("brightness_slider"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = strings.paperStyleToggle,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Text(
            text = strings.paperStyleSub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
        Switch(
          checked = settings.paperTextureEnabled,
          onCheckedChange = onTogglePaperTexture,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
          modifier = Modifier.testTag("theme_paper_texture_switch"),
        )
      }

      // Battery-Saver Mode toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = strings.batterySaverToggle,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Text(
            text = strings.batterySaverSub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
        Switch(
          checked = settings.batterySaverModeEnabled,
          onCheckedChange = onToggleBatterySaver,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
          modifier = Modifier.testTag("theme_battery_saver_switch"),
        )
      }

      if (bookFormat == BookFormat.PDF) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = strings.adaptPdfColors,
              style = MaterialTheme.typography.titleSmall,
              color = palette.text,
            )
            Text(
              text = strings.adaptPdfColorsSub,
              style = MaterialTheme.typography.bodySmall,
              color = palette.secondaryText,
            )
          }
          Switch(
            checked = settings.pdfThemeFilterEnabled,
            onCheckedChange = onTogglePdfThemeFilter,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
                checkedTrackColor = palette.accent,
              ),
          )
        }
      }
    }
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
  onApplyComfortPreset: (ComfortPreset) -> Unit,
  onResetComfort: () -> Unit,
  onOpenSettings: () -> Unit,
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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (bookFormat == BookFormat.EPUB) strings.typographyAndLayout else strings.pageDisplay,
          style = MaterialTheme.typography.titleMedium,
          color = palette.text,
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = palette.controlSurface.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, palette.dockBorder),
          modifier =
            Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onResetComfort)
              .testTag("sheet_reset_comfort"),
        ) {
          Text(
            text = strings.resetDefaults,
            style = MaterialTheme.typography.labelSmall,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      // Quick Comfort Presets inside TT sheet
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = strings.comfortPresets,
          style = MaterialTheme.typography.labelLarge,
          color = palette.secondaryText,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ComfortPreset.entries.forEach { preset ->
            Surface(
              shape = CircleShape,
              color = palette.controlSurface.copy(alpha = 0.72f),
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
                color = palette.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              )
            }
          }
        }
      }

      // Page Flow selector (Book Pages 1-by-1 vs Continuous Vertical Scroll)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = strings.readingModeSection,
          style = MaterialTheme.typography.labelLarge,
          color = palette.secondaryText,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ReadingLayoutMode.entries.forEach { layoutMode ->
            val selected = settings.readingLayoutMode == layoutMode
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (selected) palette.accent.copy(alpha = 0.16f)
                else palette.controlSurface.copy(alpha = 0.72f),
              border =
                BorderStroke(
                  width = 1.dp,
                  color = if (selected) palette.accent else palette.dockBorder,
                ),
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectLayoutMode(layoutMode) }
                  .testTag("reader_layout_${layoutMode.name.lowercase()}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector =
                    if (layoutMode == ReadingLayoutMode.PAGED) Icons.AutoMirrored.Rounded.MenuBook
                    else Icons.Rounded.SwapVert,
                  contentDescription = null,
                  tint = if (selected) palette.accent else palette.text,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isFa) layoutMode.faLabel else layoutMode.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (selected) palette.accent else palette.text,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      if (bookFormat == BookFormat.PDF) {
        Text(
          text = "Pinch with two fingers or double-tap any page to zoom in and pan smoothly.",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.secondaryText,
        )
        LabeledSliderRow(
          label = strings.margins,
          valueLabel = "${settings.horizontalMarginDp} dp",
          value = settings.horizontalMarginDp.toFloat(),
          valueRange = 8f..56f,
          palette = palette,
          testTag = "margin_slider",
          onValueChange = { onChangeMargin(it.roundToInt()) },
        )
        LabeledSliderRow(
          label = strings.verticalMargins,
          valueLabel = "${settings.verticalMarginDp} dp",
          value = settings.verticalMarginDp.toFloat(),
          valueRange = 8f..60f,
          palette = palette,
          testTag = "vertical_margin_slider",
          onValueChange = { onChangeVerticalMargin(it.roundToInt()) },
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.adaptPdfColors,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Switch(
            checked = settings.pdfThemeFilterEnabled,
            onCheckedChange = onTogglePdfThemeFilter,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
                checkedTrackColor = palette.accent,
              ),
          )
        }
      } else {
        // Font Family chips (built-in + custom imported)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.typeface,
              style = MaterialTheme.typography.labelLarge,
              color = palette.secondaryText,
            )
            Text(
              text = strings.manageFontsTts,
              style = MaterialTheme.typography.labelMedium,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ReaderFontOption.entries.forEach { option ->
              val selected = settings.customFontId == null && settings.fontOption == option
              Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                  if (selected) palette.accent.copy(alpha = 0.16f)
                  else palette.controlSurface.copy(alpha = 0.7f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (selected) palette.accent else palette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectFont(option) }
                    .testTag("font_option_${option.name.lowercase()}"),
              ) {
                Text(
                  text = option.displayName,
                  fontFamily = option.toComposeFontFamily(),
                  fontSize = 14.sp,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (selected) palette.accent else palette.text,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
              }
            }
            customFonts.forEach { cf ->
              val selected = settings.customFontId == cf.id
              Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                  if (selected) palette.accent.copy(alpha = 0.16f)
                  else palette.controlSurface.copy(alpha = 0.7f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (selected) palette.accent else palette.dockBorder,
                  ),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectCustomFont(cf.id) }
                    .testTag("custom_font_${cf.id}"),
              ) {
                Text(
                  text = cf.displayName,
                  fontSize = 14.sp,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (selected) palette.accent else palette.text,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
              }
            }
          }
        }

        // Font Size
        LabeledSliderRow(
          label = strings.textSize,
          valueLabel = "${settings.fontSizeSp} sp",
          value = settings.fontSizeSp.toFloat(),
          valueRange = 12f..32f,
          palette = palette,
          testTag = "font_size_slider",
          onValueChange = { onChangeFontSize(it.roundToInt()) },
        )

        // Line Height
        LabeledSliderRow(
          label = strings.lineSpacing,
          valueLabel = String.format("%.2f×", settings.lineHeightMultiplier),
          value = settings.lineHeightMultiplier,
          valueRange = 1.15f..2.4f,
          palette = palette,
          testTag = "line_height_slider",
          onValueChange = onChangeLineHeight,
        )

        // Side Margin Borders
        LabeledSliderRow(
          label = strings.margins,
          valueLabel = "${settings.horizontalMarginDp} dp",
          value = settings.horizontalMarginDp.toFloat(),
          valueRange = 8f..56f,
          palette = palette,
          testTag = "margin_slider",
          onValueChange = { onChangeMargin(it.roundToInt()) },
        )

        // Top & Bottom Margin Borders
        LabeledSliderRow(
          label = strings.verticalMargins,
          valueLabel = "${settings.verticalMarginDp} dp",
          value = settings.verticalMarginDp.toFloat(),
          valueRange = 8f..60f,
          palette = palette,
          testTag = "vertical_margin_slider",
          onValueChange = { onChangeVerticalMargin(it.roundToInt()) },
        )

        // Paragraph Spacing
        LabeledSliderRow(
          label = strings.paragraphSpacing,
          valueLabel = String.format("%.2f em", settings.paragraphSpacingEm),
          value = settings.paragraphSpacingEm,
          valueRange = 0.4f..2.0f,
          palette = palette,
          testTag = "paragraph_spacing_slider",
          onValueChange = onChangeParagraphSpacing,
        )

        // Letter Spacing
        LabeledSliderRow(
          label = strings.letterSpacing,
          valueLabel = String.format("%+.2f em", settings.letterSpacingEm),
          value = settings.letterSpacingEm,
          valueRange = -0.02f..0.12f,
          palette = palette,
          testTag = "letter_spacing_slider",
          onValueChange = onChangeLetterSpacing,
        )

        // Text Weight
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.fontWeightLabel,
            style = MaterialTheme.typography.labelLarge,
            color = palette.secondaryText,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              400 to strings.weightRegular,
              500 to strings.weightMedium,
              600 to strings.weightBold,
            ).forEach { (weight, lbl) ->
              val selected = settings.fontWeightValue == weight
              Surface(
                shape = RoundedCornerShape(12.dp),
                color =
                  if (selected) palette.accent.copy(alpha = 0.16f)
                  else palette.controlSurface.copy(alpha = 0.7f),
                border =
                  BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChangeFontWeight(weight) }
                    .testTag("weight_$weight"),
              ) {
                Text(
                  text = lbl,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) palette.accent else palette.text,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }
          }
        }

        // Alignment
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.alignment,
            style = MaterialTheme.typography.labelLarge,
            color = palette.secondaryText,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextAlignOption.entries.forEach { align ->
              val selected = settings.textAlign == align
              val alignLabel = if (isFa) align.faLabel else align.label
              Surface(
                shape = RoundedCornerShape(12.dp),
                color =
                  if (selected) palette.accent.copy(alpha = 0.16f)
                  else palette.controlSurface.copy(alpha = 0.7f),
                border =
                  BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectAlign(align) }
                    .testTag("align_${align.name.lowercase()}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector =
                      if (align == TextAlignOption.LEFT) Icons.AutoMirrored.Rounded.FormatAlignLeft
                      else Icons.Rounded.FormatAlignJustify,
                    contentDescription = alignLabel,
                    tint = if (selected) palette.accent else palette.text,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = alignLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) palette.accent else palette.text,
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = palette.divider)

      // Eye-Comfort Warmth
      LabeledSliderRow(
        label = strings.eyeComfortWarmth,
        valueLabel = "${(settings.eyeComfortWarmth * 100f).roundToInt()}%",
        value = settings.eyeComfortWarmth,
        valueRange = 0f..1f,
        palette = palette,
        testTag = "eye_warmth_slider",
        onValueChange = onChangeEyeComfortWarmth,
      )

      // Page Frame Border toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.pageBorderToggle,
          style = MaterialTheme.typography.titleSmall,
          color = palette.text,
        )
        Switch(
          checked = settings.showPageBorder,
          onCheckedChange = onTogglePageBorder,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
        )
      }

      // Paper Texture toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = strings.paperStyleToggle,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Text(
            text = strings.paperStyleSub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
        Switch(
          checked = settings.paperTextureEnabled,
          onCheckedChange = onTogglePaperTexture,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
          modifier = Modifier.testTag("sheet_paper_texture_switch"),
        )
      }

      // Keep Screen Awake toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.keepScreenAwake,
          style = MaterialTheme.typography.titleSmall,
          color = palette.text,
        )
        Switch(
          checked = settings.keepScreenOn,
          onCheckedChange = onToggleKeepScreenOn,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
        )
      }

      // Volume Key Page Turn toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = strings.volumeKeyPageTurnToggle,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Text(
            text = strings.volumeKeyPageTurnSub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
        Switch(
          checked = settings.volumeKeyPageTurnEnabled,
          onCheckedChange = onToggleVolumeKeyPageTurn,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
          modifier = Modifier.testTag("sheet_volume_key_switch"),
        )
      }

      // Battery-Saver Mode toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = strings.batterySaverToggle,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
          )
          Text(
            text = strings.batterySaverSub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
          )
        }
        Switch(
          checked = settings.batterySaverModeEnabled,
          onCheckedChange = onToggleBatterySaver,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = if (palette.isDark) Color(0xFF082115) else Color.White,
              checkedTrackColor = palette.accent,
            ),
          modifier = Modifier.testTag("sheet_battery_saver_switch"),
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
          .heightIn(min = 340.dp, max = 560.dp)
          .padding(bottom = 16.dp)
    ) {
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
          // Table of Contents or PDF Pages
          if (bookFormat == BookFormat.EPUB) {
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
