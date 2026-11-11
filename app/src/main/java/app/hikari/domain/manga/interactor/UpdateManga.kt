package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.MangaUpdate
import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class UpdateManga(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaUpdate: MangaUpdate): Boolean {
        return mangaRepository.update(mangaUpdate)
    }

    suspend fun awaitAll(mangaUpdates: List<MangaUpdate>): Boolean {
        return mangaRepository.updateAll(mangaUpdates)
    }

    suspend fun awaitUpdateLastUpdate(mangaId: Long, lastUpdate: Instant = Clock.System.now()): Boolean {
        return mangaRepository.update(
            MangaUpdate(mangaId) {
                this.lastUpdate = lastUpdate
            },
        )
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(mangaId) {
                coverLastModified = Clock.System.now()
            },
        )
    }

    suspend fun awaitUpdateFavorite(mangaId: Long, favorite: Boolean): Boolean {
        val update = when (favorite) {
            true -> MangaUpdate(mangaId) { favoriteAt = Clock.System.now() }
            false -> MangaUpdate(mangaId) { favoriteAt = null }
        }
        return mangaRepository.update(update)
    }
}
