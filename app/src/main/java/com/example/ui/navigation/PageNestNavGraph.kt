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
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first

const val ROUTE_LIBRARY = "library"
const val ROUTE_READER = "reader/{bookId}"
const val ROUTE_SETTINGS = "settings"

@Composable
fun PageNestNavGraph(container: AppContainer) {
  val navController = rememberNavController()
  val context = LocalContext.current.applicationContext as Application
  var initialRestoreDone by rememberSaveable { mutableStateOf(false) }

  // Restore last active reading session on cold start if enabled and book exists
  LaunchedEffect(Unit) {
    if (!initialRestoreDone) {
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
  }
}
