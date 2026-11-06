package app.hikari.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.hikari.i18n.MR
import app.hikari.presentation.components.AppBar
import app.hikari.presentation.core.components.material.Scaffold
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.screens.LoadingScreen
import app.hikari.presentation.more.stats.StatsScreenContent
import app.hikari.presentation.more.stats.StatsScreenState
import app.hikari.presentation.util.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<StatsViewModel>()
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_stats),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (state is StatsScreenState.Loading) {
                LoadingScreen()
                return@Scaffold
            }

            StatsScreenContent(
                state = state as StatsScreenState.Success,
                paddingValues = paddingValues,
            )
        }
    }
}
