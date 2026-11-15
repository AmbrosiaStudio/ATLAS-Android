package com.example

import android.app.Application
import com.example.data.local.PageNestDatabase
import com.example.data.local.ReaderPreferencesDataStore
import com.example.data.repository.LibraryRepository
import com.example.data.repository.ReaderRepository
import com.example.engine.epub.EpubParser
import com.example.engine.fonts.FontManager
import com.example.engine.pdf.PdfEngine
import com.example.engine.translate.TextTranslator

class AppContainer(application: Application) {
  private val database = PageNestDatabase.getInstance(application)
  private val dao = database.pageNestDao()
  private val preferences = ReaderPreferencesDataStore(application)

  val epubParser = EpubParser(application)
  val pdfEngine = PdfEngine(application)
  val fontManager = FontManager(application)
  val textTranslator = TextTranslator()
  val libraryRepository = LibraryRepository(application, dao, epubParser, pdfEngine)
  val readerRepository = ReaderRepository(dao, preferences)
}

class PageNestApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
