package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.ReadingThemeMode

// Primary Brand & Accent: ATLAS Swiss Editorial Monochrome
val AtlasBlue = Color(0xFF111111)
val AtlasBlueDark = Color(0xFFF5F5F2)
val AtlasBlueContainerLight = Color(red = 0xEE, green = 0xEE, blue = 0xEB)
val AtlasBlueContainerDark = Color(0xFF242424)
val AtlasOnAccentDark = Color(0xFF111111)

// Legacy aliases mapped directly to ATLAS Monochrome tokens so every module stays unified
val NestGreen = AtlasBlue
val NestGreenDarkAccent = AtlasBlueDark
val NestGreenContainerLight = AtlasBlueContainerLight
val NestGreenContainerDark = AtlasBlueContainerDark

// App Chrome — Warm Off-White (Light) & Charcoal Noir (Dark)
val AtlasLightBg = Color(0xFFF5F5F2)
val AtlasLightSurface = Color(0xFFFFFFFF)
val AtlasLightSurfaceElevated = Color(red = 0xEE, green = 0xEE, blue = 0xEB)
val AtlasLightOnBg = Color(0xFF111111)
val AtlasLightOnMuted = Color(0xFF777777)
val AtlasLightOutline = Color(0xFFE6E6E2)

val AtlasDarkBg = Color(0xFF111111)
val AtlasDarkSurface = Color(0xFF1B1B1B)
val AtlasDarkSurfaceElevated = Color(0xFF242424)
val AtlasDarkOnBg = Color(0xFFF5F5F2)
val AtlasDarkOnMuted = Color(0xFF999999)
val AtlasDarkOutline = Color(0xFF303030)

// Backward-compatible chrome aliases
val CreamAppBg = AtlasLightBg
val CreamAppSurface = AtlasLightSurface
val CreamAppSurfaceElevated = AtlasLightSurfaceElevated
val CreamAppOnBg = AtlasLightOnBg
val CreamAppOnMuted = AtlasLightOnMuted
val CreamAppOutline = AtlasLightOutline

val DarkAppBg = AtlasDarkBg
val DarkAppSurface = AtlasDarkSurface
val DarkAppSurfaceElevated = AtlasDarkSurfaceElevated
val DarkAppOnBg = AtlasDarkOnBg
val DarkAppOnMuted = AtlasDarkOnMuted
val DarkAppOutline = AtlasDarkOutline

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
      background = Color(0xFFF5F5F2),
      backgroundHex = "#F5F5F2",
      text = Color(0xFF111111),
      textHex = "#111111",
      secondaryText = Color(0xFF777777),
      accent = Color(0xFF111111),
      accentHex = "#111111",
      dockSurface = Color(0xFFFFFFFF),
      dockBorder = Color(0xFFE6E6E2),
      controlSurface = Color(red = 0xEE, green = 0xEE, blue = 0xEB),
      divider = Color(0xFFE6E6E2),
      isDark = false,
    )
    ReadingThemeMode.SEPIA -> ReadingPalette(
      mode = this,
      background = Color(0xFFF4ECDF),
      backgroundHex = "#F4ECDF",
      text = Color(0xFF2B251E),
      textHex = "#2B251E",
      secondaryText = Color(0xFF6F6559),
      accent = Color(0xFF2B251E),
      accentHex = "#2B251E",
      dockSurface = Color(0xFFFAF4EA),
      dockBorder = Color(0xFFE0D5C3),
      controlSurface = Color(0xFFECE2D1),
      divider = Color(0xFFE0D5C3),
      isDark = false,
    )
    ReadingThemeMode.SAGE_MIST -> ReadingPalette(
      mode = this,
      background = Color(red = 0xEE, green = 0xEE, blue = 0xEB),
      backgroundHex = "#EEEEEB",
      text = Color(0xFF111111),
      textHex = "#111111",
      secondaryText = Color(0xFF666666),
      accent = Color(0xFF111111),
      accentHex = "#111111",
      dockSurface = Color(0xFFF8F8F5),
      dockBorder = Color(0xFFDFDFDA),
      controlSurface = Color(0xFFE4E4E0),
      divider = Color(0xFFDFDFDA),
      isDark = false,
    )
    ReadingThemeMode.DARK -> ReadingPalette(
      mode = this,
      background = Color(0xFF111111),
      backgroundHex = "#111111",
      text = Color(0xFFF5F5F2),
      textHex = "#F5F5F2",
      secondaryText = Color(0xFF999999),
      accent = Color(0xFFF5F5F2),
      accentHex = "#F5F5F2",
      dockSurface = Color(0xFF1B1B1B),
      dockBorder = Color(0xFF303030),
      controlSurface = Color(0xFF242424),
      divider = Color(0xFF303030),
      isDark = true,
    )
    ReadingThemeMode.AMOLED -> ReadingPalette(
      mode = this,
      background = Color(0xFF000000),
      backgroundHex = "#000000",
      text = Color(0xFFF5F5F2),
      textHex = "#F5F5F2",
      secondaryText = Color(0xFF999999),
      accent = Color(0xFFF5F5F2),
      accentHex = "#F5F5F2",
      dockSurface = Color(0xFF111111),
      dockBorder = Color(0xFF2A2A2A),
      controlSurface = Color(0xFF1B1B1B),
      divider = Color(0xFF262626),
      isDark = true,
    )
    ReadingThemeMode.NIGHT_BW -> ReadingPalette(
      mode = this,
      background = Color(0xFF000000),
      backgroundHex = "#000000",
      text = Color(0xFFFFFFFF),
      textHex = "#FFFFFF",
      secondaryText = Color(0xFFA3A3A3),
      accent = Color(0xFFFFFFFF),
      accentHex = "#FFFFFF",
      dockSurface = Color(0xFF111111),
      dockBorder = Color(0xFF303030),
      controlSurface = Color(0xFF1B1B1B),
      divider = Color(0xFF2B2B2B),
      isDark = true,
    )
  }
