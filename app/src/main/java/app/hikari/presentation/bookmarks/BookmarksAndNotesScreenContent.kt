package app.hikari.presentation.bookmarks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hikari.domain.bookmark.model.BookmarksAndNotesFilter
import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.bookmark.model.BookmarksAndNotesSort
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.asMangaCover
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.BookmarkRemove
import app.hikari.icons.materialsymbols.rounded.EditNote
import app.hikari.icons.materialsymbols.rounded.FlipToBack
import app.hikari.icons.materialsymbols.rounded.SelectAll
import app.hikari.icons.materialsymbols.rounded.Share
import app.hikari.icons.materialsymbols.rounded.SortByAlpha
import app.hikari.icons.materialsymbols.roundedfilled.Bookmark
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.components.AppBarActions
import app.hikari.presentation.components.AppBarTitle
import app.hikari.presentation.components.DropdownMenu
import app.hikari.presentation.components.RadioMenuItem
import app.hikari.presentation.components.SearchToolbar
import app.hikari.presentation.components.relativeDateText
import app.hikari.presentation.core.components.Pill
import app.hikari.presentation.core.components.material.DISABLED_ALPHA
import app.hikari.presentation.core.components.material.SECONDARY_ALPHA
import app.hikari.presentation.core.components.material.Scaffold
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.screens.EmptyScreen
import app.hikari.presentation.core.screens.LoadingScreen
import app.hikari.presentation.core.util.selectedBackground
import app.hikari.presentation.manga.components.DotSeparatorText
import app.hikari.presentation.manga.components.MangaCover
import app.hikari.presentation.util.formatChapterNumber
import app.hikari.ui.bookmarks.BookmarksAndNotesViewModel

@Composable
fun BookmarksAndNotesScreenContent(
    state: BookmarksAndNotesViewModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onSelectFilter: (BookmarksAndNotesFilter) -> Unit,
    onToggleLibraryOnly: () -> Unit,
    onSelectSort: (BookmarksAndNotesSort) -> Unit,
    onToggleChapterSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onUnbookmarkSelected: () -> Unit,
    onClickExport: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onOpenSeriesNotes: (Manga) -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onEditBookmarkNote: (Chapter) -> Unit,
    navigateUp: () -> Unit,
) {
    val successState = state as? BookmarksAndNotesViewModel.State.Success
    val selectionMode = successState?.selectionMode == true

    BackHandler(enabled = selectionMode) {
        onClearSelection()
    }

    Scaffold(
        topBar = { scrollBehavior ->
            if (successState != null && selectionMode) {
                AppBar(
                    titleContent = { Text(text = "${successState.selectedChapterIds.size}") },
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_select_all),
                                    icon = MaterialSymbols.Rounded.SelectAll,
                                    onClick = onSelectAll,
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_select_inverse),
                                    icon = MaterialSymbols.Rounded.FlipToBack,
                                    onClick = onInvertSelection,
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_remove_bookmark),
                                    icon = MaterialSymbols.Rounded.BookmarkRemove,
                                    onClick = onUnbookmarkSelected,
                                ),
                            ),
                        )
                    },
                    isActionMode = true,
                    onCancelActionMode = onClearSelection,
                )
            } else {
                var sortMenuExpanded by remember { mutableStateOf(false) }
                SearchToolbar(
                    titleContent = { AppBarTitle(stringResource(MR.strings.label_bookmarks_and_notes)) },
                    searchQuery = successState?.query?.searchQuery,
                    onChangeSearchQuery = onSearchQueryChange,
                    placeholderText = stringResource(MR.strings.search_bookmarks_and_notes),
                    navigateUp = navigateUp,
                    actions = {
                        if (successState != null) {
                            Box {
                                AppBarActions(
                                    listOf(
                                        AppBar.Action(
                                            title = stringResource(MR.strings.action_sort),
                                            icon = MaterialSymbols.Rounded.SortByAlpha,
                                            onClick = { sortMenuExpanded = true },
                                        ),
                                    ),
                                )
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                ) {
                                    RadioMenuItem(
                                        text = { Text(stringResource(MR.strings.bookmarks_sort_recent)) },
                                        isChecked = successState.query.sort == BookmarksAndNotesSort.RECENT,
                                        onClick = {
                                            onSelectSort(BookmarksAndNotesSort.RECENT)
                                            sortMenuExpanded = false
                                        },
                                    )
                                    RadioMenuItem(
                                        text = { Text(stringResource(MR.strings.bookmarks_sort_title)) },
                                        isChecked = successState.query.sort == BookmarksAndNotesSort.TITLE,
                                        onClick = {
                                            onSelectSort(BookmarksAndNotesSort.TITLE)
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                            AppBarActions(
                                listOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_export_bookmarks_and_notes),
                                        icon = MaterialSymbols.Rounded.Share,
                                        onClick = onClickExport,
                                    ),
                                ),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        if (successState == null) {
            LoadingScreen(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            FilterChipsRow(
                selectedFilter = successState.query.filter,
                libraryOnly = successState.query.libraryOnly,
                onSelectFilter = onSelectFilter,
                onToggleLibraryOnly = onToggleLibraryOnly,
            )

            if (successState.groups.isEmpty()) {
                val emptyRes = if (!successState.query.searchQuery.isNullOrBlank()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.bookmarks_empty
                }
                EmptyScreen(
                    stringRes = emptyRes,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        items = successState.groups,
                        key = { it.manga.id },
                    ) { group ->
                        MangaBookmarksGroupItem(
                            group = group,
                            selectedChapterIds = successState.selectedChapterIds,
                            selectionMode = selectionMode,
                            onOpenManga = { onOpenManga(group.manga.id) },
                            onOpenSeriesNotes = { onOpenSeriesNotes(group.manga) },
                            onOpenChapter = onOpenChapter,
                            onToggleChapterSelection = onToggleChapterSelection,
                            onEditBookmarkNote = onEditBookmarkNote,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: BookmarksAndNotesFilter,
    libraryOnly: Boolean,
    onSelectFilter: (BookmarksAndNotesFilter) -> Unit,
    onToggleLibraryOnly: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedFilter == BookmarksAndNotesFilter.ALL,
                onClick = { onSelectFilter(BookmarksAndNotesFilter.ALL) },
                label = { Text(text = stringResource(MR.strings.bookmarks_filter_all)) },
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == BookmarksAndNotesFilter.BOOKMARKS,
                onClick = { onSelectFilter(BookmarksAndNotesFilter.BOOKMARKS) },
                label = { Text(text = stringResource(MR.strings.bookmarks_filter_bookmarks)) },
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == BookmarksAndNotesFilter.CHAPTER_NOTES,
                onClick = { onSelectFilter(BookmarksAndNotesFilter.CHAPTER_NOTES) },
                label = { Text(text = stringResource(MR.strings.bookmarks_filter_chapter_notes)) },
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == BookmarksAndNotesFilter.MANGA_NOTES,
                onClick = { onSelectFilter(BookmarksAndNotesFilter.MANGA_NOTES) },
                label = { Text(text = stringResource(MR.strings.bookmarks_filter_manga_notes)) },
            )
        }
        item {
            FilterChip(
                selected = libraryOnly,
                onClick = onToggleLibraryOnly,
                label = { Text(text = stringResource(MR.strings.in_library)) },
            )
        }
    }
}

@Composable
private fun MangaBookmarksGroupItem(
    group: BookmarksAndNotesGroup,
    selectedChapterIds: Set<Long>,
    selectionMode: Boolean,
    onOpenManga: () -> Unit,
    onOpenSeriesNotes: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onToggleChapterSelection: (Long) -> Unit,
    onEditBookmarkNote: (Chapter) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenManga)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MangaCover.Book(
                data = group.manga.asMangaCover(),
                modifier = Modifier.height(60.dp),
                onClick = onOpenManga,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = group.manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = group.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!group.manga.favorite) {
                        Pill(
                            text = stringResource(MR.strings.bookmarks_not_in_library),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            IconButton(onClick = onOpenSeriesNotes) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.EditNote,
                    contentDescription = stringResource(MR.strings.action_edit_notes),
                    tint = if (group.manga.notes.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        if (group.manga.notes.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .padding(bottom = 8.dp)
                    .clickable(onClick = onOpenSeriesNotes),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.bookmarks_series_notes_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = group.manga.notes.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        group.chapters.forEach { entry ->
            BookmarkChapterRow(
                entry = entry,
                selected = entry.chapter.id in selectedChapterIds,
                selectionMode = selectionMode,
                onClick = {
                    if (selectionMode) {
                        onToggleChapterSelection(entry.chapter.id)
                    } else {
                        onOpenChapter(entry.chapter)
                    }
                },
                onLongClick = { onToggleChapterSelection(entry.chapter.id) },
                onEditNote = { onEditBookmarkNote(entry.chapter) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun BookmarkChapterRow(
    entry: BookmarkedChapterEntry,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditNote: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val chapter = entry.chapter
    val note = chapter.bookmarkNote
    val dateText = entry.activityAt.let { relativeDateText(it) }
    val progressText = chapter.lastPageRead
        .takeIf { !chapter.read && it > 0 }
        ?.let { stringResource(MR.strings.chapter_progress, it + 1) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (chapter.bookmark) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Pill(
                        text = stringResource(MR.strings.bookmarks_note_only_label),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                val title = if (chapter.isRecognizedNumber && chapter.name.isBlank()) {
                    stringResource(MR.strings.display_mode_chapter, formatChapterNumber(chapter.chapterNumber))
                } else {
                    chapter.name
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = if (chapter.read) DISABLED_ALPHA else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val subtitleStyle = MaterialTheme.typography.bodySmall.merge(
                    color = LocalContentColor.current.copy(
                        alpha = if (chapter.read) DISABLED_ALPHA else SECONDARY_ALPHA,
                    ),
                )
                ProvideTextStyle(value = subtitleStyle) {
                    Text(
                        text = dateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (progressText != null) {
                        DotSeparatorText()
                        Text(
                            text = progressText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    chapter.scanlator?.takeIf { it.isNotBlank() }?.let { scanlator ->
                        DotSeparatorText()
                        Text(
                            text = scanlator,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!note.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = !selectionMode, onClick = onEditNote)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.EditNote,
                        contentDescription = stringResource(MR.strings.action_edit_bookmark_note),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!selectionMode) {
            IconButton(onClick = onEditNote) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.EditNote,
                    contentDescription = stringResource(MR.strings.action_edit_bookmark_note),
                    tint = if (!note.isNullOrBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
