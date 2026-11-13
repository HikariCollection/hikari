package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaUpdate
import app.hikari.domain.manga.model.extractOriginalMetadata
import app.hikari.domain.manga.model.memoWithCustomMetadataMarker
import app.hikari.domain.manga.model.memoWithoutCustomMetadataMarker
import app.hikari.domain.manga.model.normalizeCustomMetadata
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

    suspend fun awaitUpdateCustomMetadata(
        manga: Manga,
        title: String,
        author: String?,
        artist: String?,
        description: String?,
        genresInput: String?,
    ): Boolean {
        val normalized = normalizeCustomMetadata(
            title = title,
            author = author,
            artist = artist,
            description = description,
            genresInput = genresInput,
        )
        if (normalized.title.isEmpty()) return false
        return mangaRepository.updateCustomMetadata(
            id = manga.id,
            title = normalized.title,
            author = normalized.author,
            artist = normalized.artist,
            description = normalized.description,
            genre = normalized.genres,
            memo = manga.memoWithCustomMetadataMarker(),
        )
    }

    suspend fun awaitResetCustomMetadata(manga: Manga): Boolean {
        val original = manga.extractOriginalMetadata()
        return mangaRepository.updateCustomMetadata(
            id = manga.id,
            title = original?.title ?: manga.title,
            author = if (original != null) original.author else manga.author,
            artist = if (original != null) original.artist else manga.artist,
            description = if (original != null) original.description else manga.description,
            genre = if (original != null) original.genres else manga.genre,
            memo = manga.memoWithoutCustomMetadataMarker(),
        )
    }
}
