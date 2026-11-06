package app.hikari.domain.base

import app.hikari.core.common.preference.Preference
import app.hikari.core.common.preference.PreferenceStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class BasePreferences(
    preferenceStore: PreferenceStore,
) {

    val downloadedOnly: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("pref_downloaded_only"),
        false,
    )

    val incognitoMode: Preference<Boolean> = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    val shownOnboardingFlow: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("onboarding_complete"),
        false,
    )

    val highQualityRenderer: Preference<Boolean> = preferenceStore.getBoolean("pref_high_quality_renderer_key", false)

    val installationId: Preference<String> = preferenceStore.getString(Preference.appStateKey("installation_id"), "")

    val skippedAppUpdate: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("skipped_app_update"),
        "",
    )
}
