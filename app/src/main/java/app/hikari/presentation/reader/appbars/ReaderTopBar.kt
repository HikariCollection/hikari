package app.hikari.presentation.reader.appbars

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.Bookmark
import app.hikari.icons.materialsymbols.rounded.EditNote
import app.hikari.icons.materialsymbols.roundedfilled.Bookmark
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.components.AppBarActions
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.theme.active

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    bookmarked: Boolean,
    hasBookmarkNote: Boolean,
    onToggleBookmarked: () -> Unit,
    onEditBookmarkNote: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        backgroundColor = Color.Transparent,
        title = mangaTitle,
        subtitle = chapterTitle,
        navigateUp = navigateUp,
        actions = {
            AppBarActions(
                actions = buildList {
                    add(
                        AppBar.Action(
                            title = stringResource(
                                if (bookmarked) {
                                    MR.strings.action_remove_bookmark
                                } else {
                                    MR.strings.action_bookmark
                                },
                            ),
                            icon = if (bookmarked) {
                                MaterialSymbols.RoundedFilled.Bookmark
                            } else {
                                MaterialSymbols.Rounded.Bookmark
                            },
                            onClick = onToggleBookmarked,
                        ),
                    )
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_edit_bookmark_note),
                            icon = MaterialSymbols.Rounded.EditNote,
                            iconTint = if (hasBookmarkNote) {
                                MaterialTheme.colorScheme.active
                            } else {
                                LocalContentColor.current
                            },
                            onClick = onEditBookmarkNote,
                        ),
                    )
                    onOpenInWebView?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_in_web_view),
                                onClick = it,
                            ),
                        )
                    }
                    onOpenInBrowser?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_in_browser),
                                onClick = it,
                            ),
                        )
                    }
                    onShare?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_share),
                                onClick = it,
                            ),
                        )
                    }
                },
            )
        },
    )
}
