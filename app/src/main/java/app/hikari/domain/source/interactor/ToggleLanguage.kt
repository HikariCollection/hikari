package app.hikari.domain.source.interactor

import app.hikari.core.common.preference.getAndSet
import app.hikari.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

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
