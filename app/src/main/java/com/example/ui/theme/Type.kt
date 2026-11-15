package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val VazirmatnFontFamily = FontFamily(
  Font(R.font.vazirmatn, FontWeight.Normal),
  Font(R.font.vazirmatn, FontWeight.Medium),
  Font(R.font.vazirmatn, FontWeight.SemiBold),
  Font(R.font.vazirmatn, FontWeight.Bold),
)

val NotoNaskhFontFamily = FontFamily(
  Font(R.font.noto_naskh_arabic, FontWeight.Normal),
  Font(R.font.noto_naskh_arabic, FontWeight.Medium),
  Font(R.font.noto_naskh_arabic, FontWeight.SemiBold),
)

val LiterataFontFamily = FontFamily(
  Font(R.font.literata, FontWeight.Normal),
  Font(R.font.literata, FontWeight.Medium),
  Font(R.font.literata, FontWeight.SemiBold),
)

val LoraFontFamily = FontFamily(
  Font(R.font.lora, FontWeight.Normal),
  Font(R.font.lora, FontWeight.Medium),
)

val NewsreaderFontFamily = FontFamily(
  Font(R.font.newsreader, FontWeight.Normal),
  Font(R.font.newsreader, FontWeight.Medium),
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
)

fun buildPageNestTypography(isPersian: Boolean): Typography {
  val uiFont = if (isPersian) VazirmatnFontFamily else PlusJakartaSansFamily
  val editorialFont = if (isPersian) NotoNaskhFontFamily else LiterataFontFamily
  return Typography(
    displaySmall = TextStyle(
      fontFamily = editorialFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = if (isPersian) 0.sp else (-0.5).sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = editorialFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 23.sp,
      lineHeight = 31.sp,
      letterSpacing = if (isPersian) 0.sp else (-0.25).sp,
    ),
    titleLarge = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 28.sp,
      letterSpacing = if (isPersian) 0.sp else (-0.2).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Normal,
      fontSize = 13.5.sp,
      lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
      fontFamily = uiFont,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      letterSpacing = if (isPersian) 0.sp else 0.2.sp,
    ),
  )
}
