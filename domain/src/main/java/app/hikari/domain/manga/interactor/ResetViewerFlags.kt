package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject

@Inject
class ResetViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): Boolean {
        return mangaRepository.resetViewerFlags()
    }
}
