package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import dev.zacsweers.metro.Inject

@Inject
class HideManga(
    private val repository: HiddenMangaRepository,
) {
    suspend fun await(manga: Manga) {
        repository.hide(listOf(manga))
    }

    suspend fun await(mangaList: List<Manga>) {
        if (mangaList.isEmpty()) return
        repository.hide(mangaList)
    }
}
