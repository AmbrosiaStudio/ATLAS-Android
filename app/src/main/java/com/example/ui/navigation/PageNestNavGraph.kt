package com.example.ui.navigation

import android.app.Application
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.ui.catalog.CatalogScreen
import com.example.ui.catalog.CatalogViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
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
import kotlinx.coroutines.flow.first

const val ROUTE_LIBRARY = "library"
const val ROUTE_READER = "reader/{bookId}"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_STATS = "stats?bookId={bookId}"
const val ROUTE_WRITER_STUDIO = "writer_studio"
const val ROUTE_WRITER_EDITOR = "writer_editor/{bookId}"
const val ROUTE_CATALOG = "catalog"

@Composable
fun PageNestNavGraph(
  container: AppContainer,
  widgetBookId: String? = null,
  onConsumeWidgetBookId: () -> Unit = {},
  widgetRoute: String? = null,
  onConsumeWidgetRoute: () -> Unit = {},
) {
  val navController = rememberNavController()
  val context = LocalContext.current.applicationContext as Application
  var initialRestoreDone by rememberSaveable { mutableStateOf(false) }

  // Open requested book directly when launched from Home Screen "Continue Reading" Widget
  LaunchedEffect(widgetBookId) {
    val targetId = widgetBookId
    if (!targetId.isNullOrBlank()) {
      initialRestoreDone = true
      val bookExists = container.readerRepository.getBookById(targetId) != null
      onConsumeWidgetBookId()
      if (bookExists) {
        navController.navigate("reader/$targetId") {
          launchSingleTop = true
        }
      }
    }
  }

  // Open Statistics Dashboard directly when launched from Stats or 1x1 Streak Home Widgets
  LaunchedEffect(widgetRoute) {
    val targetRoute = widgetRoute
    if (!targetRoute.isNullOrBlank()) {
      initialRestoreDone = true
      onConsumeWidgetRoute()
      if (targetRoute == PageNestContinueWidget.ROUTE_STATS_DEEP_LINK) {
        navController.navigate("stats") {
          launchSingleTop = true
        }
      }
    }
  }

  // Restore last active reading session on cold start if enabled and book exists
  LaunchedEffect(Unit) {
    if (!initialRestoreDone && widgetBookId.isNullOrBlank() && widgetRoute.isNullOrBlank()) {
      initialRestoreDone = true
      val settings = container.readerRepository.settingsFlow.first()
      val lastBookId = container.readerRepository.lastOpenedBookIdFlow.first()
      if (settings.restoreLastSession && !lastBookId.isNullOrBlank()) {
        val bookExists = container.readerRepository.getBookById(lastBookId) != null
        if (bookExists) {
          navController.navigate("reader/$lastBookId") {
            launchSingleTop = true
          }
        } else {
          container.readerRepository.setLastOpenedBookId(null)
        }
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = ROUTE_LIBRARY,
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() },
  ) {
    composable(route = ROUTE_LIBRARY) {
      val libraryViewModel: LibraryViewModel =
        viewModel(
          factory =
            LibraryViewModel.Factory(
              libraryRepository = container.libraryRepository,
              readerRepository = container.readerRepository,
            )
        )
      LibraryScreen(
        viewModel = libraryViewModel,
        onOpenBook = { bookId ->
          navController.navigate("reader/$bookId") {
            launchSingleTop = true
          }
        },
        onOpenSettings = {
          navController.navigate(ROUTE_SETTINGS) {
            launchSingleTop = true
          }
        },
        onOpenStats = {
          navController.navigate("stats") {
            launchSingleTop = true
          }
        },
        onOpenStudio = {
          navController.navigate(ROUTE_WRITER_STUDIO) {
            launchSingleTop = true
          }
        },
        onOpenCatalog = {
          navController.navigate(ROUTE_CATALOG) {
            launchSingleTop = true
          }
        },
      )
    }

    composable(
      route = ROUTE_READER,
      arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
      enterTransition = { EnterTransition.None },
      exitTransition = { ExitTransition.None },
      popEnterTransition = { EnterTransition.None },
      popExitTransition = { ExitTransition.None },
    ) { backStackEntry ->
      val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
      val readerViewModel: ReaderViewModel =
        viewModel(
          key = "reader_$bookId",
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
            ),
        )
      ReaderScreen(
        viewModel = readerViewModel,
        onNavigateBack = { navController.popBackStack() },
        onOpenSettings = {
          navController.navigate(ROUTE_SETTINGS) {
            launchSingleTop = true
          }
        },
        onOpenStats = { targetBookId ->
          navController.navigate("stats?bookId=$targetBookId") {
            launchSingleTop = true
          }
        },
      )
    }

    composable(
      route = ROUTE_SETTINGS,
      enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
      exitTransition = { fadeOut() },
      popEnterTransition = { fadeIn() },
      popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
    ) {
      val settingsViewModel: SettingsViewModel =
        viewModel(
          factory =
            SettingsViewModel.Factory(
              context = context,
              readerRepository = container.readerRepository,
              fontManager = container.fontManager,
            )
        )
      SettingsScreen(
        viewModel = settingsViewModel,
        onNavigateBack = { navController.popBackStack() },
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
      enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
      exitTransition = { fadeOut() },
      popEnterTransition = { fadeIn() },
      popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
    ) { backStackEntry ->
      val initialBookId = backStackEntry.arguments?.getString("bookId")?.takeIf { it.isNotBlank() }
      val statsViewModel: ReadingStatsViewModel =
        viewModel(
          key = "stats_${initialBookId ?: "library"}",
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
        onOpenBook = { bookId ->
          navController.navigate("reader/$bookId") {
            launchSingleTop = true
          }
        },
      )
    }

    composable(
      route = ROUTE_WRITER_STUDIO,
      enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
      exitTransition = { fadeOut() },
      popEnterTransition = { fadeIn() },
      popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
    ) {
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
        onOpenEditor = { manuscriptId ->
          navController.navigate("writer_editor/$manuscriptId") {
            launchSingleTop = true
          }
        },
        onOpenPublishedBook = { libBookId ->
          navController.navigate("reader/$libBookId") {
            launchSingleTop = true
          }
        },
      )
    }

    composable(
      route = ROUTE_WRITER_EDITOR,
      arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
      enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
      exitTransition = { fadeOut() },
      popEnterTransition = { fadeIn() },
      popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
    ) { backStackEntry ->
      val manuscriptId = backStackEntry.arguments?.getString("bookId").orEmpty()
      val editorViewModel: WriterEditorViewModel =
        viewModel(
          key = "writer_editor_$manuscriptId",
          factory =
            WriterEditorViewModel.Factory(
              bookId = manuscriptId,
              writerRepository = container.writerRepository,
              readerRepository = container.readerRepository,
            ),
        )
      WriterEditorScreen(
        viewModel = editorViewModel,
        onNavigateBack = { navController.popBackStack() },
        onOpenPublishedBook = { libBookId ->
          navController.navigate("reader/$libBookId") {
            launchSingleTop = true
          }
        },
      )
    }

    composable(
      route = ROUTE_CATALOG,
      enterTransition = { slideInHorizontally { it / 8 } + fadeIn() },
      exitTransition = { fadeOut() },
      popEnterTransition = { fadeIn() },
      popExitTransition = { slideOutHorizontally { it / 8 } + fadeOut() },
    ) {
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
        onOpenImportedBook = { bookId ->
          navController.navigate("reader/$bookId") {
            launchSingleTop = true
          }
        },
      )
    }
  }
}
