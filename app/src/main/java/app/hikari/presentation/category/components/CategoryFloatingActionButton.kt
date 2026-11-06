package app.hikari.presentation.category.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.Add
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.util.shouldExpandFAB

@Composable
fun CategoryFloatingActionButton(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallExtendedFloatingActionButton(
        text = { Text(text = stringResource(MR.strings.action_add)) },
        icon = { Icon(imageVector = MaterialSymbols.Rounded.Add, contentDescription = null) },
        onClick = onCreate,
        expanded = lazyListState.shouldExpandFAB(),
        modifier = modifier,
    )
}
