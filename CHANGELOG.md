# PageNest Changelog

All notable releases, features, engine upgrades, and bug fixes from **v1.0** through **v3.1.0** (`ir.pagenest.mrambrosia`).

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
