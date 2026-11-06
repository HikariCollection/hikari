package app.hikari.core.migration.migrations

import app.hikari.core.common.util.system.logcat
import app.hikari.core.migration.Migration
import app.hikari.core.migration.MigrationContext
import app.hikari.domain.extension.model.ExtensionStore
import app.hikari.domain.extension.repository.ExtensionStoreRepository
import app.hikari.domain.source.service.SourcePreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority

@Inject
@ContributesIntoSet(AppScope::class)
class TrustExtensionRepositoryMigration(
    private val sourcePreferences: SourcePreferences,
    private val repository: ExtensionStoreRepository,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withContext(Dispatchers.IO) {
        repository.upsert(
            ExtensionStore(
                indexUrl = "https://raw.githubusercontent.com/HikariCollection/extensions/repo/index.min.json",
                name = "Hikari Extensions",
                badgeLabel = "Official",
                signingKey = "956e72c8d1dc56c663b07c09d537e10614c25620650015b0f3e22a3d3a2e2e32",
                contact = ExtensionStore.Contact(
                    website = "https://github.com/HikariCollection",
                    discord = null,
                ),
                isLegacy = false,
                extensionListUrl = null,
            ),
        )
        for ((index, source) in sourcePreferences.extensionRepos.get().withIndex()) {
            try {
                repository.insertFromPreference(
                    indexUrl = source.removeSuffix("/index.min.json").removeSuffix("/index.json") + "/repo.json",
                    name = "Repo #${index + 1}",
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error Migrating Extension Repo with baseUrl: $source" }
            }
        }
        sourcePreferences.extensionRepos.delete()
        return@withContext true
    }
}
