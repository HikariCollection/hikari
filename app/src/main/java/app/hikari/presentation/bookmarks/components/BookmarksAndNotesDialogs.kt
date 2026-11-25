package app.hikari.presentation.bookmarks.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hikari.i18n.MR
import app.hikari.presentation.core.components.LabeledCheckbox
import app.hikari.presentation.core.i18n.stringResource

enum class BookmarksExportFormat {
    MARKDOWN,
    JSON,
}

@Composable
fun BookmarksExportDialog(
    onDismissRequest: () -> Unit,
    onExport: (BookmarksExportFormat) -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(BookmarksExportFormat.MARKDOWN) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.export_bookmarks_dialog_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExportFormatOption(
                    title = stringResource(MR.strings.export_bookmarks_as_markdown),
                    selected = selectedFormat == BookmarksExportFormat.MARKDOWN,
                    onClick = { selectedFormat = BookmarksExportFormat.MARKDOWN },
                )
                ExportFormatOption(
                    title = stringResource(MR.strings.export_history_as_json),
                    selected = selectedFormat == BookmarksExportFormat.JSON,
                    onClick = { selectedFormat = BookmarksExportFormat.JSON },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(selectedFormat)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun UnbookmarkConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (clearNotes: Boolean) -> Unit,
) {
    var clearNotes by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.unbookmark_confirm_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledCheckbox(
                    label = stringResource(MR.strings.unbookmark_clear_notes_option),
                    checked = clearNotes,
                    onCheckedChange = { clearNotes = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(clearNotes)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_remove_bookmark))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun ExportFormatOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
