package app.hikari.domain.upcoming.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import app.hikari.source.model.SManga
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

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
