package app.hikari.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.hasCustomMetadata
import app.hikari.i18n.MR
import app.hikari.presentation.core.i18n.stringResource

@Composable
fun EditMangaInfoDialog(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onSave: (title: String, author: String, artist: String, description: String, genresInput: String) -> Unit,
    onReset: () -> Unit,
) {
    var title by rememberSaveable(manga.id) { mutableStateOf(manga.title) }
    var author by rememberSaveable(manga.id) { mutableStateOf(manga.author.orEmpty()) }
    var artist by rememberSaveable(manga.id) { mutableStateOf(manga.artist.orEmpty()) }
    var description by rememberSaveable(manga.id) { mutableStateOf(manga.description.orEmpty()) }
    var genres by rememberSaveable(manga.id) {
        mutableStateOf(manga.genre.orEmpty().joinToString(", "))
    }

    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.dialog_edit_info_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(MR.strings.label_manga_title)) },
                    singleLine = true,
                    isError = !canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(text = stringResource(MR.strings.label_manga_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(text = stringResource(MR.strings.label_manga_artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text(text = stringResource(MR.strings.label_manga_genres)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(MR.strings.label_manga_description)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (manga.hasCustomMetadata()) {
                    TextButton(
                        onClick = {
                            onReset()
                            onDismissRequest()
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_reset))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = {
                        onSave(title, author, artist, description, genres)
                        onDismissRequest()
                    },
                    enabled = canSave,
                ) {
                    Text(text = stringResource(MR.strings.action_save))
                }
            }
        },
    )
}
