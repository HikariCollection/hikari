package app.hikari.presentation.more.settings.screen.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.DeleteSweep
import app.hikari.icons.materialsymbols.rounded.SelectAll
import app.hikari.icons.materialsymbols.roundedfilled.Visibility
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.components.AppBarActions
import app.hikari.presentation.components.AppBarTitle
import app.hikari.presentation.components.SearchToolbar
import app.hikari.presentation.core.components.material.Scaffold
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.screens.EmptyScreen
import app.hikari.presentation.core.screens.LoadingScreen
import app.hikari.presentation.core.util.plus
import app.hikari.presentation.core.util.selectedBackground
import app.hikari.presentation.manga.components.MangaCover
import app.hikari.presentation.util.Screen
import app.hikari.presentation.util.relativeTimeSpanString
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import app.hikari.domain.manga.model.MangaCover as MangaCoverModel

class HiddenMangaScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<HiddenMangaViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        when (val current = state) {
            is HiddenMangaScreenState.Loading -> LoadingScreen()
            is HiddenMangaScreenState.Success -> {
                HiddenMangaContentScreen(
                    state = current,
                    onClickUnhide = viewModel::unhide,
                    onToggleSelection = viewModel::toggleSelection,
                    onClearSelection = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onUnhideSelected = viewModel::unhideSelected,
                    onShowUnhideAllDialog = viewModel::showUnhideAllConfirmDialog,
                    onDismissUnhideAllDialog = viewModel::dismissUnhideAllConfirmDialog,
                    onConfirmUnhideAll = viewModel::unhideAll,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    navigateUp = navigator::pop,
                )
            }
        }
    }
}

@Composable
private fun HiddenMangaContentScreen(
    state: HiddenMangaScreenState.Success,
    onClickUnhide: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onUnhideSelected: () -> Unit,
    onShowUnhideAllDialog: () -> Unit,
    onDismissUnhideAllDialog: () -> Unit,
    onConfirmUnhideAll: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    navigateUp: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = state.selectionMode) {
        onClearSelection()
    }

    Scaffold(
        topBar = { scrollBehavior ->
            if (state.selectionMode) {
                AppBar(
                    titleContent = { Text(text = "${state.selectedIds.size}") },
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_select_all),
                                    icon = MaterialSymbols.Rounded.SelectAll,
                                    onClick = onSelectAll,
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_unhide),
                                    icon = MaterialSymbols.RoundedFilled.Visibility,
                                    onClick = onUnhideSelected,
                                ),
                            ),
                        )
                    },
                    isActionMode = true,
                    onCancelActionMode = onClearSelection,
                )
            } else {
                SearchToolbar(
                    titleContent = { AppBarTitle(stringResource(MR.strings.pref_hidden_manga)) },
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = onSearchQueryChange,
                    placeholderText = stringResource(MR.strings.search_hidden_manga),
                    navigateUp = navigateUp,
                    actions = {
                        if (state.items.isNotEmpty()) {
                            AppBarActions(
                                listOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_unhide_all),
                                        icon = MaterialSymbols.Rounded.DeleteSweep,
                                        onClick = onShowUnhideAllDialog,
                                    ),
                                ),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { paddingValues ->
        if (state.items.isEmpty()) {
            EmptyScreen(
                stringRes = if (state.totalCount == 0) MR.strings.empty_hidden_manga else MR.strings.no_results_found,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues + PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = state.items,
                key = { it.hiddenManga.id },
            ) { item ->
                val isSelected = item.hiddenManga.id in state.selectedIds
                HiddenMangaRow(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (state.selectionMode) {
                            onToggleSelection(item.hiddenManga.id)
                        }
                    },
                    onLongClick = {
                        onToggleSelection(item.hiddenManga.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClickUnhide = { onClickUnhide(item.hiddenManga.id) },
                )
            }
        }
    }

    if (state.showUnhideAllDialog) {
        AlertDialog(
            onDismissRequest = onDismissUnhideAllDialog,
            dismissButton = {
                TextButton(onClick = onDismissUnhideAllDialog) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmUnhideAll) {
                    Text(text = stringResource(MR.strings.action_unhide))
                }
            },
            title = {
                Text(text = stringResource(MR.strings.action_unhide_all))
            },
            text = {
                Text(text = stringResource(MR.strings.are_you_sure))
            },
        )
    }
}

@Composable
private fun HiddenMangaRow(
    item: HiddenMangaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickUnhide: () -> Unit,
) {
    val hidden = item.hiddenManga
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectedBackground(isSelected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        MangaCover.Book(
            data = MangaCoverModel(
                mangaId = 0L,
                sourceId = hidden.sourceId,
                isMangaFavorite = false,
                url = hidden.thumbnailUrl,
                lastModified = null,
            ),
            modifier = Modifier.height(56.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = hidden.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.sourceName} • ${relativeTimeSpanString(hidden.createdAt.toEpochMilliseconds())}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onClickUnhide) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Visibility,
                contentDescription = stringResource(MR.strings.action_unhide),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
