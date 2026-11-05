package app.hikari.domain.category.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import app.hikari.core.common.util.system.logcat
import app.hikari.domain.manga.repository.MangaRepository

@Inject
class SetMangaCategories(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.setMangaCategories(mangaId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
