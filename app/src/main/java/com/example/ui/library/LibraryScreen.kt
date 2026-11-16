package com.example.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookEntity
import com.example.data.local.ShelfEntity
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.LibraryLayoutMode
import com.example.model.LibrarySortOrder
import com.example.model.LibraryStatusFilter
import com.example.model.ReadingStatus
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.monetization.PageNestProDialog
import com.example.ui.monetization.TipJarDialog
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
  viewModel: LibraryViewModel,
  onOpenBook: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenStats: () -> Unit = {},
  onOpenStudio: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val snackbarHostState = remember { SnackbarHostState() }

  var showSortMenu by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showGoalDialog by remember { mutableStateOf(false) }
  var showCreateShelfDialog by remember { mutableStateOf(false) }
  var showTipJarDialog by remember { mutableStateOf(false) }
  var showProDialog by remember { mutableStateOf(false) }
  var shelfPendingDelete by remember { mutableStateOf<ShelfEntity?>(null) }
  var bookPendingEdit by remember { mutableStateOf<BookEntity?>(null) }
  var bookPendingDelete by remember { mutableStateOf<BookEntity?>(null) }

  val documentPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.importBookFromUri(uri)
      }
    }

  val launchImportPicker = {
    documentPicker.launch(arrayOf("*/*"))
  }

  LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
    val msg = uiState.errorMessage ?: uiState.infoMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
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
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val gridColumns =
        when {
          maxWidth >= 840.dp -> 4
          maxWidth >= 600.dp -> 3
          else -> 2
        }

      Column(modifier = Modifier.fillMaxSize().widthIn(max = 900.dp)) {
        // Clean Two-Tier Editorial Bookstore Header (Solid Cream/Green or #121212/Green)
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            // Tier 1: Brand Emblem + Title + Primary Utility Actions
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = if (uiState.books.isNotEmpty()) 8.dp else 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 4.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(11.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.AutoStories,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = strings.appName,
                      fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                      fontSize = if (isFa) 22.sp else 25.sp,
                      lineHeight = if (isFa) 28.sp else 30.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onBackground,
                      letterSpacing = if (isFa) 0.sp else (-0.5).sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (uiState.settings.isProUnlocked) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier =
                          Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showProDialog = true }
                            .testTag("library_pro_badge"),
                      ) {
                        Text(
                          text = strings.proBadge,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    }
                  }
                  Text(
                    text =
                      if (uiState.totalLibraryCount == 0) strings.librarySubtitle
                      else strings.volumesCount(uiState.totalLibraryCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .clip(CircleShape)
                      .clickable(enabled = !uiState.isImporting) { launchImportPicker() }
                      .testTag("import_book_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Add,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = strings.importLabel,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onPrimary,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                }

                IconButton(
                  onClick = onOpenSettings,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("library_settings_button"),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp),
                      )
                    }
                  }
                }

                Box {
                  IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("library_more_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.MoreVert,
                      contentDescription = "More options",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    DropdownMenuItem(
                      text = { Text(strings.studioMenuAction, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                      onClick = {
                        showMoreMenu = false
                        onOpenStudio()
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(strings.readingStatsMenu, color = MaterialTheme.colorScheme.onSurface) },
                      onClick = {
                        showMoreMenu = false
                        onOpenStats()
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(strings.loadSamples, color = MaterialTheme.colorScheme.onSurface) },
                      onClick = {
                        showMoreMenu = false
                        viewModel.loadSampleClassics()
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(strings.tipJarMenu, color = MaterialTheme.colorScheme.onSurface) },
                      onClick = {
                        showMoreMenu = false
                        showTipJarDialog = true
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(strings.proMenu, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                      onClick = {
                        showMoreMenu = false
                        showProDialog = true
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(strings.settingsAndAbout, color = MaterialTheme.colorScheme.onSurface) },
                      onClick = {
                        showMoreMenu = false
                        onOpenSettings()
                      },
                    )
                  }
                }
              }
            }

            // Tier 2: Sort Selector Pill & Segmented Grid/List Switcher
            if (uiState.totalLibraryCount > 0) {
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Box {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier =
                      Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSortMenu = true }
                        .testTag("sort_menu_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort library",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Text(
                        text = if (isFa) uiState.sortOrder.faLabel else uiState.sortOrder.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                      )
                      Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                  DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    LibrarySortOrder.entries.forEach { order ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = if (isFa) order.faLabel else order.label,
                            fontWeight =
                              if (uiState.sortOrder == order) FontWeight.SemiBold
                              else FontWeight.Normal,
                            color =
                              if (uiState.sortOrder == order) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.onSurface,
                          )
                        },
                        onClick = {
                          viewModel.setSortOrder(order)
                          showSortMenu = false
                        },
                      )
                    }
                  }
                }

                // Segmented Grid / List Toggle Pill
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable(onClick = viewModel::toggleLayoutMode)
                      .testTag("toggle_layout_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    val isGrid = uiState.layoutMode == LibraryLayoutMode.GRID
                    Box(
                      modifier =
                        Modifier
                          .clip(RoundedCornerShape(9.dp))
                          .background(
                            if (isGrid) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                          )
                          .padding(horizontal = 10.dp, vertical = 5.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Grid view",
                        tint =
                          if (isGrid) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                    Box(
                      modifier =
                        Modifier
                          .clip(RoundedCornerShape(9.dp))
                          .background(
                            if (!isGrid) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                          )
                          .padding(horizontal = 10.dp, vertical = 5.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = "List view",
                        tint =
                          if (!isGrid) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        if (uiState.totalLibraryCount == 0 && !uiState.hasActiveFilterOrSearch) {
          EmptyLibraryView(
            isImporting = uiState.isImporting,
            onImportClick = launchImportPicker,
            onLoadSamplesClick = viewModel::loadSampleClassics,
            onOpenStudioClick = onOpenStudio,
            modifier =
              Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp),
            modifier =
              Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
          ) {
            item {
              AnimatedVisibility(visible = uiState.isImporting) {
                LinearProgressIndicator(
                  color = MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                )
              }
            }

            // Solid Clean Search Bar
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
              ) {
                OutlinedTextField(
                  value = uiState.searchQuery,
                  onValueChange = viewModel::setSearchQuery,
                  placeholder = {
                    Text(
                      strings.searchLibraryHint,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Rounded.Search,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                      IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                          Icons.Rounded.Clear,
                          contentDescription = "Clear search",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  },
                  singleLine = true,
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedTextColor = MaterialTheme.colorScheme.onSurface,
                      unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                      cursorColor = MaterialTheme.colorScheme.primary,
                      focusedContainerColor = Color.Transparent,
                      unfocusedContainerColor = Color.Transparent,
                      focusedBorderColor = Color.Transparent,
                      unfocusedBorderColor = Color.Transparent,
                    ),
                  modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
                )
              }
            }

            // Reading Status & Favorites Filter Chips + Custom Shelves Bar
            item {
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(LibraryStatusFilter.entries) { filter ->
                  val selected = uiState.selectedStatusFilter == filter
                  Surface(
                    shape = CircleShape,
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surface,
                    border =
                      BorderStroke(
                        width = 1.dp,
                        color =
                          if (selected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
                      ),
                    modifier =
                      Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.setStatusFilter(filter) }
                        .testTag("filter_status_${filter.name.lowercase()}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      if (filter == LibraryStatusFilter.FAVORITES) {
                        Icon(
                          imageVector = Icons.Rounded.Favorite,
                          contentDescription = null,
                          tint =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(13.dp),
                        )
                      }
                      Text(
                        text = if (isFa) filter.faLabel else filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color =
                          if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }
              }

              // Custom Shelves & Collections Row
              LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                item {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier =
                      Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenStudio)
                        .testTag("library_studio_chip"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                      )
                      Text(
                        text = strings.studioTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                      )
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                      ) {
                        Text(
                          text = strings.studioBetaBadge,
                          style = MaterialTheme.typography.labelSmall,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                      }
                    }
                  }
                }

                item {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier =
                      Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCreateShelfDialog = true }
                        .testTag("new_shelf_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp),
                      )
                      Text(
                        text = strings.newShelfAction,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }

                items(uiState.shelves, key = { "shelf_${it.id}" }) { shelf ->
                  val selected = uiState.selectedShelfId == shelf.id
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color =
                      if (selected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surface,
                    border =
                      BorderStroke(
                        width = 1.dp,
                        color =
                          if (selected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
                      ),
                    modifier =
                      Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectShelfFilter(shelf.id) }
                        .testTag("shelf_chip_${shelf.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(start = 11.dp, end = if (selected) 6.dp else 11.dp, top = 7.dp, bottom = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Bookmarks,
                        contentDescription = null,
                        tint =
                          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                      )
                      Text(
                        text = shelf.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color =
                          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                      )
                      if (selected) {
                        Icon(
                          imageVector = Icons.Rounded.Close,
                          contentDescription = "Delete shelf ${shelf.name}",
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier =
                            Modifier
                              .size(16.dp)
                              .clip(CircleShape)
                              .clickable { shelfPendingDelete = shelf },
                        )
                      }
                    }
                  }
                }
              }
            }

            // Daily Reading Goal Tracker Card
            if (!uiState.hasActiveFilterOrSearch) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                DailyReadingGoalCard(
                  todayMinutes = uiState.todayReadingMinutes,
                  todaySeconds = uiState.todayReadingSeconds,
                  dailyGoalMinutes = uiState.dailyGoalMinutes,
                  totalReadingMinutesAllTime = uiState.totalReadingMinutesAllTime,
                  progressFraction = uiState.dailyGoalProgressFraction,
                  goalMet = uiState.isDailyGoalMet,
                  onEditGoal = { showGoalDialog = true },
                  onOpenStats = onOpenStats,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp)
                      .testTag("daily_goal_card"),
                )
              }
            }

            // Continue Reading Hero Card
            if (!uiState.hasActiveFilterOrSearch && uiState.continueBook != null) {
              val heroBook = uiState.continueBook!!
              item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeading(
                  title =
                    if (uiState.lastSessionBookId == heroBook.id) strings.resumeSession
                    else strings.continueReading,
                )
                ContinueReadingCard(
                  book = heroBook,
                  onClick = { onOpenBook(heroBook.id) },
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp, vertical = 6.dp)
                      .testTag("continue_reading_card"),
                )
              }
            }

            // Recently Opened Rail
            if (!uiState.hasActiveFilterOrSearch && uiState.recentBooks.size > 1) {
              item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeading(title = strings.recentlyOpened)
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  items(uiState.recentBooks, key = { "recent_${it.id}" }) { book ->
                    RecentBookRailCard(book = book, onClick = { onOpenBook(book.id) })
                  }
                }
              }
            }

            // All Books Section
            item {
              Spacer(modifier = Modifier.height(14.dp))
              SectionHeading(
                title =
                  if (uiState.hasActiveFilterOrSearch) strings.matchingBooks
                  else strings.allBooks,
              )
            }

            if (uiState.books.isEmpty()) {
              item {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text =
                      if (uiState.searchQuery.isNotBlank()) strings.noMatchingBooks(uiState.searchQuery)
                      else strings.noBooksInFilter,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            } else if (uiState.layoutMode == LibraryLayoutMode.GRID) {
              val rows = uiState.books.chunked(gridColumns)
              items(rows) { rowBooks ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp),
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  rowBooks.forEach { book ->
                    BookGridCard(
                      book = book,
                      isFa = isFa,
                      onClick = { onOpenBook(book.id) },
                      onEditClick = { bookPendingEdit = book },
                      onToggleFavorite = { viewModel.toggleBookFavorite(book) },
                      onDeleteClick = { bookPendingDelete = book },
                      modifier = Modifier.weight(1f).testTag("book_card_${book.id}"),
                    )
                  }
                  repeat(gridColumns - rowBooks.size) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            } else {
              items(uiState.books, key = { "list_${it.id}" }) { book ->
                BookListRow(
                  book = book,
                  isFa = isFa,
                  onClick = { onOpenBook(book.id) },
                  onEditClick = { bookPendingEdit = book },
                  onToggleFavorite = { viewModel.toggleBookFavorite(book) },
                  onDeleteClick = { bookPendingDelete = book },
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp, vertical = 5.dp)
                      .testTag("book_row_${book.id}"),
                )
              }
            }
          }
        }
      }
    }
  }

  if (showGoalDialog) {
    SetDailyGoalDialog(
      currentTargetMinutes = uiState.dailyGoalMinutes,
      onSave = { minutes ->
        viewModel.setDailyGoalMinutes(minutes)
        showGoalDialog = false
      },
      onDismiss = { showGoalDialog = false },
    )
  }

  if (showCreateShelfDialog) {
    CreateShelfDialog(
      onCreate = { shelfName ->
        viewModel.createShelf(shelfName)
        showCreateShelfDialog = false
      },
      onDismiss = { showCreateShelfDialog = false },
    )
  }

  shelfPendingDelete?.let { shelf ->
    AlertDialog(
      onDismissRequest = { shelfPendingDelete = null },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(strings.deleteShelfTitle, color = MaterialTheme.colorScheme.onSurface) },
      text = { Text(strings.deleteShelfBody(shelf.name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteShelf(shelf)
            shelfPendingDelete = null
          }
        ) {
          Text(strings.remove, color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { shelfPendingDelete = null }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  bookPendingEdit?.let { target ->
    EditBookMetadataDialog(
      book = target,
      shelves = uiState.shelves,
      assignedShelfIds = uiState.bookShelfMap[target.id].orEmpty(),
      isFa = isFa,
      onToggleShelf = { shelfId, assign ->
        viewModel.toggleBookShelf(target.id, shelfId, assign)
      },
      onCreateShelfQuick = { showCreateShelfDialog = true },
      onSave = { title, author, status, isFavorite, pickedUri, resetCover ->
        viewModel.updateBookMetadataAndCover(
          book = target,
          newTitle = title,
          newAuthor = author,
          newStatus = status,
          isFavorite = isFavorite,
          pickedCoverUri = pickedUri,
          resetCover = resetCover,
        )
        bookPendingEdit = null
      },
      onDismiss = { bookPendingEdit = null },
    )
  }

  bookPendingDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { bookPendingDelete = null },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = {
        Text(strings.removeBookTitle, color = MaterialTheme.colorScheme.onSurface)
      },
      text = {
        Text(strings.removeBookBody(target.title), color = MaterialTheme.colorScheme.onSurfaceVariant)
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteBook(target)
            bookPendingDelete = null
          }
        ) {
          Text(strings.remove, color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { bookPendingDelete = null }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  val autoRecommendTip =
    uiState.shouldSuggestTipJar &&
      !showTipJarDialog &&
      !showProDialog &&
      !showGoalDialog &&
      !showCreateShelfDialog &&
      bookPendingEdit == null &&
      bookPendingDelete == null

  if (showTipJarDialog || autoRecommendTip) {
    val fromRecommend = autoRecommendTip && !showTipJarDialog
    TipJarDialog(
      totalReadingMinutes = uiState.totalReadingMinutesAllTime,
      isFromRecommendation = fromRecommend,
      isProUnlocked = uiState.settings.isProUnlocked,
      hasSupportedAlready = uiState.settings.hasSupportedViaTipJar,
      isFa = isFa,
      onMarkSupported = viewModel::markSupportedViaTipJar,
      onOpenProDialog = {
        if (fromRecommend) viewModel.dismissTipJarPrompt()
        showTipJarDialog = false
        showProDialog = true
      },
      onDismiss = {
        if (fromRecommend) {
          viewModel.dismissTipJarPrompt()
        }
        showTipJarDialog = false
      },
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
private fun DailyReadingGoalCard(
  todayMinutes: Int,
  todaySeconds: Int,
  dailyGoalMinutes: Int,
  totalReadingMinutesAllTime: Int,
  progressFraction: Float,
  goalMet: Boolean,
  onEditGoal: () -> Unit,
  onOpenStats: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val displayMins =
    if (todayMinutes == 0 && todaySeconds in 1..59) 1 else todayMinutes
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (goalMet) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenStats),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (goalMet) Icons.Rounded.CheckCircle else Icons.Rounded.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(19.dp),
            )
          }
          Column {
            Text(
              text = strings.dailyGoalTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (goalMet) strings.dailyGoalComplete
                else strings.dailyGoalProgressLabel(displayMins, dailyGoalMinutes),
              style = MaterialTheme.typography.bodySmall,
              color =
                if (goalMet) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenStats)
                .testTag("library_stats_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = strings.statsDashboardTitle,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = strings.openStatsAction,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                softWrap = false,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(onClick = onEditGoal)
                .testTag("edit_daily_goal_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = strings.adjustGoalAction,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }
      }

      LinearProgressIndicator(
        progress = { progressFraction },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.totalReadingTimeLabel(totalReadingMinutesAllTime),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${(progressFraction * 100f).roundToInt()}%",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetDailyGoalDialog(
  currentTargetMinutes: Int,
  onSave: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var selectedMinutes by remember { mutableIntStateOf(currentTargetMinutes.coerceIn(5, 120)) }
  val presets = listOf(10, 15, 20, 30, 45, 60)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurface,
    title = {
      Text(
        text = strings.setDailyGoalTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
          text = strings.dailyTargetMinutesLabel(selectedMinutes),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )

        Slider(
          value = selectedMinutes.toFloat(),
          onValueChange = { selectedMinutes = ((it / 5f).roundToInt() * 5).coerceIn(5, 120) },
          valueRange = 5f..120f,
          colors =
            SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          modifier = Modifier.fillMaxWidth().testTag("daily_goal_slider"),
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          presets.forEach { preset ->
            val active = selectedMinutes == preset
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
              border =
                BorderStroke(
                  width = 1.dp,
                  color =
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { selectedMinutes = preset }
                  .testTag("goal_preset_$preset"),
            ) {
              Text(
                text = "${preset}m",
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(selectedMinutes) },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag("save_daily_goal_button"),
      ) {
        Text(strings.saveGoal)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
  )
}

@Composable
private fun EmptyLibraryView(
  isImporting: Boolean,
  onImportClick: () -> Unit,
  onLoadSamplesClick: () -> Unit,
  onOpenStudioClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Column(
    modifier = modifier.padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_empty_library),
      contentDescription = "Minimalist book stack illustration",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .fillMaxWidth(0.76f)
          .widthIn(max = 280.dp)
          .aspectRatio(4f / 3f)
          .clip(RoundedCornerShape(24.dp)),
    )

    Spacer(modifier = Modifier.height(26.dp))

    Text(
      text = strings.emptyTitle,
      fontFamily = LiterataFontFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = strings.emptySubtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 360.dp),
    )

    Spacer(modifier = Modifier.height(26.dp))

    Button(
      onClick = onImportClick,
      enabled = !isImporting,
      shape = RoundedCornerShape(16.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
      modifier = Modifier.testTag("empty_import_button"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = strings.importEpubOrPdf,
        style = MaterialTheme.typography.labelLarge,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = strings.importFormatsHint,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
      onClick = onLoadSamplesClick,
      enabled = !isImporting,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      modifier = Modifier.testTag("load_samples_button"),
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoStories,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = strings.loadSamples,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    TextButton(
      onClick = onOpenStudioClick,
      modifier = Modifier.testTag("empty_studio_button"),
    ) {
      Icon(
        imageVector = Icons.Rounded.EditNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = strings.studioMenuAction,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun SectionHeading(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
  )
}

@Composable
private fun ContinueReadingCard(
  book: BookEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCoverThumbnail(
        book = book,
        modifier = Modifier.width(74.dp).height(108.dp),
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        FormatPill(format = book.bookFormat())
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = book.title,
          fontFamily = LiterataFontFamily,
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          LinearProgressIndicator(
            progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "${book.progressPercent.roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun RecentBookRailCard(
  book: BookEntity,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .width(104.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(bottom = 4.dp)
  ) {
    BookCoverThumbnail(
      book = book,
      modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = book.title,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "${book.progressPercent.roundToInt()}%",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun BookGridCard(
  book: BookEntity,
  isFa: Boolean,
  onClick: () -> Unit,
  onEditClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var showCardMenu by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Box(modifier = Modifier.fillMaxWidth()) {
        BookCoverThumbnail(
          book = book,
          modifier = Modifier.fillMaxWidth().aspectRatio(0.70f),
        )
        FormatPill(
          format = book.bookFormat(),
          modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
        )
        if (book.isFavorite) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.50f),
            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).size(24.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = strings.favoriteLabel,
                tint = Color(0xFFFF6B81),
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            modifier =
              Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { showCardMenu = true }
                .testTag("book_menu_${book.id}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Book options",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          DropdownMenu(
            expanded = showCardMenu,
            onDismissRequest = { showCardMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            DropdownMenuItem(
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              text = { Text(strings.editBookAction) },
              onClick = {
                showCardMenu = false
                onEditClick()
              },
            )
            DropdownMenuItem(
              leadingIcon = {
                Icon(
                  imageVector = if (book.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                  contentDescription = null,
                )
              },
              text = {
                Text(if (book.isFavorite) strings.removeFavoriteAction else strings.addFavoriteAction)
              },
              onClick = {
                showCardMenu = false
                onToggleFavorite()
              },
            )
            DropdownMenuItem(
              leadingIcon = {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              },
              text = { Text(strings.remove, color = MaterialTheme.colorScheme.error) },
              onClick = {
                showCardMenu = false
                onDeleteClick()
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = book.title,
        fontFamily = LiterataFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 19.sp,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        val st = book.status()
        if (st != ReadingStatus.UNREAD) {
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isFa) st.faLabel else st.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
      )
    }
  }
}

@Composable
private fun BookListRow(
  book: BookEntity,
  isFa: Boolean,
  onClick: () -> Unit,
  onEditClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var showRowMenu by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCoverThumbnail(
        book = book,
        modifier = Modifier.width(52.dp).height(76.dp),
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FormatPill(format = book.bookFormat())
          Text(
            text = "${book.progressPercent.roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          val st = book.status()
          Text(
            text = if (isFa) st.faLabel else st.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (book.isFavorite) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = strings.favoriteLabel,
              tint = Color(0xFFFF6B81),
              modifier = Modifier.size(13.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = book.title,
          fontFamily = LiterataFontFamily,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box {
        IconButton(
          onClick = { showRowMenu = true },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Book options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DropdownMenu(
          expanded = showRowMenu,
          onDismissRequest = { showRowMenu = false },
          containerColor = MaterialTheme.colorScheme.surface,
        ) {
          DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            text = { Text(strings.editBookAction) },
            onClick = {
              showRowMenu = false
              onEditClick()
            },
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(
                imageVector = if (book.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
              )
            },
            text = {
              Text(if (book.isFavorite) strings.removeFavoriteAction else strings.addFavoriteAction)
            },
            onClick = {
              showRowMenu = false
              onToggleFavorite()
            },
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            text = { Text(strings.remove, color = MaterialTheme.colorScheme.error) },
            onClick = {
              showRowMenu = false
              onDeleteClick()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun BookCoverThumbnail(
  book: BookEntity,
  modifier: Modifier = Modifier,
) {
  val coverFile = book.coverPath?.let { File(it) }?.takeIf { it.exists() }
  val shape = RoundedCornerShape(12.dp)

  if (coverFile != null) {
    AsyncImage(
      model = coverFile,
      contentDescription = "${book.title} cover",
      contentScale = ContentScale.Crop,
      modifier = modifier.clip(shape),
    )
  } else {
    // Solid flat editorial cloth colors (no gradients)
    val clothColors =
      listOf(
        Color(0xFF2D6A4F), // Botanical Forest Green
        Color(0xFF7D4838), // Terracotta Brick
        Color(0xFF295264), // Deep Slate Teal
        Color(0xFF5D5342), // Espresso Linen
      )
    val clothColor = clothColors[abs(book.title.hashCode()) % clothColors.size]
    Box(
      modifier =
        modifier
          .clip(shape)
          .background(clothColor)
          .padding(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxHeight()
            .width(2.dp)
            .background(Color.White.copy(alpha = 0.16f))
      )
      Column(
        modifier = Modifier.fillMaxSize().padding(start = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = book.title,
          fontFamily = LiterataFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFF6F2E9),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 15.sp,
        )
        Text(
          text = book.author,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFF6F2E9).copy(alpha = 0.72f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun FormatPill(
  format: BookFormat,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = modifier,
  ) {
    Text(
      text = format.name,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
    )
  }
}
