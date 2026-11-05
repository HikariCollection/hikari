package app.hikari.domain.upcoming.interactor

import dev.zacsweers.metro.Inject
import app.hikari.source.model.SManga
import kotlinx.coroutines.flow.Flow
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository

@Inject
class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
) {

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Manga>> {
        return mangaRepository.getUpcomingManga(
            includedStatuses,
            excludedCategories = excludedCategories,
            includedCategories = includedCategories,
        )
    }
}
