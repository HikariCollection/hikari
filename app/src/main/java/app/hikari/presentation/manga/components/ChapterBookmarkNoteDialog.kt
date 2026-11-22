package app.hikari.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.i18n.MR
import app.hikari.presentation.core.i18n.stringResource

@Composable
fun ChapterBookmarkNoteDialog(
    chapter: Chapter,
    onDismissRequest: () -> Unit,
    onSave: (note: String?) -> Unit,
) {
    val existingNote = chapter.bookmarkNote.orEmpty()
    var noteText by rememberSaveable(chapter.id) { mutableStateOf(existingNote) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(text = stringResource(MR.strings.action_edit_bookmark_note)) },
                    placeholder = { Text(text = stringResource(MR.strings.bookmark_note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                )
            }
        },
        confirmButton = {
            Row {
                if (existingNote.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onSave(null)
                            onDismissRequest()
                        },
                    ) {
                        Text(
                            text = stringResource(MR.strings.action_remove_bookmark_note),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = {
                        onSave(noteText.trim())
                        onDismissRequest()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_save))
                }
            }
        },
    )
}
