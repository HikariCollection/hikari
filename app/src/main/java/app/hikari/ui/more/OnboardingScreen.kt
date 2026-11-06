package app.hikari.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.hikari.app.di.appGraph
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.core.util.collectAsState
import app.hikari.presentation.more.onboarding.OnboardingScreen
import app.hikari.presentation.more.settings.screen.SearchableSettings
import app.hikari.presentation.more.settings.screen.SettingsDataScreen
import app.hikari.presentation.util.Screen
import app.hikari.ui.browse.BrowseTab
import app.hikari.ui.setting.SettingsScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class OnboardingScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val basePreferences = remember { context.appGraph.basePreferences }
        val shownOnboardingFlow by basePreferences.shownOnboardingFlow.collectAsState()

        val finishOnboarding: () -> Unit = {
            basePreferences.shownOnboardingFlow.set(true)
            navigator.pop()
        }

        val restoreSettingKey = stringResource(SettingsDataScreen.restorePreferenceKeyString)

        BackHandler(enabled = !shownOnboardingFlow) {
            // Prevent exiting if onboarding hasn't been completed
        }

        OnboardingScreen(
            onComplete = finishOnboarding,
            onRestoreBackup = {
                finishOnboarding()
                SearchableSettings.highlightKey = restoreSettingKey
                navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage))
            },
            onBrowseExtensions = {
                finishOnboarding()
                BrowseTab.showExtension()
            },
        )
    }
}
