package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.BookEntity
import com.example.data.local.ShelfEntity
import com.example.model.AppLanguage
import com.example.model.ReadingStatus
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import java.io.File

@Composable
fun CreateShelfDialog(
  onCreate: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var name by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurface,
    title = {
      Text(
        text = strings.createShelfTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 40) name = it },
        placeholder = {
          Text(
            text = strings.shelfNameHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier = Modifier.fillMaxWidth().testTag("create_shelf_input"),
      )
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name) },
        enabled = name.trim().isNotEmpty(),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag("confirm_create_shelf_button"),
      ) {
        Text(strings.createShelfButton)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditBookMetadataDialog(
  book: BookEntity,
  shelves: List<ShelfEntity>,
  assignedShelfIds: Set<Long>,
  isFa: Boolean,
  onToggleShelf: (Long, Boolean) -> Unit,
  onCreateShelfQuick: () -> Unit,
  onSave: (
    title: String,
    author: String,
    status: ReadingStatus,
    isFavorite: Boolean,
    pickedCoverUri: Uri?,
    resetCover: Boolean,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var title by remember(book.id) { mutableStateOf(book.title) }
  var author by remember(book.id) { mutableStateOf(book.author) }
  var status by remember(book.id) { mutableStateOf(book.status()) }
  var isFavorite by remember(book.id) { mutableStateOf(book.isFavorite) }
  var pickedCoverUri by remember(book.id) { mutableStateOf<Uri?>(null) }
  var resetCover by remember(book.id) { mutableStateOf(false) }

  val coverPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        pickedCoverUri = uri
        resetCover = false
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurface,
    title = {
      Text(
        text = strings.editBookTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Cover Picker Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          val previewModel: Any? =
            when {
              pickedCoverUri != null -> pickedCoverUri
              !resetCover && book.coverPath != null && File(book.coverPath).exists() -> File(book.coverPath)
              else -> null
            }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.width(62.dp).height(90.dp),
          ) {
            if (previewModel != null) {
              AsyncImage(
                model = previewModel,
                contentDescription = strings.customCoverLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
              )
            } else {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Image,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = strings.customCoverLabel,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = {
                coverPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.fillMaxWidth().testTag("choose_cover_button"),
            ) {
              Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = strings.changeCoverAction,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (previewModel != null) {
              TextButton(
                onClick = {
                  pickedCoverUri = null
                  resetCover = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.removeCoverAction, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.bookTitleLabel) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("edit_book_title_input"),
        )

        OutlinedTextField(
          value = author,
          onValueChange = { author = it },
          label = { Text(strings.bookAuthorLabel) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("edit_book_author_input"),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Reading Status Selector
        Text(
          text = strings.readingStatusLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReadingStatus.entries.forEach { st ->
            val active = status == st
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { status = st }
                  .testTag("edit_status_${st.name.lowercase()}"),
            ) {
              Text(
                text = if (isFa) st.faLabel else st.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
              )
            }
          }
        }

        // Favorite Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(19.dp),
            )
            Text(
              text = strings.favoriteLabel,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Switch(
            checked = isFavorite,
            onCheckedChange = { isFavorite = it },
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
              ),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Custom Shelves Assignment
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.assignToShelves,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          TextButton(
            onClick = onCreateShelfQuick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.newShelfAction, style = MaterialTheme.typography.labelSmall)
          }
        }

        if (shelves.isEmpty()) {
          Text(
            text = strings.noShelvesYet,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            shelves.forEach { shelf ->
              val assigned = shelf.id in assignedShelfIds
              Surface(
                shape = CircleShape,
                color =
                  if (assigned) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant,
                border =
                  BorderStroke(
                    width = 1.dp,
                    color =
                      if (assigned) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier =
                  Modifier
                    .clip(CircleShape)
                    .clickable { onToggleShelf(shelf.id, !assigned) }
                    .testTag("assign_shelf_${shelf.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = if (assigned) Icons.Rounded.Check else Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint =
                      if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = shelf.name,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                      if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, author, status, isFavorite, pickedCoverUri, resetCover) },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag("save_book_metadata_button"),
      ) {
        Text(strings.saveChangesAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
  )
}
