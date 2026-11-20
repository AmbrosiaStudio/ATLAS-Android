package com.example.ui.swiss

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.BookEntity
import com.example.data.local.ShelfEntity
import com.example.model.AppLanguage
import com.example.model.ReadingLayoutMode
import com.example.model.ReadingThemeMode
import com.example.ui.library.LibraryUiState
import com.example.ui.settings.SettingsUiState
import com.example.ui.settings.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SwissCollectionsScreen(
  uiState: LibraryUiState,
  onOpenBook: (String) -> Unit,
  onCreateCollection: (String) -> Unit,
  onRenameCollection: (Long, String) -> Unit,
  onDeleteCollection: (ShelfEntity) -> Unit,
  onToggleBookInCollection: (String, Long, Boolean) -> Unit,
) {
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  var activeCollectionId by remember { mutableStateOf<Long?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }
  var optionsShelf by remember { mutableStateOf<ShelfEntity?>(null) }
  var renameShelfTarget by remember { mutableStateOf<ShelfEntity?>(null) }
  var deleteShelfTarget by remember { mutableStateOf<ShelfEntity?>(null) }
  var showManageDocsDialog by remember { mutableStateOf(false) }

  val activeCollection = remember(uiState.shelves, activeCollectionId) {
    uiState.shelves.firstOrNull { it.id == activeCollectionId }
  }

  if (activeCollection != null) {
    BackHandler { activeCollectionId = null }
    val memberBooks =
      remember(uiState.allBooks, uiState.bookShelfMap, activeCollection.id) {
        uiState.allBooks.filter { uiState.bookShelfMap[it.id]?.contains(activeCollection.id) == true }
      }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
      AtlasTopBar(
        isPersian = isFa,
        title = activeCollection.name,
        subtitle =
          if (isFa) "${memberBooks.size} سند"
          else "${memberBooks.size} ${if (memberBooks.size == 1) "document" else "documents"}",
        onNavigateBack = { activeCollectionId = null },
        trailingActions = {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { showManageDocsDialog = true },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (isFa) "ویرایش اسناد" else "Documents",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        },
      )

      if (memberBooks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          AtlasEmptyState(
            title = if (isFa) "مجموعه خالی است" else "Collection is empty",
            subtitle = if (isFa) "اسناد کتابخانه را به این مجموعه اضافه کنید." else "Add documents from your library into this collection.",
            primaryActionLabel = if (isFa) "افزودن اسناد" else "Add Documents",
            onPrimaryAction = { showManageDocsDialog = true },
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(memberBooks, key = { "col_doc_${it.id}" }) { book ->
            AtlasDocumentRow(
              book = book,
              onClick = { onOpenBook(book.id) },
              trailingContent = {
                TextButton(
                  onClick = { onToggleBookInCollection(book.id, activeCollection.id, false) },
                ) {
                  Text(
                    text = if (isFa) "حذف" else "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              },
            )
          }
        }
      }
    }

    if (showManageDocsDialog) {
      SwissCollectionDocsDialog(
        collection = activeCollection,
        allBooks = uiState.allBooks,
        bookShelfMap = uiState.bookShelfMap,
        isPersian = isFa,
        onToggle = { bookId, assign -> onToggleBookInCollection(bookId, activeCollection.id, assign) },
        onDismiss = { showManageDocsDialog = false },
      )
    }
  } else {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
      AtlasTopBar(
        isPersian = isFa,
        title = if (isFa) "مجموعه‌ها" else "Collections",
        subtitle =
          if (isFa) "${uiState.shelves.size} مجموعه"
          else "${uiState.shelves.size} ${if (uiState.shelves.size == 1) "collection" else "collections"}",
        trailingActions = {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { showCreateDialog = true }
                .testTag("swiss_new_collection_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (isFa) "جدید" else "New",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        },
      )

      if (uiState.shelves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          AtlasEmptyState(
            title = if (isFa) "مجموعه‌ای ایجاد نشده است" else "No collections yet",
            subtitle =
              if (isFa) "کتاب‌ها و اسناد خود را بر اساس موضوع، پژوهش یا علاقه دسته‌بندی کنید."
              else "Organize your documents into curated topics, research archives, or reading lists.",
            primaryActionLabel = if (isFa) "ساخت مجموعه جدید" else "Create Collection",
            onPrimaryAction = { showCreateDialog = true },
          )
        }
      } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          val cols = if (maxWidth >= 640.dp) 3 else 2
          val rows = uiState.shelves.chunked(cols)
          LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(rows) { rowShelves ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowShelves.forEach { shelf ->
                  val docs =
                    uiState.allBooks.filter { uiState.bookShelfMap[it.id]?.contains(shelf.id) == true }
                  AtlasCollectionCard(
                    name = shelf.name,
                    documentCount = docs.size,
                    representativeBooks = docs.take(3),
                    isPersian = isFa,
                    onClick = { activeCollectionId = shelf.id },
                    onMoreClick = { optionsShelf = shelf },
                    modifier = Modifier.weight(1f),
                  )
                }
                repeat(cols - rowShelves.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    SwissNameInputDialog(
      title = if (isFa) "مجموعه جدید" else "New Collection",
      initialText = "",
      confirmLabel = if (isFa) "ایجاد" else "Create",
      isPersian = isFa,
      onConfirm = { name ->
        onCreateCollection(name)
        showCreateDialog = false
      },
      onDismiss = { showCreateDialog = false },
    )
  }

  optionsShelf?.let { shelf ->
    AlertDialog(
      onDismissRequest = { optionsShelf = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = { Text(shelf.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
      text = {
        Column {
          AtlasSettingsItem(
            title = if (isFa) "تغییر نام مجموعه" else "Rename Collection",
            onClick = {
              optionsShelf = null
              renameShelfTarget = shelf
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "حذف مجموعه" else "Delete Collection",
            onClick = {
              optionsShelf = null
              deleteShelfTarget = shelf
            },
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { optionsShelf = null }) {
          Text(if (isFa) "بستن" else "Close", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  renameShelfTarget?.let { shelf ->
    SwissNameInputDialog(
      title = if (isFa) "تغییر نام مجموعه" else "Rename Collection",
      initialText = shelf.name,
      confirmLabel = if (isFa) "ذخیره" else "Save",
      isPersian = isFa,
      onConfirm = { newName ->
        onRenameCollection(shelf.id, newName)
        renameShelfTarget = null
      },
      onDismiss = { renameShelfTarget = null },
    )
  }

  deleteShelfTarget?.let { shelf ->
    AlertDialog(
      onDismissRequest = { deleteShelfTarget = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = { Text(if (isFa) "حذف مجموعه؟" else "Delete Collection?", color = MaterialTheme.colorScheme.primary) },
      text = {
        Text(
          text =
            if (isFa) "مجموعه «${shelf.name}» حذف می‌شود اما اسناد شما در کتابخانه باقی می‌مانند."
            else "\"${shelf.name}\" will be deleted. Your documents will remain in your library.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteCollection(shelf)
            deleteShelfTarget = null
          },
        ) {
          Text(if (isFa) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteShelfTarget = null }) {
          Text(if (isFa) "انصراف" else "Cancel", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }
}

@Composable
private fun SwissNameInputDialog(
  title: String,
  initialText: String,
  confirmLabel: String,
  isPersian: Boolean,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var value by remember { mutableStateOf(initialText) }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(6.dp),
    title = { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        placeholder = { Text(if (isPersian) "نام مجموعه" else "Collection name") },
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(
        onClick = { if (value.trim().isNotEmpty()) onConfirm(value.trim()) },
        enabled = value.trim().isNotEmpty(),
      ) {
        Text(confirmLabel, color = MaterialTheme.colorScheme.primary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isPersian) "انصراف" else "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
  )
}

@Composable
private fun SwissCollectionDocsDialog(
  collection: ShelfEntity,
  allBooks: List<BookEntity>,
  bookShelfMap: Map<String, Set<Long>>,
  isPersian: Boolean,
  onToggle: (String, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(6.dp),
    title = {
      Text(
        text = if (isPersian) "اسناد ${collection.name}" else "Documents in ${collection.name}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    },
    text = {
      if (allBooks.isEmpty()) {
        Text(
          text = if (isPersian) "سندی در کتابخانه وجود ندارد." else "No documents in your library yet.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        LazyColumn(modifier = Modifier.height(280.dp)) {
          items(allBooks, key = { "pick_doc_${it.id}" }) { book ->
            val assigned = bookShelfMap[book.id]?.contains(collection.id) == true
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { onToggle(book.id, !assigned) }
                  .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Checkbox(
                checked = assigned,
                onCheckedChange = { onToggle(book.id, it) },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                  ),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = book.title,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                )
                Text(
                  text = book.author,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(if (isPersian) "تایید" else "Done", color = MaterialTheme.colorScheme.primary)
      }
    },
  )
}

@Composable
fun SwissProfileScreen(
  settingsUiState: SettingsUiState,
  libraryUiState: LibraryUiState,
  settingsViewModel: SettingsViewModel,
  onOpenStats: () -> Unit,
  onOpenStudio: () -> Unit,
  onOpenCatalog: () -> Unit,
  onOpenAdvancedSettings: () -> Unit,
) {
  val settings = settingsUiState.settings
  val isFa = settings.appLanguage == AppLanguage.PERSIAN
  val isDark = settings.themeMode.isDark
  val totalStorageBytes = settingsUiState.storageStats.booksSizeBytes + settingsUiState.storageStats.epubCacheBytes

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    AtlasTopBar(
      isPersian = isFa,
      title = if (isFa) "پروفایل" else "Profile",
      subtitle = if (isFa) "تنظیمات و هویت محلی" else "Local reader & preferences",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        // Local Account & Brand Header Card
        item {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              AtlasIconSpecimen(darkVariant = isDark, size = 54.dp)
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = if (isFa) "کتابخانه شخصی اطلس" else "Personal ATLAS Archive",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text =
                    if (isFa) "${libraryUiState.allBooks.size} سند · ${libraryUiState.totalReadingMinutesAllTime} دقیقه مطالعه · محلی و آفلاین"
                    else "${libraryUiState.allBooks.size} documents · ${libraryUiState.totalReadingMinutesAllTime}m read · Offline local storage",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Group 1: Appearance & Reading Themes
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "ظاهر و پوسته" else "APPEARANCE")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              AtlasSettingsItem(
                title = if (isFa) "حالت تیره" else "Dark Appearance",
                subtitle = if (isFa) "پس‌زمینه مشکی #111111" else "Monochrome dark (#111111)",
                trailingContent = {
                  Switch(
                    checked = isDark,
                    onCheckedChange = { dark ->
                      settingsViewModel.setThemeMode(
                        if (dark) ReadingThemeMode.DARK else ReadingThemeMode.PAPER
                      )
                    },
                    colors =
                      SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                      ),
                  )
                },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = if (isFa) "پوسته صفحه مطالعه" else "Reading Surface Theme",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                val swissThemes =
                  listOf(
                    Triple(ReadingThemeMode.PAPER, if (isFa) "روشن" else "Light", Color(0xFFF5F5F2)),
                    Triple(ReadingThemeMode.SEPIA, if (isFa) "کاغذ گرم" else "Warm Paper", Color(0xFFF4ECDF)),
                    Triple(ReadingThemeMode.DARK, if (isFa) "تیره" else "Dark", Color(0xFF1B1B1B)),
                    Triple(ReadingThemeMode.AMOLED, if (isFa) "مشکی" else "Black", Color(0xFF000000)),
                  )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  swissThemes.forEach { (mode, label, swatch) ->
                    val selected = settings.themeMode == mode
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border =
                        BorderStroke(
                          width = if (selected) 1.8.dp else 1.dp,
                          color =
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                      modifier =
                        Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(4.dp))
                          .clickable { settingsViewModel.setThemeMode(mode) },
                    ) {
                      Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Box(
                          modifier =
                            Modifier
                              .size(22.dp)
                              .background(swatch, RoundedCornerShape(3.dp))
                              .then(
                                Modifier.padding(1.dp)
                              ),
                          contentAlignment = Alignment.Center,
                        ) {
                          if (selected) {
                            Icon(
                              imageVector = Icons.Rounded.Check,
                              contentDescription = null,
                              tint = if (mode.isDark) Color.White else Color.Black,
                              modifier = Modifier.size(13.dp),
                            )
                          }
                        }
                        Text(
                          text = label,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                          color = MaterialTheme.colorScheme.primary,
                          maxLines = 1,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Group 2: Reading Preferences
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "تنظیمات مطالعه" else "READING PREFERENCES")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              AtlasSettingsItem(
                title = if (isFa) "حالت نمایش صفحات" else "Page Navigation Mode",
                valueText =
                  if (settings.readingLayoutMode == ReadingLayoutMode.PAGED)
                    (if (isFa) "صفحه‌بندی کتابی" else "Paged")
                  else (if (isFa) "پیمایش عمودی" else "Vertical Scroll"),
                onClick = {
                  val next =
                    if (settings.readingLayoutMode == ReadingLayoutMode.PAGED) ReadingLayoutMode.SCROLL
                    else ReadingLayoutMode.PAGED
                  settingsViewModel.setReadingLayoutMode(next)
                },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "بیدار نگه داشتن صفحه" else "Keep Screen Awake",
                trailingContent = {
                  Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = settingsViewModel::setKeepScreenOn,
                    colors =
                      SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                      ),
                  )
                },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "ورق زدن با کلید صدا" else "Volume Key Page Turn",
                trailingContent = {
                  Switch(
                    checked = settings.volumeKeyPageTurnEnabled,
                    onCheckedChange = settingsViewModel::setVolumeKeyPageTurnEnabled,
                    colors =
                      SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                      ),
                  )
                },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "بازگشت خودکار به آخرین کتاب" else "Restore Last Reading Session",
                trailingContent = {
                  Switch(
                    checked = settings.restoreLastSession,
                    onCheckedChange = settingsViewModel::setRestoreLastSession,
                    colors =
                      SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                      ),
                  )
                },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "تنظیمات پیشرفته، یادآور و فونت‌ها" else "Advanced Settings, Reminders & Fonts",
                subtitle = if (isFa) "هدف روزانه، اعلان رشته، نواحی لمس، فونت سفارشی و تغییرات نسخه" else "Daily goal, streak notifications, tap zones, custom fonts & changelog",
                onClick = onOpenAdvancedSettings,
              )
            }
          }
        }

        // Group 3: Language
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "زبان برنامه" else "LANGUAGE")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AppLanguage.entries.forEach { lang ->
                  val active = settings.appLanguage == lang
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier =
                      Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { settingsViewModel.setAppLanguage(lang) },
                  ) {
                    Box(
                      modifier = Modifier.padding(vertical = 10.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = lang.nativeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Group 4: Workspace Tools
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "ابزارها و کشف" else "TOOLS & DISCOVERY")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              AtlasSettingsItem(
                title = if (isFa) "آمار و نمودارهای مطالعه" else "Reading Statistics",
                subtitle = if (isFa) "رشته روزانه، نمودار هفتگی و تاریخچه" else "Daily streak, 7-day trend & history",
                onClick = onOpenStats,
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "استودیو نویسندگی اطلس" else "ATLAS Writing Studio",
                subtitle = if (isFa) "نگارش، ویرایش و انتشار EPUB" else "Write, edit & publish EPUB manuscripts",
                onClick = onOpenStudio,
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "کتابخانه آزاد بین‌المللی" else "Free Public-Domain Catalog",
                subtitle = if (isFa) "گوتنبرگ و کتاب‌های کلاسیک استاندارد" else "Project Gutenberg & Standard Ebooks",
                onClick = onOpenCatalog,
              )
            }
          }
        }

        // Group 5: Storage Management
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "حافظه و داده‌ها" else "STORAGE")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              AtlasSettingsItem(
                title = if (isFa) "فضای کتابخانه" else "Library Storage",
                valueText = formatBytesCompact(totalStorageBytes),
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "پاکسازی کش فایل‌ها" else "Clear Extracted Cache",
                subtitle = formatBytesCompact(settingsUiState.storageStats.epubCacheBytes),
                onClick = settingsViewModel::clearEpubCache,
              )
            }
          }
        }

        // Group 6: About ATLAS & Identity Specimen
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasSectionHeader(title = if (isFa) "درباره اطلس" else "ABOUT ATLAS")
            SwissGroupedSection(modifier = Modifier.padding(horizontal = 20.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  AtlasHorizontalWordmark(isPersian = isFa, symbolSize = 20.dp)
                  Text(
                    text = if (isFa) "دنیای دانش شما." else "Your world of knowledge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  AtlasIconSpecimen(darkVariant = false, size = 38.dp)
                  AtlasIconSpecimen(darkVariant = true, size = 38.dp)
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "نسخه برنامه" else "Version",
                valueText = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AtlasSettingsItem(
                title = if (isFa) "شناسه بسته" else "Package",
                valueText = BuildConfig.APPLICATION_ID,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SwissGroupedSection(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      content()
    }
  }
}
