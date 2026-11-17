package com.example.ui.writer

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.launch

@Composable
fun WriterEditorScreen(
  viewModel: WriterEditorViewModel,
  onNavigateBack: () -> Unit,
  onOpenPublishedBook: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current
  val isFa = uiState.settings.appLanguage == AppLanguage.PERSIAN
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var editorFontSizeSp by remember { mutableIntStateOf(17) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showDetailsDialog by remember { mutableStateOf(false) }
  var showCoverStudioDialog by remember { mutableStateOf(false) }
  var showReorderChaptersDialog by remember { mutableStateOf(false) }
  var showRenameChapterDialog by remember { mutableStateOf(false) }
  var showDeleteChapterDialog by remember { mutableStateOf(false) }

  // Maintain TextFieldValue selection locally keyed by chapter ID for smooth cursor & formatting insertions
  var bodyFieldValue by
    remember(uiState.selectedChapterId) {
      mutableStateOf(
        TextFieldValue(
          text = uiState.draftBody,
          selection = TextRange(uiState.draftBody.length),
        )
      )
    }

  LaunchedEffect(uiState.draftBody, uiState.selectedChapterId) {
    if (bodyFieldValue.text != uiState.draftBody) {
      bodyFieldValue =
        TextFieldValue(
          text = uiState.draftBody,
          selection = TextRange(uiState.draftBody.length),
        )
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer =
      LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
          viewModel.flushDraftNow()
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  BackHandler {
    viewModel.flushDraftNow()
    onNavigateBack()
  }

  LaunchedEffect(uiState.message, uiState.errorMessage) {
    val err = uiState.errorMessage
    val msg = uiState.message
    val pubId = uiState.publishedLibraryBookId
    if (err != null) {
      snackbarHostState.showSnackbar(err)
      viewModel.clearMessages()
    } else if (msg != null) {
      val res =
        snackbarHostState.showSnackbar(
          message = msg,
          actionLabel = if (pubId != null) strings.openPublishedBookAction else null,
        )
      viewModel.clearMessages()
      if (res == SnackbarResult.ActionPerformed && pubId != null) {
        onOpenPublishedBook(pubId)
      }
    }
  }

  val book = uiState.book
  val isRtlBook = book?.isRtl ?: isFa
  val bookDirection = if (isRtlBook) LayoutDirection.Rtl else LayoutDirection.Ltr
  val bodyFont = if (isRtlBook) VazirmatnFontFamily else LiterataFontFamily
  val headingFont = if (isRtlBook) NotoNaskhFontFamily else LiterataFontFamily

  fun applyFormatting(prefix: String, suffix: String = "", placeholder: String = "") {
    val current = bodyFieldValue
    val text = current.text
    val start = current.selection.min.coerceIn(0, text.length)
    val end = current.selection.max.coerceIn(start, text.length)
    val selected = text.substring(start, end)
    val insertion =
      if (selected.isNotEmpty()) "$prefix$selected$suffix"
      else "$prefix$placeholder$suffix"
    val nextText = text.substring(0, start) + insertion + text.substring(end)
    val cursorPos =
      if (selected.isNotEmpty()) start + insertion.length
      else start + prefix.length + placeholder.length
    bodyFieldValue = TextFieldValue(text = nextText, selection = TextRange(cursorPos))
    viewModel.onChapterBodyChange(nextText)
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
      modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 820.dp)) {
        // Editor Top Bar
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(start = 4.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                IconButton(
                  onClick = {
                    viewModel.flushDraftNow()
                    onNavigateBack()
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_back_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.returnToLibrary,
                    tint = MaterialTheme.colorScheme.onBackground,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = book?.title ?: strings.studioTitle,
                    fontFamily = headingFont,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = strings.wordCountLabel(uiState.activeChapterWords),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = "·",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = if (uiState.isSaving) "…" else strings.savedLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                IconButton(
                  onClick = {
                    scope.launch {
                      val mdText = viewModel.buildMarkdownShareText()
                      if (mdText.isNotBlank()) {
                        val sendIntent =
                          Intent(Intent.ACTION_SEND).apply {
                            type = "text/markdown"
                            putExtra(Intent.EXTRA_SUBJECT, book?.title ?: "Book")
                            putExtra(Intent.EXTRA_TEXT, mdText)
                          }
                        runCatching {
                          context.startActivity(Intent.createChooser(sendIntent, strings.shareMarkdownAction))
                        }
                      }
                    }
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_share_md_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = strings.shareMarkdownAction,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                  )
                }

                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .clip(CircleShape)
                      .clickable(enabled = !uiState.isPublishing) {
                        viewModel.publishToLibrary(strings.publishedSuccessLabel)
                      }
                      .testTag("editor_publish_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    if (uiState.isPublishing) {
                      CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Rounded.Publish,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp),
                      )
                    }
                    Text(
                      text =
                        if (uiState.isPublishing) strings.publishingLabel
                        else if (isFa) "انتشار" else "Publish",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      maxLines = 1,
                    )
                  }
                }

                Box {
                  IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.MoreVert,
                      contentDescription = "Editor options",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    DropdownMenuItem(
                      leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                      text = { Text(strings.editBookDetailsTitle) },
                      onClick = {
                        showMoreMenu = false
                        showDetailsDialog = true
                      },
                    )
                    DropdownMenuItem(
                      leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                      text = { Text(strings.coverStudioAction) },
                      onClick = {
                        showMoreMenu = false
                        showCoverStudioDialog = true
                      },
                    )
                    if (uiState.chapters.size > 1) {
                      DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                        text = { Text(strings.reorderChaptersAction) },
                        onClick = {
                          showMoreMenu = false
                          showReorderChaptersDialog = true
                        },
                      )
                    }
                    if (!book?.publishedBookId.isNullOrBlank()) {
                      DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                        text = { Text(strings.openPublishedBookAction) },
                        onClick = {
                          showMoreMenu = false
                          book?.publishedBookId?.let(onOpenPublishedBook)
                        },
                      )
                    }
                    if (uiState.chapters.size > 1) {
                      DropdownMenuItem(
                        leadingIcon = {
                          Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                          )
                        },
                        text = { Text(strings.deleteChapterTitle, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                          showMoreMenu = false
                          showDeleteChapterDialog = true
                        },
                      )
                    }
                  }
                }
              }
            }

            // Chapter Tabs Strip + Add Chapter
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { idx, ch ->
                val selected = ch.id == uiState.selectedChapterId
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                  border =
                    BorderStroke(
                      1.dp,
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
                    ),
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        if (selected) showRenameChapterDialog = true
                        else viewModel.selectChapter(ch.id)
                      }
                      .testTag("editor_chapter_tab_${idx}"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = "${idx + 1}. ${if (selected) uiState.draftTitle.ifBlank { ch.title } else ch.title}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                      color =
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.widthIn(max = 150.dp),
                    )
                  }
                }
              }

              item {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier =
                    Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { viewModel.addChapter() }
                      .testTag("editor_add_chapter_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Add,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = strings.addChapterAction,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }

        // Distraction-Free Editorial Writing Canvas
        CompositionLocalProvider(LocalLayoutDirection provides bookDirection) {
          val scrollState = rememberScrollState()
          Column(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            BasicTextField(
              value = uiState.draftTitle,
              onValueChange = viewModel::onChapterTitleChange,
              singleLine = true,
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              textStyle =
                TextStyle(
                  fontFamily = headingFont,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onBackground,
                ),
              decorationBox = { inner ->
                Box {
                  if (uiState.draftTitle.isEmpty()) {
                    Text(
                      text = strings.chapterTitleLabel,
                      fontFamily = headingFont,
                      fontSize = 24.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                  }
                  inner()
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("editor_chapter_title_input"),
            )

            HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth(0.32f),
            )

            BasicTextField(
              value = bodyFieldValue,
              onValueChange = { next ->
                bodyFieldValue = next
                viewModel.onChapterBodyChange(next.text)
              },
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              textStyle =
                TextStyle(
                  fontFamily = bodyFont,
                  fontSize = editorFontSizeSp.sp,
                  lineHeight = (editorFontSizeSp * 1.68f).sp,
                  color = MaterialTheme.colorScheme.onBackground,
                ),
              decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                  if (bodyFieldValue.text.isEmpty()) {
                    Text(
                      text = strings.writeHereHint,
                      fontFamily = bodyFont,
                      fontSize = editorFontSizeSp.sp,
                      lineHeight = (editorFontSizeSp * 1.68f).sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                    )
                  }
                  inner()
                }
              },
              modifier = Modifier.fillMaxWidth().height(420.dp).testTag("editor_body_input"),
            )
          }
        }

        // Bottom Lightweight Markdown Toolbar + Font Size Control
        Surface(
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier =
              Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FormatToolChip("H2") { applyFormatting("\n## ", "\n", if (isRtlBook) "تیتر" else "Heading") }
            FormatToolChip(if (isRtlBook) "نقل‌قول" else "Quote") {
              applyFormatting("\n> ", "\n", if (isRtlBook) "نقل‌قول" else "Quote")
            }
            FormatToolChip("B") { applyFormatting("**", "**", if (isRtlBook) "متن" else "bold") }
            FormatToolChip("I") { applyFormatting("*", "*", if (isRtlBook) "مورب" else "italic") }
            FormatToolChip("• List") { applyFormatting("\n- ", "", "") }
            FormatToolChip("---") { applyFormatting("\n---\n", "", "") }

            Spacer(modifier = Modifier.width(6.dp))

            FormatToolChip("A-") { editorFontSizeSp = (editorFontSizeSp - 1).coerceAtLeast(13) }
            Text(
              text = "${editorFontSizeSp}sp",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FormatToolChip("A+") { editorFontSizeSp = (editorFontSizeSp + 1).coerceAtMost(24) }
          }
        }
      }
    }
  }

  if (showDetailsDialog && book != null) {
    ManuscriptDetailsDialog(
      dialogTitle = strings.editBookDetailsTitle,
      initialTitle = book.title,
      initialAuthor = book.author,
      initialSynopsis = book.synopsis,
      initialRtl = book.isRtl,
      confirmLabel = strings.saveChangesAction,
      onDismiss = { showDetailsDialog = false },
      onConfirm = { t, a, s, rtl ->
        viewModel.updateBookDetails(t, a, s, rtl)
        showDetailsDialog = false
      },
    )
  }

  if (showCoverStudioDialog && book != null) {
    StudioCoverDesignerDialog(
      book = book,
      isFa = isFa,
      onDismiss = { showCoverStudioDialog = false },
      onSaveCover = { themeId, pickedUri, clearCustomImage ->
        viewModel.updateBookCover(themeId, pickedUri, clearCustomImage)
        showCoverStudioDialog = false
      },
    )
  }

  if (showReorderChaptersDialog) {
    AlertDialog(
      onDismissRequest = { showReorderChaptersDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = {
        Text(
          text = strings.reorderChaptersAction,
          fontFamily = headingFont,
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          uiState.chapters.forEachIndexed { idx, ch ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth().testTag("reorder_chapter_row_${ch.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "${idx + 1}. ${if (ch.id == uiState.selectedChapterId) uiState.draftTitle.ifBlank { ch.title } else ch.title}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Row {
                  IconButton(
                    onClick = { viewModel.moveChapter(ch.id, -1) },
                    enabled = idx > 0,
                    modifier = Modifier.testTag("move_chapter_up_${ch.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.KeyboardArrowUp,
                      contentDescription = strings.moveUpAction,
                    )
                  }
                  IconButton(
                    onClick = { viewModel.moveChapter(ch.id, +1) },
                    enabled = idx < uiState.chapters.lastIndex,
                    modifier = Modifier.testTag("move_chapter_down_${ch.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.KeyboardArrowDown,
                      contentDescription = strings.moveDownAction,
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showReorderChaptersDialog = false }) {
          Text(strings.done)
        }
      },
    )
  }

  if (showRenameChapterDialog) {
    var tempTitle by remember(uiState.selectedChapterId) { mutableStateOf(uiState.draftTitle) }
    AlertDialog(
      onDismissRequest = { showRenameChapterDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(strings.renameChapterTitle, fontFamily = headingFont) },
      text = {
        OutlinedTextField(
          value = tempTitle,
          onValueChange = { tempTitle = it },
          label = { Text(strings.chapterTitleLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      dismissButton = {
        TextButton(onClick = { showRenameChapterDialog = false }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.renameActiveChapter(tempTitle)
            showRenameChapterDialog = false
          }
        ) {
          Text(strings.saveChangesAction)
        }
      },
    )
  }

  if (showDeleteChapterDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteChapterDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text(strings.deleteChapterTitle, fontFamily = headingFont) },
      text = { Text("“${uiState.draftTitle}”", color = MaterialTheme.colorScheme.onSurfaceVariant) },
      dismissButton = {
        TextButton(onClick = { showDeleteChapterDialog = false }) {
          Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteActiveChapter()
            showDeleteChapterDialog = false
          }
        ) {
          Text(strings.remove, color = MaterialTheme.colorScheme.error)
        }
      },
    )
  }
}

@Composable
private fun FormatToolChip(
  label: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}
