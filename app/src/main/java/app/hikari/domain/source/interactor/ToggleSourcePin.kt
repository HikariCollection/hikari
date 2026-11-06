package app.hikari.domain.source.interactor

import app.hikari.core.common.preference.getAndSet
import app.hikari.domain.source.model.Source
import app.hikari.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

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
