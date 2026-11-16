package com.example

import android.app.Application
import com.example.data.local.PageNestDatabase
import com.example.data.local.ReaderPreferencesDataStore
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.data.repository.WriterRepository
import com.example.engine.cbz.CbzEngine
import com.example.engine.epub.EpubParser
import com.example.engine.fonts.FontManager
import com.example.engine.formats.TextFormatsConverter
import com.example.engine.pdf.PdfEngine
import com.example.engine.translate.TextTranslator
import com.example.notifications.StreakReminderScheduler
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
  private val database = PageNestDatabase.getInstance(application)
  private val dao = database.pageNestDao()
  private val preferences = ReaderPreferencesDataStore(application)

  val epubParser = EpubParser(application)
  val pdfEngine = PdfEngine(application)
  val textFormatsConverter = TextFormatsConverter(application)
  val cbzEngine = CbzEngine(application)
  val fontManager = FontManager(application)
  val textTranslator = TextTranslator()
  val libraryRepository =
    LibraryRepository(
      context = application,
      dao = dao,
      epubParser = epubParser,
      pdfEngine = pdfEngine,
      textFormatsConverter = textFormatsConverter,
      cbzEngine = cbzEngine,
    )
  val readerRepository = ReaderRepository(dao = dao, preferences = preferences, appContext = application)
  val writerRepository = WriterRepository(context = application, dao = dao, libraryRepository = libraryRepository)
}

class PageNestApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    appScope.launch {
      val settings = container.readerRepository.settingsFlow.first()
      if (settings.streakReminderEnabled) {
        StreakReminderScheduler.scheduleDaily(
          context = this@PageNestApplication,
          hour = settings.streakReminderHour,
          minute = settings.streakReminderMinute,
        )
      }
      WidgetUpdater.requestAllUpdates(this@PageNestApplication)
    }
  }
}
