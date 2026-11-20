package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
  suspend fun requestAllUpdates(context: Context) {
    val appContext = context.applicationContext
    runCatching { PageNestContinueWidget().updateAll(appContext) }
    runCatching { PageNestStatsWidget().updateAll(appContext) }
    runCatching { PageNestStreakWidget().updateAll(appContext) }
    runCatching { AtlasShelfWidget().updateAll(appContext) }
  }
}
