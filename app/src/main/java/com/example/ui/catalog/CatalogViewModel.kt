package com.example.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.CatalogBookItem
import com.example.data.catalog.CatalogSource
import com.example.data.catalog.OpdsCatalogRepository
import com.example.data.repository.ReaderRepository
import com.example.model.ReaderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
  val settings: ReaderSettings = ReaderSettings(),
  val selectedSource: CatalogSource = CatalogSource.GUTENDEX,
  val searchQuery: String = "",
  val languageFilter: String? = null,
  val customFeedUrl: String = "",
  val items: List<CatalogBookItem> = emptyList(),
  val isLoading: Boolean = false,
  val downloadingIds: Set<String> = emptySet(),
  val importedBookIds: Map<String, String> = emptyMap(),
  val errorMessage: String? = null,
  val infoMessage: String? = null,
)

class CatalogViewModel(
  private val catalogRepository: OpdsCatalogRepository,
  readerRepository: ReaderRepository,
) : ViewModel() {

  private val selectedSource = MutableStateFlow(CatalogSource.GUTENDEX)
  private val searchQuery = MutableStateFlow("")
  private val languageFilter = MutableStateFlow<String?>(null)
  private val customFeedUrl = MutableStateFlow("")
  private val items = MutableStateFlow<List<CatalogBookItem>>(emptyList())
  private val isLoading = MutableStateFlow(false)
  private val downloadingIds = MutableStateFlow<Set<String>>(emptySet())
  private val importedBookIds = MutableStateFlow<Map<String, String>>(emptyMap())
  private val errorMessage = MutableStateFlow<String?>(null)
  private val infoMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<CatalogUiState> =
    combine(
      readerRepository.settingsFlow,
      combine(selectedSource, searchQuery, languageFilter, customFeedUrl) { s, q, l, u ->
        Quad(s, q, l, u)
      },
      combine(items, isLoading, downloadingIds, importedBookIds) { list, loading, dl, imp ->
        Quad(list, loading, dl, imp)
      },
      combine(errorMessage, infoMessage) { err, info -> err to info },
    ) { settings, filters, data, messages ->
      CatalogUiState(
        settings = settings,
        selectedSource = filters.first,
        searchQuery = filters.second,
        languageFilter = filters.third,
        customFeedUrl = filters.fourth,
        items = data.first,
        isLoading = data.second,
        downloadingIds = data.third,
        importedBookIds = data.fourth,
        errorMessage = messages.first,
        infoMessage = messages.second,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = CatalogUiState(isLoading = true),
    )

  init {
    refreshCatalog()
  }

  fun selectSource(source: CatalogSource) {
    selectedSource.value = source
    if (source != CatalogSource.CUSTOM_OPDS || customFeedUrl.value.isNotBlank()) {
      refreshCatalog()
    } else {
      items.value = emptyList()
      errorMessage.value = null
    }
  }

  fun setSearchQuery(q: String) {
    searchQuery.value = q
  }

  fun setCustomFeedUrl(url: String) {
    customFeedUrl.value = url
  }

  fun setLanguageFilter(lang: String?) {
    languageFilter.value = lang
    refreshCatalog()
  }

  fun refreshCatalog() {
    val src = selectedSource.value
    val url = customFeedUrl.value.trim()
    if (src == CatalogSource.CUSTOM_OPDS && url.isBlank()) {
      items.value = emptyList()
      isLoading.value = false
      return
    }
    viewModelScope.launch {
      isLoading.value = true
      errorMessage.value = null
      catalogRepository
        .browseCatalog(
          source = src,
          query = searchQuery.value,
          languageFilter = languageFilter.value,
          customFeedUrl = url.takeIf { it.isNotBlank() },
        )
        .onSuccess { list ->
          items.value = list
          isLoading.value = false
        }
        .onFailure { err ->
          isLoading.value = false
          errorMessage.value = err.message ?: "Unable to load catalog."
        }
    }
  }

  fun downloadBook(item: CatalogBookItem, successMessage: String) {
    if (downloadingIds.value.contains(item.id)) return
    viewModelScope.launch {
      downloadingIds.value = downloadingIds.value + item.id
      errorMessage.value = null
      catalogRepository
        .downloadAndImport(item)
        .onSuccess { book ->
          downloadingIds.value = downloadingIds.value - item.id
          importedBookIds.value = importedBookIds.value + (item.id to book.id)
          infoMessage.value = "$successMessage · ${book.title}"
        }
        .onFailure { err ->
          downloadingIds.value = downloadingIds.value - item.id
          errorMessage.value = err.message ?: "Unable to download EPUB."
        }
    }
  }

  fun clearMessages() {
    errorMessage.value = null
    infoMessage.value = null
  }

  private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )

  class Factory(
    private val catalogRepository: OpdsCatalogRepository,
    private val readerRepository: ReaderRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return CatalogViewModel(catalogRepository, readerRepository) as T
    }
  }
}
