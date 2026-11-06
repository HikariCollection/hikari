package app.hikari.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hikari.extension.model.Extension
import app.hikari.i18n.MR
import app.hikari.presentation.browse.ExtensionScreen
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.components.TabContent
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.more.settings.screen.browse.ExtensionStoresScreen
import app.hikari.ui.browse.extension.details.ExtensionDetailsScreen
import app.hikari.ui.webview.WebViewScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
fun extensionsTab(
    extensionsViewModel: ExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    val updatesCount by extensionsViewModel.updatesCount.collectAsStateWithLifecycle()

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = updatesCount.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_filter),
                onClick = { navigator.push(ExtensionFilterScreen()) },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.extensionStores),
                onClick = { navigator.push(ExtensionStoresScreen()) },
            ),
        ),
        content = { contentPadding, _ ->
            val state by extensionsViewModel.state.collectAsStateWithLifecycle()

            BackHandler(enabled = state.searchQuery != null) {
                extensionsViewModel.search(null)
            }

            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is Extension.Available -> extensionsViewModel.installExtension(extension)
                        is Extension.Installed -> extensionsViewModel.uninstallExtension(extension)
                    }
                },
                onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsViewModel::updateAllExtensions,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsViewModel::installExtension,
                onOpenExtension = { navigator.push(ExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsViewModel.trustExtension(it) },
                onUninstallExtension = { extensionsViewModel.uninstallExtension(it) },
                onUpdateExtension = extensionsViewModel::updateExtension,
                onRefresh = extensionsViewModel::findAvailableExtensions,
            )
        },
    )
}
