package com.example.ui.settings

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.ReaderFontOption
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.toPalette
import java.io.File

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

  var showVoicePicker by remember { mutableStateOf(false) }
  var showChangelogDialog by remember { mutableStateOf(false) }

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
          text = strings.changelog,
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "New & Updated",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "• Added Battery-Saver Reading Mode (caps display refresh at 60Hz, simplifies animations, and bypasses GPU blur shaders)\n" +
                "• Added hardware Volume Key page turning (Volume Down / Up) for EPUB & PDF in Read mode\n" +
                "• Added 2 new themes (Sage Mist & Night B&W) and tactile Paper Texture toggle for EPUB & PDF\n" +
                "• Upgraded reader bottom & top bars to theme-responsive clear Liquid Glass and unified all typography/comfort settings inside TT\n" +
                "• Book-style Page-by-Page & Vertical Scroll modes, Daily Reading Goal tracker, and full Persian (فارسی) RTL support",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Bugs Fixed",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "• Fixed Persian (FA) library header typography, letter spacing, and RTL action layout spacing\n" +
                "• Replaced Chromium WebView with a 100% native Jetpack Compose EPUB reader engine — permanently eliminating renderer process termination crashes (aw_browser_terminator code -1) and open/scroll screen flashes\n" +
                "• Fixed text contrast across EPUB elements and Home/Settings surfaces on theme change",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showChangelogDialog = false }) {
          Text(strings.done, color = MaterialTheme.colorScheme.primary)
        }
      },
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

private fun formatBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format("%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format("%.2f MB", mb)
}
