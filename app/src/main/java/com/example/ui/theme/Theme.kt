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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val CreamGreenColorScheme = lightColorScheme(
  primary = NestGreen,
  onPrimary = Color.White,
  primaryContainer = NestGreenContainerLight,
  onPrimaryContainer = Color(0xFF18422F),
  secondary = CreamAppOnMuted,
  onSecondary = Color.White,
  secondaryContainer = CreamAppSurfaceElevated,
  onSecondaryContainer = CreamAppOnBg,
  background = CreamAppBg,
  onBackground = CreamAppOnBg,
  surface = CreamAppSurface,
  onSurface = CreamAppOnBg,
  surfaceVariant = CreamAppSurfaceElevated,
  onSurfaceVariant = CreamAppOnMuted,
  outline = CreamAppOutline,
  outlineVariant = Color(0xFFE7DFD0),
)

private val SepiaGreenColorScheme = lightColorScheme(
  primary = Color(0xFF2A6248),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD6E7DC),
  onPrimaryContainer = Color(0xFF173D2B),
  secondary = Color(0xFF746350),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE4D6B8),
  onSecondaryContainer = Color(0xFF382C1E),
  background = Color(0xFFF2E8D2),
  onBackground = Color(0xFF382C1E),
  surface = Color(0xFFFAF2DF),
  onSurface = Color(0xFF382C1E),
  surfaceVariant = Color(0xFFE6D8BA),
  onSurfaceVariant = Color(0xFF746350),
  outline = Color(0xFFCBBBA0),
  outlineVariant = Color(0xFFDDD0B6),
)

private val SageMistColorScheme = lightColorScheme(
  primary = NestGreen,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD0E6D5),
  onPrimaryContainer = Color(0xFF153B28),
  secondary = Color(0xFF536558),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD8E4D9),
  onSecondaryContainer = Color(0xFF17231B),
  background = Color(0xFFEAF0EA),
  onBackground = Color(0xFF17231B),
  surface = Color(0xFFF3F7F3),
  onSurface = Color(0xFF17231B),
  surfaceVariant = Color(0xFFDBE6DC),
  onSurfaceVariant = Color(0xFF536558),
  outline = Color(0xFFB5C9B8),
  outlineVariant = Color(0xFFC9D9CB),
)

private val DarkCharcoalGreenColorScheme = darkColorScheme(
  primary = NestGreenDarkAccent,
  onPrimary = Color(0xFF0A2114),
  primaryContainer = NestGreenContainerDark,
  onPrimaryContainer = Color(0xFFA8E6C3),
  secondary = DarkAppOnMuted,
  onSecondary = DarkAppBg,
  secondaryContainer = DarkAppSurfaceElevated,
  onSecondaryContainer = DarkAppOnBg,
  background = DarkAppBg,
  onBackground = DarkAppOnBg,
  surface = DarkAppSurface,
  onSurface = DarkAppOnBg,
  surfaceVariant = DarkAppSurfaceElevated,
  onSurfaceVariant = DarkAppOnMuted,
  outline = DarkAppOutline,
  outlineVariant = Color(0xFF252A25),
)

private val AmoledGreenColorScheme = darkColorScheme(
  primary = NestGreenDarkAccent,
  onPrimary = Color(0xFF0A2114),
  primaryContainer = Color(0xFF152D20),
  onPrimaryContainer = Color(0xFFA8E6C3),
  secondary = Color(0xFF949B94),
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF171C18),
  onSecondaryContainer = Color(0xFFE5E2DA),
  background = Color(0xFF000000),
  onBackground = Color(0xFFE5E2DA),
  surface = Color(0xFF0E100E),
  onSurface = Color(0xFFE5E2DA),
  surfaceVariant = Color(0xFF181C18),
  onSurfaceVariant = Color(0xFF949B94),
  outline = Color(0xFF242B25),
  outlineVariant = Color(0xFF1B201C),
)

private val NightBwColorScheme = darkColorScheme(
  primary = Color.White,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF242424),
  onPrimaryContainer = Color.White,
  secondary = Color(0xFFB3B3B3),
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF1B1B1B),
  onSecondaryContainer = Color.White,
  background = Color(0xFF000000),
  onBackground = Color.White,
  surface = Color(0xFF0C0C0C),
  onSurface = Color.White,
  surfaceVariant = Color(0xFF1A1A1A),
  onSurfaceVariant = Color(0xFFB3B3B3),
  outline = Color(0xFF3A3A3A),
  outlineVariant = Color(0xFF262626),
)

private val PageNestShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PageNestTheme(
  themeMode: ReadingThemeMode = ReadingThemeMode.PAPER,
  appLanguage: AppLanguage = AppLanguage.ENGLISH,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when (themeMode) {
      ReadingThemeMode.PAPER -> CreamGreenColorScheme
      ReadingThemeMode.SEPIA -> SepiaGreenColorScheme
      ReadingThemeMode.SAGE_MIST -> SageMistColorScheme
      ReadingThemeMode.DARK -> DarkCharcoalGreenColorScheme
      ReadingThemeMode.AMOLED -> AmoledGreenColorScheme
      ReadingThemeMode.NIGHT_BW -> NightBwColorScheme
    }

  val layoutDir = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
  val strings = appLanguage.strings()
  val typography = buildPageNestTypography(appLanguage == AppLanguage.PERSIAN)

  CompositionLocalProvider(
    LocalLayoutDirection provides layoutDir,
    LocalAppStrings provides strings,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = PageNestShapes,
      content = content,
    )
  }
}

/**
 * Specular Liquid-Glass container — translucent theme-tinted glass with crisp light-catching rim
 * and clean clarity (never heavy matte frost). Reserved strictly for the Reader screen.
 */
@Composable
fun LiquidGlassCard(
  hazeState: HazeState?,
  isDark: Boolean,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  tintColor: Color = if (isDark) Color(0xFF171A18).copy(alpha = 0.64f) else Color(0xFFFFFCF5).copy(alpha = 0.66f),
  batterySaverEnabled: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val rimBrush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
      colors =
        if (isDark) {
          listOf(
            Color.White.copy(alpha = 0.34f),
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.08f),
          )
        } else {
          listOf(
            Color.White.copy(alpha = 0.88f),
            Color.White.copy(alpha = 0.42f),
            Color.Black.copy(alpha = 0.09f),
          )
        }
    )
  val specularSheen =
    androidx.compose.ui.graphics.Brush.verticalGradient(
      colors =
        if (isDark) {
          listOf(
            Color.White.copy(alpha = 0.13f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.10f),
          )
        } else {
          listOf(
            Color.White.copy(alpha = 0.42f),
            Color.White.copy(alpha = 0.06f),
            Color.Black.copy(alpha = 0.03f),
          )
        }
    )

  Box(
    modifier =
      modifier
        .clip(shape)
        .then(
          if (hazeState != null && !batterySaverEnabled) {
            Modifier.hazeEffect(
              state = hazeState,
              style =
                HazeStyle(
                  backgroundColor = tintColor,
                  tint = HazeTint(tintColor),
                  blurRadius = 8.dp,
                ),
            )
          } else {
            Modifier.background(if (batterySaverEnabled) tintColor.copy(alpha = 0.94f) else tintColor)
          }
        )
        .then(if (batterySaverEnabled) Modifier else Modifier.background(specularSheen))
        .border(BorderStroke(0.95.dp, rimBrush), shape),
    content = content,
  )
}
