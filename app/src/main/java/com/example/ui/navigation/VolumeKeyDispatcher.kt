package com.example.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lightweight bridge allowing MainActivity hardware volume-key interception to forward presses
 * directly to the active ReaderScreen when Volume Key Page Turn is enabled in Read mode.
 */
class VolumeKeyDispatcher {
  var onVolumeKey: ((keyCode: Int) -> Boolean)? = null
}

val LocalVolumeKeyDispatcher = staticCompositionLocalOf<VolumeKeyDispatcher?> { null }
