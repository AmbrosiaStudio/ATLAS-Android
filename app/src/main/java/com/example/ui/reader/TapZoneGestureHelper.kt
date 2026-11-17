package com.example.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.ReaderSettings
import com.example.model.TapZoneAction
import kotlin.math.abs

fun resolveTapZoneAction(
  tapOffset: Offset,
  containerWidthPx: Int,
  settings: ReaderSettings,
): TapZoneAction {
  val width = containerWidthPx.toFloat().coerceAtLeast(1f)
  val ratioX = (tapOffset.x / width).coerceIn(0f, 1f)
  return when {
    ratioX < 0.28f -> settings.leftTapAction
    ratioX > 0.72f -> settings.rightTapAction
    else -> settings.centerTapAction
  }
}

fun Modifier.leftEdgeBrightnessSwipe(
  enabled: Boolean,
  initialBrightness: Float,
  onPreviewBrightness: (Float?) -> Unit,
  onCommitBrightness: (Float) -> Unit,
): Modifier {
  if (!enabled) return this
  return this.pointerInput(enabled) {
    val startBrightness = if (initialBrightness in 0.04f..1.0f) initialBrightness else 0.5f
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false)
      val edgeWidth = size.width * 0.14f
      if (down.position.x > edgeWidth || size.height <= 0) return@awaitEachGesture

      var current = startBrightness
      var draggingBrightness = false
      val touchSlop = viewConfiguration.touchSlop
      val startPos = down.position
      var lastY = startPos.y

      do {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) {
          if (draggingBrightness) {
            onPreviewBrightness(null)
          }
          return@awaitEachGesture
        }
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        val dyFromStart = change.position.y - startPos.y
        val dxFromStart = change.position.x - startPos.x

        if (!draggingBrightness) {
          if (abs(dxFromStart) > touchSlop && abs(dxFromStart) >= abs(dyFromStart)) {
            return@awaitEachGesture
          }
          if (abs(dyFromStart) > touchSlop && abs(dyFromStart) > abs(dxFromStart) * 1.2f) {
            draggingBrightness = true
            lastY = change.position.y
          }
        }

        if (draggingBrightness) {
          val stepDy = change.position.y - lastY
          lastY = change.position.y
          val delta = (-stepDy / (size.height.toFloat() * 0.65f))
          current = (current + delta).coerceIn(0.05f, 1.0f)
          onPreviewBrightness(current)
          change.consume()
        }
      } while (change.pressed)

      if (draggingBrightness) {
        onCommitBrightness(current)
      }
    }
  }
}
