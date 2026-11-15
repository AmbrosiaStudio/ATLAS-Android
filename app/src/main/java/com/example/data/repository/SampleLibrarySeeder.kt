package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates real, standards-compliant EPUB 3.0 and PDF files on demand when the user chooses
 * "Load Sample Classics" from the empty library or import menu.
 */
object SampleLibrarySeeder {

  suspend fun createSampleEpub(context: Context): File =
    withContext(Dispatchers.IO) {
      val tempDir = File(context.cacheDir, "sample_gen").apply { mkdirs() }
      val epubFile = File(tempDir, "On_Quiet_Reading_Sample.epub")

      val coverBytes = renderSampleCoverBytes()

      ZipOutputStream(FileOutputStream(epubFile)).use { zip ->
        // 1. mimetype (stored uncompressed per EPUB spec)
        val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
        val mimeEntry =
          ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            crc = java.util.zip.CRC32().apply { update(mimetypeBytes) }.value
          }
        zip.putNextEntry(mimeEntry)
        zip.write(mimetypeBytes)
        zip.closeEntry()

        // 2. META-INF/container.xml
        writeEntry(
          zip,
          "META-INF/container.xml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            <rootfiles>
              <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            </rootfiles>
          </container>
          """.trimIndent(),
        )

        // 3. OEBPS/cover.jpg
        zip.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
        zip.write(coverBytes)
        zip.closeEntry()

        // 4. OEBPS/content.opf
        writeEntry(
          zip,
          "OEBPS/content.opf",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="BookId">
            <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
              <dc:identifier id="BookId">urn:uuid:pagenest-sample-001</dc:identifier>
              <dc:title>Letters on Slow Reading</dc:title>
              <dc:creator>Marcus Aurelius &amp; Seneca</dc:creator>
              <dc:language>en</dc:language>
              <meta name="cover" content="cover-img"/>
            </metadata>
            <manifest>
              <item id="cover-img" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>
              <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
              <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
              <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
              <item id="ch3" href="chapter3.xhtml" media-type="application/xhtml+xml"/>
              <item id="ch4" href="chapter4.xhtml" media-type="application/xhtml+xml"/>
            </manifest>
            <spine>
              <itemref idref="ch1"/>
              <itemref idref="ch2"/>
              <itemref idref="ch3"/>
              <itemref idref="ch4"/>
            </spine>
          </package>
          """.trimIndent(),
        )

        // 5. OEBPS/nav.xhtml
        writeEntry(
          zip,
          "OEBPS/nav.xhtml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
          <head><title>Table of Contents</title></head>
          <body>
            <nav epub:type="toc" id="toc">
              <h1>Contents</h1>
              <ol>
                <li><a href="chapter1.xhtml">I. On Discursiveness in Reading</a></li>
                <li><a href="chapter2.xhtml">II. The Architecture of Solitude</a></li>
                <li><a href="chapter3.xhtml">III. Meditations at Dawn</a></li>
                <li><a href="chapter4.xhtml">IV. Keeping a Commonplace Book</a></li>
              </ol>
            </nav>
          </body>
          </html>
          """.trimIndent(),
        )

        writeEntry(zip, "OEBPS/chapter1.xhtml", chapterOneHtml())
        writeEntry(zip, "OEBPS/chapter2.xhtml", chapterTwoHtml())
        writeEntry(zip, "OEBPS/chapter3.xhtml", chapterThreeHtml())
        writeEntry(zip, "OEBPS/chapter4.xhtml", chapterFourHtml())
      }
      epubFile
    }

  suspend fun createSamplePersianEpub(context: Context): File =
    withContext(Dispatchers.IO) {
      val tempDir = File(context.cacheDir, "sample_gen").apply { mkdirs() }
      val epubFile = File(tempDir, "Quiet_Persian_Reflections_Sample.epub")
      val coverBytes = renderPersianCoverBytes()

      ZipOutputStream(FileOutputStream(epubFile)).use { zip ->
        val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
        val mimeEntry =
          ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            crc = java.util.zip.CRC32().apply { update(mimetypeBytes) }.value
          }
        zip.putNextEntry(mimeEntry)
        zip.write(mimetypeBytes)
        zip.closeEntry()

        writeEntry(
          zip,
          "META-INF/container.xml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            <rootfiles>
              <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            </rootfiles>
          </container>
          """.trimIndent(),
        )

        zip.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
        zip.write(coverBytes)
        zip.closeEntry()

        writeEntry(
          zip,
          "OEBPS/content.opf",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="fa" unique-identifier="FaBookId">
            <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
              <dc:identifier id="FaBookId">urn:uuid:pagenest-fa-sample-001</dc:identifier>
              <dc:title>در ستایش خواندن آرام</dc:title>
              <dc:creator>گزیده ادبیات فارسی</dc:creator>
              <dc:language>fa</dc:language>
              <meta name="cover" content="cover-img"/>
            </metadata>
            <manifest>
              <item id="cover-img" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>
              <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
              <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
              <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
            </manifest>
            <spine page-progression-direction="rtl">
              <itemref idref="ch1"/>
              <itemref idref="ch2"/>
            </spine>
          </package>
          """.trimIndent(),
        )

        writeEntry(
          zip,
          "OEBPS/nav.xhtml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" dir="rtl" lang="fa">
          <head><title>فهرست مطالب</title></head>
          <body>
            <nav epub:type="toc" id="toc">
              <h1>فهرست</h1>
              <ol>
                <li><a href="chapter1.xhtml">بخش اول: هنر درنگ در واژه‌ها</a></li>
                <li><a href="chapter2.xhtml">بخش دوم: دفتر یادداشت‌های حاشیه</a></li>
              </ol>
            </nav>
          </body>
          </html>
          """.trimIndent(),
        )

        writeEntry(
          zip,
          "OEBPS/chapter1.xhtml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" lang="fa">
          <head><title>بخش اول: هنر درنگ در واژه‌ها</title></head>
          <body dir="rtl">
            <h1>بخش اول: هنر درنگ در واژه‌ها</h1>
            <p>خواندنِ عمیق، گفت‌وگویی آرام میان ذهن خواننده و روح کتاب است. هنگامی که شتاب روزمره را کنار می‌گذاریم و به هر جمله فرصت ته‌نشین شدن می‌دهیم، کلمات رنگ و عمق تازه‌ای پیدا می‌کنند.</p>
            <blockquote>«بنی‌آدم اعضای یکدیگرند که در آفرینش ز یک گوهرند — چو عضوی به درد آورد روزگار، دگر عضوها را نماند قرار.»</blockquote>
            <p>در سنت ادبیات فارسی، از گلستان سعدی تا غزلیات حافظ و دفترهای مولانا، زیبایی متن همیشه در پیوند با تناسب خط، آرامش حاشیه و درنگ خواننده معنا یافته است.</p>
            <p>می‌توانید روی هر واژه یا جمله نگه دارید تا منوی شیشه‌ای بالای متن باز شود و آن را هایلایت کنید، یادداشت بنویسید، نقل‌قول بسازید یا به زبان دیگر ترجمه کنید.</p>
          </body>
          </html>
          """.trimIndent(),
        )

        writeEntry(
          zip,
          "OEBPS/chapter2.xhtml",
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" lang="fa">
          <head><title>بخش دوم: دفتر یادداشت‌های حاشیه</title></head>
          <body dir="rtl">
            <h1>بخش دوم: دفتر یادداشت‌های حاشیه</h1>
            <p>کتاب‌هایی که در حاشیه آن‌ها یادداشت نوشته‌ایم، تنها یک اثر چاپی نیستند؛ بلکه آینه‌ای از فکر و حالِ ما در زمان‌های گوناگون زندگی‌اند.</p>
            <blockquote>«تو کز محنت دیگران بی‌غمی، نشاید که نامت نهند آدمی.»</blockquote>
            <p>برای تجربه بهتر متون فارسی، می‌توانید از بخش تایپوگرافی قلم «وزیرمتن» یا «نوتو نسخ» را انتخاب کنید و پوسته دلخواه خود را برگزینید.</p>
          </body>
          </html>
          """.trimIndent(),
        )
      }
      epubFile
    }

  suspend fun createSamplePdf(context: Context): File =
    withContext(Dispatchers.IO) {
      val tempDir = File(context.cacheDir, "sample_gen").apply { mkdirs() }
      val pdfFile = File(tempDir, "Editorial_Typography_Pocket_Guide.pdf")

      val document = PdfDocument()
      val pagesContent =
        listOf(
          "Editorial Typography" to
            listOf(
              "A Pocket Field Guide to Screen & Print Legibility",
              "",
              "Good typography is invisible architecture. When proportions, line length, and leading work in harmony, the reader forgets the medium and enters the author's voice directly.",
              "",
              "1. Measure & Margins",
              "The ideal measure spans between 55 and 72 characters per line. Narrower columns cause erratic eye jumps; wider lines make returning to the next line tiring.",
              "",
              "2. Vertical Rhythm",
              "Generous line spacing—typically 1.45x to 1.7x the type size—gives ascenders and descenders room to breathe on backlit OLED and paper-white displays.",
            ),
          "Serif & Sans-Serif Pairing" to
            listOf(
              "Why Classic Serifs Endure",
              "",
              "Serifs guide the eye horizontally across long passages of continuous prose. Typefaces such as Literata, Lora, and Newsreader carry crisp terminals tuned specifically for digital screens.",
              "",
              "Contrast with Functional Sans",
              "Reserve geometric or humanist sans-serif faces like Plus Jakarta Sans for chrome, navigation headers, progress indicators, and subtle metadata.",
              "",
              "This clear separation lets the reader recognize instantly what belongs to the book and what belongs to the tool.",
            ),
          "Light, Sepia & AMOLED" to
            listOf(
              "Designing for Ambient Light",
              "",
              "Pure #FFFFFF backgrounds can create harsh halation in dim rooms. Warm paper (#FAF8F5) and soft sepia (#F4ECD8) reduce blue-light glare while preserving crisp ink contrast.",
              "",
              "Night & OLED Reading",
              "In dark environments, switching to charcoal ink or true AMOLED black eliminates surrounding bezel glow, letting paragraphs float serenely in focus.",
            ),
          "Marginalia & Memory" to
            listOf(
              "Reading as Active Conversation",
              "",
              "Bookmarks hold our physical place across days, while highlights preserve sentences that caught our attention mid-thought.",
              "",
              "Return to your notes periodically. A quiet personal library grows richer each time you revisit a marked passage and discover how your perspective has shifted.",
            ),
        )

      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#1E1D1B")
          textSize = 24f
          typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
      val accentPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#6D4AFF")
          textSize = 11f
          typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
      val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#2B2A27")
          textSize = 13.5f
          typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
      val footerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#8A857E")
          textSize = 10f
          typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

      val pageWidth = 595
      val pageHeight = 842
      val marginX = 56f
      val maxLineWidth = pageWidth - marginX * 2

      pagesContent.forEachIndexed { idx, (heading, paragraphs) ->
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.parseColor("#FAF8F5"))

        canvas.drawText("PAGENEST POCKET EDITION", marginX, 74f, accentPaint)
        canvas.drawText(heading, marginX, 112f, titlePaint)

        var y = 160f
        for (para in paragraphs) {
          if (para.isBlank()) {
            y += 14f
            continue
          }
          val words = para.split(" ")
          var currentLine = StringBuilder()
          for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (bodyPaint.measureText(candidate) > maxLineWidth) {
              canvas.drawText(currentLine.toString(), marginX, y, bodyPaint)
              y += 23f
              currentLine = StringBuilder(word)
            } else {
              currentLine = StringBuilder(candidate)
            }
          }
          if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), marginX, y, bodyPaint)
            y += 23f
          }
        }

        canvas.drawText("Page ${idx + 1} of ${pagesContent.size}", marginX, pageHeight - 44f, footerPaint)
        document.finishPage(page)
      }

      FileOutputStream(pdfFile).use { out -> document.writeTo(out) }
      document.close()
      pdfFile
    }

  private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
  }

  private fun renderSampleCoverBytes(): ByteArray {
    val bmp = Bitmap.createBitmap(420, 620, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint =
      Paint().apply {
        shader =
          LinearGradient(
            0f,
            0f,
            420f,
            620f,
            intArrayOf(Color.parseColor("#241B42"), Color.parseColor("#151126")),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, 420f, 620f, bgPaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#5C4899")
      }
    canvas.drawRect(28f, 28f, 392f, 592f, borderPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F2EC")
        textSize = 40f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      }
    canvas.drawText("Letters on", 56f, 220f, titlePaint)
    canvas.drawText("Slow Reading", 56f, 272f, titlePaint)

    val authorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B5A7E8")
        textSize = 20f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      }
    canvas.drawText("Marcus Aurelius & Seneca", 56f, 520f, authorPaint)

    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    bmp.recycle()
    return baos.toByteArray()
  }

  private fun renderPersianCoverBytes(): ByteArray {
    val bmp = Bitmap.createBitmap(420, 620, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint =
      Paint().apply {
        shader =
          LinearGradient(
            0f,
            0f,
            420f,
            620f,
            intArrayOf(Color.parseColor("#1D2E38"), Color.parseColor("#111A21")),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, 420f, 620f, bgPaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#578899")
      }
    canvas.drawRect(28f, 28f, 392f, 592f, borderPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F2EC")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      }
    canvas.drawText("در ستایش", 210f, 235f, titlePaint)
    canvas.drawText("خواندن آرام", 210f, 290f, titlePaint)

    val authorPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A5C7D4")
        textSize = 20f
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("گزیده ادبیات فارسی", 210f, 520f, authorPaint)

    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    bmp.recycle()
    return baos.toByteArray()
  }

  private fun chapterOneHtml() =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head><title>I. On Discursiveness in Reading</title></head>
    <body>
      <h1>I. On Discursiveness in Reading</h1>
      <p>Judging from what you write me, and from what I hear, I am forming a good opinion regarding your future. You do not run hither and thither and distract yourself by changing your abode; for such restlessness is the sign of a disordered spirit.</p>
      <p>The primary indication, to my thinking, of a well-ordered mind is a man's ability to remain in one place and linger in his own company. Be careful, however, lest this reading of many authors and books of every sort may tend to make you discursive and unsteady.</p>
      <blockquote>“To be everywhere is to be nowhere. People who spend their whole life travelling end up having plenty of places where they can find hospitality, but no real friendships.”</blockquote>
      <p>You must linger among a limited number of master thinkers, and digest their works, if you would derive ideas which shall win firm hold in your mind. Food does no good and is not assimilated into the body if it leaves the stomach as soon as it is eaten; nothing hinders a cure so much as frequent change of medicine.</p>
      <p>No wound will heal when one salve is tried after another; a plant which is often moved can never grow strong. There is nothing so efficacious that it can be helpful while it is being shifted about. And in reading of many books is distraction.</p>
      <p>Accordingly, since you cannot read all the books which you may possess, it is enough to possess only as many books as you can read. Read always standard authors; and when you crave a change, fall back upon those whom you read before.</p>
      <p>Each day acquire something that will fortify you against poverty, against death, indeed against other misfortunes as well; and after you have run over many thoughts, select one to be thoroughly digested that day.</p>
    </body>
    </html>
    """.trimIndent()

  private fun chapterTwoHtml() =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head><title>II. The Architecture of Solitude</title></head>
    <body>
      <h1>II. The Architecture of Solitude</h1>
      <p>People seek retreats for themselves in the country, by the sea, and in the hills; and you yourself are especially fond of longing for such places. Yet this is altogether the mark of the most commonplace sort of person, when it is in your power at any hour you choose to retreat into yourself.</p>
      <p>For nowhere can a person find a retreat more peaceful or more untroubled than in his own soul, especially when he has within him such thoughts that by looking into them he is immediately at perfect ease.</p>
      <blockquote>“Constant attention to a quiet page is not an escape from the world; it is how we return to it with clearer eyes.”</blockquote>
      <p>Give yourself this retreat continually, and renew yourself. Let your maxims be brief and fundamental, which as soon as you encounter them will suffice to wash away all distress, and send you back without discontent to the things to which you return.</p>
      <p>When a reader sits in quiet light without notifications, alerts, or endless feeds, attention deepens naturally. Every paragraph becomes a small room with a single window looking outward.</p>
    </body>
    </html>
    """.trimIndent()

  private fun chapterThreeHtml() =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head><title>III. Meditations at Dawn</title></head>
    <body>
      <h1>III. Meditations at Dawn</h1>
      <p>At dawn, when you have trouble getting out of bed, tell yourself: I have to go to work—as a human being. What do I have to complain of, if I am going to do what I was born for, the things I was brought into the world to do?</p>
      <p>In the morning stillness, before the day begins making demands on your attention, twenty minutes with a thoughtful book sets the cadence of the hours ahead. The mind is unhurried and receptive.</p>
      <p>Observe how swiftly all things vanish: bodies themselves in the universe, and the memory of them in time. What is the nature of all sensible things, especially those that entice with pleasure or frighten with pain, or are noisily trumpeted by fame?</p>
      <p>How cheap, how contemptible, how sordid, how perishable, how dead they are! It is the part of the intellectual faculty to examine each thing thoroughly and see it clearly for what it is.</p>
    </body>
    </html>
    """.trimIndent()

  private fun chapterFourHtml() =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head><title>IV. Keeping a Commonplace Book</title></head>
    <body>
      <h1>IV. Keeping a Commonplace Book</h1>
      <p>We ought to imitate bees, as the saying goes, which wander about and cull the flowers suitable for making honey, and then arrange and distribute through the comb whatever they have brought home.</p>
      <p>Select a passage, mark it with quiet care, and note why it struck you in that moment. Years later, reading your own highlights feels like shaking hands with a younger version of yourself.</p>
      <blockquote>“What we read shapes our vocabulary of thought; what we highlight shapes our memory.”</blockquote>
      <p>Take care, then, to read slowly enough that wisdom has time to settle. Turn each page not to finish the book, but to inhabit it.</p>
    </body>
    </html>
    """.trimIndent()
}
