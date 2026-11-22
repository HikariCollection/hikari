package app.hikari.presentation.history.components

import androidx.compose.foundation.clickable
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
import app.hikari.presentation.core.i18n.stringResource

enum class HistoryExportFormat {
    CSV,
    JSON,
}

@Composable
fun HistoryExportDialog(
    onDismissRequest: () -> Unit,
    onExport: (HistoryExportFormat) -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(HistoryExportFormat.CSV) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.export_history_dialog_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FormatOption(
                    title = stringResource(MR.strings.export_history_as_csv),
                    selected = selectedFormat == HistoryExportFormat.CSV,
                    onClick = { selectedFormat = HistoryExportFormat.CSV },
                )
                FormatOption(
                    title = stringResource(MR.strings.export_history_as_json),
                    selected = selectedFormat == HistoryExportFormat.JSON,
                    onClick = { selectedFormat = HistoryExportFormat.JSON },
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
                Text(text = stringResource(MR.strings.action_export_history))
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
private fun FormatOption(
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
