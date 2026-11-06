package app.hikari.data.backup.create.creators

import app.hikari.core.common.preference.Preference
import app.hikari.core.common.preference.PreferenceStore
import app.hikari.data.backup.models.BackupPreference
import app.hikari.data.backup.models.BackupSourcePreferences
import app.hikari.data.backup.models.BooleanPreferenceValue
import app.hikari.data.backup.models.FloatPreferenceValue
import app.hikari.data.backup.models.IntPreferenceValue
import app.hikari.data.backup.models.LongPreferenceValue
import app.hikari.data.backup.models.StringPreferenceValue
import app.hikari.data.backup.models.StringSetPreferenceValue
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.ConfigurableSource
import app.hikari.source.preferenceKey
import app.hikari.source.sourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class PreferenceBackupCreator(
    private val sourceManager: SourceManager,
    private val preferenceStore: PreferenceStore,
) {

    fun createApp(includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStore.getAll().toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    suspend fun createSource(includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourceManager.getAll()
            .filterIsInstance<ConfigurableSource>()
            .map {
                BackupSourcePreferences(
                    it.preferenceKey(),
                    it.sourcePreferences().all.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                )
            }
            .filter { it.prefs.isNotEmpty() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, *>.toBackupPreferences(): List<BackupPreference> {
        return this
            .filterKeys { !Preference.isAppState(it) }
            .mapNotNull { (key, value) ->
                when (value) {
                    is Int -> BackupPreference(key, IntPreferenceValue(value))
                    is Long -> BackupPreference(key, LongPreferenceValue(value))
                    is Float -> BackupPreference(key, FloatPreferenceValue(value))
                    is String -> BackupPreference(key, StringPreferenceValue(value))
                    is Boolean -> BackupPreference(key, BooleanPreferenceValue(value))
                    is Set<*> -> (value as? Set<String>)?.let {
                        BackupPreference(key, StringSetPreferenceValue(it))
                    }
                    else -> null
                }
            }
    }

    private fun List<BackupPreference>.withPrivatePreferences(include: Boolean) =
        if (include) {
            this
        } else {
            this.filter { !Preference.isPrivate(it.key) }
        }
}
