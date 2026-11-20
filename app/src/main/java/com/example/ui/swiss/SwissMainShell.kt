package com.example.ui.swiss

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.data.local.BookEntity
import com.example.model.AppLanguage
import com.example.model.BookFormat
import com.example.ui.audiobook.AudiobookPlayerScreen
import com.example.ui.audiobook.AudiobookViewModel
import com.example.ui.bookinfo.BookInfoScreen
import com.example.ui.bookinfo.BookInfoViewModel
import com.example.ui.catalog.CatalogScreen
import com.example.ui.catalog.CatalogViewModel
import com.example.ui.library.LibraryViewModel
import com.example.ui.navigation.ROUTE_AUDIOBOOK
import com.example.ui.navigation.ROUTE_BOOK_INFO
import com.example.ui.navigation.ROUTE_CATALOG
import com.example.ui.navigation.ROUTE_READER
import com.example.ui.navigation.ROUTE_SETTINGS
import com.example.ui.navigation.ROUTE_STATS
import com.example.ui.navigation.ROUTE_WRITER_EDITOR
import com.example.ui.navigation.ROUTE_WRITER_STUDIO
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.stats.ReadingStatsScreen
import com.example.ui.stats.ReadingStatsViewModel
import com.example.ui.writer.WriterEditorScreen
import com.example.ui.writer.WriterEditorViewModel
import com.example.ui.writer.WriterStudioScreen
import com.example.ui.writer.WriterStudioViewModel
import com.example.widget.PageNestContinueWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Self-contained Swiss Monochrome Navigation Shell for ATLAS.
 * Uses Navigation Compose with 4 primary bottom destinations (Home, Library, Collections, Profile)
 * with state restoration, plus full access to the real Reader, Audiobook, Insights, Stats, Studio, and Catalog screens.
 */
@Composable
fun SwissAtlasMainShell(
  container: AppContainer,
  widgetBookId: String? = null,
  onConsumeWidgetBookId: () -> Unit = {},
  widgetRoute: String? = null,
  onConsumeWidgetRoute: () -> Unit = {},
) {
  val navController = rememberNavController()
  val context = LocalContext.current.applicationContext as Application
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val libraryViewModel: LibraryViewModel =
    viewModel(
      key = "swiss_shared_library_vm",
      factory =
        LibraryViewModel.Factory(
          libraryRepository = container.libraryRepository,
          readerRepository = container.readerRepository,
        ),
    )
  val settingsViewModel: SettingsViewModel =
    viewModel(
      key = "swiss_shared_settings_vm",
      factory =
        SettingsViewModel.Factory(
          context = context,
          readerRepository = container.readerRepository,
          fontManager = container.fontManager,
        ),
    )

  val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
  val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
  val isFa = libraryUiState.settings.appLanguage == AppLanguage.PERSIAN

  var showImportDialog by remember { mutableStateOf(false) }
  var bookForCollectionManage by remember { mutableStateOf<BookEntity?>(null) }

  val documentPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) libraryViewModel.importBookFromUri(uri)
    }
  val audioFilesPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      if (uris.isNotEmpty()) libraryViewModel.importAudioFilesFromUris(uris)
    }
  val audioFolderPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
      if (treeUri != null) libraryViewModel.importAudioFolderFromUri(treeUri)
    }

  LaunchedEffect(libraryUiState.errorMessage, libraryUiState.infoMessage, settingsUiState.message) {
    val msg = libraryUiState.errorMessage ?: libraryUiState.infoMessage ?: settingsUiState.message
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      libraryViewModel.clearMessages()
      settingsViewModel.clearMessage()
    }
  }

  val navigateToBook: (String) -> Unit = { targetBookId ->
    scope.launch {
      val book = container.readerRepository.getBookById(targetBookId) ?: return@launch
      val dest =
        if (book.bookFormat() == BookFormat.AUDIOBOOK) "audiobook/$targetBookId"
        else "reader/$targetBookId"
      navController.navigate(dest) { launchSingleTop = true }
    }
  }

  val navigateToBookInfo: (String) -> Unit = { targetBookId ->
    navController.navigate("book_info/$targetBookId") { launchSingleTop = true }
  }

  val navigateToBookAtChapter: (String, Int) -> Unit = { targetBookId, chapterIdx ->
    scope.launch {
      val book = container.readerRepository.getBookById(targetBookId) ?: return@launch
      val dest =
        if (book.bookFormat() == BookFormat.AUDIOBOOK) "audiobook/$targetBookId"
        else "reader/$targetBookId?chapter=$chapterIdx"
      navController.navigate(dest) { launchSingleTop = true }
    }
  }

  val selectBottomTab: (SwissNavTab) -> Unit = { tab ->
    navController.navigate(tab.route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  // Handle widget deep links cleanly in Swiss mode too
  LaunchedEffect(widgetBookId) {
    val targetId = widgetBookId
    if (!targetId.isNullOrBlank()) {
      onConsumeWidgetBookId()
      navigateToBook(targetId)
    }
  }
  LaunchedEffect(widgetRoute) {
    val targetRoute = widgetRoute
    if (!targetRoute.isNullOrBlank()) {
      onConsumeWidgetRoute()
      if (targetRoute == PageNestContinueWidget.ROUTE_STATS_DEEP_LINK) {
        navController.navigate("stats") { launchSingleTop = true }
      }
    }
  }

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val showBottomBar = SwissNavTab.entries.any { it.route == currentRoute }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      if (showBottomBar) {
        AtlasBottomNavigation(
          currentRoute = currentRoute,
          isPersian = isFa,
          onSelectTab = selectBottomTab,
        )
      }
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      NavHost(
        navController = navController,
        startDestination = SwissNavTab.HOME.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
      ) {
        composable(SwissNavTab.HOME.route) {
          SwissHomeScreen(
            uiState = libraryUiState,
            onOpenBook = navigateToBook,
            onOpenBookInfo = navigateToBookInfo,
            onNavigateToSearch = { selectBottomTab(SwissNavTab.LIBRARY) },
            onNavigateToProfile = { selectBottomTab(SwissNavTab.PROFILE) },
            onNavigateToLibrary = { selectBottomTab(SwissNavTab.LIBRARY) },
            onOpenStats = { navController.navigate("stats") { launchSingleTop = true } },
            onImportClick = { showImportDialog = true },
            onStartNewNote = {
              scope.launch {
                val stamp = SimpleDateFormat(if (isFa) "MM/dd" else "MMM d", Locale.US).format(Date())
                val created =
                  container.writerRepository.createManuscript(
                    title = if (isFa) "یادداشت $stamp" else "Note — $stamp",
                    author = if (isFa) "یادداشت شخصی" else "Personal Note",
                    synopsis = if (isFa) "ایجادشده از اقدام سریع اطلس" else "Created via ATLAS Quick Actions",
                    isRtl = isFa,
                  )
                navController.navigate("writer_editor/${created.id}") { launchSingleTop = true }
              }
            },
            onLoadSamples = libraryViewModel::loadSampleClassics,
          )
        }

        composable(SwissNavTab.LIBRARY.route) {
          SwissLibraryScreen(
            uiState = libraryUiState,
            onOpenBook = navigateToBook,
            onOpenBookInfo = navigateToBookInfo,
            onToggleLayout = libraryViewModel::toggleLayoutMode,
            onImportClick = { showImportDialog = true },
            onLoadSamples = libraryViewModel::loadSampleClassics,
            onDeleteBook = libraryViewModel::deleteBook,
            onManageCollections = { bookForCollectionManage = it },
          )
        }

        composable(SwissNavTab.COLLECTIONS.route) {
          SwissCollectionsScreen(
            uiState = libraryUiState,
            onOpenBook = navigateToBook,
            onCreateCollection = libraryViewModel::createShelf,
            onRenameCollection = libraryViewModel::renameShelf,
            onDeleteCollection = libraryViewModel::deleteShelf,
            onToggleBookInCollection = libraryViewModel::toggleBookShelf,
          )
        }

        composable(SwissNavTab.PROFILE.route) {
          SwissProfileScreen(
            settingsUiState = settingsUiState,
            libraryUiState = libraryUiState,
            settingsViewModel = settingsViewModel,
            onOpenStats = { navController.navigate("stats") { launchSingleTop = true } },
            onOpenStudio = { navController.navigate(ROUTE_WRITER_STUDIO) { launchSingleTop = true } },
            onOpenCatalog = { navController.navigate(ROUTE_CATALOG) { launchSingleTop = true } },
            onOpenAdvancedSettings = { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
          )
        }

        composable(
          route = ROUTE_READER,
          arguments =
            listOf(
              navArgument("bookId") { type = NavType.StringType },
              navArgument("chapter") {
                type = NavType.IntType
                defaultValue = -1
              },
            ),
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
          popEnterTransition = { EnterTransition.None },
          popExitTransition = { ExitTransition.None },
        ) { entry ->
          val bookId = entry.arguments?.getString("bookId").orEmpty()
          val initialChapter = entry.arguments?.getInt("chapter") ?: -1
          val readerViewModel: ReaderViewModel =
            viewModel(
              key = "swiss_reader_${bookId}_$initialChapter",
              factory =
                ReaderViewModel.Factory(
                  context = context,
                  bookId = bookId,
                  readerRepository = container.readerRepository,
                  epubParser = container.epubParser,
                  pdfEngine = container.pdfEngine,
                  textFormatsConverter = container.textFormatsConverter,
                  cbzEngine = container.cbzEngine,
                  fontManager = container.fontManager,
                  textTranslator = container.textTranslator,
                  initialChapterIndex = initialChapter,
                  readingInsightsEngine = container.readingInsightsEngine,
                ),
            )
          ReaderScreen(
            viewModel = readerViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenSettings = { selectBottomTab(SwissNavTab.PROFILE) },
            onOpenStats = { targetId ->
              navController.navigate("stats?bookId=$targetId") { launchSingleTop = true }
            },
            onOpenBookInfo = navigateToBookInfo,
          )
        }

        composable(
          route = ROUTE_BOOK_INFO,
          arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
          enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
          exitTransition = { fadeOut() },
          popEnterTransition = { fadeIn() },
          popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
        ) { entry ->
          val bookId = entry.arguments?.getString("bookId").orEmpty()
          val bookInfoViewModel: BookInfoViewModel =
            viewModel(
              key = "swiss_book_info_$bookId",
              factory =
                BookInfoViewModel.Factory(
                  bookId = bookId,
                  libraryRepository = container.libraryRepository,
                  readerRepository = container.readerRepository,
                  readingInsightsEngine = container.readingInsightsEngine,
                ),
            )
          BookInfoScreen(
            viewModel = bookInfoViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenBook = navigateToBook,
            onOpenBookAtChapter = navigateToBookAtChapter,
          )
        }

        composable(
          route = ROUTE_AUDIOBOOK,
          arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
          val bookId = entry.arguments?.getString("bookId").orEmpty()
          val audiobookViewModel: AudiobookViewModel =
            viewModel(
              key = "swiss_audiobook_$bookId",
              factory =
                AudiobookViewModel.Factory(
                  context = context,
                  bookId = bookId,
                  readerRepository = container.readerRepository,
                  libraryRepository = container.libraryRepository,
                  audiobookEngine = container.audiobookEngine,
                  readingInsightsEngine = container.readingInsightsEngine,
                ),
            )
          AudiobookPlayerScreen(
            viewModel = audiobookViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenCompanionBook = { companionId ->
              navController.navigate("reader/$companionId") { launchSingleTop = true }
            },
          )
        }

        composable(
          route = ROUTE_STATS,
          arguments =
            listOf(
              navArgument("bookId") {
                type = NavType.StringType
                defaultValue = ""
              }
            ),
        ) { entry ->
          val initialBookId = entry.arguments?.getString("bookId")?.takeIf { it.isNotBlank() }
          val statsViewModel: ReadingStatsViewModel =
            viewModel(
              key = "swiss_stats_${initialBookId ?: "library"}",
              factory =
                ReadingStatsViewModel.Factory(
                  initialBookId = initialBookId,
                  libraryRepository = container.libraryRepository,
                  readerRepository = container.readerRepository,
                ),
            )
          ReadingStatsScreen(
            viewModel = statsViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenBook = navigateToBook,
            onOpenBookInfo = navigateToBookInfo,
          )
        }

        composable(ROUTE_WRITER_STUDIO) {
          val studioViewModel: WriterStudioViewModel =
            viewModel(
              factory =
                WriterStudioViewModel.Factory(
                  writerRepository = container.writerRepository,
                  readerRepository = container.readerRepository,
                )
            )
          WriterStudioScreen(
            viewModel = studioViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenEditor = { id -> navController.navigate("writer_editor/$id") { launchSingleTop = true } },
            onOpenPublishedBook = { id -> navController.navigate("reader/$id") { launchSingleTop = true } },
          )
        }

        composable(
          route = ROUTE_WRITER_EDITOR,
          arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
          val manuscriptId = entry.arguments?.getString("bookId").orEmpty()
          val editorViewModel: WriterEditorViewModel =
            viewModel(
              key = "swiss_editor_$manuscriptId",
              factory =
                WriterEditorViewModel.Factory(
                  bookId = manuscriptId,
                  writerRepository = container.writerRepository,
                  readerRepository = container.readerRepository,
                )
            )
          WriterEditorScreen(
            viewModel = editorViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenPublishedBook = { id -> navController.navigate("reader/$id") { launchSingleTop = true } },
          )
        }

        composable(ROUTE_CATALOG) {
          val catalogViewModel: CatalogViewModel =
            viewModel(
              factory =
                CatalogViewModel.Factory(
                  catalogRepository = container.opdsCatalogRepository,
                  readerRepository = container.readerRepository,
                )
            )
          CatalogScreen(
            viewModel = catalogViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenImportedBook = { id -> navController.navigate("reader/$id") { launchSingleTop = true } },
          )
        }

        composable(ROUTE_SETTINGS) {
          SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { navController.popBackStack() },
          )
        }
      }
    }
  }

  // SAF Document Import Picker Dialog
  if (showImportDialog) {
    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = {
        Text(
          text = if (isFa) "افزودن به کتابخانه" else "Import to ATLAS",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      },
      text = {
        Column {
          AtlasSettingsItem(
            title = if (isFa) "سند یا کتاب الکترونیکی" else "Document or Ebook",
            subtitle = "EPUB, PDF, TXT, MD, FB2, CBZ",
            onClick = {
              showImportDialog = false
              documentPicker.launch(arrayOf("*/*"))
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "فایل‌های صوتی" else "Audiobook Files",
            subtitle = "MP3, M4B, M4A, OGG",
            onClick = {
              showImportDialog = false
              audioFilesPicker.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream"))
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          AtlasSettingsItem(
            title = if (isFa) "پوشه کتاب صوتی" else "Audiobook Folder",
            subtitle = if (isFa) "انتخاب کل پوشه با ترتیب فصل‌ها" else "Import a folder of chapters",
            onClick = {
              showImportDialog = false
              audioFolderPicker.launch(null)
            },
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showImportDialog = false }) {
          Text(if (isFa) "انصراف" else "Cancel", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }

  // Quick Collection Assignment Dialog from Library
  bookForCollectionManage?.let { target ->
    AlertDialog(
      onDismissRequest = { bookForCollectionManage = null },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(6.dp),
      title = {
        Text(
          text = if (isFa) "مجموعه‌های «${target.title}»" else "Collections for \"${target.title}\"",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      },
      text = {
        if (libraryUiState.shelves.isEmpty()) {
          Text(
            text = if (isFa) "ابتدا در تب مجموعه‌ها یک مجموعه ایجاد کنید." else "Create a collection first from the Collections tab.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Column(modifier = Modifier.fillMaxWidth()) {
            libraryUiState.shelves.forEachIndexed { idx, shelf ->
              val assigned = libraryUiState.bookShelfMap[target.id]?.contains(shelf.id) == true
              AtlasSettingsItem(
                title = shelf.name,
                valueText = if (assigned) (if (isFa) "عضو" else "Added") else (if (isFa) "افزودن" else "Add"),
                onClick = { libraryViewModel.toggleBookShelf(target.id, shelf.id, !assigned) },
              )
              if (idx < libraryUiState.shelves.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { bookForCollectionManage = null }) {
          Text(if (isFa) "بستن" else "Done", color = MaterialTheme.colorScheme.primary)
        }
      },
    )
  }
}
