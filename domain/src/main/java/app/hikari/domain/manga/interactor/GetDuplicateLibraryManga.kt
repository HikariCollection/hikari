package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaWithChapterCount
import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject

@Inject
class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> {
        return mangaRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
    }
}
