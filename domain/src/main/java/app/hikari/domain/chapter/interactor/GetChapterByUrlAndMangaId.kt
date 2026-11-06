package app.hikari.domain.chapter.interactor

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject

@Inject
class GetChapterByUrlAndMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String, sourceId: Long): Chapter? {
        return try {
            chapterRepository.getChapterByUrlAndMangaId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
