package app.hikari.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hikari.domain.library.model.LibraryManga
import app.hikari.domain.manga.model.Manga
import app.hikari.i18n.MR
import app.hikari.presentation.core.components.material.SECONDARY_ALPHA
import app.hikari.presentation.core.i18n.pluralStringResource
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.manga.components.MangaCover
import app.hikari.ui.library.LibraryItem
import app.hikari.ui.library.RandomMangaFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomMangaPickerDialog(
    item: LibraryItem?,
    filter: RandomMangaFilter,
    candidateCount: Int,
    showAllCategoriesFilter: Boolean,
    onToggleUnreadOnly: () -> Unit,
    onToggleStartedOnly: () -> Unit,
    onToggleAllCategories: () -> Unit,
    onReroll: () -> Unit,
    onOpenManga: (Manga) -> Unit,
    onReadManga: (LibraryManga) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.random_manga_picker_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = filter.unreadOnly,
                        onClick = onToggleUnreadOnly,
                        label = { Text(text = stringResource(MR.strings.action_filter_unread)) },
                    )
                    FilterChip(
                        selected = filter.startedOnly,
                        onClick = onToggleStartedOnly,
                        label = { Text(text = stringResource(MR.strings.label_started)) },
                    )
                    if (showAllCategoriesFilter) {
                        FilterChip(
                            selected = filter.allCategories,
                            onClick = onToggleAllCategories,
                            label = { Text(text = stringResource(MR.strings.random_manga_all_categories)) },
                        )
                    }
                }

                if (item == null) {
                    Text(
                        text = stringResource(MR.strings.information_no_entries_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val manga = item.libraryManga.manga
                    val unreadCount = item.libraryManga.unreadCount.toInt()
                    val totalChapters = item.libraryManga.totalChapters.toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        MangaCover.Book(
                            data = manga,
                            modifier = Modifier.width(84.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = manga.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append(
                                        pluralStringResource(
                                            MR.plurals.manga_num_chapters,
                                            count = totalChapters,
                                            totalChapters,
                                        ),
                                    )
                                    if (unreadCount > 0) {
                                        append(
                                            " • $unreadCount ${stringResource(
                                                MR.strings.action_filter_unread,
                                            ).lowercase()}",
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_ALPHA),
                            )
                            val genres = manga.genre?.take(3)?.joinToString(", ")
                            if (!genres.isNullOrBlank()) {
                                Text(
                                    text = genres,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    enabled = candidateCount > 1,
                    onClick = onReroll,
                ) {
                    Text(text = stringResource(MR.strings.action_reroll))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (item != null) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onOpenManga(item.libraryManga.manga)
                        },
                    ) {
                        Text(text = stringResource(MR.strings.random_manga_open_details))
                    }
                    if (item.libraryManga.unreadCount > 0) {
                        TextButton(
                            onClick = {
                                onDismissRequest()
                                onReadManga(item.libraryManga)
                            },
                        ) {
                            Text(
                                text = stringResource(
                                    if (item.libraryManga.hasStarted) {
                                        MR.strings.action_resume
                                    } else {
                                        MR.strings.action_start
                                    },
                                ),
                            )
                        }
                    }
                } else {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(MR.strings.action_close))
                    }
                }
            }
        },
    )
}
