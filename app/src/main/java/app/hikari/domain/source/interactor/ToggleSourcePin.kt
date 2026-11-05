package app.hikari.domain.source.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.source.service.SourcePreferences
import app.hikari.core.common.preference.getAndSet
import app.hikari.domain.source.model.Source

@Inject
class ToggleSourcePin(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        val isPinned = source.id.toString() in preferences.pinnedSources.get()
        preferences.pinnedSources.getAndSet { pinned ->
            if (isPinned) pinned.minus("${source.id}") else pinned.plus("${source.id}")
        }
    }
}
