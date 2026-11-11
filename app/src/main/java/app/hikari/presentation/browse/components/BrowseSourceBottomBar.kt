package app.hikari.presentation.browse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hikari.i18n.MR
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.Favorite
import app.hikari.icons.materialsymbols.roundedfilled.Favorite
import app.hikari.icons.materialsymbols.roundedfilled.Visibility
import app.hikari.icons.materialsymbols.roundedfilled.VisibilityOff
import app.hikari.presentation.core.i18n.stringResource

@Composable
fun BrowseSourceBottomBar(
    visible: Boolean,
    allFavorite: Boolean,
    allHidden: Boolean,
    onFavoriteClicked: () -> Unit,
    onHideClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 150)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom),
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                BottomBarAction(
                    title = stringResource(
                        if (allFavorite) MR.strings.action_remove else MR.strings.action_favorite,
                    ),
                    icon = if (allFavorite) {
                        MaterialSymbols.RoundedFilled.Favorite
                    } else {
                        MaterialSymbols.Rounded.Favorite
                    },
                    onClick = onFavoriteClicked,
                )
                BottomBarAction(
                    title = stringResource(
                        if (allHidden) MR.strings.action_unhide else MR.strings.action_hide,
                    ),
                    icon = if (allHidden) {
                        MaterialSymbols.RoundedFilled.Visibility
                    } else {
                        MaterialSymbols.RoundedFilled.VisibilityOff
                    },
                    onClick = onHideClicked,
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
