package app.hikari.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaCover
import app.hikari.presentation.core.util.plus
import app.hikari.presentation.library.components.CommonMangaItemDefaults
import app.hikari.presentation.library.components.MangaCompactGridItem
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseSourceCompactGrid(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    selectedMangaIds: Set<Long> = emptySet(),
    hiddenMangaUrls: Set<String> = emptySet(),
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
    ) {
        if (mangaList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = mangaList.itemCount) { index ->
            val manga by mangaList[index]?.collectAsState() ?: return@items
            BrowseSourceCompactGridItem(
                manga = manga,
                isSelected = manga.id in selectedMangaIds,
                isHidden = manga.url in hiddenMangaUrls,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        if (mangaList.loadState.refresh is LoadState.Loading || mangaList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceCompactGridItem(
    manga: Manga,
    isSelected: Boolean = false,
    isHidden: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    MangaCompactGridItem(
        title = manga.title,
        isSelected = isSelected,
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        coverAlpha = when {
            isHidden -> CommonMangaItemDefaults.BrowseHiddenCoverAlpha
            manga.favorite -> CommonMangaItemDefaults.BrowseFavoriteCoverAlpha
            else -> 1f
        },
        coverBadgeStart = {
            InLibraryBadge(enabled = manga.favorite)
        },
        coverBadgeEnd = {
            HiddenBadge(enabled = isHidden)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
