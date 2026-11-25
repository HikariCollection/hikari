package app.hikari.domain.bookmark.interactor

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject

@Inject
class UnbookmarkChapters(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapters: List<Chapter>, clearNotes: Boolean) {
        if (chapters.isEmpty()) return

        val updates = chapters.map { chapter ->
            val shouldClearNote = clearNotes && !chapter.bookmarkNote.isNullOrBlank()
            val updatedMemo = if (shouldClearNote) chapter.withBookmarkNote(null) else null
            ChapterUpdate(chapter.id) {
                bookmark = false
                if (updatedMemo != null) {
                    memo = updatedMemo
                }
            }
        }
        chapterRepository.updateAll(updates)
    }
}
