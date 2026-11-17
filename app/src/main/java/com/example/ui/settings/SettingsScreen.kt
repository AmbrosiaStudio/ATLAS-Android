package com.example.ui.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.ReaderFontOption
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TapZoneAction
import com.example.model.TipPromptMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.monetization.PageNestProDialog
import com.example.ui.monetization.TipJarDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.toPalette
import com.example.widget.PageNestContinueWidgetReceiver
import com.example.widget.PageNestStatsWidgetReceiver
import com.example.widget.PageNestStreakWidgetReceiver
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showVoicePicker by remember { mutableStateOf(false) }
  var showChangelogDialog by remember { mutableStateOf(false) }
  var showReminderTimeDialog by remember { mutableStateOf(false) }
  var showTipJarDialog by remember { mutableStateOf(false) }
  var showProDialog by remember { mutableStateOf(false) }
  var pendingNotificationAction by remember { mutableStateOf<String?>(null) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val action = pendingNotificationAction
      pendingNotificationAction = null
      if (granted) {
        when (action) {
          "enable" -> viewModel.setStreakReminderEnabled(true)
          "test" -> viewModel.sendTestStreakReminder()
        }
      } else {
        viewModel.setStreakReminderEnabled(false)
        scope.launch { snackbarHostState.showSnackbar(strings.reminderPermissionNeeded) }
      }
    }

  val hasPostNotificationPermission: () -> Boolean = {
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
  }

  val fontPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.importCustomFont(uri)
      }
    }

  val openExternalUrl: (String) -> Unit = { url ->
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
  }

  LaunchedEffect(uiState.message) {
    val msg = uiState.message
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
      )
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)) {
        // Solid Clean Top Header (No Liquid Glass / Blur)
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onBackground,
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.settingsTitle,
                  fontFamily = LiterataFontFamily,
                  fontSize = 23.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = strings.settingsSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        LazyColumn(
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
          modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          // 0. App Language (EN / فارسی)
          item {
            SettingsSectionLabel(strings.appLanguageSection)
            SettingsGroupCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Translate,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                AppLanguage.entries.forEach { lang ->
                  val selected = uiState.settings.appLanguage == lang
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
                    border =
                      BorderStroke(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                      ),
                    modifier =
                      Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.setAppLanguage(lang) }
                        .testTag("app_lang_${lang.code}"),
                  ) {
                    Box(
                      modifier = Modifier.padding(vertical = 12.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = lang.nativeLabel,
                        fontFamily = if (lang == AppLanguage.PERSIAN) VazirmatnFontFamily else PlusJakartaSansFamily,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color =
                          if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }

          // 1. Reading Experience & Page Flow
          item {
            SettingsSectionLabel(strings.readingDefaultsSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = strings.defaultTheme,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  ReadingThemeMode.entries.chunked(3).forEach { rowModes ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      rowModes.forEach { mode ->
                        val mp = mode.toPalette()
                        val selected = uiState.settings.themeMode == mode
                        Surface(
                          shape = RoundedCornerShape(14.dp),
                          color = mp.background,
                          border =
                            BorderStroke(
                              width = if (selected) 2.dp else 1.dp,
                              color = if (selected) MaterialTheme.colorScheme.primary else mp.dockBorder,
                            ),
                          modifier =
                            Modifier
                              .weight(1f)
                              .clip(RoundedCornerShape(14.dp))
                              .clickable { viewModel.setThemeMode(mode) }
                              .testTag("settings_theme_${mode.name.lowercase()}"),
                        ) {
                          Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                          ) {
                            Text(
                              text = "Aa",
                              fontFamily = LiterataFontFamily,
                              fontWeight = FontWeight.SemiBold,
                              color = mp.text,
                            )
                            Text(
                              text = if (isFa) mode.faLabel else mode.label,
                              style = MaterialTheme.typography.labelSmall,
                              color = mp.secondaryText,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                        }
                      }
                    }
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                  text = strings.readingModeSection,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ReadingLayoutMode.entries.forEach { layoutMode ->
                    val selected = uiState.settings.readingLayoutMode == layoutMode
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                      border =
                        BorderStroke(
                          width = 1.dp,
                          color =
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                      modifier =
                        Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(14.dp))
                          .clickable { viewModel.setReadingLayoutMode(layoutMode) }
                          .testTag("settings_layout_${layoutMode.name.lowercase()}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                      ) {
                        Icon(
                          imageVector =
                            if (layoutMode == ReadingLayoutMode.PAGED) Icons.AutoMirrored.Rounded.MenuBook
                            else Icons.Rounded.SwapVert,
                          contentDescription = null,
                          tint =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = if (isFa) layoutMode.faLabel else layoutMode.label,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                          color =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.keepScreenAwake,
                  subtitle = strings.keepScreenAwakeSub,
                  checked = uiState.settings.keepScreenOn,
                  onCheckedChange = viewModel::setKeepScreenOn,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.resumeLastBook,
                  subtitle = strings.resumeLastBookSub,
                  checked = uiState.settings.restoreLastSession,
                  onCheckedChange = viewModel::setRestoreLastSession,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.paperStyleToggle,
                  subtitle = strings.paperStyleSub,
                  checked = uiState.settings.paperTextureEnabled,
                  onCheckedChange = viewModel::setPaperTextureEnabled,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.volumeKeyPageTurnToggle,
                  subtitle = strings.volumeKeyPageTurnSub,
                  checked = uiState.settings.volumeKeyPageTurnEnabled,
                  onCheckedChange = viewModel::setVolumeKeyPageTurnEnabled,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.batterySaverToggle,
                  subtitle = strings.batterySaverSub,
                  checked = uiState.settings.batterySaverModeEnabled,
                  onCheckedChange = viewModel::setBatterySaverModeEnabled,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.adaptPdfColors,
                  subtitle = strings.adaptPdfColorsSub,
                  checked = uiState.settings.pdfThemeFilterEnabled,
                  onCheckedChange = viewModel::setPdfThemeFilter,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                  text = strings.tapZonesSection,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  SettingsTapZoneTile(
                    zoneLabel = strings.leftTapZoneLabel,
                    selectedAction = uiState.settings.leftTapAction,
                    isFa = isFa,
                    onSelect = viewModel::setLeftTapAction,
                    modifier = Modifier.weight(1f).testTag("settings_tap_zone_left"),
                  )
                  SettingsTapZoneTile(
                    zoneLabel = strings.centerTapZoneLabel,
                    selectedAction = uiState.settings.centerTapAction,
                    isFa = isFa,
                    onSelect = viewModel::setCenterTapAction,
                    modifier = Modifier.weight(1f).testTag("settings_tap_zone_center"),
                  )
                  SettingsTapZoneTile(
                    zoneLabel = strings.rightTapZoneLabel,
                    selectedAction = uiState.settings.rightTapAction,
                    isFa = isFa,
                    onSelect = viewModel::setRightTapAction,
                    modifier = Modifier.weight(1f).testTag("settings_tap_zone_right"),
                  )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.edgeBrightnessSwipeToggle,
                  subtitle = strings.edgeBrightnessSwipeSub,
                  checked = uiState.settings.edgeBrightnessSwipeEnabled,
                  switchTestTag = "settings_edge_brightness_switch",
                  onCheckedChange = viewModel::setEdgeBrightnessSwipeEnabled,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ToggleSettingRow(
                  title = strings.doubleTapBookmarkToggle,
                  subtitle = strings.doubleTapBookmarkSub,
                  checked = uiState.settings.doubleTapBookmarkEnabled,
                  switchTestTag = "settings_double_tap_bookmark_switch",
                  onCheckedChange = viewModel::setDoubleTapBookmarkEnabled,
                )
              }
            }
          }

          // 1b. Daily Streak Reminders & Home Widget
          item {
            SettingsSectionLabel(strings.remindersSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                ToggleSettingRow(
                  title = strings.streakReminderToggle,
                  subtitle = strings.streakReminderSub,
                  checked = uiState.settings.streakReminderEnabled,
                  switchTestTag = "settings_streak_reminder_switch",
                  onCheckedChange = { enabled ->
                    if (enabled && !hasPostNotificationPermission() &&
                      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                      pendingNotificationAction = "enable"
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                      viewModel.setStreakReminderEnabled(enabled)
                    }
                  },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val formattedReminderTime =
                  String.format(
                    "%02d:%02d",
                    uiState.settings.streakReminderHour,
                    uiState.settings.streakReminderMinute,
                  )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.AccessTime,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = strings.reminderTimeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = formattedReminderTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { showReminderTimeDialog = true },
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("settings_reminder_time_button"),
                    ) {
                      Text(formattedReminderTime, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                      onClick = {
                        if (!hasPostNotificationPermission() &&
                          Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                          pendingNotificationAction = "test"
                          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                          viewModel.sendTestStreakReminder()
                        }
                      },
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("settings_test_reminder_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(strings.testReminderAction, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val canPinWidget =
                  remember(context) {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                      runCatching {
                        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
                      }.getOrDefault(false)
                  }
                val pinWidget: (Class<*>) -> Unit = { receiverClass ->
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runCatching {
                      val manager = AppWidgetManager.getInstance(context)
                      val provider = ComponentName(context, receiverClass)
                      manager.requestPinAppWidget(provider, null, null)
                    }
                  }
                }

                WidgetSettingRow(
                  icon = Icons.Rounded.Widgets,
                  title = strings.widgetSectionTitle,
                  subtitle = strings.widgetSectionSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_widget_button",
                  onPinClick = { pinWidget(PageNestContinueWidgetReceiver::class.java) },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                WidgetSettingRow(
                  icon = Icons.Rounded.BarChart,
                  title = strings.statsWidgetTitle,
                  subtitle = strings.statsWidgetSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_stats_widget_button",
                  onPinClick = { pinWidget(PageNestStatsWidgetReceiver::class.java) },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                WidgetSettingRow(
                  icon = Icons.Rounded.LocalFireDepartment,
                  title = strings.streakWidgetTitle,
                  subtitle = strings.streakWidgetSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_streak_widget_button",
                  onPinClick = { pinWidget(PageNestStreakWidgetReceiver::class.java) },
                )
              }
            }
          }

          // 1c. Support & PageNest Pro
          item {
            SettingsSectionLabel(strings.supportAndProSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color =
                    if (uiState.settings.isProUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                  border =
                    BorderStroke(
                      width = 1.dp,
                      color =
                        if (uiState.settings.isProUnlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { showProDialog = true }
                      .testTag("settings_pro_card"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text =
                            if (uiState.settings.isProUnlocked) strings.proActiveTitle
                            else strings.proHeroTitle,
                          style = MaterialTheme.typography.titleSmall,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text =
                            if (uiState.settings.isProUnlocked) strings.proActiveSub
                            else strings.proHeroSub,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text =
                        if (uiState.settings.isProUnlocked) strings.proBadge
                        else strings.openLinkAction,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Coffee,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = strings.tipJarTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = strings.tipJarSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  OutlinedButton(
                    onClick = { showTipJarDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("settings_tip_jar_button"),
                  ) {
                    Text(strings.tipJarTitle, style = MaterialTheme.typography.labelMedium)
                  }
                }

                if (!uiState.settings.isProUnlocked) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                      text = strings.tipJarScheduleLabel,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = strings.tipJarScheduleSub,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      TipPromptMode.entries.forEach { mode ->
                        val selected = uiState.settings.tipPromptMode == mode
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color =
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                          border =
                            BorderStroke(
                              width = 1.dp,
                              color =
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                          modifier =
                            Modifier
                              .weight(1f)
                              .clip(RoundedCornerShape(12.dp))
                              .clickable { viewModel.setTipPromptMode(mode) }
                              .testTag("settings_tip_mode_${mode.name.lowercase()}"),
                        ) {
                          Box(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center,
                          ) {
                            Text(
                              text = if (isFa) mode.faLabel else mode.label,
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                              color =
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                        }
                      }
                    }

                    if (uiState.settings.tipPromptMode == TipPromptMode.FIXED_USAGE) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Text(
                          text = strings.tipJarFixedMinutesLabel(uiState.settings.tipFixedMinutes),
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                      Slider(
                        value = uiState.settings.tipFixedMinutes.toFloat(),
                        onValueChange = { viewModel.setTipFixedMinutes(it.roundToInt()) },
                        valueRange = 10f..120f,
                        steps = 10,
                        colors =
                          SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                          ),
                        modifier = Modifier.testTag("settings_tip_fixed_minutes_slider"),
                      )
                    }
                  }
                }
              }
            }
          }

          // 2. Font Management (Latin & Persian)
          item {
            SettingsSectionLabel(strings.fontManagementSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = strings.readerTypefaces,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = strings.readerTypefacesSub,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  OutlinedButton(
                    onClick = {
                      fontPicker.launch(
                        arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream")
                      )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("import_font_button"),
                  ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.importLabel, style = MaterialTheme.typography.labelMedium)
                  }
                }

                ReaderFontOption.entries.forEach { option ->
                  val active = uiState.settings.customFontId == null && uiState.settings.fontOption == option
                  val sampleLine =
                    if (option.supportsPersian) "«در ستایش خواندن آرام و تایپوگرافی دقیق.»"
                    else "“Sphinx of black quartz, judge my vow.”"
                  FontRowItem(
                    name = option.displayName,
                    subtitle =
                      when {
                        option.supportsPersian -> if (isFa) "قلم فارسی" else "Persian / RTL"
                        option.isSerif -> if (isFa) "سریف" else "Bundled Serif"
                        else -> if (isFa) "سنز / مونو" else "Bundled Sans / Mono"
                      },
                    sampleLine = sampleLine,
                    fontFamily = option.toFontFamily(),
                    selected = active,
                    canDelete = false,
                    onSelect = { viewModel.selectBuiltInFont(option) },
                    onDelete = {},
                  )
                }

                uiState.customFonts.forEach { custom ->
                  val active = uiState.settings.customFontId == custom.id
                  val customFamily =
                    remember(custom.filePath) {
                      runCatching { FontFamily(Typeface.createFromFile(File(custom.filePath))) }
                        .getOrDefault(LiterataFontFamily)
                    }
                  FontRowItem(
                    name = custom.displayName,
                    subtitle = "Custom Typeface",
                    sampleLine = "“Sphinx of black quartz” · «در ستایش خواندن آرام»",
                    fontFamily = customFamily,
                    selected = active,
                    canDelete = true,
                    onSelect = { viewModel.selectCustomFont(custom.id) },
                    onDelete = { viewModel.deleteCustomFont(custom.id) },
                  )
                }
              }
            }
          }

          // 3. Text-to-Speech (TTS) Management
          item {
            SettingsSectionLabel(strings.ttsSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = strings.voiceEngine,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    val selectedLabel =
                      uiState.ttsState.availableVoices
                        .firstOrNull { it.name == uiState.settings.ttsVoiceName }
                        ?.displayName ?: "System Default Voice"
                    Text(
                      text = selectedLabel,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }

                  Box {
                    OutlinedButton(
                      onClick = { showVoicePicker = true },
                      enabled = uiState.ttsState.availableVoices.isNotEmpty(),
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                      modifier = Modifier.testTag("select_tts_voice_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Voice", style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                      expanded = showVoicePicker,
                      onDismissRequest = { showVoicePicker = false },
                      containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                      DropdownMenuItem(
                        text = {
                          Text("System Default Voice", color = MaterialTheme.colorScheme.onSurface)
                        },
                        onClick = {
                          viewModel.setTtsVoice(null)
                          showVoicePicker = false
                        },
                      )
                      uiState.ttsState.availableVoices.forEach { voice ->
                        DropdownMenuItem(
                          text = {
                            Column {
                              Text(
                                voice.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                              )
                              if (voice.localeLabel.isNotBlank()) {
                                Text(
                                  voice.localeLabel,
                                  style = MaterialTheme.typography.labelSmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                              }
                            }
                          },
                          onClick = {
                            viewModel.setTtsVoice(voice.name)
                            showVoicePicker = false
                          },
                        )
                      }
                    }
                  }
                }

                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      strings.speechSpeed,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = String.format("%.2fx", uiState.settings.ttsSpeechRate),
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Slider(
                    value = uiState.settings.ttsSpeechRate,
                    onValueChange = viewModel::setTtsRate,
                    valueRange = 0.6f..2.0f,
                    colors =
                      SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                      ),
                    modifier = Modifier.testTag("tts_rate_slider"),
                  )
                }

                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      strings.voicePitch,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = String.format("%.2fx", uiState.settings.ttsPitch),
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Slider(
                    value = uiState.settings.ttsPitch,
                    onValueChange = viewModel::setTtsPitch,
                    valueRange = 0.7f..1.4f,
                    colors =
                      SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                      ),
                    modifier = Modifier.testTag("tts_pitch_slider"),
                  )
                }

                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable(enabled = uiState.ttsState.isReady) { viewModel.testTtsVoice() }
                      .testTag("tts_test_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                  ) {
                    Icon(
                      imageVector =
                        if (uiState.ttsState.isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text =
                        if (!uiState.ttsState.isReady) "Voice Engine Initializing…"
                        else if (uiState.ttsState.isPlaying) strings.stopPreview
                        else strings.previewVoice,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                }
              }
            }
          }

          // 4. Version Management & Storage
          item {
            SettingsSectionLabel(strings.versionSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Verified,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "PageNest v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = "Build ${BuildConfig.VERSION_CODE} · Local-First Release",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  TextButton(
                    onClick = { showChangelogDialog = true },
                    modifier = Modifier.testTag("changelog_button"),
                  ) {
                    Text(strings.changelog, color = MaterialTheme.colorScheme.primary)
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Storage,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        strings.localStorage,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text =
                          "${formatBytes(uiState.storageStats.booksSizeBytes)} · Cache ${formatBytes(uiState.storageStats.epubCacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  TextButton(
                    onClick = viewModel::clearEpubCache,
                    modifier = Modifier.testTag("clear_cache_button"),
                  ) {
                    Text(strings.clearCache, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }

          // 5. About Me
          item {
            SettingsSectionLabel(strings.aboutMeSection)
            SettingsGroupCard {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = strings.craftedBy,
                    fontFamily = LiterataFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = strings.aboutBio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CreatorLinkRow(
                  icon = Icons.Rounded.Code,
                  platform = "GitHub",
                  handle = "github.com/AmirSenpai",
                  testTag = "about_github_link",
                  onClick = { openExternalUrl("https://github.com/AmirSenpai") },
                )

                CreatorLinkRow(
                  icon = Icons.AutoMirrored.Rounded.Send,
                  platform = "Telegram",
                  handle = "@itsdekusenpai",
                  testTag = "about_telegram_link",
                  onClick = { openExternalUrl("https://t.me/itsdekusenpai") },
                )

                CreatorLinkRow(
                  icon = Icons.Rounded.Language,
                  platform = "Website",
                  handle = "mrambrosia.ir",
                  testTag = "about_website_link",
                  onClick = { openExternalUrl("https://mrambrosia.ir") },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showChangelogDialog) {
    AlertDialog(
      onDismissRequest = { showChangelogDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = {
        Text(
          text = "${strings.changelog} · v${BuildConfig.VERSION_NAME}",
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (isFa) {
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۲.۰ (بیلد ${BuildConfig.VERSION_CODE}) · صوت و تمرکز، ژست‌ها، فید OPDS و استودیو جلد",
              bullets =
                listOf(
                  "جدید · تایمر خواب با محو شدن ملایم صدا در حالت شنیداری: زمان‌بند ۱۵ تا ۶۰ دقیقه یا «پایان فصل» همراه با کاهش تدریجی و آرام صدا در ۱۵ ثانیه پایانی پیش از توقف خودکار.",
                  "جدید · کنترل پخش در صفحه قفل و دکمه‌های هدفون/هندزفری بلوتوث برای روخوانی کتاب (MediaSession + اعلان استاندارد رسانه).",
                  "جدید · شخصی‌سازی نواحی لمس صفحه (چپ، مرکز، راست)، تنظیم فوری روشنایی با کشیدن لبه چپ صفحه و نشانک‌گذاری سریع با دو بار ضربه.",
                  "جدید · صداهای آرامش‌بخش محیطی (باران، شومینه، صدای قهوه‌ای، امواج اقیانوس بدون نیاز به اینترنت) و تایمر تمرکز مطالعه پومودورو.",
                  "جدید · کتابخانه آنلاین کتاب‌های رایگان و کلاسیک آزاد (پروژه گوتنبرگ، Standard Ebooks و فیدهای سفارشی OPDS) با دانلود مستقیم و افزودن آنی به قفسه.",
                  "جدید · استودیو طراحی جلد کتاب (۶ قالب هنری + تصویر سفارشی با Photo Picker امن) و مرتب‌سازی و جابه‌جایی فصل‌ها در استودیو نویسندگی پیج‌نست.",
                  "رفع خطا · بازطراحی منوی شناور انتخاب متن هماهنگ با تم مطالعه (شیشه مایع) و رفع مشکل بریدگی و جای‌گذاری در زبان فارسی و انگلیسی.",
                  "رفع خطا · بازسازی کامل موتور صداهای محیطی به استریو ۴۴.۱ کیلوهرتز با سنتز طبیعی باران، شومینه، نویز قهوه‌ای و امواج دریا به جای صدای خش‌خش.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۱.۰ · استودیو نویسندگی، ویجت‌های جدید و دقت بازگشت به صفحه",
              bullets =
                listOf(
                  "جدید · استودیو نویسندگی پیج‌نست (آزمایشی): محیط نوشتن کتاب چندفصلی با پشتیبانی کامل از فارسی و انگلیسی، نوار ابزار مارک‌داون، ذخیره خودکار و انتشار یک‌لمسی به صورت EPUB.",
                  "جدید · ویجت آمار مطالعه (۴×۲) و ویجت ۱×۱ شعله رشته مطالعه با دسترسی مستقیم به داشبورد آمار.",
                  "بهبود و رفع خطا · حذف دکمه‌های اضافی فصل قبل/بعد از صفحه کتاب، رفع باگ پرش کل کتاب با دکمه‌های صدا و ذخیره دقیق شماره صفحه خوانده‌شده هنگام بستن و باز کردن مجدد برنامه.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۰ · قفسه‌ها، فرمت‌های گسترده، داشبورد آمار و یادآورها",
              bullets =
                listOf(
                  "جدید · قفسه‌ها و دسته‌بندی‌های دلخواه، فیلتر وضعیت مطالعه، ویرایشگر مشخصات و جلد کتاب.",
                  "جدید · پشتیبانی بومی از فایل‌های TXT، Markdown (.md)، FictionBook (.fb2) و کمیک بوک (.cbz) در کنار EPUB و PDF.",
                  "جدید · داشبورد آمار مطالعه با نمودار روزانه و روند پیشرفت، یادآور روزانه رشته مطالعه و ویجت ادامه مطالعه.",
                  "جدید · بخش حمایت از توسعه‌دهنده (Tip Jar) با لینک‌های قابل تنظیم ایرانی و بین‌المللی و نسخه حامیان Pro.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۲.۰ · موتور بومی مطالعه و حالت صرفه‌جویی باتری",
              bullets =
                listOf(
                  "موتور جدید · جایگزینی کامل WebView با موتور تماماً بومی Jetpack Compose برای حذف کرش‌های مرورگر و روانی بیشتر صفحات.",
                  "جدید · حالت صرفه‌جویی باتری (محدودیت نرخ نوسازی و حذف افکت‌های سنگین)، ورق زدن با دکمه‌های صدا، بافت کاغذی و تم‌های شب.",
                  "رفع خطا · اصلاح چیدمان راست‌به‌چپ فارسی در هدر کتابخانه و بهینه‌سازی کنتراست در تمام تم‌ها.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۱.۰ · هسته اولیه کتابخوان آرام پیج‌نست",
              bullets =
                listOf(
                  "هسته اصلی · کتابخانه کاملاً آفلاین و محلی برای EPUB و PDF با رابط کاربری دوزبانه فارسی/انگلیسی، قلم‌های بومی و سفارشی، هایلایت و یادداشت، ترجمه در لحظه، نشانک، جستجوی متنی و روخوانی صوتی.",
                ),
            )
          } else {
            ChangelogVersionBlock(
              versionHeading = "v3.2.0 (Build ${BuildConfig.VERSION_CODE}) · Listen Immersion, Gestures, OPDS & Cover Studio",
              bullets =
                listOf(
                  "NEW · Sleep Timer & Gentle Fade-Out in Listen Mode: 15/30/45/60m or End-of-Chapter timers with smooth 15-second linear volume fade-out before pausing.",
                  "NEW · Lock-Screen Media Controls & Bluetooth Earbud Shortcuts for Text-to-Speech via native Android MediaSession & MediaStyle foreground playback notification.",
                  "NEW · Tap-Zone & Gesture Customization: assign Left / Center / Right tap actions, left-edge vertical brightness swipe, and double-tap quick bookmarking.",
                  "NEW · Offline Procedural Ambient Soundscapes (Rain, Fireplace, Brown Noise, Ocean Waves) & Pomodoro Focus Timer inside the Reader.",
                  "NEW · Free Public-Domain Book Browser: explore Project Gutenberg, Standard Ebooks, and custom OPDS 1.2 feeds with one-tap EPUB import.",
                  "NEW · Studio Cover Studio & Chapter Reordering: pick from 6 editorial gradients or upload a custom cover photo in PageNest Studio, and reorder chapters anytime.",
                  "BUG FIX · Redesigned the text-selection popup in theme-matched Liquid Glass with a balanced 3×2 grid and fixed RTL/LTR physical clamping across Persian and English.",
                  "BUG FIX · Rebuilt Ambient Soundscapes into a 44.1 kHz stereo acoustic synthesizer (pink-noise biquads, water-droplet pings, wood crackles, and sweeping ocean surf) replacing raw static.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v3.1.0 · Studio, Widgets & Reader Precision",
              bullets =
                listOf(
                  "NEW · PageNest Studio (Beta): full offline book-writing studio with multi-chapter drafts, Markdown toolbar, RTL/LTR support, live autosave, Markdown export, and one-tap EPUB 3.0 publishing.",
                  "NEW · Reading Statistics Widget (4×2) & 1×1 Streak Flame Widget on the home screen.",
                  "UX & FIX · Removed inline Prev/Next Chapter buttons, fixed Volume key chapter-skipping bug, and persisted exact per-page resume position across restarts.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v3.0 · Collections, Formats, Statistics & Indie Support",
              bullets =
                listOf(
                  "NEW · Custom Shelves, Collections & Reading Status Filters plus Book Metadata & Cover Editor via Android Photo Picker.",
                  "NEW · Expanded Format Support: native TXT, Markdown (.md), FictionBook (.fb2), and Comic Archives (.cbz) alongside EPUB and PDF.",
                  "NEW · Reading Statistics Dashboard, WorkManager Daily Streak Reminders, Continue Reading Widget, Tip Jar & PageNest Pro supporter edition.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v2.0 · Native Reading Engine & Comfort Upgrade",
              bullets =
                listOf(
                  "ENGINE · Replaced WebView with a 100% native Jetpack Compose EPUB reader engine — eliminating renderer crashes.",
                  "NEW · Battery-Saver Reading Mode, Volume Key page turning, tactile Paper Texture mode, Black & White Night theme, and Persian RTL header polish.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v1.0 · Foundation Sanctuary (ir.pagenest.mrambrosia)",
              bullets =
                listOf(
                  "CORE · Local-first EPUB & PDF library with zero-account private storage, bilingual English/Persian UI, sample classics, custom .ttf/.otf font importer, highlights, notes, translation, bookmarks, search, and Text-to-Speech.",
                ),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showChangelogDialog = false }) {
          Text(strings.done, color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  if (showReminderTimeDialog) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = uiState.settings.streakReminderHour,
        initialMinute = uiState.settings.streakReminderMinute,
        is24Hour = true,
      )
    AlertDialog(
      onDismissRequest = { showReminderTimeDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      title = {
        Text(
          text = strings.setReminderTimeTitle,
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          TimePicker(state = timePickerState)
        }
      },
      dismissButton = {
        TextButton(onClick = { showReminderTimeDialog = false }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.setStreakReminderTime(timePickerState.hour, timePickerState.minute)
            showReminderTimeDialog = false
          },
          modifier = Modifier.testTag("confirm_reminder_time_button"),
        ) {
          Text(strings.saveGoal, color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  if (showTipJarDialog) {
    val totalMins = (uiState.settings.totalReadingSecondsAllTime / 60L).toInt()
    TipJarDialog(
      totalReadingMinutes = totalMins,
      isFromRecommendation = false,
      isProUnlocked = uiState.settings.isProUnlocked,
      hasSupportedAlready = uiState.settings.hasSupportedViaTipJar,
      isFa = isFa,
      onMarkSupported = viewModel::markSupportedViaTipJar,
      onOpenProDialog = {
        showTipJarDialog = false
        showProDialog = true
      },
      onDismiss = { showTipJarDialog = false },
    )
  }

  if (showProDialog) {
    PageNestProDialog(
      isProUnlocked = uiState.settings.isProUnlocked,
      isFa = isFa,
      onRedeemSupporterCode = viewModel::redeemSupporterCode,
      onDismiss = { showProDialog = false },
    )
  }
}

@Composable
private fun SettingsSectionLabel(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
  )
}

@Composable
private fun SettingsGroupCard(
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    content()
  }
}

@Composable
private fun ToggleSettingRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  switchTestTag: String? = null,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
          checkedTrackColor = MaterialTheme.colorScheme.primary,
          uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
          uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
  }
}

@Composable
private fun FontRowItem(
  name: String,
  subtitle: String,
  sampleLine: String,
  fontFamily: FontFamily,
  selected: Boolean,
  canDelete: Boolean,
  onSelect: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
      else MaterialTheme.colorScheme.surfaceVariant,
    border =
      BorderStroke(
        width = 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onSelect),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = name,
          fontFamily = fontFamily,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "$subtitle · $sampleLine",
          fontFamily = fontFamily,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = "Active font",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
      if (canDelete) {
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete $name",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CreatorLinkRow(
  icon: ImageVector,
  platform: String,
  handle: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = platform,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = platform,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = handle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = "Open $platform",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(17.dp),
      )
    }
  }
}

private fun ReaderFontOption.toFontFamily(): FontFamily =
  when (this) {
    ReaderFontOption.LITERATA -> LiterataFontFamily
    ReaderFontOption.LORA -> LoraFontFamily
    ReaderFontOption.NEWSREADER -> NewsreaderFontFamily
    ReaderFontOption.VAZIRMATN -> VazirmatnFontFamily
    ReaderFontOption.NOTO_NASKH -> NotoNaskhFontFamily
    ReaderFontOption.PLUS_JAKARTA -> PlusJakartaSansFamily
    ReaderFontOption.JETBRAINS_MONO -> JetBrainsMonoFontFamily
  }

@Composable
private fun WidgetSettingRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  canPin: Boolean,
  pinLabel: String,
  testTag: String,
  onPinClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.weight(1f),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 2.dp).size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (canPin && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Spacer(modifier = Modifier.width(8.dp))
      OutlinedButton(
        onClick = onPinClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.testTag(testTag),
      ) {
        Text(
          text = pinLabel,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
private fun ChangelogVersionBlock(
  versionHeading: String,
  bullets: List<String>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = versionHeading,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
    )
    bullets.forEach { bullet ->
      Text(
        text = "• $bullet",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun formatBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format("%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format("%.2f MB", mb)
}

@Composable
private fun SettingsTapZoneTile(
  zoneLabel: String,
  selectedAction: TapZoneAction,
  isFa: Boolean,
  onSelect: (TapZoneAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { expanded = true },
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = zoneLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (isFa) selectedAction.faLabel else selectedAction.label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      TapZoneAction.entries.forEach { action ->
        DropdownMenuItem(
          text = {
            Text(
              text = if (isFa) action.faLabel else action.label,
              color =
                if (action == selectedAction) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
          },
          trailingIcon = {
            if (action == selectedAction) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            }
          },
          onClick = {
            expanded = false
            onSelect(action)
          },
        )
      }
    }
  }
}
