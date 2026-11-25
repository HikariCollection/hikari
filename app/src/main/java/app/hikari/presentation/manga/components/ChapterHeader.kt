package app.hikari.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.Close
import app.hikari.icons.materialsymbols.rounded.Search
import app.hikari.presentation.core.components.material.SECONDARY_ALPHA
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.pluralStringResource
import app.hikari.presentation.core.i18n.stringResource

@Composable
fun ChapterHeader(
    enabled: Boolean,
    chapterCount: Int?,
    missingChapterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onSearchQueryChange: (String?) -> Unit = {},
) {
    val isSearchActive = searchQuery != null
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                Text(
                    text = if (chapterCount == null) {
                        stringResource(MR.strings.chapters)
                    } else {
                        pluralStringResource(MR.plurals.manga_num_chapters, count = chapterCount, chapterCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                MissingChaptersWarning(missingChapterCount)
            }

            IconButton(
                enabled = enabled,
                onClick = {
                    if (isSearchActive) {
                        onSearchQueryChange(null)
                    } else {
                        onSearchQueryChange("")
                    }
                },
            ) {
                Icon(
                    imageVector = if (isSearchActive) MaterialSymbols.Rounded.Close else MaterialSymbols.Rounded.Search,
                    contentDescription = stringResource(
                        if (isSearchActive) MR.strings.action_close else MR.strings.action_search_chapters,
                    ),
                )
            }
        }

        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery.orEmpty(),
                onValueChange = { onSearchQueryChange(it) },
                placeholder = { Text(text = stringResource(MR.strings.action_search_chapters)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon = if (!searchQuery.isNullOrEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = stringResource(MR.strings.action_reset),
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, bottom = 4.dp)
                    .focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun MissingChaptersWarning(count: Int) {
    if (count == 0) {
        return
    }

    Text(
        text = pluralStringResource(MR.plurals.missing_chapters, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = SECONDARY_ALPHA),
    )
}
