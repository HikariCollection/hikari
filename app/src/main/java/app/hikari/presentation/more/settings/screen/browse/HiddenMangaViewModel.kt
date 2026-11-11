package app.hikari.presentation.more.settings.screen.browse

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hikari.domain.manga.interactor.GetHiddenManga
import app.hikari.domain.manga.interactor.UnhideManga
import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.source.service.SourceManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HiddenMangaViewModel(
    getHiddenManga: GetHiddenManga,
    private val unhideManga: UnhideManga,
    private val sourceManager: SourceManager,
) : ViewModel() {

    private val searchQuery = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showUnhideAllDialog = MutableStateFlow(false)

    val state: StateFlow<HiddenMangaScreenState> = combine(
        getHiddenManga.subscribeAll(),
        searchQuery,
        selectedIds,
        showUnhideAllDialog,
    ) { hiddenList, query, selection, unhideAllDialog ->
        val items = hiddenList.map { hidden ->
            HiddenMangaItem(
                hiddenManga = hidden,
                sourceName = sourceManager.getOrStub(hidden.sourceId).name,
            )
        }
        val filteredItems = if (query.isNullOrBlank()) {
            items
        } else {
            val normalizedQuery = query.trim().lowercase()
            items.filter { item ->
                item.hiddenManga.title.lowercase().contains(normalizedQuery) ||
                    item.sourceName.lowercase().contains(normalizedQuery)
            }
        }
        val validSelection = selection.intersect(filteredItems.mapTo(mutableSetOf()) { it.hiddenManga.id })
        HiddenMangaScreenState.Success(
            items = filteredItems,
            totalCount = items.size,
            searchQuery = query,
            selectedIds = validSelection,
            showUnhideAllDialog = unhideAllDialog,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), HiddenMangaScreenState.Loading)

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    fun toggleSelection(id: Long) {
        selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun selectAll() {
        val currentState = state.value as? HiddenMangaScreenState.Success ?: return
        selectedIds.value = currentState.items.mapTo(mutableSetOf()) { it.hiddenManga.id }
    }

    fun unhide(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            unhideManga.awaitByIds(setOf(id))
            selectedIds.update { it - id }
        }
    }

    fun unhideSelected() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            unhideManga.awaitByIds(ids)
        }
    }

    fun showUnhideAllConfirmDialog() {
        showUnhideAllDialog.value = true
    }

    fun dismissUnhideAllConfirmDialog() {
        showUnhideAllDialog.value = false
    }

    fun unhideAll() {
        val currentState = state.value as? HiddenMangaScreenState.Success ?: return
        val ids = currentState.items.mapTo(mutableSetOf()) { it.hiddenManga.id }
        dismissUnhideAllConfirmDialog()
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            unhideManga.awaitByIds(ids)
        }
    }
}

@Immutable
data class HiddenMangaItem(
    val hiddenManga: HiddenManga,
    val sourceName: String,
)

sealed interface HiddenMangaScreenState {
    @Immutable
    data object Loading : HiddenMangaScreenState

    @Immutable
    data class Success(
        val items: List<HiddenMangaItem>,
        val totalCount: Int,
        val searchQuery: String?,
        val selectedIds: Set<Long>,
        val showUnhideAllDialog: Boolean,
    ) : HiddenMangaScreenState {
        val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    }
}
