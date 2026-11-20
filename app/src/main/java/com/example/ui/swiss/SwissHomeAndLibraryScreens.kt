package com.example.ui.swiss

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.model.LibraryLayoutMode
import com.example.model.ReadingStatus
import com.example.ui.library.LibraryUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class SwissLibrarySort(val labelEn: String, val labelFa: String) {
  RECENTLY_OPENED("Recently Opened", "اخیراً خوانده‌شده"),
  RECENTLY_ADDED("Recently Added", "اخیراً اضافه‌شده"),
  TITLE("Title", "عنوان"),
  FILE_SIZE("File Size", "حجم فایل"),
}

@Composable
fun SwissHomeScreen(
  uiState: LibraryUiState,
  onOpenBook: (String) -> Unit,
  onOpenBookInfo: (String) -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onNavigateToLibrary: () -> Unit,
  onOpenStats: () -> Unit,
  onImportClick: () -> Unit,
  onStartNewNote: () -> Unit,
  onLoadSamples: () -> Unit,
) {
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val greeting = remember(isFa) { buildSwissGreeting(isFa) }
  val allBooks = uiState.allBooks
  val recentlyAdded = remember(allBooks) { allBooks.sortedByDescending { it.addedAt }.take(8) }
  val completedCount = remember(allBooks) {
    allBooks.count { it.status() == ReadingStatus.FINISHED || it.progressPercent >= 98f }
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(
      modifier = Modifier.fillMaxSize(),
    ) {
    AtlasTopBar(
      isPersian = isFa,
      showWordmark = true,
      subtitle = greeting,
      trailingActions = {
        IconButton(
          onClick = onNavigateToSearch,
          modifier = Modifier.size(38.dp).minimumInteractiveComponentSize().testTag("swiss_home_search"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = if (isFa) "جستجو" else "Search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
        IconButton(
          onClick = onNavigateToProfile,
          modifier = Modifier.size(38.dp).minimumInteractiveComponentSize().testTag("swiss_home_profile"),
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(32.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = if (isFa) "پروفایل" else "Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
              )
            }
          }
        }
      },
    )

    if (allBooks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AtlasEmptyState(
          title = if (isFa) "دنیای دانش شما آماده است" else "Your world of knowledge.",
          subtitle =
            if (isFa) "فایل‌های EPUB، PDF و متنی خود را وارد کنید تا مطالعه آرام و متمرکز را آغاز کنید."
            else "Import your EPUB, PDF, and reading documents to begin a quiet, organized personal library.",
          primaryActionLabel = if (isFa) "افزودن سند" else "Import Document",
          onPrimaryAction = onImportClick,
          secondaryActionLabel = if (isFa) "بارگذاری کتاب‌های نمونه" else "Load Sample Classics",
          onSecondaryAction = onLoadSamples,
        )
      }
    } else {
      BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().widthIn(max = 820.dp),
          contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
          if (uiState.isImporting) {
            item {
              LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
              )
            }
          }

          // 1. Continue Reading (Prominent Horizontal Section)
          val activeBook = uiState.continueBook
          if (activeBook != null) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AtlasSectionHeader(
                  title = if (isFa) "ادامه مطالعه" else "CONTINUE READING",
                  actionLabel = if (isFa) "جزئیات" else "Details",
                  onActionClick = { onOpenBookInfo(activeBook.id) },
                )
                SwissContinueReadingCard(
                  book = activeBook,
                  isPersian = isFa,
                  onClick = { onOpenBook(activeBook.id) },
                  modifier = Modifier.padding(horizontal = 20.dp),
                )
              }
            }
          }

          // 2. Recently Added (Horizontal Compact Collection)
          if (recentlyAdded.isNotEmpty()) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AtlasSectionHeader(
                  title = if (isFa) "اخیراً اضافه‌شده" else "RECENTLY ADDED",
                  actionLabel = if (isFa) "مشاهده همه" else "View All",
                  onActionClick = onNavigateToLibrary,
                )
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 20.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(recentlyAdded, key = { "swiss_added_${it.id}" }) { book ->
                    SwissCompactRecentCard(
                      book = book,
                      onClick = { onOpenBook(book.id) },
                    )
                  }
                }
              }
            }
          }

          // 3. Minimal Reading Activity Strip
          item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              AtlasSectionHeader(
                title = if (isFa) "فعالیت مطالعه" else "READING ACTIVITY",
                actionLabel = if (isFa) "آمار کامل" else "Statistics",
                onActionClick = onOpenStats,
              )
              SwissReadingActivityCard(
                totalMinutes = uiState.totalReadingMinutesAllTime,
                todayMinutes = uiState.todayReadingMinutes,
                completedDocuments = completedCount,
                streakDays = uiState.activeStreakDays,
                isPersian = isFa,
                onClick = onOpenStats,
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }
        }
      }
    }
    }

    SwissHomeQuickActionOverlay(
      continueBook = uiState.continueBook,
      allBooks = allBooks,
      isPersian = isFa,
      isImporting = uiState.isImporting,
      onImportFiles = onImportClick,
      onStartNewNote = onStartNewNote,
      onOpenLastRead = onOpenBook,
    )
  }
}

@Composable
private fun SwissContinueReadingCard(
  book: BookEntity,
  isPersian: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pct = book.progressPercent.roundToInt().coerceIn(0, 100)
  val lastOpenedLabel = remember(book.lastOpenedAt, book.addedAt, isPersian) {
    formatLastOpenedLabel(book.lastOpenedAt.takeIf { it > 0L } ?: book.addedAt, isPersian)
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SwissCoverFrame(
        book = book,
        modifier = Modifier.width(86.dp).height(124.dp),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "${book.bookFormat().name} · $lastOpenedLabel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
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

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AtlasProgressIndicator(
            progress = (book.progressPercent / 100f).coerceIn(0f, 1f),
            heightDp = 3,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick),
        ) {
          Text(
            text = if (isPersian) "ادامه مطالعه" else "Continue Reading",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SwissCompactRecentCard(
  book: BookEntity,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .width(106.dp)
        .clip(RoundedCornerShape(6.dp))
        .clickable(onClick = onClick),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    SwissCoverFrame(
      book = book,
      modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = book.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = book.bookFormat().name,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun SwissReadingActivityCard(
  totalMinutes: Int,
  todayMinutes: Int,
  completedDocuments: Int,
  streakDays: Int,
  isPersian: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val timeValue =
    if (totalMinutes >= 60) {
      val h = totalMinutes / 60
      val m = totalMinutes % 60
      if (isPersian) "${h}س ${m}د" else "${h}h ${m}m"
    } else {
      if (isPersian) "$totalMinutes دقیقه" else "${totalMinutes}m"
    }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SwissMetricCell(
        value = timeValue,
        label = if (isPersian) "زمان مطالعه" else "Reading Time",
        sub = if (isPersian) "امروز: $todayMinutes د" else "Today ${todayMinutes}m",
        modifier = Modifier.weight(1f),
      )
      VerticalDivider(
        modifier = Modifier.height(42.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )
      SwissMetricCell(
        value = completedDocuments.toString(),
        label = if (isPersian) "تکمیل‌شده" else "Completed",
        sub = null,
        modifier = Modifier.weight(1f),
      )
      VerticalDivider(
        modifier = Modifier.height(42.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )
      SwissMetricCell(
        value = if (isPersian) "$streakDays روز" else "${streakDays}d",
        label = if (isPersian) "رشته روزانه" else "Streak",
        sub = null,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun SwissMetricCell(
  value: String,
  label: String,
  sub: String?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (sub != null) {
      Text(
        text = sub,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        maxLines = 1,
      )
    }
  }
}

@Composable
fun SwissLibraryScreen(
  uiState: LibraryUiState,
  onOpenBook: (String) -> Unit,
  onOpenBookInfo: (String) -> Unit,
  onToggleLayout: () -> Unit,
  onImportClick: () -> Unit,
  onLoadSamples: () -> Unit,
  onDeleteBook: (BookEntity) -> Unit,
  onManageCollections: (BookEntity) -> Unit,
) {
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  var query by remember { mutableStateOf("") }
  var selectedFormat by remember { mutableStateOf<BookFormat?>(null) }
  var sortOrder by remember { mutableStateOf(SwissLibrarySort.RECENTLY_OPENED) }
  var showSortMenu by remember { mutableStateOf(false) }
  var actionBook by remember { mutableStateOf<BookEntity?>(null) }
  var deleteTarget by remember { mutableStateOf<BookEntity?>(null) }

  val availableFormats =
    remember(uiState.allBooks) {
      val present = uiState.allBooks.map { it.bookFormat() }.toSet()
      val base = listOf(BookFormat.EPUB, BookFormat.PDF, BookFormat.TXT, BookFormat.MD, BookFormat.FB2, BookFormat.CBZ, BookFormat.AUDIOBOOK)
      base.filter { it in present || it == BookFormat.EPUB || it == BookFormat.PDF }
    }

  val formatOptions: List<Pair<BookFormat?, String>> =
    remember(availableFormats, isFa) {
      buildList {
        add(null to if (isFa) "همه" else "All")
        availableFormats.forEach { fmt -> add(fmt to fmt.name) }
      }
    }

  val filteredBooks =
    remember(uiState.allBooks, query, selectedFormat, sortOrder) {
      val byFormat =
        if (selectedFormat == null) uiState.allBooks
        else uiState.allBooks.filter { it.bookFormat() == selectedFormat }
      val byQuery =
        if (query.isBlank()) byFormat
        else {
          val q = query.trim()
          byFormat.filter {
            it.title.contains(q, ignoreCase = true) ||
              it.author.contains(q, ignoreCase = true) ||
              it.format.contains(q, ignoreCase = true)
          }
        }
      when (sortOrder) {
        SwissLibrarySort.RECENTLY_OPENED ->
          byQuery.sortedByDescending { if (it.lastOpenedAt > 0L) it.lastOpenedAt else it.addedAt }
        SwissLibrarySort.RECENTLY_ADDED -> byQuery.sortedByDescending { it.addedAt }
        SwissLibrarySort.TITLE -> byQuery.sortedBy { it.title.lowercase() }
        SwissLibrarySort.FILE_SIZE -> byQuery.sortedByDescending { it.fileSizeBytes }
      }
    }

  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    AtlasTopBar(
      isPersian = isFa,
      title = if (isFa) "کتابخانه" else "Library",
      subtitle =
        if (isFa) "${uiState.allBooks.size} سند"
        else "${uiState.allBooks.size} ${if (uiState.allBooks.size == 1) "document" else "documents"}",
      trailingActions = {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(4.dp))
              .clickable(enabled = !uiState.isImporting, onClick = onImportClick)
              .testTag("swiss_import_button"),
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
              text = if (isFa) "افزودن" else "Import",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
      },
    )

    if (uiState.allBooks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AtlasEmptyState(
          title = if (isFa) "هنوز سندی وارد نشده است" else "No documents yet",
          subtitle =
            if (isFa) "فرمت‌های پشتیبانی‌شده: EPUB، PDF، TXT، Markdown، FB2، CBZ و کتاب‌های صوتی."
            else "Supported formats: EPUB, PDF, TXT, Markdown, FB2, CBZ, and Audiobooks.",
          primaryActionLabel = if (isFa) "افزودن سند" else "Import Document",
          onPrimaryAction = onImportClick,
          secondaryActionLabel = if (isFa) "بارگذاری کتاب‌های نمونه" else "Load Sample Classics",
          onSecondaryAction = onLoadSamples,
        )
      }
    } else {
      BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val gridCols =
          when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
          }

        LazyColumn(
          modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Search Field
          item {
            AtlasSearchField(
              query = query,
              placeholder = if (isFa) "جستجوی عنوان، نویسنده یا فرمت…" else "Search title, author, or format…",
              onQueryChange = { query = it },
              modifier = Modifier.padding(horizontal = 20.dp),
            )
          }

          // File-Type Filter Chips
          item {
            AtlasFilterChips(
              options = formatOptions,
              selected = selectedFormat,
              onSelect = { selectedFormat = it },
            )
          }

          // Sort + Grid/List Controls Row
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Box {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .clickable { showSortMenu = true },
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.Sort,
                      contentDescription = "Sort",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(15.dp),
                    )
                    Text(
                      text = if (isFa) sortOrder.labelFa else sortOrder.labelEn,
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                DropdownMenu(
                  expanded = showSortMenu,
                  onDismissRequest = { showSortMenu = false },
                  containerColor = MaterialTheme.colorScheme.surface,
                ) {
                  SwissLibrarySort.entries.forEach { order ->
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = if (isFa) order.labelFa else order.labelEn,
                          fontWeight = if (order == sortOrder) FontWeight.SemiBold else FontWeight.Normal,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      },
                      onClick = {
                        sortOrder = order
                        showSortMenu = false
                      },
                    )
                  }
                }
              }

              // Grid / List Toggle
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onToggleLayout),
              ) {
                Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  val isGrid = uiState.layoutMode == LibraryLayoutMode.GRID
                  Box(
                    modifier =
                      Modifier
                        .background(
                          if (isGrid) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.surface,
                          RoundedCornerShape(3.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.GridView,
                      contentDescription = "Grid",
                      tint =
                        if (isGrid) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                  Box(
                    modifier =
                      Modifier
                        .background(
                          if (!isGrid) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.surface,
                          RoundedCornerShape(3.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                      contentDescription = "List",
                      tint =
                        if (!isGrid) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
              }
            }
          }

          if (filteredBooks.isEmpty()) {
            item {
              AtlasEmptyState(
                title = if (isFa) "موردی یافت نشد" else "No matching documents",
                subtitle = if (isFa) "فیلتر یا عبارت جستجو را تغییر دهید." else "Try adjusting your filter or search query.",
              )
            }
          } else if (uiState.layoutMode == LibraryLayoutMode.GRID) {
            val rows = filteredBooks.chunked(gridCols)
            items(rows) { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowItems.forEach { book ->
                  AtlasDocumentCard(
                    book = book,
                    onClick = { onOpenBook(book.id) },
                    onMoreClick = { actionBook = book },
                    modifier = Modifier.weight(1f),
                  )
                }
                repeat(gridCols - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          } else {
            items(filteredBooks, key = { "swiss_list_${it.id}" }) { book ->
              AtlasDocumentRow(
                book = book,
                onClick = { onOpenBook(book.id) },
                onMoreClick = { actionBook = book },
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }
        }
      }
    }
  }

  // Minimal Document Action Dialog
  actionBook?.let { target ->
    AlertDialog(
      onDismissRequest = { actionBook = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = {
        Text(
          text = target.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          AtlasSettingsItem(
            title = if (isFa) "باز کردن سند" else "Open Document",
            onClick = {
              actionBook = null
              onOpenBook(target.id)
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "اطلاعات و تحلیل مطالعه" else "Document Info & Insights",
            onClick = {
              actionBook = null
              onOpenBookInfo(target.id)
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "مدیریت مجموعه‌ها" else "Manage Collections",
            onClick = {
              actionBook = null
              onManageCollections(target)
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "حذف از کتابخانه" else "Remove from Library",
            onClick = {
              actionBook = null
              deleteTarget = target
            },
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { actionBook = null }) {
          Text(if (isFa) "بستن" else "Close", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  deleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = { Text(if (isFa) "حذف سند؟" else "Remove Document?", color = MaterialTheme.colorScheme.primary) },
      text = {
        Text(
          text = if (isFa) "«${target.title}» از کتابخانه حذف خواهد شد." else "\"${target.title}\" will be removed from ATLAS.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteBook(target)
            deleteTarget = null
          },
        ) {
          Text(if (isFa) "حذف" else "Remove", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) {
          Text(if (isFa) "انصراف" else "Cancel", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }
}

private fun buildSwissGreeting(isPersian: Boolean): String {
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  return if (isPersian) {
    when {
      hour < 12 -> "صبح بخیر · دنیای دانش شما"
      hour < 18 -> "روز بخیر · دنیای دانش شما"
      else -> "عصر بخیر · دنیای دانش شما"
    }
  } else {
    when {
      hour < 12 -> "Good morning · Your world of knowledge."
      hour < 18 -> "Good afternoon · Your world of knowledge."
      else -> "Good evening · Your world of knowledge."
    }
  }
}

private fun formatLastOpenedLabel(timestampMs: Long, isPersian: Boolean): String {
  if (timestampMs <= 0L) return if (isPersian) "جدید" else "New"
  val diffMin = ((System.currentTimeMillis() - timestampMs) / 60_000L).coerceAtLeast(0L)
  return when {
    diffMin < 2 -> if (isPersian) "هم‌اکنون" else "Just now"
    diffMin < 60 -> if (isPersian) "$diffMin دقیقه پیش" else "${diffMin}m ago"
    diffMin < 1440 -> if (isPersian) "${diffMin / 60} ساعت پیش" else "${diffMin / 60}h ago"
    else -> {
      val fmt = SimpleDateFormat("MMM d", if (isPersian) Locale("fa") else Locale.US)
      fmt.format(Date(timestampMs))
    }
  }
}
