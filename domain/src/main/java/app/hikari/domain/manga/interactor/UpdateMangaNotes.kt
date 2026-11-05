package app.hikari.domain.manga.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.manga.model.MangaUpdate
import app.hikari.domain.manga.repository.MangaRepository

@Inject
class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUpdate(mangaId) {
                this.notes = notes
            },
        )
    }
}
