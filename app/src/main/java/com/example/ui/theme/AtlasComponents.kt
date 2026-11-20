package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bespoke ATLAS concentric ring-dot radio indicator — replaces generic stock Material RadioButtons
 * with a crisp architectural cobalt-blue double-ring design.
 */
@Composable
fun AtlasRadioDot(
  selected: Boolean,
  modifier: Modifier = Modifier,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  inactiveBorderColor: Color = MaterialTheme.colorScheme.outline,
) {
  val ringColor by animateColorAsState(
    targetValue = if (selected) activeColor else inactiveBorderColor,
    label = "atlas_radio_ring",
  )
  val dotSize by animateDpAsState(
    targetValue = if (selected) 10.dp else 0.dp,
    animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
    label = "atlas_radio_dot",
  )
  val haloBg by animateColorAsState(
    targetValue = if (selected) activeColor.copy(alpha = 0.14f) else Color.Transparent,
    label = "atlas_radio_halo",
  )

  Box(
    modifier =
      modifier
        .size(22.dp)
        .clip(CircleShape)
        .background(haloBg)
        .border(
          width = if (selected) 2.dp else 1.5.dp,
          color = ringColor,
          shape = CircleShape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (dotSize > 0.dp) {
      Box(
        modifier =
          Modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(activeColor),
      )
    }
  }
}

/**
 * Full-width interactive ATLAS radio row with custom [AtlasRadioDot], optional icon badge, and
 * subtle highlighted selection container.
 */
@Composable
fun AtlasRadioRow(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  leadingIcon: ImageVector? = null,
  trailingBadge: String? = null,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  val bg by animateColorAsState(
    targetValue = if (selected) activeColor.copy(alpha = 0.09f) else Color.Transparent,
    label = "atlas_radio_row_bg",
  )
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(bg)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (leadingIcon != null) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (selected) activeColor.copy(alpha = 0.16f)
              else secondaryColor.copy(alpha = 0.10f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = if (selected) activeColor else secondaryColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = if (selected) activeColor else textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = secondaryColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (!trailingBadge.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = activeColor.copy(alpha = 0.13f),
      ) {
        Text(
          text = trailingBadge,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = activeColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }
    AtlasRadioDot(
      selected = selected,
      activeColor = activeColor,
      inactiveBorderColor = secondaryColor.copy(alpha = 0.45f),
    )
  }
}

/**
 * Sleek capsule toggle inspired by minimalist modern control centers — crisp rounded pill with
 * floating circular thumb and smooth spring slide.
 */
@Composable
fun AtlasToggleSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  activeTrackColor: Color = MaterialTheme.colorScheme.primary,
  inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
  val trackColor by animateColorAsState(
    targetValue =
      if (!enabled) inactiveTrackColor.copy(alpha = 0.55f)
      else if (checked) activeTrackColor
      else inactiveTrackColor,
    label = "atlas_switch_track",
  )
  val thumbOffset by animateDpAsState(
    targetValue = if (checked) 22.dp else 3.dp,
    animationSpec = spring(stiffness = 520f, dampingRatio = 0.78f),
    label = "atlas_switch_thumb",
  )
  val borderColor =
    if (checked) activeTrackColor.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

  Box(
    modifier =
      modifier
        .size(width = 50.dp, height = 29.dp)
        .clip(CircleShape)
        .background(trackColor)
        .border(0.8.dp, borderColor, CircleShape)
        .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) },
    contentAlignment = Alignment.CenterStart,
  ) {
    Box(
      modifier =
        Modifier
          .offset(x = thumbOffset)
          .size(23.dp)
          .shadow(elevation = 2.dp, shape = CircleShape)
          .clip(CircleShape)
          .background(Color.White),
    )
  }
}

/**
 * Recessed architectural segmented control with an elevated floating pill for the selected tab.
 */
@Composable
fun AtlasSegmentedControl(
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  activeSurfaceColor: Color = MaterialTheme.colorScheme.surface,
  activeTextColor: Color = MaterialTheme.colorScheme.primary,
  inactiveTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(trackColor)
        .border(0.8.dp, borderColor, RoundedCornerShape(16.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    options.forEachIndexed { idx, label ->
      val selected = idx == selectedIndex
      Surface(
        onClick = { onSelect(idx) },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeSurfaceColor else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        border = if (selected) BorderStroke(0.8.dp, activeTextColor.copy(alpha = 0.25f)) else null,
        modifier = Modifier.weight(1f).height(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) activeTextColor else inactiveTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

/**
 * Vision-inspired vertical rounded-capsule scrubber for Reader Text Size (`Tt`) and Brightness (`☀`).
 * Supports smooth vertical dragging and direct tap-to-jump anywhere along the capsule height.
 */
@Composable
fun AtlasVerticalCapsuleSlider(
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  label: String,
  valueBadge: String,
  accentColor: Color,
  trackColor: Color,
  fillColor: Color,
  borderColor: Color,
  iconColor: Color,
  labelColor: Color,
  modifier: Modifier = Modifier,
  height: Dp = 128.dp,
  icon: ImageVector? = null,
  glyphText: String? = null,
) {
  val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
  val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
  var boxHeightPx by remember { mutableFloatStateOf(1f) }

  fun updateFromY(y: Float) {
    val norm = (1f - (y / boxHeightPx)).coerceIn(0f, 1f)
    onValueChange(valueRange.start + norm * span)
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(height)
          .clip(RoundedCornerShape(24.dp))
          .background(trackColor)
          .border(1.dp, borderColor, RoundedCornerShape(24.dp))
          .onSizeChanged { sz -> boxHeightPx = sz.height.toFloat().coerceAtLeast(1f) }
          .pointerInput(valueRange) {
            detectTapGestures { offset -> updateFromY(offset.y) }
          }
          .pointerInput(valueRange) {
            detectVerticalDragGestures { change, _ ->
              change.consume()
              updateFromY(change.position.y)
            }
          },
      contentAlignment = Alignment.BottomCenter,
    ) {
      // Filled vertical progress bar rising from bottom
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = fraction.coerceAtLeast(0.10f))
            .background(fillColor),
      )

      // Top live value badge
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = 10.dp),
      ) {
        Text(
          text = valueBadge,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (fraction > 0.78f) iconColor else labelColor,
          maxLines = 1,
        )
      }

      // Bottom centered glyph/icon (matching vision panel)
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (glyphText != null) {
          Text(
            text = glyphText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = iconColor,
          )
        } else if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(21.dp),
          )
        }
      }

      // Subtle active indicator notch at fill level
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = ((height - 12.dp) * fraction).coerceIn(6.dp, height - 10.dp))
            .width(24.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.55f)),
      )
    }

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Crisp icon-badged section header used across the redesigned ATLAS Settings & Reader Studios.
 */
@Composable
fun AtlasSectionHeader(
  title: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(30.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accentColor.copy(alpha = 0.13f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(16.dp),
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Architectural grouped surface card with soft hairline border and generous corner radius.
 */
@Composable
fun AtlasGroupedCard(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  contentPadding: Dp = 14.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = 0.5.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(contentPadding),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      content = content,
    )
  }
}

/**
 * Clean toggle row combining an optional icon badge, title/subtitle, and [AtlasToggleSwitch].
 */
@Composable
fun AtlasToggleRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(role = Role.Switch) { onCheckedChange(!checked) }
        .padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (icon != null) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (checked) activeColor.copy(alpha = 0.14f)
              else secondaryColor.copy(alpha = 0.10f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (checked) activeColor else secondaryColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = textColor,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = secondaryColor,
        )
      }
    }
    AtlasToggleSwitch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      activeTrackColor = activeColor,
      inactiveTrackColor = inactiveTrackColor,
    )
  }
}

/**
 * Hairline divider tuned for inside [AtlasGroupedCard].
 */
@Composable
fun AtlasHairlineDivider(
  color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
) {
  HorizontalDivider(
    thickness = 0.8.dp,
    color = color,
    modifier = Modifier.padding(vertical = 2.dp),
  )
}

/**
 * Compact ATLAS slider row with pill value indicator.
 */
@Composable
fun AtlasSliderRow(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  leadingGlyph: String? = null,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (leadingGlyph != null) {
          Text(
            text = leadingGlyph,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
        }
        Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = textColor,
        )
      }
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.12f),
      ) {
        Text(
          text = valueBadgeText(valueText),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = accentColor,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors =
        SliderDefaults.colors(
          thumbColor = accentColor,
          activeTrackColor = accentColor,
          inactiveTrackColor = trackColor,
        ),
    )
  }
}

private fun valueBadgeText(raw: String): String = raw
