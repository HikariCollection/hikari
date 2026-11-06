package app.hikari.domain.chapter.interactor

import app.hikari.core.common.util.system.logcat
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class GetBookmarkedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): List<Chapter> {
        return try {
            chapterRepository.getBookmarkedChaptersByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
