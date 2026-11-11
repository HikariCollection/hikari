package app.hikari.data.backup.restore.restorers

import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.models.BackupManga
import app.hikari.domain.backup.model.RestoredHistory
import app.hikari.domain.backup.model.RestoredManga
import app.hikari.domain.backup.repository.RestoreRepository
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.manga.model.MangaUpdate
import dev.zacsweers.metro.Inject

@Inject
class MangaRestorer(
    private val restoreRepository: RestoreRepository,
    private val getCategories: GetCategories,
) {

    /**
     * Restores [backupMangas] all together, so either every one of them is restored or none is.
     */
    suspend fun restore(
        backupMangas: List<BackupManga>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategoriesByName = getCategories.await().associateBy { it.name }
        val backupCategoriesByOrder = backupCategories.associateBy { it.order }

        val entries = backupMangas.map { backupManga ->
            RestoredManga(
                manga = backupManga.getMangaImpl(),
                chapters = backupManga.chapters.map { it.toChapterImpl() },
                categoryIds = backupManga.categories.mapNotNull { backupCategoryOrder ->
                    backupCategoriesByOrder[backupCategoryOrder]?.let { backupCategory ->
                        dbCategoriesByName[backupCategory.name]?.id
                    }
                },
                history = backupManga.history.map {
                    val history = it.getHistoryImpl()
                    RestoredHistory(it.url, history.readAt, history.readDuration)
                },
                tracks = backupManga.tracking.map { it.getTrackImpl() },
                excludedScanlators = backupManga.excludedScanlators,
            )
        }

        restoreRepository.restoreManga(entries) { manga ->
            MangaUpdate(manga.id) {}
        }
    }
}
