# PageNest Changelog

All notable releases, features, engine upgrades, and bug fixes from **v1.0** through **v6.0** (`ir.pagenest.mrambrosia`).

---

## v6.0 (Build 6) — Reading Ruler & Audiobook Folder Player Sync

### English
- **Reading Ruler / Line Guide Overlay:** Toggle a smooth, non-touch-blocking reading guide right inside the Reader Typography sheet (`Spotlight Band`, `Highlight Strip`, or `Underline Bar`) with customizable height (`32–140 dp`), mask contrast, and an ergonomic edge handle to glide or tap-cycle styles without interfering with text selection or paging.
- **Audiobook Folder / MP3 & M4B Player Sync:** Import single or multi-file audiobooks (`.m4b`, `.mp3`, `.m4a`, `.aac`, `.ogg`, `.flac`, `.wav`) or entire audiobook folders via zero-permission Android SAF (`OpenDocumentTree`) with automatic natural track sorting, M4B embedded Nero chapter (`chpl`) parsing, album cover extraction, playback speed (`0.75×–2.5×`), lock-screen/earbud `MediaSession` controls, sleep timer fade-out, ambient soundscape mixing, and **Companion Ebook ↔ Audiobook Progress Sync** (link any book in your library and jump or sync progress percentages both ways).

### فارسی (Persian Release Notes)
- **خط‌کش مطالعه و راهنمای تمرکز خط:** فعال‌سازی خط‌کش قابل جابه‌جایی در صفحه مطالعه در سه سبک (باند نوری، نوار رنگی یا خط زیرین) با امکان تنظیم ارتفاع، تیرگی پس‌زمینه و دستگیره کناری بدون اختلال در ورق زدن یا انتخاب متن.
- **پخش‌کننده کتاب صوتی (پوشه، MP3 و M4B) با همگام‌سازی کتاب:** وارد کردن تک‌فایل یا کل پوشه کتاب صوتی بدون نیاز به مجوز حافظه با مرتب‌سازی هوشمند شماره‌گذاری، استخراج خودکار فصل‌های M4B، کاور، سرعت پخش (`۰.۷۵×` تا `۲.۵×`)، کنترل صفحه قفل و هدفون، تایمر خواب و صداهای محیطی، به همراه **همگام‌سازی دوطرفه درصد پیشرفت بین کتاب صوتی و نسخه متنی** در کتابخانه.

---

## v3.2.0 (Build 5) — Listen Immersion, Gestures, Public-Domain Catalog & Cover Studio

### English
- **Sleep Timer & Gentle Fade-Out (Listen Mode):** Built-in sleep timer (`Off`, `15m`, `30m`, `45m`, `60m`, or `End of Chapter`) with a live countdown pill in Listen Mode and smooth linear TTS volume fade-out (`KEY_PARAM_VOLUME`) over the last 15 seconds before pausing.
- **Lock-Screen Media Controls & Earbud Shortcuts:** Native Android `MediaSession` + foreground `MediaStyle` notification (`TtsPlaybackService`) exposing Play, Pause, Next Chunk, Previous Chunk, and Stop controls on the lock screen, notification shade, and Bluetooth earbuds/headsets.
- **Tap-Zone & Gesture Customization:** Assign independent actions (`Prev Page`, `Next Page`, `Toggle Controls`, `Quick Bookmark`, `None`) to the Left (28%), Center (44%), and Right (28%) tap zones across EPUB, PDF, and CBZ, plus **Left-Edge Vertical Brightness Swipe** and **Double-Tap Quick Bookmark** (available in Reader Typography sheet & Settings).
- **Offline Ambient Soundscapes & Reading Focus Timer:** Procedural `AudioTrack` sound synthesizer (`Rain`, `Fireplace`, `Brown Noise`, `Ocean Waves`) with volume slider, paired with an in-reader Pomodoro Focus Timer (`15/3m`, `25/5m`, `45/10m`, `60/15m`) and cycle tracker.
- **Free Public-Domain Book Browser (`CatalogScreen`):** Browse and search **Project Gutenberg** (Gutendex JSON API), **Standard Ebooks** (OPDS Atom XML), and **Custom OPDS** feeds with language filters, cover art, and 1-tap EPUB download directly into your Library.
- **PageNest Studio Cover Studio & Chapter Reordering:** Design covers using 6 curated editorial color themes or upload custom cover artwork via Android Photo Picker (`PickVisualMedia`), and reorder manuscript chapters (`Move Earlier` / `Move Later`) before publishing to EPUB.

#### Bug Fixes
- **Text-Selection Popup Redesign & RTL Alignment Fix:** Redesigned the floating reader selection toolbar with PageNest's theme-responsive Liquid-Glass aesthetic and a 3×2 action grid across all 6 reading themes, and fixed physical-coordinate positioning (`AbsoluteAlignment.TopLeft` + `absoluteOffset`) so both English (LTR) and Persian (RTL) never clip or overflow off-screen.
- **Real Acoustic Ambient Soundscapes (Replacing Raw Static):** Rebuilt `AmbientSoundPlayer` into a 44.1 kHz stereo physical-modeling synthesizer featuring Paul Kellet pink-noise filtering, 2-pole Butterworth biquads, resonant water-droplet pings, organic fireplace flutter & wood crackle transients, and phase-swept ocean surf.
- **ART Startup `VerifyError` Fix:** Converted `AppStrings` from a 295-parameter `data class` to an `@Immutable interface` with singleton `object` localizations to eliminate the DEX 255-argument-register overflow on launch.

### فارسی (Persian Release Notes)
- **تایمر خواب و کاهش ملایم صدا (حالت شنیداری):** زمان‌بند ۱۵، ۳۰، ۴۵، ۶۰ دقیقه یا «پایان فصل» با شمارش معکوس زنده در نوار پخش و کاهش تدریجی و آرام صدا در ۱۵ ثانیه پایانی پیش از توقف.
- **کنترل صفحه قفل و دکمه‌های هندزفری/هدفون بلوتوث:** اتصال بومی `MediaSession` و سرویس پیش‌زمینه اعلان رسانه برای کنترل پخش، مکث، بند بعدی/قبلی و توقف از روی صفحه قفل و هدفون.
- **شخصی‌سازی مناطق لمس و ژست‌های حرکتی:** تنظیم دلخواه عملکرد بخش چپ، مرکز و راست صفحه در مطالعه EPUB، PDF و CBZ، تنظیم فوری روشنایی با کشیدن لبه چپ صفحه، و نشانک سریع با دو بار ضربه.
- **صداهای آرامش‌بخش محیطی و تایمر تمرکز (پومودورو):** تولید صدای باران، شومینه، نویز قهوه‌ای و امواج دریا به‌صورت کاملاً آفلاین به همراه تایمر تمرکز پومودورو و شمارنده دوره‌های مطالعه.
- **مرورگر کتاب‌های رایگان و آزاد (OPDS):** جستجو و دانلود یک‌لمسی کتاب‌های EPUB رایگان از پروژه گوتنبرگ، Standard Ebooks و فیدهای دلخواه OPDS همراه با فیلتر زبان و افزودن خودکار به کتابخانه.
- **استودیو طراحی جلد و چیدمان فصل‌ها در PageNest Studio:** انتخاب از میان ۶ تم هنری جلد یا تصویر دلخواه با Photo Picker امن و جابه‌جایی و مرتب‌سازی فصل‌های کتاب پیش از انتشار نهایی EPUB.

#### رفع خطاها (Bug Fixes)
- **بازطراحی منوی شناور انتخاب متن و رفع مشکل راست‌به‌چپ (فارسی و انگلیسی):** بازطراحی کامل منوی شناور با شیشه مایع هم‌رنگ با تم مطالعه و چیدمان منظم ۳×۲ بدون بریدگی دکمه‌ها و اصلاح مختصات فیزیکی صفحه برای جلوگیری از بیرون‌زدگی منو در زبان فارسی و انگلیسی.
- **بازسازی واقعی صداهای محیطی (حذف صدای خش‌خش/استاتیک):** ارتقای موتور `AmbientSoundPlayer` به سنتز استریو ۴۴.۱ کیلوهرتز با فیلتر نویز صورتی، فیلترهای Butterworth، قطرات واقعی باران، جرقه و صدای هیزم شومینه و موج‌سواری پویا در امواج دریا.
- **رفع کرش استارتاپ (`VerifyError` در ART):** تبدیل `AppStrings` به اینترفیس و آبجکت‌های مجزا برای رفع محدودیت ۲۵۵ آرگومان DEX هنگام اجرای برنامه.

---

## v3.1.0 (Build 4) — PageNest Studio (Beta), Widgets & Reader Precision

### Features
- **PageNest Studio (Beta):** Built-in offline book authoring studio for writers.
  - Create and organize multi-chapter manuscripts (`Room` v4 `writer_books` & `writer_chapters` with cascade integrity).
  - Distraction-free chapter editor supporting **LTR (English)** and **RTL (Persian / Arabic)** writing directions with Vazirmatn / Literata typography, font size controls, live autosave, and instant word counters.
  - Lightweight **Markdown formatting toolbar** (`H2`, `Quote`, `Bold`, `Italic`, `Bullet`, `---`), `.md` share export, and **1-tap Publish to Library** that compiles a full EPUB 3.0 book (with custom generated editorial cover art) straight onto the shelf.
- **Reading Statistics Widget (4×2 Glance):** Resizable home screen widget displaying today's minutes vs daily goal, total pages read, all-time hours, streak badge, and a 7-day activity trend bar chart with direct deep-link into Statistics.
- **Streak Flame Widget (1×1 Glance):** Compact 1×1 home badge pairing a layered flame emblem with your active daily reading streak count; taps open Reading Statistics.

### UX & Reader Fixes
- **Distraction-Free EPUB Canvas:** Removed inline "Previous Chapter" / "Next Chapter" buttons from the EPUB reader canvas.
- **Volume Key Bug Fix:** Fixed `V+` / `V-` hardware page turning skipping uncontrollably to the end/start of the book by tracking a persistent handled-step nonce across chapter boundaries and waiting for pagination anchor settlement before firing adjacent chapter transitions.
- **Exact Page Restoration:** Fixed EPUB chapter loading & lifecycle saving so reopening a book or relaunching the app resumes on the **exact last read page** inside a chapter (`currentScrollFraction`) instead of jumping back to chapter page 1. Going backward across chapters via volume keys now lands cleanly on the last page of the previous chapter, and leaving/backgrounding flushes progress under `NonCancellable`.

---

## v3.0 (Build 3) — Collections, Expanded Formats, Analytics & Support

### Features
- **Custom Shelves, Collections & Status Filters:** Create/delete custom shelves, filter Library by `All`, `Reading`, `Unread`, `Finished`, or `Favorites`, and assign books to multiple shelves.
- **Book Metadata & Cover Editor:** Edit book title, author, reading status, favorites, and pick custom cover art safely via the zero-permission Android Photo Picker.
- **Expanded Format Support:** Native parsers/renderers for **TXT**, **Markdown (`.md`)**, **FictionBook (`.fb2`)**, and **Comic Archives (`.cbz`)** alongside **EPUB** and **PDF**.
- **Reading Statistics Dashboard:** Interactive daily reading time bar chart (7/14-day window), daily goal line, total pages read counter, active streak tracker, and per-book progress trend chart.
- **Daily Streak Reminders & Continue Reading Widget:** Opt-in WorkManager local notifications at a user-chosen time plus a `4×2` Continue Reading home screen widget with cover art and progress deep-linking.
- **Tip Jar & PageNest Pro:** Configurable creator links (`MonetizationConfig.kt`) supporting both Iran and international supporters, timed/random Tip Jar prompts, and offline PageNest Pro supporter activation.

---

## v2.0 (Build 2) — Native Compose EPUB Engine, Battery Saver & Comfort Upgrade

### Architecture & Bug Fixes
- **100% Native Jetpack Compose EPUB Engine:** Replaced Android WebView with a custom Kotlin XHTML block parser & Compose paginator/scroller (`EpubHtmlBlockParser` + `EpubReaderView`), permanently eliminating Chromium renderer crashes (`aw_browser_terminator code -1`) and white/dark reload flashes.
- **Persian (FA) Header & RTL Polish:** Fixed RTL header alignment, letter spacing, Liquid Glass bottom reader controls reacting to theme changes, and surface contrast across Home, Reader, and Settings.

### Features
- **Battery-Saver Reading Mode:** Caps display refresh rate at 60Hz, simplifies animations, and disables expensive blur shaders during long sessions.
- **Volume Key Page Turning:** Hardware Volume Up / Down page navigation in EPUB, PDF, and CBZ readers.
- **Themes & Tactile Paper:** Added 2 new reading themes, dedicated **Black & White Night Mode**, **Paper Texture** canvas overlay toggle, and reader comfort presets.

---

## v1.0 (Build 1) — Initial PageNest Release

- **Local-First Library (`ir.pagenest.mrambrosia`):** Offline EPUB & PDF importing into private app storage, cover extraction, grid/list layout modes, search/sort, and preloaded English & Persian sample classics.
- **Reader Sanctuary:** Paged & Vertical Scroll layouts, Table of Contents, Bookmarks, Text Highlights & Marginalia Notes, Shareable Literary Quote cards, On-device ML Kit Translation, In-book search, Custom `.ttf`/`.otf` font manager, and Text-to-Speech **Listen Mode**.
