package app.hikari.domain.chapter.interactor

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.mergeLocalMemoInto
import dev.zacsweers.metro.Inject

@Inject
class ShouldUpdateDbChapter {

    fun await(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != dbChapter.mergeLocalMemoInto(sourceChapter)
    }
}
