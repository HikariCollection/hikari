package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import dev.zacsweers.metro.Inject

@Inject
class UnhideManga(
    private val repository: HiddenMangaRepository,
) {
    suspend fun await(mangaList: List<Manga>) {
        if (mangaList.isEmpty()) return
        repository.unhide(mangaList)
    }

    suspend fun await(sourceId: Long, url: String) {
        repository.unhide(sourceId, url)
    }

    suspend fun awaitByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        repository.unhideByIds(ids)
    }
}
