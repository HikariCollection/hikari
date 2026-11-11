package app.hikari.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
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
import app.hikari.presentation.library.components.MangaListItem
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseSourceList(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    selectedMangaIds: Set<Long> = emptySet(),
    hiddenMangaUrls: Set<String> = emptySet(),
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (mangaList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = mangaList.itemCount) { index ->
            val manga by mangaList[index]?.collectAsState() ?: return@items
            BrowseSourceListItem(
                manga = manga,
                isSelected = manga.id in selectedMangaIds,
                isHidden = manga.url in hiddenMangaUrls,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        item {
            if (mangaList.loadState.refresh is LoadState.Loading || mangaList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceListItem(
    manga: Manga,
    isSelected: Boolean = false,
    isHidden: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    MangaListItem(
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
        badge = {
            InLibraryBadge(enabled = manga.favorite)
            HiddenBadge(enabled = isHidden)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
