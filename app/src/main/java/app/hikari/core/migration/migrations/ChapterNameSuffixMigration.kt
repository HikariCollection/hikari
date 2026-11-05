package app.hikari.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import app.hikari.core.migration.Migration
import app.hikari.core.migration.MigrationContext
import app.hikari.domain.library.service.LibraryPreferences

@Inject
@ContributesIntoSet(AppScope::class)
class ChapterNameSuffixMigration(
    private val preference: LibraryPreferences,
) : Migration {
    override val version: Float = 34f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        if (migrationContext.previousVersion in 13..33) {
            preference.enableChapterNameHash.set(true)
        }
        return true
    }
}
