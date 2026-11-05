package app.hikari.domain.source.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.source.service.SourcePreferences
import app.hikari.core.common.preference.getAndSet

@Inject
class ToggleLanguage(
    val preferences: SourcePreferences,
) {

    fun await(language: String) {
        val isEnabled = language in preferences.enabledLanguages.get()
        preferences.enabledLanguages.getAndSet { enabled ->
            if (isEnabled) enabled.minus(language) else enabled.plus(language)
        }
    }
}
