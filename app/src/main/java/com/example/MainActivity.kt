package com.example

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ReaderSettings
import com.example.ui.navigation.LocalVolumeKeyDispatcher
import com.example.ui.navigation.PageNestNavGraph
import com.example.ui.navigation.VolumeKeyDispatcher
import com.example.ui.theme.PageNestTheme

class MainActivity : ComponentActivity() {
  private val volumeKeyDispatcher = VolumeKeyDispatcher()
  private var interceptingVolumeUp = false
  private var interceptingVolumeDown = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val appContainer = (application as PageNestApplication).container
    setContent {
      val settings by
        appContainer.readerRepository.settingsFlow.collectAsStateWithLifecycle(
          initialValue = ReaderSettings()
        )
      val view = LocalView.current
      val isDark = settings.themeMode.isDark
      LaunchedEffect(isDark) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
      }

      CompositionLocalProvider(LocalVolumeKeyDispatcher provides volumeKeyDispatcher) {
        PageNestTheme(
          themeMode = settings.themeMode,
          appLanguage = settings.appLanguage,
        ) {
          PageNestNavGraph(container = appContainer)
        }
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      val handler = volumeKeyDispatcher.onVolumeKey
      if (event.action == KeyEvent.ACTION_DOWN) {
        if (handler != null) {
          val handled = if (event.repeatCount == 0) handler(keyCode) else true
          if (handled) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) interceptingVolumeUp = true
            else interceptingVolumeDown = true
            return true
          }
        }
      } else if (event.action == KeyEvent.ACTION_UP) {
        val wasIntercepted =
          if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            interceptingVolumeUp.also { interceptingVolumeUp = false }
          } else {
            interceptingVolumeDown.also { interceptingVolumeDown = false }
          }
        if (wasIntercepted) return true
      }
    }
    return super.dispatchKeyEvent(event)
  }
}
