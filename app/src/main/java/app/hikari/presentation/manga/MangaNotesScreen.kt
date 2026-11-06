package app.hikari.presentation.manga

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.hikari.i18n.MR
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.components.AppBarTitle
import app.hikari.presentation.core.components.material.Scaffold
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.manga.components.MangaNotesTextArea
import app.hikari.ui.manga.notes.MangaNotesScreen

@Composable
fun MangaNotesScreen(
    state: MangaNotesScreen.State,
    navigateUp: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    Scaffold(
        topBar = { topBarScrollBehavior ->
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.action_edit_notes),
                        subtitle = state.manga.title,
                    )
                },
                navigateUp = navigateUp,
                scrollBehavior = topBarScrollBehavior,
            )
        },
    ) { contentPadding ->
        MangaNotesTextArea(
            state = state,
            onUpdate = onUpdate,
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding(),
        )
    }
}
