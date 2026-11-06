package app.hikari.ui.browse.migration.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hikari.domain.manga.model.Manga
import app.hikari.feature.migration.config.MigrationConfigScreen
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.automirroredrounded.ArrowForward
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.core.components.FastScrollLazyColumn
import app.hikari.presentation.core.components.material.Scaffold
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.screens.EmptyScreen
import app.hikari.presentation.core.screens.LoadingScreen
import app.hikari.presentation.core.util.selectedBackground
import app.hikari.presentation.core.util.shouldExpandFAB
import app.hikari.presentation.manga.components.BaseMangaListItem
import app.hikari.presentation.util.Screen
import app.hikari.ui.manga.MangaScreen
import app.hikari.util.system.toast
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.flow.collectLatest

data class MigrateMangaScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel =
            assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> { create(sourceId = sourceId) }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        BackHandler(enabled = state.selectionMode) {
            viewModel.clearSelection()
        }

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = state.source!!.name,
                    navigateUp = {
                        if (state.selectionMode) {
                            viewModel.clearSelection()
                        } else {
                            navigator.pop()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                    icon = {
                        Icon(imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward, contentDescription = null)
                    },
                    onClick = {
                        val selection = state.selection
                        viewModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.selectionMode,
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
        ) { contentPadding ->
            if (state.isEmpty) {
                EmptyScreen(
                    stringRes = MR.strings.empty_screen,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            MigrateMangaContent(
                lazyListState = lazyListState,
                contentPadding = contentPadding,
                state = state,
                onClickItem = viewModel::toggleSelection,
                onClickCover = { navigator.push(MangaScreen(it.id)) },
            )
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MigrationMangaEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
                    }
                }
            }
        }
    }

    @Composable
    private fun MigrateMangaContent(
        lazyListState: LazyListState,
        contentPadding: PaddingValues,
        state: MigrateMangaViewModel.State,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
    ) {
        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            items(state.titles) { manga ->
                MigrateMangaItem(
                    manga = manga,
                    isSelected = manga.id in state.selection,
                    onClickItem = onClickItem,
                    onClickCover = onClickCover,
                )
            }
        }
    }

    @Composable
    private fun MigrateMangaItem(
        manga: Manga,
        isSelected: Boolean,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BaseMangaListItem(
            modifier = modifier.selectedBackground(isSelected),
            manga = manga,
            onClickItem = { onClickItem(manga) },
            onClickCover = { onClickCover(manga) },
        )
    }
}
