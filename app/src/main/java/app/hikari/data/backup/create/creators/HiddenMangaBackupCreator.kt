package app.hikari.data.backup.create.creators

import app.hikari.data.backup.models.BackupHiddenManga
import app.hikari.data.backup.models.backupHiddenMangaMapper
import app.hikari.domain.manga.interactor.GetHiddenManga
import dev.zacsweers.metro.Inject

@Inject
class HiddenMangaBackupCreator(
    private val getHiddenManga: GetHiddenManga,
) {

    suspend operator fun invoke(): List<BackupHiddenManga> {
        return getHiddenManga.awaitAll()
            .map(backupHiddenMangaMapper)
    }
}
