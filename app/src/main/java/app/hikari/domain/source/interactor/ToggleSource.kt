package app.hikari.domain.source.interactor

import app.hikari.core.common.preference.getAndSet
import app.hikari.domain.source.model.Source
import app.hikari.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class ToggleSource(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source, enable: Boolean = isEnabled(source.id)) {
        await(source.id, enable)
    }

    fun await(sourceId: Long, enable: Boolean = isEnabled(sourceId)) {
        preferences.disabledSources.getAndSet { disabled ->
            if (enable) disabled.minus("$sourceId") else disabled.plus("$sourceId")
        }
    }

    fun await(sourceIds: List<Long>, enable: Boolean) {
        val transformedSourceIds = sourceIds.map { it.toString() }
        preferences.disabledSources.getAndSet { disabled ->
            if (enable) disabled.minus(transformedSourceIds) else disabled.plus(transformedSourceIds)
        }
    }

    private fun isEnabled(sourceId: Long): Boolean {
        return sourceId.toString() in preferences.disabledSources.get()
    }
}
