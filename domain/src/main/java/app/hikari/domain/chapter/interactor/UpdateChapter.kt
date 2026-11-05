package app.hikari.domain.chapter.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import app.hikari.core.common.util.system.logcat
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.repository.ChapterRepository

@Inject
class UpdateChapter(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapterUpdate: ChapterUpdate) {
        try {
            chapterRepository.update(chapterUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(chapterUpdates: List<ChapterUpdate>) {
        try {
            chapterRepository.updateAll(chapterUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
