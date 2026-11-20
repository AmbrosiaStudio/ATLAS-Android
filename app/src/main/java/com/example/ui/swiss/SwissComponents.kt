package com.example.ui.swiss

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.BookEntity
import java.io.File
import kotlin.math.roundToInt

/**
 * Reusable Swiss Monochrome Components for ATLAS:
 * 1. AtlasTopBar
 * 2. AtlasBottomNavigation
 * 3. AtlasDocumentCard
 * 4. AtlasDocumentRow
 * 5. AtlasCollectionCard
 * 6. AtlasProgressIndicator
 * 7. AtlasSearchField
 * 8. AtlasFilterChips
 * 9. AtlasEmptyState
 * 10. AtlasSectionHeader
 * 11. AtlasSettingsItem
 * 12. AtlasReaderToolbar
 */

enum class SwissNavTab(
  val route: String,
  val labelEn: String,
  val labelFa: String,
  val iconOutlined: ImageVector,
  val iconFilled: ImageVector,
) {
  HOME("swiss_home", "Home", "خانه", Icons.Outlined.Home, Icons.Rounded.Home),
  LIBRARY("swiss_library", "Library", "کتابخانه", Icons.Outlined.MenuBook, Icons.Rounded.MenuBook),
  COLLECTIONS("swiss_collections", "Collections", "مجموعه‌ها", Icons.Outlined.CollectionsBookmark, Icons.Rounded.CollectionsBookmark),
  PROFILE("swiss_profile", "Profile", "پروفایل", Icons.Outlined.Person, Icons.Rounded.Person),
}

// 1. AtlasTopBar
@Composable
fun AtlasTopBar(
  isPersian: Boolean,
  title: String? = null,
  subtitle: String? = null,
  showWordmark: Boolean = false,
  onNavigateBack: (() -> Unit)? = null,
  trailingActions: @Composable () -> Unit = {},
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          if (onNavigateBack != null) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.size(36.dp).minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = if (isPersian) "بازگشت" else "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showWordmark) {
              AtlasHorizontalWordmark(isPersian = isPersian)
            } else if (title != null) {
              Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (!subtitle.isNullOrBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          trailingActions()
        }
      }
      HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

// 2. AtlasBottomNavigation
@Composable
fun AtlasBottomNavigation(
  currentRoute: String?,
  isPersian: Boolean,
  onSelectTab: (SwissNavTab) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
      HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SwissNavTab.entries.forEach { tab ->
          val selected = currentRoute == tab.route
          val label = if (isPersian) tab.labelFa else tab.labelEn
          Column(
            modifier =
              Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelectTab(tab) }
                .padding(vertical = 6.dp)
                .testTag("swiss_nav_${tab.name.lowercase()}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .width(if (selected) 20.dp else 0.dp)
                  .height(2.dp)
                  .background(MaterialTheme.colorScheme.primary),
            )
            Icon(
              imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
              contentDescription = label,
              tint =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

// 3. AtlasDocumentCard
@Composable
fun AtlasDocumentCard(
  book: BookEntity,
  onClick: () -> Unit,
  onMoreClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val pct = book.progressPercent.roundToInt().coerceIn(0, 100)
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.70f)) {
        SwissCoverFrame(
          book = book,
          modifier = Modifier.fillMaxSize(),
        )
        // Minimal monochrome format tag
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
        ) {
          Text(
            text = book.bookFormat().name.take(5),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          )
        }
        if (onMoreClick != null) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onMoreClick),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Document options",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = book.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        minLines = 2,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = book.author,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AtlasProgressIndicator(
          progress = (book.progressPercent / 100f).coerceIn(0f, 1f),
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "$pct%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// 4. AtlasDocumentRow
@Composable
fun AtlasDocumentRow(
  book: BookEntity,
  onClick: () -> Unit,
  onMoreClick: (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val pct = book.progressPercent.roundToInt().coerceIn(0, 100)
  val sizeLabel = formatBytesCompact(book.fileSizeBytes)

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SwissCoverFrame(
        book = book,
        modifier = Modifier.width(46.dp).height(66.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${book.author} · ${book.bookFormat().name} · $sizeLabel",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AtlasProgressIndicator(
            progress = (book.progressPercent / 100f).coerceIn(0f, 1f),
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (trailingContent != null) {
        trailingContent()
      } else if (onMoreClick != null) {
        IconButton(
          onClick = onMoreClick,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreHoriz,
            contentDescription = "Document options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

// 5. AtlasCollectionCard
@Composable
fun AtlasCollectionCard(
  name: String,
  documentCount: Int,
  representativeBooks: List<BookEntity>,
  isPersian: Boolean,
  onClick: () -> Unit,
  onMoreClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val countText =
    if (isPersian) "$documentCount سند"
    else "$documentCount ${if (documentCount == 1) "document" else "documents"}"

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // 2x2 geometric cover preview mosaic
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().aspectRatio(1.35f),
      ) {
        if (representativeBooks.isEmpty()) {
          Box(contentAlignment = Alignment.Center) {
            AtlasGeometricSymbol(
              size = 28.dp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
              contentDescriptionText = null,
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            representativeBooks.take(3).forEach { b ->
              SwissCoverFrame(
                book = b,
                modifier = Modifier.weight(1f).fillMaxHeight(),
              )
            }
            repeat((3 - representativeBooks.size).coerceAtLeast(0)) {
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(3.dp)),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
        IconButton(
          onClick = onMoreClick,
          modifier = Modifier.size(32.dp).minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreHoriz,
            contentDescription = "Collection options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

// 6. AtlasProgressIndicator
@Composable
fun AtlasProgressIndicator(
  progress: Float,
  modifier: Modifier = Modifier,
  heightDp: Int = 2,
) {
  val animated by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 260),
      label = "atlas_progress",
    )
  Box(
    modifier =
      modifier
        .height(heightDp.dp)
        .background(MaterialTheme.colorScheme.outlineVariant),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxHeight()
          .fillMaxWidth(animated)
          .background(MaterialTheme.colorScheme.primary),
    )
  }
}

// 7. AtlasSearchField
@Composable
fun AtlasSearchField(
  query: String,
  placeholder: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth().height(46.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (query.isEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.size(28.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Clear",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

// 8. AtlasFilterChips
@Composable
fun <T> AtlasFilterChips(
  options: List<Pair<T, String>>,
  selected: T,
  onSelect: (T) -> Unit,
  contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(options) { (value, label) ->
      val active = value == selected
      Surface(
        shape = RoundedCornerShape(4.dp),
        color =
          if (active) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surface,
        border =
          BorderStroke(
            width = 1.dp,
            color =
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier =
          Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onSelect(value) },
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color =
            if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          maxLines = 1,
        )
      }
    }
  }
}

// 9. AtlasEmptyState
@Composable
fun AtlasEmptyState(
  title: String,
  subtitle: String,
  primaryActionLabel: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    AtlasGeometricSymbol(
      size = 44.dp,
      color = MaterialTheme.colorScheme.primary,
      contentDescriptionText = null,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (primaryActionLabel != null && onPrimaryAction != null) {
      Spacer(modifier = Modifier.height(24.dp))
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onPrimaryAction),
      ) {
        Text(
          text = primaryActionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
      }
    }
    if (secondaryActionLabel != null && onSecondaryAction != null) {
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onSecondaryAction),
      ) {
        Text(
          text = secondaryActionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
      }
    }
  }
}

// 10. AtlasSectionHeader
@Composable
fun AtlasSectionHeader(
  title: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
    if (actionLabel != null && onActionClick != null) {
      Text(
        text = actionLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier =
          Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onActionClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
      )
    }
  }
}

// 11. AtlasSettingsItem
@Composable
fun AtlasSettingsItem(
  title: String,
  subtitle: String? = null,
  valueText: String? = null,
  onClick: (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (trailingContent != null) {
      trailingContent()
    } else if (valueText != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = valueText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onClick != null) {
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    } else if (onClick != null) {
      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

// 12. AtlasReaderToolbar
@Composable
fun AtlasReaderToolbar(
  title: String,
  progressPercent: Float,
  isBookmarked: Boolean,
  onBack: () -> Unit,
  onToggleBookmark: () -> Unit,
  onOpenToc: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenTypography: () -> Unit,
  surfaceColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = MaterialTheme.colorScheme.primary,
  dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
  Surface(color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = contentColor,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${progressPercent.roundToInt().coerceIn(0, 100)}%",
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.6f),
          )
        }
        IconButton(onClick = onOpenSearch, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search in document",
            tint = contentColor,
            modifier = Modifier.size(19.dp),
          )
        }
        IconButton(onClick = onOpenToc, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Outlined.FormatListBulleted,
            contentDescription = "Contents",
            tint = contentColor,
            modifier = Modifier.size(19.dp),
          )
        }
        IconButton(onClick = onOpenTypography, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.Rounded.TextFields,
            contentDescription = "Reading settings",
            tint = contentColor,
            modifier = Modifier.size(19.dp),
          )
        }
        IconButton(onClick = onToggleBookmark, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = contentColor,
            modifier = Modifier.size(19.dp),
          )
        }
      }
      HorizontalDivider(thickness = 1.dp, color = dividerColor)
    }
  }
}

/**
 * Clean monochrome/editorial cover frame used across Swiss cards and rows.
 */
@Composable
internal fun SwissCoverFrame(
  book: BookEntity,
  modifier: Modifier = Modifier,
) {
  val coverFile = book.coverPath?.let { File(it) }?.takeIf { it.exists() }
  val shape = RoundedCornerShape(4.dp)

  if (coverFile != null) {
    AsyncImage(
      model = coverFile,
      contentDescription = "${book.title} cover",
      contentScale = ContentScale.Crop,
      modifier = modifier.clip(shape),
    )
  } else {
    Surface(
      shape = shape,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = modifier,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier =
            Modifier
              .width(14.dp)
              .height(2.dp)
              .background(MaterialTheme.colorScheme.primary),
        )
        Text(
          text = book.title,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

internal fun formatBytesCompact(bytes: Long): String {
  if (bytes <= 0L) return "0 KB"
  val kb = bytes / 1024.0
  if (kb < 1024.0) return "${kb.roundToInt().coerceAtLeast(1)} KB"
  val mb = kb / 1024.0
  return String.format("%.1f MB", mb)
}
