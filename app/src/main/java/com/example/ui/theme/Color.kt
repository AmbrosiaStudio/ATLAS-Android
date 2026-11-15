package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.ReadingThemeMode

// Brand & Accent: Botanical Forest Green & Cream (no purple)
val NestGreen = Color(0xFF2D6A4F)
val NestGreenDarkAccent = Color(0xFF5CC98B)
val NestGreenContainerLight = Color(0xFFDDEEE4)
val NestGreenContainerDark = Color(0xFF1A3628)

// App Chrome — Warm Cream (Light) & #121212 Charcoal (Dark)
val CreamAppBg = Color(0xFFF7F2E7)
val CreamAppSurface = Color(0xFFFFFCF5)
val CreamAppSurfaceElevated = Color(0xFFEFE7D6)
val CreamAppOnBg = Color(0xFF1B201C)
val CreamAppOnMuted = Color(0xFF5D655E)
val CreamAppOutline = Color(0xFFDCD3C1)

val DarkAppBg = Color(0xFF121212)
val DarkAppSurface = Color(0xFF1B1D1B)
val DarkAppSurfaceElevated = Color(0xFF242824)
val DarkAppOnBg = Color(0xFFEDEAE2)
val DarkAppOnMuted = Color(0xFF9EA69F)
val DarkAppOutline = Color(0xFF2D332E)

data class ReadingPalette(
  val mode: ReadingThemeMode,
  val background: Color,
  val backgroundHex: String,
  val text: Color,
  val textHex: String,
  val secondaryText: Color,
  val accent: Color,
  val accentHex: String,
  val dockSurface: Color,
  val dockBorder: Color,
  val controlSurface: Color,
  val divider: Color,
  val isDark: Boolean,
)

fun ReadingThemeMode.toPalette(): ReadingPalette =
  when (this) {
    ReadingThemeMode.PAPER -> ReadingPalette(
      mode = this,
      background = Color(0xFFF8F3E8),
      backgroundHex = "#F8F3E8",
      text = Color(0xFF1C201C),
      textHex = "#1C201C",
      secondaryText = Color(0xFF5F665F),
      accent = NestGreen,
      accentHex = "#2D6A4F",
      dockSurface = Color(0xFFF3ECE0),
      dockBorder = Color(0xFFE0D7C5),
      controlSurface = Color(0xFFE9E1D1),
      divider = Color(0xFFE3DAC9),
      isDark = false,
    )
    ReadingThemeMode.SEPIA -> ReadingPalette(
      mode = this,
      background = Color(0xFFF2E8D2),
      backgroundHex = "#F2E8D2",
      text = Color(0xFF382C1E),
      textHex = "#382C1E",
      secondaryText = Color(0xFF746350),
      accent = Color(0xFF2A6248),
      accentHex = "#2A6248",
      dockSurface = Color(0xFFEADDC2),
      dockBorder = Color(0xFFD8C9A8),
      controlSurface = Color(0xFFE1D2B1),
      divider = Color(0xFFDCCEA9),
      isDark = false,
    )
    ReadingThemeMode.SAGE_MIST -> ReadingPalette(
      mode = this,
      background = Color(0xFFEAF0EA),
      backgroundHex = "#EAF0EA",
      text = Color(0xFF17231B),
      textHex = "#17231B",
      secondaryText = Color(0xFF536558),
      accent = NestGreen,
      accentHex = "#2D6A4F",
      dockSurface = Color(0xFFE1EAE1),
      dockBorder = Color(0xFFC5D5C7),
      controlSurface = Color(0xFFD6E2D7),
      divider = Color(0xFFCEE0D0),
      isDark = false,
    )
    ReadingThemeMode.DARK -> ReadingPalette(
      mode = this,
      background = Color(0xFF121212),
      backgroundHex = "#121212",
      text = Color(0xFFECE9E1),
      textHex = "#ECE9E1",
      secondaryText = Color(0xFFA0A8A1),
      accent = NestGreenDarkAccent,
      accentHex = "#5CC98B",
      dockSurface = Color(0xFF1C1E1C),
      dockBorder = Color(0xFF2D332E),
      controlSurface = Color(0xFF252925),
      divider = Color(0xFF2A2E2A),
      isDark = true,
    )
    ReadingThemeMode.AMOLED -> ReadingPalette(
      mode = this,
      background = Color(0xFF000000),
      backgroundHex = "#000000",
      text = Color(0xFFE5E2DA),
      textHex = "#E5E2DA",
      secondaryText = Color(0xFF949B94),
      accent = NestGreenDarkAccent,
      accentHex = "#5CC98B",
      dockSurface = Color(0xFF101311),
      dockBorder = Color(0xFF242B25),
      controlSurface = Color(0xFF181D19),
      divider = Color(0xFF1E241F),
      isDark = true,
    )
    ReadingThemeMode.NIGHT_BW -> ReadingPalette(
      mode = this,
      background = Color(0xFF000000),
      backgroundHex = "#000000",
      text = Color(0xFFFFFFFF),
      textHex = "#FFFFFF",
      secondaryText = Color(0xFFB3B3B3),
      accent = Color(0xFFFFFFFF),
      accentHex = "#FFFFFF",
      dockSurface = Color(0xFF0E0E0E),
      dockBorder = Color(0xFF343434),
      controlSurface = Color(0xFF1B1B1B),
      divider = Color(0xFF262626),
      isDark = true,
    )
  }
