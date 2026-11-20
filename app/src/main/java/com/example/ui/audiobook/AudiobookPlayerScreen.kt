package com.example.ui.audiobook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.engine.audiobook.AudiobookEngine
import com.example.model.AmbientSoundType
import com.example.model.AppLanguage
import com.example.model.SleepTimerPreset
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.toPalette
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudiobookPlayerScreen(
  viewModel: AudiobookViewModel,
  onNavigateBack: () -> Unit,
  onOpenCompanionBook: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val palette = uiState.settings.themeMode.toPalette()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN

  var showTracksSheet by remember { mutableStateOf(false) }
  var showSleepAudioSheet by remember { mutableStateOf(false) }
  var showCompanionPicker by remember { mutableStateOf(false) }
  var isScrubbing by remember { mutableStateOf(false) }
  var scrubFraction by remember { mutableFloatStateOf(0f) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, viewModel) {
    val obs = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        viewModel.flushAndSaveProgressNow()
      }
    }
    lifecycleOwner.lifecycle.addObserver(obs)
    onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
  }

  LaunchedEffect(uiState.toastMessage) {
    if (uiState.toastMessage != null) {
      delay(2200)
      viewModel.clearToast()
    }
  }

  BackHandler {
    when {
      showCompanionPicker -> showCompanionPicker = false
      showTracksSheet -> showTracksSheet = false
      showSleepAudioSheet -> showSleepAudioSheet = false
      else -> {
        viewModel.flushAndSaveProgressNow()
        onNavigateBack()
      }
    }
  }

  val onAccent = if (palette.isDark) Color(0xFF071630) else Color.White

  Scaffold(
    containerColor = palette.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(palette.background),
      contentAlignment = Alignment.TopCenter,
    ) {
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.accent)
          }
        }
        uiState.errorMessage != null || uiState.manifest == null -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = uiState.errorMessage ?: strings.unableToOpen,
              style = MaterialTheme.typography.titleMedium,
              color = palette.text,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onNavigateBack,
              colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = onAccent),
            ) {
              Text(strings.returnToLibrary)
            }
          }
        }
        else -> {
          val manifest = uiState.manifest!!
          val pb = uiState.playback
          val currentTrack = manifest.tracks.getOrNull(pb.currentTrackIndex)
          val trackDur = pb.trackDurationMs.coerceAtLeast(1L)
          val activePosMs = if (isScrubbing) (scrubFraction * trackDur).roundToLong() else pb.positionInTrackMs

          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            // Top Bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              IconButton(
                onClick = {
                  viewModel.flushAndSaveProgressNow()
                  onNavigateBack()
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = strings.returnToLibrary,
                  tint = palette.text,
                )
              }

              Column(
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = manifest.title,
                  fontFamily = LiterataFontFamily,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.text,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = manifest.author,
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.secondaryText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { showTracksSheet = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_chapters_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ListAlt,
                    contentDescription = strings.audiobookChaptersTitle,
                    tint = palette.text,
                  )
                }
                val toolsActive =
                  uiState.sleepTimer.preset != SleepTimerPreset.OFF ||
                    uiState.activeAmbientSound != AmbientSoundType.NONE
                IconButton(
                  onClick = { showSleepAudioSheet = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_sleep_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = strings.focusAndAudioTitle,
                    tint = if (toolsActive) palette.accent else palette.text,
                  )
                }
              }
            }

            // Hero Cover Card
            Surface(
              shape = RoundedCornerShape(26.dp),
              color = palette.dockSurface,
              border = BorderStroke(1.dp, palette.dockBorder),
              shadowElevation = 6.dp,
              modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth(0.68f).aspectRatio(1f),
            ) {
              val coverFile = (manifest.coverPath ?: uiState.book?.coverPath)?.let { File(it) }?.takeIf { it.exists() }
              if (coverFile != null) {
                AsyncImage(
                  model = coverFile,
                  contentDescription = "${manifest.title} cover",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Box(
                  modifier =
                    Modifier
                      .fillMaxSize()
                      .background(
                        Brush.linearGradient(
                          listOf(
                            palette.accent.copy(alpha = 0.24f),
                            palette.dockSurface,
                            palette.accent.copy(alpha = 0.12f),
                          )
                        )
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Headphones,
                      contentDescription = null,
                      tint = palette.accent,
                      modifier = Modifier.size(54.dp),
                    )
                    Text(
                      text = manifest.title,
                      fontFamily = LiterataFontFamily,
                      style = MaterialTheme.typography.titleMedium,
                      color = palette.text,
                      textAlign = TextAlign.Center,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }

            // Current Track Info + Overall Book Progress
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = palette.accent.copy(alpha = 0.14f),
              ) {
                Text(
                  text = strings.audiobookTrackOf(pb.currentTrackIndex + 1, manifest.tracks.size.coerceAtLeast(1)),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.accent,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
              }
              Text(
                text = currentTrack?.title ?: manifest.title,
                fontFamily = LiterataFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }

            // Chapter Scrubber
            Column(modifier = Modifier.fillMaxWidth()) {
              Slider(
                value = if (isScrubbing) scrubFraction else (activePosMs.toFloat() / trackDur.toFloat()).coerceIn(0f, 1f),
                onValueChange = { next ->
                  isScrubbing = true
                  scrubFraction = next.coerceIn(0f, 1f)
                },
                onValueChangeFinished = {
                  viewModel.seekTrackPosition((scrubFraction * trackDur).roundToLong())
                  isScrubbing = false
                },
                colors =
                  SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.dockBorder,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("audiobook_seek_slider"),
              )
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = AudiobookEngine.formatDurationClock(activePosMs),
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.secondaryText,
                )
                Text(
                  text = "${uiState.overallProgressPercent.roundToInt()}% · ${AudiobookEngine.formatDurationClock(manifest.totalDurationMs)}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Medium,
                  color = palette.accent,
                )
                Text(
                  text = "-${AudiobookEngine.formatDurationClock((trackDur - activePosMs).coerceAtLeast(0L))}",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.secondaryText,
                )
              }
            }

            // Primary Transport Bar (-track, -15s, Play/Pause, +30s, +track)
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              IconButton(
                onClick = viewModel::previousTrack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_prev_track"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipPrevious,
                  contentDescription = "Previous track",
                  tint = palette.text,
                  modifier = Modifier.size(28.dp),
                )
              }

              IconButton(
                onClick = { viewModel.skipSeconds(-15) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_rewind_15"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Replay10,
                  contentDescription = "Rewind 15s",
                  tint = palette.text,
                  modifier = Modifier.size(28.dp),
                )
              }

              Surface(
                shape = CircleShape,
                color = palette.accent,
                shadowElevation = 6.dp,
                modifier =
                  Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable(onClick = viewModel::togglePlayPause)
                    .testTag("audiobook_play_pause_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (pb.isPlaying) "Pause" else "Play",
                    tint = onAccent,
                    modifier = Modifier.size(34.dp),
                  )
                }
              }

              IconButton(
                onClick = { viewModel.skipSeconds(30) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_forward_30"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Forward30,
                  contentDescription = "Forward 30s",
                  tint = palette.text,
                  modifier = Modifier.size(28.dp),
                )
              }

              IconButton(
                onClick = viewModel::nextTrack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("audiobook_next_track"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipNext,
                  contentDescription = "Next track",
                  tint = palette.text,
                  modifier = Modifier.size(28.dp),
                )
              }
            }

            // Playback Speed Pills
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                val selected = kotlin.math.abs(pb.playbackSpeed - speed) < 0.04f
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color =
                    if (selected) palette.accent.copy(alpha = 0.18f)
                    else palette.dockSurface,
                  border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { viewModel.setPlaybackSpeed(speed) }
                      .testTag("audiobook_speed_${speed.toString().replace('.', '_')}"),
                ) {
                  Text(
                    text = String.format("%.2g×", speed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) palette.accent else palette.text,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                  )
                }
              }
            }

            // Companion Ebook <-> Audiobook Progress Sync Card
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = palette.dockSurface,
              border = BorderStroke(1.dp, palette.dockBorder),
              modifier = Modifier.fillMaxWidth().testTag("companion_sync_card"),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Sync,
                      contentDescription = null,
                      tint = palette.accent,
                      modifier = Modifier.size(18.dp),
                    )
                    Column {
                      Text(
                        text = strings.companionSyncTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                      )
                      Text(
                        text = strings.companionSyncSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText,
                      )
                    }
                  }
                  TextButton(
                    onClick = { showCompanionPicker = true },
                    modifier = Modifier.testTag("audiobook_sync_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Link,
                      contentDescription = null,
                      tint = palette.accent,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.linkCompanionBookAction, color = palette.accent)
                  }
                }

                val companion = uiState.companionBook
                if (companion == null) {
                  Text(
                    text = strings.noCompanionBookLinked,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText,
                  )
                } else {
                  HorizontalDivider(color = palette.divider)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = companion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text =
                          "${strings.audiobookTitle}: ${uiState.overallProgressPercent.roundToInt()}%  ·  ${companion.format}: ${companion.progressPercent.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                      )
                    }
                    IconButton(onClick = { viewModel.linkCompanionBook(null) }) {
                      Icon(
                        imageVector = Icons.Rounded.LinkOff,
                        contentDescription = strings.unlinkCompanionBookAction,
                        tint = palette.secondaryText,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }

                  LinearProgressIndicator(
                    progress = { (companion.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = palette.accent,
                    trackColor = palette.controlSurface,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                  )

                  FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = viewModel::pullProgressFromCompanionBook,
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, palette.dockBorder),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      modifier = Modifier.testTag("pull_from_companion_button"),
                    ) {
                      Text(strings.pullFromBookAction, style = MaterialTheme.typography.labelMedium, color = palette.text)
                    }
                    OutlinedButton(
                      onClick = viewModel::pushProgressToCompanionBook,
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, palette.dockBorder),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      modifier = Modifier.testTag("push_to_companion_button"),
                    ) {
                      Text(strings.pushToBookAction, style = MaterialTheme.typography.labelMedium, color = palette.text)
                    }
                    Button(
                      onClick = {
                        viewModel.pause()
                        onOpenCompanionBook(companion.id)
                      },
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = onAccent),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      modifier = Modifier.testTag("open_companion_book_button"),
                    ) {
                      Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(strings.openCompanionBookAction, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
          }
        }
      }

      // Transient Toast
      AnimatedVisibility(
        visible = uiState.toastMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 64.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = palette.dockSurface,
          border = BorderStroke(1.dp, palette.dockBorder),
          shadowElevation = 6.dp,
        ) {
          Text(
            text = uiState.toastMessage.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = palette.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }
    }
  }

  // Tracks & Chapters Sheet
  if (showTracksSheet && uiState.manifest != null) {
    val tracks = uiState.manifest!!.tracks
    val currentIdx = uiState.playback.currentTrackIndex
    ModalBottomSheet(
      onDismissRequest = { showTracksSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = palette.dockSurface,
      contentColor = palette.text,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 520.dp).padding(bottom = 20.dp),
      ) {
        Text(
          text = strings.audiobookChaptersTitle,
          style = MaterialTheme.typography.titleMedium,
          color = palette.text,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        HorizontalDivider(color = palette.divider)
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
          itemsIndexed(tracks, key = { idx, t -> "${idx}_${t.title}" }) { idx, track ->
            val active = idx == currentIdx
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.selectTrack(idx, autoPlay = true)
                    showTracksSheet = false
                  }
                  .background(if (active) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
                  .padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 10.dp),
              ) {
                Text(
                  text = "${idx + 1}.",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (active) palette.accent else palette.secondaryText,
                  modifier = Modifier.width(28.dp),
                )
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (active) palette.accent else palette.text,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = AudiobookEngine.formatDurationClock(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) palette.accent else palette.secondaryText,
              )
            }
          }
        }
      }
    }
  }

  // Sleep Timer & Ambient Soundscapes Sheet
  if (showSleepAudioSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSleepAudioSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = palette.dockSurface,
      contentColor = palette.text,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Text(strings.sleepTimerLabel, style = MaterialTheme.typography.titleMedium, color = palette.text)
          }
          if (uiState.sleepTimer.preset != SleepTimerPreset.OFF) {
            val badge =
              if (uiState.sleepTimer.preset == SleepTimerPreset.END_OF_CHAPTER) {
                if (isFa) uiState.sleepTimer.preset.faLabel else uiState.sleepTimer.preset.label
              } else {
                String.format(
                  "%02d:%02d",
                  uiState.sleepTimer.remainingSeconds / 60,
                  uiState.sleepTimer.remainingSeconds % 60,
                )
              }
            Text(badge, style = MaterialTheme.typography.labelLarge, color = palette.accent)
          }
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SleepTimerPreset.entries.forEach { preset ->
            val selected = uiState.sleepTimer.preset == preset
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.controlSurface.copy(alpha = 0.72f),
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { viewModel.selectSleepTimer(preset) },
            ) {
              Text(
                text = if (isFa) preset.faLabel else preset.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        HorizontalDivider(color = palette.divider)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
          Text(strings.ambientSoundscapesLabel, style = MaterialTheme.typography.titleSmall, color = palette.text)
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AmbientSoundType.entries.forEach { sound ->
            val selected = uiState.activeAmbientSound == sound
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.controlSurface.copy(alpha = 0.72f),
              border = BorderStroke(1.dp, if (selected) palette.accent else palette.dockBorder),
              modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { viewModel.selectAmbientSound(sound) },
            ) {
              Text(
                text = if (isFa) sound.faLabel else sound.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.accent else palette.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        if (uiState.activeAmbientSound != AmbientSoundType.NONE) {
          Slider(
            value = uiState.settings.ambientVolume,
            onValueChange = viewModel::setAmbientVolume,
            valueRange = 0.05f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = palette.accent,
              activeTrackColor = palette.accent,
              inactiveTrackColor = palette.controlSurface,
            ),
          )
        }
      }
    }
  }

  // Link Companion Ebook Dialog
  if (showCompanionPicker) {
    AlertDialog(
      onDismissRequest = { showCompanionPicker = false },
      containerColor = palette.dockSurface,
      titleContentColor = palette.text,
      textContentColor = palette.secondaryText,
      title = {
        Text(
          text = strings.selectCompanionBookTitle,
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = {
        if (uiState.eligibleCompanionBooks.isEmpty()) {
          Text(strings.noEligibleCompanionBooks)
        } else {
          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            uiState.eligibleCompanionBooks.forEach { candidate ->
              val linked = uiState.companionBook?.id == candidate.id
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (linked) palette.accent.copy(alpha = 0.16f) else palette.controlSurface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (linked) palette.accent else palette.dockBorder),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      viewModel.linkCompanionBook(candidate.id)
                      showCompanionPicker = false
                    },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                      text = candidate.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = palette.text,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${candidate.format} · ${candidate.progressPercent.roundToInt()}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.secondaryText,
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCompanionPicker = false }) {
          Text(strings.done, color = palette.accent)
        }
      },
    )
  }
}
