package com.example.ui.swiss

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom geometric ATLAS compass symbol:
 * - Outer circular cartographic navigation ring
 * - Inner precision geometric compass diamond (North/South primary axis + East/West cross-points)
 * - Subtle cardinal meridian ticks
 * - Zero gradients, pure geometric linework readable at 20dp to 96dp.
 */
@Composable
fun AtlasGeometricSymbol(
  modifier: Modifier = Modifier,
  size: Dp = 26.dp,
  color: Color = MaterialTheme.colorScheme.primary,
  strokeWidthRatio: Float = 0.068f,
  contentDescriptionText: String? = "ATLAS",
) {
  Canvas(
    modifier =
      modifier
        .size(size)
        .semantics {
          if (contentDescriptionText != null) {
            contentDescription = contentDescriptionText
          }
        },
  ) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h / 2f
    val strokePx = (minOf(w, h) * strokeWidthRatio).coerceAtLeast(1.3f)
    val outerRadius = (minOf(w, h) / 2f) - strokePx

    // Outer navigation circle
    drawCircle(
      color = color,
      radius = outerRadius,
      center = Offset(cx, cy),
      style = Stroke(width = strokePx),
    )

    // Geometric compass diamond inside circle
    val verticalArm = outerRadius * 0.68f
    val horizontalArm = outerRadius * 0.44f
    val diamond =
      Path().apply {
        moveTo(cx, cy - verticalArm)
        lineTo(cx + horizontalArm, cy)
        lineTo(cx, cy + verticalArm)
        lineTo(cx - horizontalArm, cy)
        close()
      }
    drawPath(
      path = diamond,
      color = color,
      style =
        Stroke(
          width = strokePx,
          cap = StrokeCap.Butt,
          join = StrokeJoin.Miter,
        ),
    )

    // Solid upper half of diamond to indicate North cartographic bearing
    val northHalf =
      Path().apply {
        moveTo(cx, cy - verticalArm)
        lineTo(cx + horizontalArm * 0.56f, cy)
        lineTo(cx - horizontalArm * 0.56f, cy)
        close()
      }
    drawPath(path = northHalf, color = color)

    // Minimal cardinal hairline ticks on outer ring
    val tickLen = outerRadius * 0.14f
    drawLine(color, Offset(cx, cy - outerRadius), Offset(cx, cy - outerRadius + tickLen), strokePx * 0.85f)
    drawLine(color, Offset(cx, cy + outerRadius - tickLen), Offset(cx, cy + outerRadius), strokePx * 0.85f)
    drawLine(color, Offset(cx - outerRadius, cy), Offset(cx - outerRadius + tickLen, cy), strokePx * 0.85f)
    drawLine(color, Offset(cx + outerRadius - tickLen, cy), Offset(cx + outerRadius, cy), strokePx * 0.85f)
  }
}

/**
 * Horizontal ATLAS Wordmark: Geometric Symbol + tracked uppercase ATLAS / اطلس.
 */
@Composable
fun AtlasHorizontalWordmark(
  isPersian: Boolean,
  modifier: Modifier = Modifier,
  symbolSize: Dp = 24.dp,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    AtlasGeometricSymbol(
      size = symbolSize,
      color = color,
      contentDescriptionText = null,
    )
    Text(
      text = if (isPersian) "اطلس" else "ATLAS",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      fontSize = if (isPersian) 19.sp else 18.sp,
      letterSpacing = if (isPersian) 0.sp else 3.2.sp,
      color = color,
      maxLines = 1,
    )
  }
}

/**
 * Standalone Light & Dark Geometric App Icon Tile (used in Profile Brand Identity & Switcher).
 */
@Composable
fun AtlasIconSpecimen(
  darkVariant: Boolean,
  size: Dp = 48.dp,
  modifier: Modifier = Modifier,
) {
  val bg = if (darkVariant) SwissPalette.DarkBackground else SwissPalette.LightBackground
  val fg = if (darkVariant) SwissPalette.DarkPrimary else SwissPalette.LightPrimary
  val border = if (darkVariant) SwissPalette.DarkDivider else SwissPalette.LightDivider

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
    modifier = modifier.size(size),
  ) {
    Box(contentAlignment = Alignment.Center) {
      AtlasGeometricSymbol(
        size = size * 0.58f,
        color = fg,
        contentDescriptionText = if (darkVariant) "ATLAS dark icon" else "ATLAS light icon",
      )
    }
  }
}
