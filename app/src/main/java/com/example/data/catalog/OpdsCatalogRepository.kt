package com.example.data.catalog

import android.content.Context
import com.example.data.local.BookEntity
import com.example.data.repository.LibraryRepository
import com.example.model.BookFormat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

enum class CatalogSource(val label: String, val faLabel: String) {
  GUTENDEX("Project Gutenberg", "پروژه گوتنبرگ"),
  STANDARD_EBOOKS("Standard Ebooks", "Standard Ebooks"),
  CUSTOM_OPDS("Custom OPDS", "فید OPDS دلخواه"),
}

data class CatalogBookItem(
  val id: String,
  val title: String,
  val author: String,
  val summary: String,
  val languageCode: String,
  val coverUrl: String?,
  val epubDownloadUrl: String,
  val source: CatalogSource,
)

class OpdsCatalogRepository(
  private val context: Context,
  private val libraryRepository: LibraryRepository,
) {

  suspend fun browseCatalog(
    source: CatalogSource,
    query: String,
    languageFilter: String?,
    customFeedUrl: String? = null,
  ): Result<List<CatalogBookItem>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanQuery = query.trim()
        when (source) {
          CatalogSource.GUTENDEX -> {
            val urlBuilder = StringBuilder("https://gutendex.com/books?")
            var hasParam = false
            if (cleanQuery.isNotBlank()) {
              urlBuilder.append("search=").append(URLEncoder.encode(cleanQuery, "UTF-8"))
              hasParam = true
            }
            if (!languageFilter.isNullOrBlank()) {
              if (hasParam) urlBuilder.append('&')
              urlBuilder.append("languages=").append(URLEncoder.encode(languageFilter, "UTF-8"))
            }
            val payload = fetchTextWithRedirects(urlBuilder.toString())
            parseGutendexJson(payload)
          }
          CatalogSource.STANDARD_EBOOKS -> {
            val feedUrl =
              if (cleanQuery.isNotBlank()) {
                "https://standardebooks.org/feeds/opds/all?query=${URLEncoder.encode(cleanQuery, "UTF-8")}"
              } else {
                "https://standardebooks.org/feeds/opds/all"
              }
            val xml = fetchTextWithRedirects(feedUrl)
            filterOpdsLocally(
              items = parseOpdsAtomXml(xml, feedUrl, CatalogSource.STANDARD_EBOOKS),
              query = cleanQuery,
              languageFilter = languageFilter,
            )
          }
          CatalogSource.CUSTOM_OPDS -> {
            val rawUrl = customFeedUrl?.trim().orEmpty()
            require(rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
              "Enter a valid http:// or https:// OPDS feed URL."
            }
            val xml = fetchTextWithRedirects(rawUrl)
            filterOpdsLocally(
              items = parseOpdsAtomXml(xml, rawUrl, CatalogSource.CUSTOM_OPDS),
              query = cleanQuery,
              languageFilter = languageFilter,
            )
          }
        }
      }
    }

  suspend fun downloadAndImport(item: CatalogBookItem): Result<BookEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cacheDir = File(context.cacheDir, "catalog_downloads").apply { mkdirs() }
        val tempFile = File(cacheDir, "opds_${UUID.randomUUID()}.epub")
        try {
          downloadToFileWithRedirects(item.epubDownloadUrl, tempFile)
          val safeFileName =
            "${item.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(64).ifBlank { "book" }}.epub"
          libraryRepository.importDownloadedBookFile(
            tempFile = tempFile,
            displayFileName = safeFileName,
            format = BookFormat.EPUB,
          ).getOrThrow()
        } finally {
          tempFile.delete()
        }
      }
    }

  private fun filterOpdsLocally(
    items: List<CatalogBookItem>,
    query: String,
    languageFilter: String?,
  ): List<CatalogBookItem> {
    return items.filter { item ->
      val matchesQuery =
        query.isBlank() ||
          item.title.contains(query, ignoreCase = true) ||
          item.author.contains(query, ignoreCase = true) ||
          item.summary.contains(query, ignoreCase = true)
      val matchesLang =
        languageFilter.isNullOrBlank() ||
          item.languageCode.equals(languageFilter, ignoreCase = true) ||
          item.languageCode.startsWith("${languageFilter}-", ignoreCase = true)
      matchesQuery && matchesLang
    }
  }

  private fun fetchTextWithRedirects(urlString: String): String {
    var currentUrl = urlString
    repeat(5) {
      val conn =
        (URL(currentUrl).openConnection() as HttpURLConnection).apply {
          connectTimeout = 15_000
          readTimeout = 20_000
          instanceFollowRedirects = false
          requestMethod = "GET"
          setRequestProperty("User-Agent", "PageNest/3.2.0 (Android Ebook Reader)")
          setRequestProperty("Accept", "application/json, application/atom+xml, application/xml, text/xml")
        }
      try {
        val code = conn.responseCode
        if (code in 300..399) {
          val location = conn.getHeaderField("Location")
            ?: throw IllegalStateException("Redirect missing Location header ($code)")
          currentUrl = resolveHref(currentUrl, location)
        } else if (code in 200..299) {
          return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
          throw IllegalStateException("Catalog server returned HTTP $code")
        }
      } finally {
        conn.disconnect()
      }
    }
    throw IllegalStateException("Too many redirects while loading catalog")
  }

  private fun downloadToFileWithRedirects(urlString: String, targetFile: File) {
    var currentUrl = urlString
    repeat(6) {
      val conn =
        (URL(currentUrl).openConnection() as HttpURLConnection).apply {
          connectTimeout = 15_000
          readTimeout = 30_000
          instanceFollowRedirects = false
          requestMethod = "GET"
          setRequestProperty("User-Agent", "PageNest/3.2.0 (Android Ebook Reader)")
        }
      try {
        val code = conn.responseCode
        if (code in 300..399) {
          val location = conn.getHeaderField("Location")
            ?: throw IllegalStateException("Download redirect missing Location header ($code)")
          currentUrl = resolveHref(currentUrl, location)
        } else if (code in 200..299) {
          conn.inputStream.use { input ->
            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
          }
          return
        } else {
          throw IllegalStateException("Download failed with HTTP $code")
        }
      } finally {
        conn.disconnect()
      }
    }
    throw IllegalStateException("Too many redirects while downloading EPUB")
  }

  companion object {
    fun parseGutendexJson(jsonText: String): List<CatalogBookItem> {
      if (jsonText.isBlank()) return emptyList()
      val root = JSONObject(jsonText)
      val results = root.optJSONArray("results") ?: return emptyList()
      val items = ArrayList<CatalogBookItem>(results.length())
      for (i in 0 until results.length()) {
        val obj = results.optJSONObject(i) ?: continue
        val title = obj.optString("title").trim()
        if (title.isBlank()) continue
        val formats = obj.optJSONObject("formats") ?: continue
        val epubUrl =
          formats.optString("application/epub+zip").takeIf { it.isNotBlank() }
            ?: formats.optString("application/epub+zip; charset=utf-8").takeIf { it.isNotBlank() }
            ?: continue

        val authorsArr = obj.optJSONArray("authors")
        val author =
          if (authorsArr != null && authorsArr.length() > 0) {
            authorsArr.optJSONObject(0)?.optString("name")?.trim().orEmpty().ifBlank { "Public Domain" }
          } else {
            "Public Domain"
          }

        val summariesArr = obj.optJSONArray("summaries")
        val subjectsArr = obj.optJSONArray("subjects")
        val summary =
          when {
            summariesArr != null && summariesArr.length() > 0 -> summariesArr.optString(0).trim()
            subjectsArr != null && subjectsArr.length() > 0 ->
              (0 until minOf(2, subjectsArr.length())).joinToString(" · ") { idx -> subjectsArr.optString(idx) }
            else -> ""
          }

        val langsArr = obj.optJSONArray("languages")
        val lang =
          if (langsArr != null && langsArr.length() > 0) langsArr.optString(0).trim().ifBlank { "en" }
          else "en"

        val coverUrl = formats.optString("image/jpeg").takeIf { it.isNotBlank() }
        val idNum = obj.optLong("id", i.toLong())

        items.add(
          CatalogBookItem(
            id = "gutenberg_$idNum",
            title = title,
            author = author,
            summary = summary,
            languageCode = lang,
            coverUrl = coverUrl,
            epubDownloadUrl = epubUrl,
            source = CatalogSource.GUTENDEX,
          )
        )
      }
      return items
    }

    fun parseOpdsAtomXml(
      xmlText: String,
      baseUrl: String,
      source: CatalogSource = CatalogSource.CUSTOM_OPDS,
    ): List<CatalogBookItem> {
      if (xmlText.isBlank()) return emptyList()
      val factory = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
      }
      val parser = factory.newPullParser()
      parser.setInput(xmlText.reader())

      val items = mutableListOf<CatalogBookItem>()
      var inEntry = false
      var inAuthor = false
      var currentTag = ""

      var id = ""
      var title = ""
      var author = ""
      var summary = ""
      var language = "en"
      var coverUrl: String? = null
      var epubUrl: String? = null

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            val tag = parser.name.orEmpty().lowercase()
            currentTag = tag
            when {
              tag == "entry" -> {
                inEntry = true
                inAuthor = false
                id = ""
                title = ""
                author = ""
                summary = ""
                language = "en"
                coverUrl = null
                epubUrl = null
              }
              inEntry && tag == "author" -> {
                inAuthor = true
              }
              inEntry && tag == "link" -> {
                val rel = parser.getAttributeValue(null, "rel").orEmpty().lowercase()
                val type = parser.getAttributeValue(null, "type").orEmpty().lowercase()
                val href = parser.getAttributeValue(null, "href").orEmpty().trim()
                if (href.isNotBlank()) {
                  val resolved = resolveHref(baseUrl, href)
                  if (type.contains("application/epub+zip") &&
                    (epubUrl == null || rel.contains("acquisition"))
                  ) {
                    epubUrl = resolved
                  } else if (
                    rel.contains("opds-spec.org/image") ||
                    rel.contains("thumbnail") ||
                    (type.startsWith("image/") && coverUrl == null)
                  ) {
                    if (coverUrl == null || rel.contains("opds-spec.org/image/thumbnail")) {
                      coverUrl = resolved
                    }
                  }
                }
              }
            }
          }
          XmlPullParser.TEXT -> {
            if (inEntry) {
              val text = parser.text.orEmpty()
              when {
                inAuthor && currentTag == "name" -> author += text
                !inAuthor && currentTag == "id" -> id += text
                !inAuthor && currentTag == "title" -> title += text
                !inAuthor && (currentTag == "summary" || currentTag == "content") -> {
                  if (summary.isBlank() || currentTag == "summary") {
                    summary += text
                  }
                }
                !inAuthor && currentTag == "language" -> language = text.trim().ifBlank { language }
              }
            }
          }
          XmlPullParser.END_TAG -> {
            val tag = parser.name.orEmpty().lowercase()
            when {
              tag == "author" -> inAuthor = false
              tag == "entry" && inEntry -> {
                val cleanTitle = title.trim()
                val cleanEpub = epubUrl
                if (cleanTitle.isNotBlank() && !cleanEpub.isNullOrBlank()) {
                  val safeId = id.trim().ifBlank { cleanEpub }
                  items.add(
                    CatalogBookItem(
                      id = "opds_${safeId.hashCode()}",
                      title = cleanTitle,
                      author = author.trim().ifBlank { "Public Domain" },
                      summary = summary.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim(),
                      languageCode = language.trim().ifBlank { "en" },
                      coverUrl = coverUrl,
                      epubDownloadUrl = cleanEpub,
                      source = source,
                    )
                  )
                }
                inEntry = false
                inAuthor = false
              }
            }
            currentTag = ""
          }
        }
        eventType = parser.next()
      }
      return items
    }

    private fun resolveHref(baseUrl: String, href: String): String {
      return runCatching {
        URI(baseUrl).resolve(href).toString()
      }.getOrDefault(href)
    }
  }
}
