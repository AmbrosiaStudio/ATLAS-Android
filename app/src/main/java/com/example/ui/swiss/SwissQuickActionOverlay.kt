package com.example.ui.swiss

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import kotlin.math.roundToInt

enum class HomeQuickActionType {
  RESUME_LAST_READ,
  NEW_NOTE,
  IMPORT_FILES,
}

data class HomeQuickActionItem(
  val type: HomeQuickActionType,
  val title: String,
  val subtitle: String,
  val isPrimaryContext: Boolean,
  val targetBookId: String? = null,
)

/**
 * Resolves context-sensitive Home Screen quick actions based on library and reading state:
 * - Active recently-read document -> prioritizes "Resume: <title>", followed by New Note & Import Files.
 * - Unread books present -> prioritizes "Start: <title>", followed by Import Files & New Note.
 * - Empty library -> prioritizes Import Files & New Note (omits unavailable resume action).
 */
fun buildContextSensitiveQuickActions(
  continueBook: BookEntity?,
  allBooks: List<BookEntity>,
  isPersian: Boolean,
): List<HomeQuickActionItem> {
  val lastRead =
    continueBook?.takeIf { it.lastOpenedAt > 0L }
      ?: allBooks.filter { it.lastOpenedAt > 0L }.maxByOrNull { it.lastOpenedAt }
  val firstUnread =
    if (lastRead == null) continueBook ?: allBooks.maxByOrNull { it.addedAt } else null

  val newNoteItem =
    HomeQuickActionItem(
      type = HomeQuickActionType.NEW_NOTE,
      title = if (isPersian) "یادداشت جدید در استودیو" else "New Note in Writer Studio",
      subtitle = if (isPersian) "ایجاد و باز کردن پیش‌نویس جدید" else "Create and open a fresh draft",
      isPrimaryContext = false,
    )

  if (lastRead != null) {
    val pct = lastRead.progressPercent.roundToInt().coerceIn(0, 100)
    return listOf(
      HomeQuickActionItem(
        type = HomeQuickActionType.RESUME_LAST_READ,
        title = if (isPersian) "ادامه: ${lastRead.title}" else "Resume: ${lastRead.title}",
        subtitle =
          if (isPersian) "${lastRead.bookFormat().name} · $pct% خوانده‌شده"
          else "${lastRead.bookFormat().name} · $pct% read",
        isPrimaryContext = true,
        targetBookId = lastRead.id,
      ),
      newNoteItem,
      HomeQuickActionItem(
        type = HomeQuickActionType.IMPORT_FILES,
        title = if (isPersian) "افزودن فایل به کتابخانه" else "Import Files",
        subtitle = "EPUB, PDF, TXT, MD, FB2, CBZ, Audio",
        isPrimaryContext = false,
      ),
    )
  }

  if (firstUnread != null) {
    return listOf(
      HomeQuickActionItem(
        type = HomeQuickActionType.RESUME_LAST_READ,
        title = if (isPersian) "شروع مطالعه: ${firstUnread.title}" else "Start: ${firstUnread.title}",
        subtitle =
          if (isPersian) "آماده مطالعه · ${firstUnread.bookFormat().name}"
          else "Ready to read · ${firstUnread.bookFormat().name}",
        isPrimaryContext = true,
        targetBookId = firstUnread.id,
      ),
      HomeQuickActionItem(
        type = HomeQuickActionType.IMPORT_FILES,
        title = if (isPersian) "افزودن فایل به کتابخانه" else "Import Files",
        subtitle = "EPUB, PDF, TXT, MD, FB2, CBZ, Audio",
        isPrimaryContext = false,
      ),
      newNoteItem,
    )
  }

  return listOf(
    HomeQuickActionItem(
      type = HomeQuickActionType.IMPORT_FILES,
      title = if (isPersian) "افزودن فایل به کتابخانه" else "Import Files",
      subtitle = if (isPersian) "وارد کردن کتاب یا فایل صوتی برای شروع" else "Add EPUB, PDF, or audio to begin",
      isPrimaryContext = true,
    ),
    newNoteItem,
  )
}

@Composable
fun SwissHomeQuickActionOverlay(
  continueBook: BookEntity?,
  allBooks: List<BookEntity>,
  isPersian: Boolean,
  isImporting: Boolean,
  onImportFiles: () -> Unit,
  onStartNewNote: () -> Unit,
  onOpenLastRead: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  BackHandler(enabled = expanded) { expanded = false }

  val actions =
    remember(continueBook, allBooks, isPersian) {
      buildContextSensitiveQuickActions(
        continueBook = continueBook,
        allBooks = allBooks,
        isPersian = isPersian,
      )
    }
  val hasActiveRead = remember(actions) {
    actions.any { it.type == HomeQuickActionType.RESUME_LAST_READ }
  }
  val iconRotation by
    animateFloatAsState(
      targetValue = if (expanded) 45f else 0f,
      animationSpec = tween(durationMillis = 180),
      label = "quick_action_rotation",
    )

  Box(modifier = modifier.fillMaxSize()) {
    // Editorial backdrop scrim when Quick Actions menu is expanded
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn(tween(160)),
      exit = fadeOut(tween(140)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
            ) {
              expanded = false
            },
      )
    }

    Column(
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Context-sensitive Quick Action Sheet Stack
      AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(200)),
        exit = fadeOut(tween(140)) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(160)),
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.widthIn(min = 248.dp, max = 312.dp).testTag("home_quick_action_menu"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // Editorial micro-header
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(horizontal = 14.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (isPersian) "اقدام‌های سریع" else "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (hasActiveRead) {
                Text(
                  text = if (isPersian) "آماده ادامه" else "CONTEXT",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            actions.forEachIndexed { index, item ->
              val itemIcon: ImageVector =
                when (item.type) {
                  HomeQuickActionType.RESUME_LAST_READ -> Icons.Rounded.AutoStories
                  HomeQuickActionType.NEW_NOTE -> Icons.Rounded.EditNote
                  HomeQuickActionType.IMPORT_FILES -> Icons.Rounded.UploadFile
                }
              val itemTag =
                when (item.type) {
                  HomeQuickActionType.RESUME_LAST_READ -> "quick_action_resume_last_read"
                  HomeQuickActionType.NEW_NOTE -> "quick_action_new_note"
                  HomeQuickActionType.IMPORT_FILES -> "quick_action_import_files"
                }

              SwissQuickActionMenuRow(
                icon = itemIcon,
                title = item.title,
                subtitle = item.subtitle,
                highlighted = item.isPrimaryContext,
                enabled = !(item.type == HomeQuickActionType.IMPORT_FILES && isImporting),
                testTag = itemTag,
                onClick = {
                  expanded = false
                  when (item.type) {
                    HomeQuickActionType.RESUME_LAST_READ -> item.targetBookId?.let(onOpenLastRead)
                    HomeQuickActionType.NEW_NOTE -> onStartNewNote()
                    HomeQuickActionType.IMPORT_FILES -> onImportFiles()
                  }
                },
              )

              if (index < actions.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
              }
            }
          }
        }
      }

      // Context-sensitive Primary Quick Action Button
      val buttonLabel =
        when {
          expanded -> if (isPersian) "بستن" else "Close"
          allBooks.isEmpty() -> if (isPersian) "شروع سریع" else "Get Started"
          else -> if (isPersian) "اقدام سریع" else "Quick Actions"
        }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(6.dp))
            .clickable { expanded = !expanded }
            .testTag("home_quick_action_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = buttonLabel,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp).rotate(iconRotation),
          )
          Text(
            text = buttonLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
          )
          if (!expanded && hasActiveRead) {
            Box(
              modifier =
                Modifier
                  .size(6.dp)
                  .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), CircleShape),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SwissQuickActionMenuRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  highlighted: Boolean,
  enabled: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(4.dp),
      color =
        if (highlighted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
      border =
        if (highlighted) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.size(34.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (highlighted) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(15.dp),
    )
  }
}
