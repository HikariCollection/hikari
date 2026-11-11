package app.hikari.presentation.browse.components

import androidx.compose.runtime.Composable
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.CollectionsBookmark
import app.hikari.icons.materialsymbols.roundedfilled.VisibilityOff
import app.hikari.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = MaterialSymbols.Rounded.CollectionsBookmark,
        )
    }
}

@Composable
internal fun HiddenBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = MaterialSymbols.RoundedFilled.VisibilityOff,
        )
    }
}
