package app.hikari.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.Info
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.util.secondaryItemAlpha
import app.hikari.presentation.theme.HikariPreviewTheme

@Composable
internal fun InfoWidget(text: String) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = PrefsHorizontalPadding,
                vertical = MaterialTheme.padding.medium,
            )
            .secondaryItemAlpha(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Info,
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    HikariPreviewTheme {
        Surface {
            InfoWidget(text = stringResource(MR.strings.download_ahead_info))
        }
    }
}
