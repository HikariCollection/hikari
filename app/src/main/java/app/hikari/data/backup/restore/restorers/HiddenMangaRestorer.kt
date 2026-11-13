package app.hikari.data.backup.restore.restorers

import app.hikari.data.backup.models.BackupHiddenManga
import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class HiddenMangaRestorer(
    private val hiddenMangaRepository: HiddenMangaRepository,
) {
    internal var clock: Clock = Clock.System

    constructor(
        hiddenMangaRepository: HiddenMangaRepository,
        clock: Clock,
    ) : this(hiddenMangaRepository) {
        this.clock = clock
    }

    suspend operator fun invoke(backupHiddenManga: List<BackupHiddenManga>) {
        if (backupHiddenManga.isEmpty()) return

        val validEntries = backupHiddenManga.filter { it.url.isNotBlank() }
        if (validEntries.isEmpty()) return

        val now = clock.now()
        val items = validEntries.map { entry ->
            val createdAt = if (entry.createdAt > 0L) {
                Instant.fromEpochMilliseconds(entry.createdAt)
            } else {
                now
            }
            HiddenManga(
                id = 0L,
                sourceId = entry.source,
                url = entry.url.trim(),
                title = entry.title,
                thumbnailUrl = entry.thumbnailUrl,
                createdAt = createdAt,
            )
        }

        hiddenMangaRepository.upsertAll(items)
    }
}
