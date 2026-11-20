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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ReaderFontOption
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.model.TapZoneAction
import com.example.model.TipPromptMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.monetization.PageNestProDialog
import com.example.ui.monetization.TipJarDialog
import com.example.ui.theme.AtlasGroupedCard
import com.example.ui.theme.AtlasHairlineDivider
import com.example.ui.theme.AtlasRadioDot
import com.example.ui.theme.AtlasRadioRow
import com.example.ui.theme.AtlasSectionHeader
import com.example.ui.theme.AtlasSegmentedControl
import com.example.ui.theme.AtlasSliderRow
import com.example.ui.theme.AtlasToggleRow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.toPalette
import com.example.widget.AtlasShelfWidgetReceiver
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
        // Crisp Architectural ATLAS Top Bar
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(42.dp),
              ) {
                IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.settingsTitle,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
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
            HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        LazyColumn(
          contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 44.dp),
          verticalArrangement = Arrangement.spacedBy(22.dp),
          modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          // Brand Identity Hero Card
          item {
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              shadowElevation = 1.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Image(
                  painter = painterResource(id = R.drawable.img_atlas_logo_foreground),
                  contentDescription = strings.appName,
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier
                      .size(54.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = strings.appName,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        text = "v${BuildConfig.VERSION_NAME} · #${BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text = "atlas.mrmabrosia.ir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = { showChangelogDialog = true },
                  shape = CircleShape,
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("changelog_button"),
                ) {
                  Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(strings.changelog, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }

          // SECTION 1: Appearance & Language
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.appearanceSectionTitle,
                icon = Icons.Rounded.Palette,
              )
              AtlasGroupedCard(contentPadding = 14.dp) {
                // App Language with bespoke AtlasRadioRow (concentric ring-dot radios)
                Text(
                  text = strings.appLanguageSection,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  AppLanguage.entries.forEach { lang ->
                    val selected = uiState.settings.appLanguage == lang
                    AtlasRadioRow(
                      title = lang.nativeLabel,
                      subtitle = if (lang == AppLanguage.PERSIAN) "راست‌به‌چپ کامل · Persian RTL" else "Left-to-Right · English",
                      selected = selected,
                      leadingIcon = Icons.Rounded.Translate,
                      onClick = { viewModel.setAppLanguage(lang) },
                      modifier = Modifier.testTag("app_lang_${lang.code}"),
                    )
                  }
                }

                AtlasHairlineDivider()

                // Default Reading Theme — Circular Swatch Bar (matching ATLAS Reader Control Center)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = strings.defaultTheme,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                  ) {
                    Text(
                      text = if (isFa) uiState.settings.themeMode.faLabel else uiState.settings.themeMode.label,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }

                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    ReadingThemeMode.entries.forEach { mode ->
                      val mp = mode.toPalette()
                      val selected = uiState.settings.themeMode == mode
                      Box(
                        modifier =
                          Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(mp.background)
                            .border(
                              width = if (selected) 2.5.dp else 1.2.dp,
                              color = if (selected) MaterialTheme.colorScheme.primary else mp.dockBorder,
                              shape = CircleShape,
                            )
                            .clickable { viewModel.setThemeMode(mode) }
                            .testTag("settings_theme_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                      ) {
                        if (selected) {
                          Box(
                            modifier =
                              Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                          ) {
                            Icon(
                              imageVector = Icons.Rounded.Check,
                              contentDescription = if (isFa) mode.faLabel else mode.label,
                              tint = MaterialTheme.colorScheme.onPrimary,
                              modifier = Modifier.size(12.dp),
                            )
                          }
                        } else {
                          Text(
                            text = "Aa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mp.text.copy(alpha = 0.8f),
                          )
                        }
                      }
                    }
                  }
                }

                AtlasHairlineDivider()

                // Page Flow Segmented Control
                Text(
                  text = strings.readingModeSection,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AtlasSegmentedControl(
                  options = ReadingLayoutMode.entries.map { if (isFa) it.faLabel else it.label },
                  selectedIndex = ReadingLayoutMode.entries.indexOf(uiState.settings.readingLayoutMode).coerceAtLeast(0),
                  onSelect = { idx -> viewModel.setReadingLayoutMode(ReadingLayoutMode.entries[idx]) },
                )
              }
            }
          }

          // SECTION 2: Reading Comfort & Behavior
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.readingComfortSectionTitle,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
              )
              AtlasGroupedCard(contentPadding = 14.dp) {
                AtlasToggleRow(
                  title = strings.keepScreenAwake,
                  subtitle = strings.keepScreenAwakeSub,
                  checked = uiState.settings.keepScreenOn,
                  onCheckedChange = viewModel::setKeepScreenOn,
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.resumeLastBook,
                  subtitle = strings.resumeLastBookSub,
                  checked = uiState.settings.restoreLastSession,
                  onCheckedChange = viewModel::setRestoreLastSession,
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.paperStyleToggle,
                  subtitle = strings.paperStyleSub,
                  checked = uiState.settings.paperTextureEnabled,
                  onCheckedChange = viewModel::setPaperTextureEnabled,
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.volumeKeyPageTurnToggle,
                  subtitle = strings.volumeKeyPageTurnSub,
                  checked = uiState.settings.volumeKeyPageTurnEnabled,
                  onCheckedChange = viewModel::setVolumeKeyPageTurnEnabled,
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.batterySaverToggle,
                  subtitle = strings.batterySaverSub,
                  checked = uiState.settings.batterySaverModeEnabled,
                  onCheckedChange = viewModel::setBatterySaverModeEnabled,
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.adaptPdfColors,
                  subtitle = strings.adaptPdfColorsSub,
                  checked = uiState.settings.pdfThemeFilterEnabled,
                  onCheckedChange = viewModel::setPdfThemeFilter,
                )
              }
            }
          }

          // SECTION 3: Tap Zones & Gestures
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.gesturesSectionTitle,
                icon = Icons.Rounded.TouchApp,
              )
              AtlasGroupedCard(contentPadding = 14.dp) {
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
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.edgeBrightnessSwipeToggle,
                  subtitle = strings.edgeBrightnessSwipeSub,
                  checked = uiState.settings.edgeBrightnessSwipeEnabled,
                  onCheckedChange = viewModel::setEdgeBrightnessSwipeEnabled,
                  modifier = Modifier.testTag("settings_edge_brightness_switch"),
                )
                AtlasHairlineDivider()
                AtlasToggleRow(
                  title = strings.doubleTapBookmarkToggle,
                  subtitle = strings.doubleTapBookmarkSub,
                  checked = uiState.settings.doubleTapBookmarkEnabled,
                  onCheckedChange = viewModel::setDoubleTapBookmarkEnabled,
                  modifier = Modifier.testTag("settings_double_tap_bookmark_switch"),
                )
              }
            }
          }

          // SECTION 4: Reminders & Home Widgets
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.remindersAndWidgetsSectionTitle,
                icon = Icons.Rounded.NotificationsActive,
              )
              AtlasGroupedCard(contentPadding = 14.dp) {
                AtlasToggleRow(
                  title = strings.streakReminderToggle,
                  subtitle = strings.streakReminderSub,
                  checked = uiState.settings.streakReminderEnabled,
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
                  modifier = Modifier.testTag("settings_streak_reminder_switch"),
                )

                AtlasHairlineDivider()

                val formattedReminderTime =
                  String.format(
                    "%02d:%02d",
                    uiState.settings.streakReminderHour,
                    uiState.settings.streakReminderMinute,
                  )
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .size(34.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = strings.reminderTimeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text =
                          if (uiState.settings.streakReminderEnabled) strings.reminderScheduledStatus(formattedReminderTime)
                          else formattedReminderTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { showReminderTimeDialog = true },
                      shape = CircleShape,
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("settings_reminder_time_button"),
                    ) {
                      Text(formattedReminderTime, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                      shape = CircleShape,
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

                AtlasHairlineDivider()

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
                AtlasHairlineDivider()
                WidgetSettingRow(
                  icon = Icons.Rounded.BarChart,
                  title = strings.statsWidgetTitle,
                  subtitle = strings.statsWidgetSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_stats_widget_button",
                  onPinClick = { pinWidget(PageNestStatsWidgetReceiver::class.java) },
                )
                AtlasHairlineDivider()
                WidgetSettingRow(
                  icon = Icons.Rounded.LocalFireDepartment,
                  title = strings.streakWidgetTitle,
                  subtitle = strings.streakWidgetSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_streak_widget_button",
                  onPinClick = { pinWidget(PageNestStreakWidgetReceiver::class.java) },
                )
                AtlasHairlineDivider()
                WidgetSettingRow(
                  icon = Icons.Rounded.AutoStories,
                  title = strings.shelfWidgetTitle,
                  subtitle = strings.shelfWidgetSub,
                  canPin = canPinWidget,
                  pinLabel = strings.pinWidgetAction,
                  testTag = "settings_pin_shelf_widget_button",
                  onPinClick = { pinWidget(AtlasShelfWidgetReceiver::class.java) },
                )
              }
            }
          }

          // SECTION 5: Typefaces & Text-to-Speech
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.audioAndVoiceSectionTitle,
                icon = Icons.Rounded.RecordVoiceOver,
              )

              // Typefaces Card (with custom AtlasRadioDot!)
              AtlasGroupedCard(contentPadding = 14.dp) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = strings.readerTypefaces,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
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
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("import_font_button"),
                  ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.importLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                    subtitle = if (isFa) "قلم سفارشی" else "Custom Typeface",
                    sampleLine = "“Sphinx of black quartz” · «در ستایش خواندن آرام»",
                    fontFamily = customFamily,
                    selected = active,
                    canDelete = true,
                    onSelect = { viewModel.selectCustomFont(custom.id) },
                    onDelete = { viewModel.deleteCustomFont(custom.id) },
                  )
                }
              }

              // Text-to-Speech Engine Card
              AtlasGroupedCard(contentPadding = 14.dp) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = strings.voiceEngine,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    val selectedLabel =
                      uiState.ttsState.availableVoices
                        .firstOrNull { it.name == uiState.settings.ttsVoiceName }
                        ?.displayName ?: if (isFa) "صدای پیش‌فرض سیستم" else "System Default Voice"
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
                      shape = CircleShape,
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                      modifier = Modifier.testTag("select_tts_voice_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(strings.voiceQuickLabel, style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                      expanded = showVoicePicker,
                      onDismissRequest = { showVoicePicker = false },
                      containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                      DropdownMenuItem(
                        text = {
                          Text(
                            if (isFa) "صدای پیش‌فرض سیستم" else "System Default Voice",
                            color = MaterialTheme.colorScheme.onSurface,
                          )
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

                AtlasHairlineDivider()

                AtlasSliderRow(
                  label = strings.speechSpeed,
                  valueText = String.format("%.2fx", uiState.settings.ttsSpeechRate),
                  value = uiState.settings.ttsSpeechRate,
                  valueRange = 0.6f..2.0f,
                  onValueChange = viewModel::setTtsRate,
                )

                AtlasSliderRow(
                  label = strings.voicePitch,
                  valueText = String.format("%.2fx", uiState.settings.ttsPitch),
                  value = uiState.settings.ttsPitch,
                  valueRange = 0.7f..1.4f,
                  onValueChange = viewModel::setTtsPitch,
                )

                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(CircleShape)
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
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text =
                        if (!uiState.ttsState.isReady) {
                          if (isFa) "در حال آماده‌سازی موتور گفتار…" else "Voice Engine Initializing…"
                        } else if (uiState.ttsState.isPlaying) {
                          strings.stopPreview
                        } else {
                          strings.previewVoice
                        },
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
                }
              }
            }
          }

          // SECTION 6: Support, Storage & About ATLAS
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AtlasSectionHeader(
                title = strings.aboutAtlasSectionTitle,
                icon = Icons.Rounded.WorkspacePremium,
              )

              // Support & ATLAS Pro Card
              AtlasGroupedCard(contentPadding = 14.dp) {
                Surface(
                  shape = RoundedCornerShape(18.dp),
                  color =
                    if (uiState.settings.isProUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
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
                      .clip(RoundedCornerShape(18.dp))
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
                      Box(
                        modifier =
                          Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.WorkspacePremium,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp),
                        )
                      }
                      Spacer(modifier = Modifier.width(12.dp))
                      Column {
                        Text(
                          text =
                            if (uiState.settings.isProUnlocked) strings.proActiveTitle
                            else strings.proHeroTitle,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
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
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }

                AtlasHairlineDivider()

                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .size(34.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = strings.tipJarTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
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
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("settings_tip_jar_button"),
                  ) {
                    Text(strings.tipJarTitle, style = MaterialTheme.typography.labelMedium)
                  }
                }

                if (!uiState.settings.isProUnlocked) {
                  AtlasHairlineDivider()
                  Text(
                    text = strings.tipJarScheduleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = strings.tipJarScheduleSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TipPromptMode.entries.forEach { mode ->
                      val selected = uiState.settings.tipPromptMode == mode
                      AtlasRadioRow(
                        title = if (isFa) mode.faLabel else mode.label,
                        selected = selected,
                        onClick = { viewModel.setTipPromptMode(mode) },
                        modifier = Modifier.testTag("settings_tip_mode_${mode.name.lowercase()}"),
                      )
                    }
                  }
                  if (uiState.settings.tipPromptMode == TipPromptMode.FIXED_USAGE) {
                    AtlasSliderRow(
                      label = strings.tipJarFixedMinutesLabel(uiState.settings.tipFixedMinutes),
                      valueText = "${uiState.settings.tipFixedMinutes}m",
                      value = uiState.settings.tipFixedMinutes.toFloat(),
                      valueRange = 10f..120f,
                      onValueChange = { viewModel.setTipFixedMinutes(it.roundToInt()) },
                    )
                  }
                }
              }

              // Storage & Cache Card
              AtlasGroupedCard(contentPadding = 14.dp) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(
                      modifier =
                        Modifier
                          .size(34.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        strings.localStorage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text =
                          "${formatBytes(uiState.storageStats.booksSizeBytes)} · ${if (isFa) "کش" else "Cache"} ${formatBytes(uiState.storageStats.epubCacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  OutlinedButton(
                    onClick = viewModel::clearEpubCache,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("clear_cache_button"),
                  ) {
                    Text(strings.clearCache, style = MaterialTheme.typography.labelMedium)
                  }
                }
              }

              // About ATLAS & Creator Links Card
              AtlasGroupedCard(contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = strings.craftedBy,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = strings.aboutBio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                AtlasHairlineDivider()

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
                  platform = if (isFa) "وب‌سایت" else "Website",
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

  // Bilingual Changelog Dialog (v3.3 Build 10 + consolidated v3.2 with all former v6.0 features)
  if (showChangelogDialog) {
    AlertDialog(
      onDismissRequest = { showChangelogDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      shape = RoundedCornerShape(26.dp),
      title = {
        Text(
          text = "${strings.changelog} · ${strings.appName} v${BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (isFa) {
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۳ (بیلد ${BuildConfig.VERSION_CODE}) · هویت جدید اطلس (ATLAS)، بازطراحی کامل رابط کاربری و رفع باگ یادآورها",
              bullets =
                listOf(
                  "هویت جدید · تغییر نام کامل برنامه به «اطلس (ATLAS)» با شناسه بسته جدید atlas.mrmabrosia.ir، لوگوی آبی اطلس و پالت رنگی مدرن آبی کبالت و چینی/شب‌تاب.",
                  "بازطراحی کامل رابط کاربری · بازسازی سراسری ظاهر تمام بخش‌ها؛ «مرکز کنترل مطالعه» با نوار دایره‌ای پوسته‌ها و اسلایدرهای عمودی کپسولی برای اندازه قلم و روشنایی، «استودیوی پیشرفته تایپوگرافی» با پیش‌نمایش زنده متن و کارت‌های بصری فونت و ترازبندی، دکمه‌های رادیویی و سوییچ‌های اختصاصی و تنظیمات کاملاً دسته‌بندی‌شده (با حفظ هدر شیشه‌ای صفحه مطالعه).",
                  "جدید · ماژول «تحلیل و سرعت مطالعه» در صفحه اطلاعات کتاب: محاسبه سرعت مطالعه واقعی (واژه/دقیقه)، زمان باقی‌مانده کل کتاب و هر فصل همراه با نمودار تعاملی پیشرفت فصل‌ها.",
                  "رفع خطا · بازنویسی کامل سیستم ارسال اعلان یادآور روزانه با AlarmManager مقاوم در برابر حالت خواب سیستم (Doze) و زمان‌بندی مجدد خودکار پس از ری‌استارت گوشی تا یادآورها دقیقاً در ساعت انتخابی ارسال شوند.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۲ (بیلد ۵ تا ۹) · خط‌کش مطالعه، پخش‌کننده کتاب صوتی، صوت و تمرکز، ژست‌ها و فید OPDS",
              bullets =
                listOf(
                  "جدید · خط‌کش مطالعه و راهنمای خط: پوشش قابل جابه‌جایی در سه سبک (باند نوری، نوار رنگی، خط راهنما) با تنظیم ارتفاع، تیرگی اطراف و دستگیره کناری بدون اختلال در لمس و ورق زدن.",
                  "جدید · پخش‌کننده کتاب صوتی (پوشه، MP3 و M4B): وارد کردن فایل یا کل پوشه صوتی بدون نیاز به مجوز حافظه با مرتب‌سازی خودکار، تشخیص فصل‌های M4B، سرعت پخش ۰.۷۵ تا ۲.۵ برابر، کنترل صفحه قفل، تایمر خواب و صداهای محیطی.",
                  "جدید · همگام‌سازی کتاب و صوت: اتصال هر کتاب متنی (EPUB، PDF و…) به کتاب صوتی و انتقال یا پرش دقیق درصد پیشرفت بین هر دو نسخه.",
                  "جدید · تایمر خواب با محو شدن ملایم صدا در حالت شنیداری (۱۵ تا ۶۰ دقیقه یا پایان فصل) و کنترل پخش صفحه قفل و دکمه‌های هندزفری بلوتوث (MediaSession).",
                  "جدید · شخصی‌سازی نواحی لمس صفحه (چپ، مرکز، راست)، تنظیم فوری روشنایی با کشیدن لبه چپ صفحه و نشانک‌گذاری سریع با دو بار ضربه.",
                  "جدید · صداهای آرامش‌بخش محیطی استریو ۴۴.۱ کیلوهرتز (باران، شومینه، صدای قهوه‌ای، امواج اقیانوس آفلاین) و تایمر تمرکز مطالعه پومودورو.",
                  "جدید · کتابخانه کتاب‌های رایگان و کلاسیک آزاد (پروژه گوتنبرگ، Standard Ebooks و فیدهای سفارشی OPDS) همراه با استودیو طراحی جلد و مرتب‌سازی فصل‌ها در استودیو نویسندگی اطلس.",
                  "رفع خطا · بازطراحی منوی شناور انتخاب متن هماهنگ با تم مطالعه و رفع مشکل چیدمان فارسی/انگلیسی.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۱ · استودیو نویسندگی، ویجت‌های جدید و دقت بازگشت به صفحه",
              bullets =
                listOf(
                  "جدید · استودیو نویسندگی اطلس (آزمایشی): محیط نوشتن کتاب چندفصلی با پشتیبانی کامل از فارسی و انگلیسی، نوار ابزار مارک‌داون، ذخیره خودکار و انتشار یک‌لمسی به صورت EPUB.",
                  "جدید · ویجت آمار مطالعه (۴×۲) و ویجت ۱×۱ شعله رشته مطالعه با دسترسی مستقیم به داشبورد آمار.",
                  "بهبود و رفع خطا · رفع باگ پرش فصل با دکمه‌های صدا و ذخیره دقیق شماره صفحه خوانده‌شده هنگام بستن و باز کردن مجدد برنامه.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۳.۰ · قفسه‌ها، فرمت‌های گسترده، داشبورد آمار و حمایت",
              bullets =
                listOf(
                  "جدید · قفسه‌ها و دسته‌بندی‌های دلخواه، فیلتر وضعیت مطالعه، ویرایشگر مشخصات و جلد کتاب.",
                  "جدید · پشتیبانی بومی از فایل‌های TXT، Markdown (.md)، FictionBook (.fb2) و کمیک بوک (.cbz) در کنار EPUB و PDF.",
                  "جدید · داشبورد آمار مطالعه، یادآور روزانه، ویجت ادامه مطالعه، صندوق حمایت (Tip Jar) و نسخه حامیان Pro.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "نسخه ۲.۰ و ۱.۰ · هسته بومی مطالعه",
              bullets =
                listOf(
                  "موتور تماماً بومی Jetpack Compose برای مطالعه روان، حالت صرفه‌جویی باتری، بافت کاغذی، فونت‌های فارسی/انگلیسی سفارشی، هایلایت و یادداشت، ترجمه در لحظه، نشانک، جستجو و روخوانی صوتی آفلاین.",
                ),
            )
          } else {
            ChangelogVersionBlock(
              versionHeading = "v3.3 (Build ${BuildConfig.VERSION_CODE}) · ATLAS Identity, Full UI/UX Overhaul & Reminder Delivery Fix",
              bullets =
                listOf(
                  "REBRAND · Reborn as ATLAS (atlas.mrmabrosia.ir) with a fresh Cobalt Blue emblem and an architectural Cool Porcelain & Midnight Slate color system.",
                  "UI/UX REDESIGN · App-wide visual overhaul featuring a Bento Reader Control Center (circular theme swatches, dual vertical capsule scrubbers for text size & brightness), Live-Preview Advanced Typography Studio (visual Aa font cards & 2-column alignment cards), bespoke capsule toggles & concentric ring-dot radios, and cleanly sectioned Settings — while keeping the signature floating Liquid-Glass reader header.",
                  "NEW · Reading Insights on Book Info: measures real reading speed (WPM / pages per hour), calculates book & per-chapter time remaining, and visualizes chapter completion with an interactive progress chart.",
                  "BUG FIX · Rebuilt daily reading reminder scheduling with Doze-safe AlarmManager delivery, automatic next-day rescheduling, and boot/timezone recovery so notifications fire reliably at your chosen time.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v3.2 (Build 5–9) · Reading Ruler, Audiobook Sync, Listen Immersion, Gestures & OPDS",
              bullets =
                listOf(
                  "NEW · Reading Ruler / Line Guide Overlay: non-touch-blocking focus band (Spotlight, Tint Strip, or Underline Guide) with adjustable height, dim contrast, and ergonomic side handle.",
                  "NEW · Audiobook Folder / MP3 & M4B Player: zero-permission SAF file or folder import with natural track sorting, M4B embedded chapter parsing, variable speed (0.75×–2.5×), lock-screen controls, sleep fade-out & ambient sound mixing.",
                  "NEW · Companion Ebook ↔ Audiobook Sync: link any library ebook to an audiobook and push or pull exact percentage progress in either direction.",
                  "NEW · Sleep Timer & Gentle 15-second Fade-Out in Listen Mode plus native Lock-Screen Media Controls & Bluetooth earbud shortcuts via MediaSession.",
                  "NEW · Tap-Zone & Gesture Customization (Left / Center / Right), left-edge vertical brightness swipe, and double-tap quick bookmarking.",
                  "NEW · 44.1 kHz Stereo Offline Ambient Soundscapes (Rain, Fireplace, Brown Noise, Ocean Waves) & Pomodoro Focus Timer inside the Reader.",
                  "NEW · Free Public-Domain Book Browser (Project Gutenberg, Standard Ebooks & custom OPDS 1.2 feeds) plus Cover Studio & Chapter Reordering in ATLAS Studio.",
                  "BUG FIX · Redesigned text-selection popup in theme-matched Liquid Glass with clean RTL/LTR clamping across Persian and English.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v3.1 · Writing Studio, Widgets & Reader Precision",
              bullets =
                listOf(
                  "NEW · ATLAS Studio (Beta): offline multi-chapter book-writing studio with Markdown toolbar, RTL/LTR support, live autosave, Markdown export, and one-tap EPUB 3.0 publishing.",
                  "NEW · Reading Statistics Widget (4×2) & 1×1 Streak Flame Widget on the home screen.",
                  "FIX · Fixed Volume key chapter-skipping and persisted exact per-page resume position across restarts.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v3.0 · Collections, Formats, Statistics & Indie Support",
              bullets =
                listOf(
                  "NEW · Custom Shelves, Reading Status Filters, Book Metadata & Cover Editor, native TXT / Markdown / FB2 / CBZ engines, Reading Statistics Dashboard, Tip Jar & ATLAS Pro.",
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChangelogVersionBlock(
              versionHeading = "v2.0 & v1.0 · Native Reading Engine Foundation",
              bullets =
                listOf(
                  "CORE · 100% native Jetpack Compose reading engine, Battery-Saver Mode, Paper Texture, bilingual EN/FA UI, custom .ttf/.otf font importer, highlights, notes, translation, bookmarks, search, and Text-to-Speech.",
                ),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showChangelogDialog = false }) {
          Text(strings.done, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
      shape = RoundedCornerShape(26.dp),
      title = {
        Text(
          text = strings.setReminderTimeTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
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
          Text(strings.saveGoal, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
    shape = RoundedCornerShape(16.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    border =
      BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onSelect),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = name,
          fontFamily = fontFamily,
          fontSize = 15.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
      if (canDelete) {
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete $name",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      AtlasRadioDot(selected = selected)
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
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
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
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
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
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = handle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
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
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.weight(1f),
    ) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
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
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
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
      fontWeight = FontWeight.Bold,
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
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { expanded = true },
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
          fontWeight = FontWeight.Bold,
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
