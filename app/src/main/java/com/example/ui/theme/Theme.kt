package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ReadingThemeMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.strings
import com.example.ui.swiss.SwissAtlasTheme
import dev.chrisbanes.haze.HazeState

private val AtlasMonochromeLightScheme = lightColorScheme(
  primary = AtlasLightOnBg,
  onPrimary = Color.White,
  primaryContainer = AtlasLightSurfaceElevated,
  onPrimaryContainer = AtlasLightOnBg,
  secondary = AtlasLightOnMuted,
  onSecondary = Color.White,
  secondaryContainer = AtlasLightSurfaceElevated,
  onSecondaryContainer = AtlasLightOnBg,
  background = AtlasLightBg,
  onBackground = AtlasLightOnBg,
  surface = AtlasLightSurface,
  onSurface = AtlasLightOnBg,
  surfaceVariant = AtlasLightSurfaceElevated,
  onSurfaceVariant = AtlasLightOnMuted,
  outline = Color(0xFFD2D2CD),
  outlineVariant = AtlasLightOutline,
)

private val AtlasMonochromeDarkScheme = darkColorScheme(
  primary = AtlasDarkOnBg,
  onPrimary = AtlasDarkBg,
  primaryContainer = AtlasDarkSurfaceElevated,
  onPrimaryContainer = AtlasDarkOnBg,
  secondary = AtlasDarkOnMuted,
  onSecondary = AtlasDarkBg,
  secondaryContainer = AtlasDarkSurfaceElevated,
  onSecondaryContainer = AtlasDarkOnBg,
  background = AtlasDarkBg,
  onBackground = AtlasDarkOnBg,
  surface = AtlasDarkSurface,
  onSurface = AtlasDarkOnBg,
  surfaceVariant = AtlasDarkSurfaceElevated,
  onSurfaceVariant = AtlasDarkOnMuted,
  outline = Color(0xFF424242),
  outlineVariant = AtlasDarkOutline,
)

private val AtlasShapes = Shapes(
  extraSmall = RoundedCornerShape(2.dp),
  small = RoundedCornerShape(4.dp),
  medium = RoundedCornerShape(6.dp),
  large = RoundedCornerShape(8.dp),
  extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun PageNestTheme(
  themeMode: ReadingThemeMode = ReadingThemeMode.PAPER,
  appLanguage: AppLanguage = AppLanguage.ENGLISH,
  content: @Composable () -> Unit,
) {
  SwissAtlasTheme(
    isDark = themeMode.isDark,
    appLanguage = appLanguage,
    content = content,
  )
}

/**
 * Swiss Editorial Monochrome floating container with crisp hairline architectural border.
 * Keeps signature compatibility with reader chrome while eliminating heavy gradients and blue glass.
 */
@Composable
fun LiquidGlassCard(
  hazeState: HazeState?,
  isDark: Boolean,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(8.dp),
  tintColor: Color = if (isDark) Color(0xFF1B1B1B) else Color(0xFFFFFFFF),
  batterySaverEnabled: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val borderColor = if (isDark) Color(0xFF303030) else Color(0xFFE6E6E2)
  val flatSurface = tintColor.copy(alpha = if (batterySaverEnabled) 0.98f else 0.95f)

  Box(
    modifier =
      modifier
        .clip(shape)
        .background(flatSurface)
        .border(BorderStroke(1.dp, borderColor), shape),
    content = content,
  )
}
