package com.example.ui.writer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.WriterBookEntity
import com.example.engine.writer.WriterEpubExporter
import com.example.model.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun WriterStudioScreen(
  viewModel: WriterStudioViewModel,
  onNavigateBack: () -> Unit,
  onOpenEditor: (String) -> Unit,
  onOpenPublishedBook: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val snackbarHostState = remember { SnackbarHostState() }

  var showCreateDialog by remember { mutableStateOf(false) }
  var pendingDeleteBook by remember { mutableStateOf<WriterBookEntity?>(null) }
  var coverStudioBook by remember { mutableStateOf<WriterBookEntity?>(null) }

  LaunchedEffect(uiState.message, uiState.lastPublishedLibraryBookId) {
    val libId = uiState.lastPublishedLibraryBookId
    val msg = uiState.message
    if (libId != null) {
      snackbarHostState.showSnackbar(strings.publishedSuccessLabel)
      viewModel.clearMessage()
    } else if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
      )
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 820.dp)) {
        // Editorial Top Header
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_back_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.returnToLibrary,
                    tint = MaterialTheme.colorScheme.onBackground,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = strings.studioTitle,
                      fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
                      fontSize = if (isFa) 20.sp else 22.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onBackground,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    ) {
                      Text(
                        text = strings.studioBetaBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text = strings.studioSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                  Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable { showCreateDialog = true }
                    .testTag("studio_new_book_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = strings.newBookAction,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        if (uiState.manuscripts.isEmpty()) {
          EmptyStudioState(
            isFa = isFa,
            onCreateClick = { showCreateDialog = true },
            modifier =
              Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier =
              Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
          ) {
            // Writer summary metrics banner
            item {
              val totalWords = uiState.manuscripts.sumOf { it.totalWords }
              val totalChapters = uiState.manuscripts.sumOf { it.chapterCount }
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(
                      text = strings.wordCountLabel(totalWords),
                      fontFamily = LiterataFontFamily,
                      fontSize = 20.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "${uiState.manuscripts.size} · ${strings.chaptersCountLabel(totalChapters)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                  )
                }
              }
            }

            items(uiState.manuscripts, key = { it.book.id }) { item ->
              ManuscriptCard(
                summary = item,
                isFa = isFa,
                isPublishing = uiState.publishingBookId == item.book.id,
                onEditClick = { onOpenEditor(item.book.id) },
                onCoverStudioClick = { coverStudioBook = item.book },
                onPublishClick = { viewModel.publishManuscript(item.book.id) },
                onOpenPublishedClick = {
                  item.book.publishedBookId?.let { onOpenPublishedBook(it) }
                },
                onDeleteClick = { pendingDeleteBook = item.book },
              )
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    ManuscriptDetailsDialog(
      dialogTitle = strings.newBookAction,
      initialTitle = "",
      initialAuthor = "",
      initialSynopsis = "",
      initialRtl = isFa,
      confirmLabel = strings.createShelfButton,
      onDismiss = { showCreateDialog = false },
      onConfirm = { title, author, synopsis, rtl ->
        showCreateDialog = false
        viewModel.createManuscript(title, author, synopsis, rtl, onOpenEditor)
      },
    )
  }

  pendingDeleteBook?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteBook = null },
      containerColor = MaterialTheme.colorScheme.surface,
      title = {
        Text(
          text = strings.deleteManuscriptTitle,
          fontFamily = LiterataFontFamily,
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = {
        Text(
          text = "“${target.title}”",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteBook = null }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteManuscript(target.id)
            pendingDeleteBook = null
          },
          modifier = Modifier.testTag("confirm_delete_manuscript_button"),
        ) {
          Text(strings.remove, color = MaterialTheme.colorScheme.error)
        }
      },
    )
  }

  coverStudioBook?.let { target ->
    val freshBook =
      uiState.manuscripts.firstOrNull { it.book.id == target.id }?.book ?: target
    StudioCoverDesignerDialog(
      book = freshBook,
      isFa = isFa,
      onDismiss = { coverStudioBook = null },
      onSaveCover = { themeId, pickedUri, clearCustomImage ->
        viewModel.updateBookCover(
          bookId = freshBook.id,
          coverThemeId = themeId,
          pickedCoverUri = pickedUri,
          clearCustomImage = clearCustomImage,
        )
        coverStudioBook = null
      },
    )
  }
}

@Composable
private fun ManuscriptCard(
  summary: WriterBookSummary,
  isFa: Boolean,
  isPublishing: Boolean,
  onEditClick: () -> Unit,
  onCoverStudioClick: () -> Unit,
  onPublishClick: () -> Unit,
  onOpenPublishedClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val book = summary.book
  val updatedDate =
    remember(book.updatedAt) {
      DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(book.updatedAt))
    }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onEditClick)
        .testTag("manuscript_card_${book.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StudioCoverPreviewThumbnail(
          title = book.title,
          author = book.author,
          coverThemeId = book.coverThemeId,
          imageModel = book.coverImagePath?.takeIf { File(it).exists() }?.let { File(it) },
          onClick = onCoverStudioClick,
          modifier = Modifier.width(62.dp).height(90.dp).testTag("manuscript_cover_${book.id}"),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = if (book.isRtl) "RTL" else "LTR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            if (!book.publishedBookId.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = if (isFa) "منتشرشده" else "On Shelf",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          }
          Text(
            text = book.title,
            fontFamily = if (book.isRtl) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${book.author} · $updatedDate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_manuscript_${book.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = strings.remove,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onCoverStudioClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("cover_studio_btn_${book.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = strings.coverStudioAction,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      if (book.synopsis.isNotBlank()) {
        Text(
          text = book.synopsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = "${strings.chaptersCountLabel(summary.chapterCount)} · ${strings.wordCountLabel(summary.totalWords)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onEditClick,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Rounded.Create, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFa) "ویرایش" else "Write",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
          )
        }

        OutlinedButton(
          onClick = onPublishClick,
          enabled = !isPublishing,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f),
        ) {
          if (isPublishing) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
          } else {
            Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isFa) "انتشار" else "Publish",
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
            )
          }
        }

        if (!book.publishedBookId.isNullOrBlank()) {
          OutlinedButton(
            onClick = onOpenPublishedClick,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.MenuBook,
              contentDescription = strings.openPublishedBookAction,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyStudioState(
  isFa: Boolean,
  onCreateClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Box(
    modifier = modifier.padding(28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.EditNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(32.dp),
            )
          }
        }
        Text(
          text = strings.studioEmptyTitle,
          fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
          fontSize = 21.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        Text(
          text = strings.studioEmptySubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onCreateClick,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("studio_empty_create_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.newBookAction, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
fun ManuscriptDetailsDialog(
  dialogTitle: String,
  initialTitle: String,
  initialAuthor: String,
  initialSynopsis: String,
  initialRtl: Boolean,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (title: String, author: String, synopsis: String, isRtl: Boolean) -> Unit,
) {
  val strings = LocalAppStrings.current
  var title by remember { mutableStateOf(initialTitle) }
  var author by remember { mutableStateOf(initialAuthor) }
  var synopsis by remember { mutableStateOf(initialSynopsis) }
  var isRtl by remember { mutableStateOf(initialRtl) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = dialogTitle,
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.manuscriptTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("manuscript_title_input"),
        )
        OutlinedTextField(
          value = author,
          onValueChange = { author = it },
          label = { Text(strings.authorNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("manuscript_author_input"),
        )
        OutlinedTextField(
          value = synopsis,
          onValueChange = { synopsis = it },
          label = { Text(strings.synopsisLabel) },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("manuscript_synopsis_input"),
        )
        Text(
          text = strings.writingDirectionLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(false to strings.ltrLabel, true to strings.rtlLabel).forEach { (rtlOpt, label) ->
            val selected = isRtl == rtlOpt
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
              border =
                BorderStroke(
                  1.dp,
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { isRtl = rtlOpt },
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title, author, synopsis, isRtl) },
        modifier = Modifier.testTag("manuscript_dialog_confirm"),
      ) {
        Text(confirmLabel)
      }
    },
  )
}

@Composable
internal fun StudioCoverPreviewThumbnail(
  title: String,
  author: String,
  coverThemeId: Int,
  imageModel: Any?,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val theme = WriterEpubExporter.coverThemeById(coverThemeId)
  val topColor = Color(android.graphics.Color.parseColor(theme.topHex))
  val bottomColor = Color(android.graphics.Color.parseColor(theme.bottomHex))
  val accentColor = Color(android.graphics.Color.parseColor(theme.accentHex))
  val textColor = Color(android.graphics.Color.parseColor(theme.textHex))

  Surface(
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Brush.verticalGradient(listOf(topColor, bottomColor))),
    ) {
      if (imageModel != null) {
        AsyncImage(
          model = imageModel,
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.62f))
                )
              ),
        )
      }
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier.width(18.dp).height(2.dp).background(accentColor, CircleShape)
        )
        Text(
          text = title.ifBlank { "ATLAS" },
          fontFamily = LiterataFontFamily,
          fontSize = 10.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (imageModel != null) Color.White else textColor,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = author,
          fontSize = 8.sp,
          color = if (imageModel != null) Color.White.copy(alpha = 0.85f) else accentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
internal fun StudioCoverDesignerDialog(
  book: WriterBookEntity,
  isFa: Boolean,
  onDismiss: () -> Unit,
  onSaveCover: (themeId: Int, pickedCoverUri: Uri?, clearCustomImage: Boolean) -> Unit,
) {
  val strings = LocalAppStrings.current
  var selectedThemeId by remember(book.id) { mutableIntStateOf(book.coverThemeId) }
  var pickedImageUri by remember(book.id) { mutableStateOf<Uri?>(null) }
  var clearCustomImage by remember(book.id) { mutableStateOf(false) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        pickedImageUri = uri
        clearCustomImage = false
      }
    }

  val previewImageModel: Any? =
    when {
      clearCustomImage -> null
      pickedImageUri != null -> pickedImageUri
      !book.coverImagePath.isNullOrBlank() && File(book.coverImagePath).exists() -> File(book.coverImagePath)
      else -> null
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = strings.coverStudioTitle,
        fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        StudioCoverPreviewThumbnail(
          title = book.title,
          author = book.author,
          coverThemeId = selectedThemeId,
          imageModel = previewImageModel,
          modifier = Modifier.width(126.dp).height(184.dp),
        )

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = strings.coverThemeLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            WriterEpubExporter.coverThemes.forEach { theme ->
              val selected = selectedThemeId == theme.id
              val topColor = Color(android.graphics.Color.parseColor(theme.topHex))
              val bottomColor = Color(android.graphics.Color.parseColor(theme.bottomHex))
              val accentColor = Color(android.graphics.Color.parseColor(theme.accentHex))
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedThemeId = theme.id }
                    .padding(4.dp)
                    .testTag("studio_cover_theme_${theme.id}"),
              ) {
                Surface(
                  shape = CircleShape,
                  border =
                    BorderStroke(
                      width = if (selected) 2.5.dp else 1.dp,
                      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                  modifier = Modifier.size(40.dp),
                ) {
                  Box(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(topColor, bottomColor))),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (selected) {
                      Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
                Text(
                  text = if (isFa) theme.faLabel else theme.label,
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("studio_pick_cover_photo_button"),
          ) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.changeCoverAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          if (previewImageModel != null) {
            TextButton(
              onClick = {
                pickedImageUri = null
                clearCustomImage = true
              },
              modifier = Modifier.testTag("studio_reset_cover_photo_button"),
            ) {
              Text(strings.removeCoverAction, color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
    confirmButton = {
      Button(
        onClick = { onSaveCover(selectedThemeId, pickedImageUri, clearCustomImage) },
        modifier = Modifier.testTag("studio_save_cover_button"),
      ) {
        Text(strings.saveChangesAction)
      }
    },
  )
}
