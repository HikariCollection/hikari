package app.hikari.domain.source.interactor

import app.hikari.core.common.preference.getAndSet
import app.hikari.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class ToggleIncognito(
    private val preferences: SourcePreferences,
) {
    fun await(extensions: String, enable: Boolean) {
        preferences.incognitoExtensions.getAndSet {
            if (enable) it.plus(extensions) else it.minus(extensions)
        }
    }
}
