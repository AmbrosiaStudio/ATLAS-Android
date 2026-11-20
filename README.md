# ATLAS — Digital Library & Reading Application

**Tagline:** *Your world of knowledge.*

ATLAS is a minimalist, Swiss-editorial native Android digital library, multi-format document reader, audiobook companion, and writing studio built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **DataStore**, and **Jetpack Glance**.

---

## Design System

### Monochrome Palette
- **Light Theme:** Background `#F5F5F2`, Surface `#FFFFFF`, Primary `#111111`, Secondary `#777777`, Divider `#E6E6E2`, Muted `#EEEEEB`.
- **Dark Theme:** Background `#111111`, Surface `#1B1B1B`, Primary `#F5F5F2`, Secondary `#999999`, Divider `#303030`, Muted `#242424`.
- **Reading Surfaces:** Light (`#F5F5F2`), Warm Paper (`#F4ECDF`), Dark (`#111111`), and Pure Black (`#000000`).

### Typography & Bilingual RTL
- **English:** Bundled `Inter` (`res/font/inter.ttf`) with generous editorial spacing.
- **Persian:** Bundled `Vazirmatn` (`res/font/vazirmatn.ttf`) with complete RTL layout mirroring (`LayoutDirection.Rtl`) and mixed Persian/English support.

### Brand Identity & Adaptive App Icon
- Custom geometric circular compass + 4-point diamond symbol (`AtlasGeometricSymbol`), horizontal wordmark (`AtlasHorizontalWordmark`), and adaptive monochrome launcher icon (`ic_launcher` / `ic_launcher_round`).

---

## Application Structure

- **4-Tab Bottom Navigation (`SwissAtlasMainShell`):**
  - **Home:** Wordmark + greeting, Continue Reading spotlight, Recently Added shelf, and 3-metric Reading Activity strip.
  - **Library:** Live search, format filter chips (`EPUB`, `PDF`, `TXT`, `MD`, `FB2`, `CBZ`, `AUDIOBOOK`), sorting, grid/list toggle, and SAF document/audiobook import.
  - **Collections:** Create, rename, delete collections, browse collection detail views, and assign/remove documents.
  - **Profile:** Light/Dark appearance, 4 reading themes, reading preferences, English/Persian switcher, advanced settings/reminders/fonts, Writing Studio, Free Catalog, Statistics, storage management, and About ATLAS.
- **Distraction-Free Reader (`ReaderScreen`):** Minimal monochrome `AtlasReaderToolbar` + Swiss bottom dock, highlights, bookmarks, TOC, in-book search, reading ruler, TTS, ambient soundscapes, and Reading Insights.

---

## Home Screen Widgets (Named & Previewed)

All 4 Glance widgets follow the unified ATLAS Swiss Monochrome design (supporting both Light and Dark app themes) and include named labels in `AndroidManifest.xml` plus live `android:previewLayout` previews in the Android Widget Picker:

1. **ATLAS — Continue Reading** (`widget_preview_continue.xml`): Active document cover, format badge, daily goal progress, and reading progress bar.
2. **ATLAS — Reading Activity** (`widget_preview_stats.xml`): Today's minutes, total pages, all-time hours, active streak pill, and 7-day monochrome bar chart.
3. **ATLAS — Reading Streak** (`widget_preview_streak.xml`): Compact 1×1 high-contrast geometric streak badge.
4. **ATLAS — Quick Shelf** (`widget_preview_shelf.xml`): 3 recent volumes with reading progress and your latest saved highlight quote.

---

## Building & Testing

```bash
# Compile debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```
