package app.hikari.domain.chapter.interactor

import app.hikari.domain.library.service.LibraryPreferences
import app.hikari.domain.manga.interactor.GetFavorites
import app.hikari.domain.manga.interactor.SetMangaChapterFlags
import app.hikari.domain.manga.model.Manga
import dev.zacsweers.metro.Inject

@Inject
class SetMangaDefaultChapterFlags(
    private val libraryPreferences: LibraryPreferences,
    private val setMangaChapterFlags: SetMangaChapterFlags,
    private val getFavorites: GetFavorites,
) {

    suspend fun await(manga: Manga) {
        setDefaultFlags(listOf(manga.id))
    }

    suspend fun awaitAll() {
        setDefaultFlags(getFavorites.await().map { it.id })
    }

    private suspend fun setDefaultFlags(mangaIds: List<Long>) {
        with(libraryPreferences) {
            setMangaChapterFlags.awaitSetAllFlags(
                mangaIds = mangaIds,
                unreadFilter = filterChapterByRead.get(),
                downloadedFilter = filterChapterByDownloaded.get(),
                bookmarkedFilter = filterChapterByBookmarked.get(),
                sortingMode = sortChapterBySourceOrNumber.get(),
                sortingDirection = sortChapterByAscendingOrDescending.get(),
                displayMode = displayChapterByNameOrNumber.get(),
            )
        }
    }
}
