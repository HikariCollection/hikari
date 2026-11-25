package app.hikari.ui.bookmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hikari.core.common.i18n.stringResource
import app.hikari.i18n.MR
import app.hikari.presentation.bookmarks.BookmarksAndNotesScreenContent
import app.hikari.presentation.bookmarks.components.BookmarksExportDialog
import app.hikari.presentation.bookmarks.components.UnbookmarkConfirmDialog
import app.hikari.presentation.manga.components.ChapterBookmarkNoteDialog
import app.hikari.presentation.util.Screen
import app.hikari.ui.manga.MangaScreen
import app.hikari.ui.manga.notes.MangaNotesScreen
import app.hikari.ui.reader.ReaderActivity
import app.hikari.util.system.toShareIntent
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

class BookmarksAndNotesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = metroViewModel<BookmarksAndNotesViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        BookmarksAndNotesScreenContent(
            state = state,
            snackbarHostState = viewModel.snackbarHostState,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSelectFilter = viewModel::selectFilter,
            onToggleLibraryOnly = viewModel::toggleLibraryOnly,
            onSelectSort = viewModel::selectSort,
            onToggleChapterSelection = viewModel::toggleChapterSelection,
            onClearSelection = viewModel::clearSelection,
            onSelectAll = viewModel::selectAll,
            onInvertSelection = viewModel::invertSelection,
            onUnbookmarkSelected = viewModel::requestUnbookmarkSelected,
            onClickExport = viewModel::showExportDialog,
            onOpenManga = { mangaId -> navigator.push(MangaScreen(mangaId)) },
            onOpenSeriesNotes = { manga -> navigator.push(MangaNotesScreen(manga)) },
            onOpenChapter = { chapter ->
                context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
            },
            onEditBookmarkNote = viewModel::showEditBookmarkNoteDialog,
            navigateUp = navigator::pop,
        )

        val onDismissDialog = viewModel::dismissDialog
        when (val dialog = (state as? BookmarksAndNotesViewModel.State.Success)?.dialog) {
            is BookmarksAndNotesViewModel.Dialog.EditBookmarkNote -> {
                ChapterBookmarkNoteDialog(
                    chapter = dialog.chapter,
                    onDismissRequest = onDismissDialog,
                    onSave = { note ->
                        viewModel.updateBookmarkNote(dialog.chapter.id, note)
                    },
                )
            }
            is BookmarksAndNotesViewModel.Dialog.ConfirmUnbookmark -> {
                UnbookmarkConfirmDialog(
                    onDismissRequest = onDismissDialog,
                    onConfirm = { clearNotes ->
                        viewModel.confirmUnbookmark(dialog.chapters, clearNotes = clearNotes)
                    },
                )
            }
            BookmarksAndNotesViewModel.Dialog.Export -> {
                BookmarksExportDialog(
                    onDismissRequest = onDismissDialog,
                    onExport = viewModel::exportAll,
                )
            }
            null -> {}
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    BookmarksAndNotesViewModel.Event.ExportEmpty -> {
                        viewModel.snackbarHostState.showSnackbar(
                            context.stringResource(MR.strings.export_bookmarks_empty),
                        )
                    }
                    is BookmarksAndNotesViewModel.Event.ShareFile -> {
                        context.startActivity(event.uri.toShareIntent(context, type = event.mimeType))
                    }
                }
            }
        }
    }
}
