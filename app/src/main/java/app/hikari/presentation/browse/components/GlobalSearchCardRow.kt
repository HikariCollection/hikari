package app.hikari.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaCover
import app.hikari.domain.manga.model.asMangaCover
import app.hikari.i18n.MR
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.library.components.CommonMangaItemDefaults
import app.hikari.presentation.library.components.MangaComfortableGridItem

@Composable
fun GlobalSearchCardRow(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
    hiddenKeys: Set<Pair<Long, String>> = emptySet(),
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(titles) {
            val title by getManga(it)
            MangaItem(
                title = title.title,
                cover = title.asMangaCover(),
                isFavorite = title.favorite,
                isHidden = (title.source to title.url) in hiddenKeys,
                onClick = { onClick(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}

@Composable
private fun MangaItem(
    title: String,
    cover: MangaCover,
    isFavorite: Boolean,
    isHidden: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(96.dp)) {
        MangaComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverBadgeEnd = {
                HiddenBadge(enabled = isHidden)
            },
            coverAlpha = when {
                isHidden -> CommonMangaItemDefaults.BrowseHiddenCoverAlpha
                isFavorite -> CommonMangaItemDefaults.BrowseFavoriteCoverAlpha
                else -> 1f
            },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@Composable
private fun EmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
    )
}
