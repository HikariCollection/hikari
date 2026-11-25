package app.hikari.ui.bookmarks

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hikari.domain.bookmark.interactor.ExportBookmarksAndNotes
import app.hikari.domain.bookmark.interactor.GetBookmarksAndNotes
import app.hikari.domain.bookmark.interactor.UnbookmarkChapters
import app.hikari.domain.bookmark.model.BookmarksAndNotesFilter
import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.bookmark.model.BookmarksAndNotesQuery
import app.hikari.domain.bookmark.model.BookmarksAndNotesSort
import app.hikari.domain.chapter.interactor.SetChapterBookmarkMemo
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.source.service.SourceManager
import app.hikari.presentation.bookmarks.components.BookmarksExportFormat
import app.hikari.util.storage.getUriCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class BookmarksAndNotesViewModel(
    private val context: Context,
    private val getBookmarksAndNotes: GetBookmarksAndNotes,
    private val unbookmarkChapters: UnbookmarkChapters,
    private val setChapterBookmarkMemo: SetChapterBookmarkMemo,
    private val exportBookmarksAndNotes: ExportBookmarksAndNotes,
    private val sourceManager: SourceManager,
) : ViewModel() {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val query = MutableStateFlow(BookmarksAndNotesQuery())
    private val selectedChapterIds = MutableStateFlow<Set<Long>>(emptySet())
    private val dialog = MutableStateFlow<Dialog?>(null)

    private val groupsFlow = getBookmarksAndNotes.subscribe(query) { sourceId ->
        sourceManager.getOrStub(sourceId).name
    }

    val state: StateFlow<State> = combine(
        groupsFlow,
        query,
        selectedChapterIds,
        dialog,
    ) { groups, currentQuery, selectedIds, currentDialog ->
        State.Success(
            groups = groups,
            query = currentQuery,
            selectedChapterIds = pruneSelection(selectedIds, groups),
            dialog = currentDialog,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State.Loading)

    fun updateSearchQuery(searchQuery: String?) {
        query.update { it.copy(searchQuery = searchQuery) }
    }

    fun selectFilter(filter: BookmarksAndNotesFilter) {
        query.update { it.copy(filter = filter) }
    }

    fun toggleLibraryOnly() {
        query.update { it.copy(libraryOnly = !it.libraryOnly) }
    }

    fun selectSort(sort: BookmarksAndNotesSort) {
        query.update { it.copy(sort = sort) }
    }

    fun toggleChapterSelection(chapterId: Long) {
        selectedChapterIds.update { current ->
            if (chapterId in current) current - chapterId else current + chapterId
        }
    }

    fun clearSelection() {
        selectedChapterIds.value = emptySet()
    }

    fun selectAll() {
        val success = state.value as? State.Success ?: return
        selectedChapterIds.value = selectAllVisible(success.groups)
    }

    fun invertSelection() {
        val success = state.value as? State.Success ?: return
        selectedChapterIds.value = invertVisibleSelection(success.selectedChapterIds, success.groups)
    }

    fun requestUnbookmarkSelected() {
        val success = state.value as? State.Success ?: return
        val selected = success.selectedChapters
        if (selected.isEmpty()) return

        if (requiresNoteClearPrompt(selected)) {
            dialog.value = Dialog.ConfirmUnbookmark(selected)
        } else {
            executeUnbookmark(selected, clearNotes = false)
        }
    }

    fun confirmUnbookmark(chapters: List<Chapter>, clearNotes: Boolean) {
        dismissDialog()
        executeUnbookmark(chapters, clearNotes)
    }

    private fun executeUnbookmark(chapters: List<Chapter>, clearNotes: Boolean) {
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            unbookmarkChapters.await(chapters, clearNotes = clearNotes)
        }
    }

    fun showEditBookmarkNoteDialog(chapter: Chapter) {
        dialog.value = Dialog.EditBookmarkNote(chapter)
    }

    fun updateBookmarkNote(chapterId: Long, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            setChapterBookmarkMemo(chapterId, note)
        }
    }

    fun showExportDialog() {
        dialog.value = Dialog.Export
    }

    fun dismissDialog() {
        dialog.value = null
    }

    fun exportAll(format: BookmarksExportFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = getBookmarksAndNotes.awaitAll { sourceId ->
                sourceManager.getOrStub(sourceId).name
            }
            if (groups.isEmpty()) {
                _events.send(Event.ExportEmpty)
                return@launch
            }

            val (content, extension, mimeType) = when (format) {
                BookmarksExportFormat.MARKDOWN -> Triple(
                    exportBookmarksAndNotes.toMarkdown(groups),
                    "md",
                    "text/markdown",
                )
                BookmarksExportFormat.JSON -> Triple(
                    exportBookmarksAndNotes.toJson(groups),
                    "json",
                    "application/json",
                )
            }
            val filename = "hikari_bookmarks_notes_${Clock.System.now().toEpochMilliseconds()}.$extension"
            val exportDir = File(context.cacheDir, "bookmark_exports").apply { mkdirs() }
            val file = File(exportDir, filename)
            file.writeText(content)
            _events.send(Event.ShareFile(file.getUriCompat(context), mimeType))
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val groups: List<BookmarksAndNotesGroup>,
            val query: BookmarksAndNotesQuery,
            val selectedChapterIds: Set<Long>,
            val dialog: Dialog?,
        ) : State {
            val selectionMode: Boolean
                get() = selectedChapterIds.isNotEmpty()

            val selectedChapters: List<Chapter>
                get() = groups
                    .asSequence()
                    .flatMap { it.chapters.asSequence() }
                    .map { it.chapter }
                    .filter { it.id in selectedChapterIds }
                    .toList()
        }
    }

    sealed interface Dialog {
        data class EditBookmarkNote(val chapter: Chapter) : Dialog
        data class ConfirmUnbookmark(val chapters: List<Chapter>) : Dialog
        data object Export : Dialog
    }

    sealed interface Event {
        data class ShareFile(val uri: Uri, val mimeType: String) : Event
        data object ExportEmpty : Event
    }
}
