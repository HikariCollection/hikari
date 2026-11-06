package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject

@Inject
class GetMangaByUrlAndSourceId(
    private val mangaRepository: MangaRepository,
) {
    suspend fun await(url: String, sourceId: Long): Manga? {
        return mangaRepository.getMangaByUrlAndSourceId(url, sourceId)
    }
}
