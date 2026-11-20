package com.example.ui.swiss

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.strings
import com.example.ui.theme.VazirmatnFontFamily

/**
 * Self-contained Swiss Editorial Monochrome Design System for ATLAS.
 *
 * Light palette:
 * - Background: #F5F5F2
 * - Surface:    #FFFFFF
 * - Primary:    #111111
 * - Secondary:  #777777
 * - Divider:    #E6E6E2
 * - Muted:      #EEEEEB
 *
 * Dark palette:
 * - Background: #111111
 * - Surface:    #1B1B1B
 * - Primary:    #F5F5F2
 * - Secondary:  #999999
 * - Divider:    #303030
 * - Muted:      #242424
 */
object SwissPalette {
  val LightBackground = Color(0xFFF5F5F2)
  val LightSurface = Color(0xFFFFFFFF)
  val LightPrimary = Color(0xFF111111)
  val LightSecondary = Color(0xFF777777)
  val LightDivider = Color(0xFFE6E6E2)
  val LightMutedSurface = Color(red = 0xEE, green = 0xEE, blue = 0xEB)

  val DarkBackground = Color(0xFF111111)
  val DarkSurface = Color(0xFF1B1B1B)
  val DarkPrimary = Color(0xFFF5F5F2)
  val DarkSecondary = Color(0xFF999999)
  val DarkDivider = Color(0xFF303030)
  val DarkMutedSurface = Color(0xFF242424)
}

val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))

private val SwissLightColorScheme =
  lightColorScheme(
    primary = SwissPalette.LightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SwissPalette.LightMutedSurface,
    onPrimaryContainer = SwissPalette.LightPrimary,
    secondary = SwissPalette.LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SwissPalette.LightMutedSurface,
    onSecondaryContainer = SwissPalette.LightPrimary,
    background = SwissPalette.LightBackground,
    onBackground = SwissPalette.LightPrimary,
    surface = SwissPalette.LightSurface,
    onSurface = SwissPalette.LightPrimary,
    surfaceVariant = SwissPalette.LightMutedSurface,
    onSurfaceVariant = SwissPalette.LightSecondary,
    outline = Color(0xFFD2D2CD),
    outlineVariant = SwissPalette.LightDivider,
    error = Color(0xFF991B1B),
    onError = Color(0xFFFFFFFF),
  )

private val SwissDarkColorScheme =
  darkColorScheme(
    primary = SwissPalette.DarkPrimary,
    onPrimary = Color(0xFF111111),
    primaryContainer = SwissPalette.DarkMutedSurface,
    onPrimaryContainer = SwissPalette.DarkPrimary,
    secondary = SwissPalette.DarkSecondary,
    onSecondary = Color(0xFF111111),
    secondaryContainer = SwissPalette.DarkMutedSurface,
    onSecondaryContainer = SwissPalette.DarkPrimary,
    background = SwissPalette.DarkBackground,
    onBackground = SwissPalette.DarkPrimary,
    surface = SwissPalette.DarkSurface,
    onSurface = SwissPalette.DarkPrimary,
    surfaceVariant = SwissPalette.DarkMutedSurface,
    onSurfaceVariant = SwissPalette.DarkSecondary,
    outline = Color(0xFF424242),
    outlineVariant = SwissPalette.DarkDivider,
    error = Color(0xFFEF4444),
    onError = Color(0xFF111111),
  )

private val SwissShapes =
  Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
  )

private fun buildSwissTypography(isPersian: Boolean): Typography {
  val family = if (isPersian) VazirmatnFontFamily else InterFontFamily
  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = if (isPersian) 0.sp else (-0.6).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = if (isPersian) 0.sp else (-0.4).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = if (isPersian) 0.sp else (-0.3).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = if (isPersian) 0.sp else (-0.2).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = if (isPersian) 0.sp else 0.4.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = if (isPersian) 0.sp else 0.6.sp,
      ),
  )
}

@Composable
fun SwissAtlasTheme(
  isDark: Boolean,
  appLanguage: AppLanguage = AppLanguage.ENGLISH,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (isDark) SwissDarkColorScheme else SwissLightColorScheme
  val layoutDirection =
    if (appLanguage == AppLanguage.PERSIAN) LayoutDirection.Rtl else LayoutDirection.Ltr
  val typography = buildSwissTypography(appLanguage == AppLanguage.PERSIAN)

  CompositionLocalProvider(
    LocalLayoutDirection provides layoutDirection,
    LocalAppStrings provides appLanguage.strings(),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = SwissShapes,
      content = content,
    )
  }
}
