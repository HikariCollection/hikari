package app.hikari.domain.chapter.interactor

import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
import dev.zacsweers.metro.Inject

@Inject
class SetChapterBookmarkMemo(
    private val chapterRepository: ChapterRepository,
) {

    suspend operator fun invoke(chapterId: Long, note: String?): Boolean {
        val chapter = chapterRepository.getChapterById(chapterId) ?: return false
        val newMemo = chapter.withBookmarkNote(note)
        val isNotePresent = !note.isNullOrBlank()
        chapterRepository.update(
            ChapterUpdate(chapterId) {
                this.memo = newMemo
                if (isNotePresent && !chapter.bookmark) {
                    this.bookmark = true
                }
            },
        )
        return true
    }
}
