package app.hikari.domain.track.interactor

import app.hikari.core.common.util.system.logcat
import app.hikari.data.track.EnhancedTracker
import app.hikari.data.track.Tracker
import app.hikari.domain.chapter.interactor.GetChaptersByMangaId
import app.hikari.domain.chapter.interactor.UpdateChapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.track.interactor.UpsertTrack
import app.hikari.domain.track.model.Track
import app.hikari.domain.track.model.toDbTrack
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import kotlin.math.max

@Inject
class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val upsertTrack: UpsertTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        if (tracker !is EnhancedTracker) {
            return
        }

        val sortedChapters = getChaptersByMangaId.await(mangaId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        val chapterUpdates = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.lastChapterRead && !chapter.read }
            .map { ChapterUpdate(it.id) { read = true } }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val lastRead = max(remoteTrack.lastChapterRead, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

        try {
            tracker.update(updatedTrack.toDbTrack())
            updateChapter.awaitAll(chapterUpdates)
            upsertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
