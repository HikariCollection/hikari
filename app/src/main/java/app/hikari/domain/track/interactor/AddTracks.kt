package app.hikari.domain.track.interactor

import app.hikari.core.common.util.system.logcat
import app.hikari.data.database.models.Track
import app.hikari.data.track.EnhancedTracker
import app.hikari.data.track.Tracker
import app.hikari.data.track.TrackerManager
import app.hikari.domain.chapter.interactor.GetChaptersByMangaId
import app.hikari.domain.history.interactor.GetHistory
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.track.interactor.UpsertTrack
import app.hikari.domain.track.model.toDbTrack
import app.hikari.domain.track.model.toDomainTrack
import app.hikari.source.Source
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import kotlin.time.Instant

@Inject
class AddTracks(
    private val upsertTrack: UpsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val trackerManager: TrackerManager,
    private val getHistory: GetHistory,
) {

    // TODO: update all trackers based on common data
    suspend fun bind(tracker: Tracker, item: Track, mangaId: Long) = withContext(Dispatchers.IO) {
        val allChapters = getChaptersByMangaId.await(mangaId)
        val hasReadChapters = allChapters.any { it.read }
        tracker.bind(item, hasReadChapters)

        var track = item.toDomainTrack(idRequired = false) ?: return@withContext

        upsertTrack.await(track)

        // TODO: merge into [SyncChapterProgressWithTrack]?
        // Update chapter progress if newer chapters marked read locally
        if (hasReadChapters) {
            val latestLocalReadChapterNumber = allChapters
                .sortedBy { it.chapterNumber }
                .takeWhile { it.read }
                .lastOrNull()
                ?.chapterNumber ?: -1.0

            if (latestLocalReadChapterNumber > track.lastChapterRead) {
                track = track.copy(
                    lastChapterRead = latestLocalReadChapterNumber,
                )
                tracker.setRemoteLastChapterRead(track.toDbTrack(), latestLocalReadChapterNumber.toInt())
            }

            if (track.startDate == null) {
                val firstReadChapterDate = getHistory.await(mangaId)
                    .mapNotNull { it.readAt }
                    .minOrNull()

                firstReadChapterDate?.let {
                    val startDate = firstReadChapterDate.toEpochMilliseconds()
                    track = track.copy(
                        startDate = Instant.fromEpochMilliseconds(startDate),
                    )
                    tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                }
            }
        }

        syncChapterProgressWithTrack.await(mangaId, track, tracker)
    }

    suspend fun bindEnhancedTrackers(manga: Manga, source: Source) = withContext(Dispatchers.IO) {
        trackerManager.loggedInTrackers()
            .filterIsInstance<EnhancedTracker>()
            .filter { it.accept(source) }
            .forEach { service ->
                try {
                    service.match(manga)?.let { track ->
                        track.manga_id = manga.id
                        (service as Tracker).bind(track)
                        upsertTrack.await(track.toDomainTrack(idRequired = false)!!)

                        syncChapterProgressWithTrack.await(
                            manga.id,
                            track.toDomainTrack(idRequired = false)!!,
                            service,
                        )
                    }
                } catch (e: Exception) {
                    logcat(
                        LogPriority.WARN,
                        e,
                    ) { "Could not match manga: ${manga.title} with service $service" }
                }
            }
    }
}
