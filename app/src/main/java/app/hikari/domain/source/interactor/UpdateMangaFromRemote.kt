package app.hikari.domain.source.interactor

import app.hikari.core.common.util.system.logcat
import app.hikari.data.cache.CoverCache
import app.hikari.data.download.DownloadManager
import app.hikari.domain.chapter.interactor.SyncChaptersWithSource
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.toSChapter
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.library.service.LibraryPreferences
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaRemoteUpdate
import app.hikari.domain.manga.model.hasCustomCover
import app.hikari.domain.manga.model.toSManga
import app.hikari.domain.manga.repository.MangaRepository
import app.hikari.domain.source.models.RemoteMangaUpdate
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.Source
import app.hikari.source.local.isLocal
import app.hikari.source.model.SManga
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import kotlin.time.Clock

@Inject
class UpdateMangaFromRemote(
    private val sourceManager: SourceManager,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val syncChaptersWithSource: SyncChaptersWithSource,
    private val coverCache: CoverCache,
    private val libraryPreferences: LibraryPreferences,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
    ): Result<RemoteMangaUpdate> {
        val source = sourceManager.getOrStub(manga.source)
        return invoke(
            source = source,
            manga = manga,
            fetchDetails = fetchDetails,
            fetchChapters = fetchChapters,
            manualFetch = manualFetch,
        )
    }

    suspend operator fun invoke(
        source: Source,
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
    ): Result<RemoteMangaUpdate> = withContext(Dispatchers.IO) {
        try {
            val chapters = chapterRepository.getChapterByMangaId(manga.id)
                .sortedBy { it.sourceOrder }
            val update = source.getMangaUpdate(
                manga = manga.toSManga(),
                chapters = chapters.map(Chapter::toSChapter),
                fetchDetails = fetchDetails,
                fetchChapters = fetchChapters,
            )
            awaitUpdateFromSource(manga, update.manga, manualFetch)
            val newChapters = syncChaptersWithSource.await(
                rawSourceChapters = update.chapters,
                manga = manga,
                source = source,
                manualFetch = manualFetch,
            )
            val updatedManga = mangaRepository.getMangaById(manga.id)

            Result.success(RemoteMangaUpdate(manga = updatedManga, newChapters = newChapters))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.failure(e)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localManga: Manga,
        remoteManga: SManga,
        manualFetch: Boolean,
    ): Boolean {
        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite (or 'update titles' preference is enabled), set its title from source and update in db
        val title =
            if (remoteTitle.isNotEmpty() && (!localManga.favorite || libraryPreferences.updateMangaTitles.get())) {
                remoteTitle
            } else {
                null
            }

        val coverLastModified = when {
            // Never refresh covers if the url is empty to avoid "losing" existing covers
            remoteManga.thumbnail_url.isNullOrEmpty() -> null
            !manualFetch && localManga.thumbnailUrl == remoteManga.thumbnail_url -> null
            localManga.isLocal() -> Clock.System.now()
            localManga.hasCustomCover(coverCache) -> {
                coverCache.deleteFromCache(localManga, false)
                null
            }
            else -> {
                coverCache.deleteFromCache(localManga, false)
                Clock.System.now()
            }
        }

        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        val success = mangaRepository.updateRemote(
            MangaRemoteUpdate(
                id = localManga.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
                memo = remoteManga.memo,
            ),
        )
        if (success && title != null) {
            downloadManager.renameManga(localManga, title)
        }
        return success
    }
}
