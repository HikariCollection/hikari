package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject

@Inject
class SetExcludedScanlators(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaId: Long, excludedScanlators: Set<String>) {
        mangaRepository.setExcludedScanlators(mangaId, excludedScanlators)
    }
}
