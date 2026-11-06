package app.hikari.data.backup.create.creators

import app.hikari.data.backup.models.BackupSource
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.Source
import dev.zacsweers.metro.Inject

@Inject
class SourcesBackupCreator(
    private val sourceManager: SourceManager,
) {

    suspend operator fun invoke(sourceIds: Set<Long>): List<BackupSource> {
        return sourceIds.map { sourceManager.getOrStub(it).toBackupSource() }
    }
}

private fun Source.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
