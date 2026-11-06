package app.hikari.domain.chapter.interactor

import app.hikari.domain.chapter.model.ChapterSummary
import app.hikari.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject

@Inject
class GetChapterSummaryByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): ChapterSummary {
        return chapterRepository.getChapterSummaryByMangaId(mangaId)
    }
}
