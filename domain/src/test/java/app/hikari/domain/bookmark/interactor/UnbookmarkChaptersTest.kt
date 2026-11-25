package app.hikari.domain.bookmark.interactor

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class UnbookmarkChaptersTest {

    private val chapterRepository: ChapterRepository = mockk(relaxed = true)
    private val unbookmarkChapters = UnbookmarkChapters(chapterRepository)

    @Test
    fun `empty list skips repository update`() = runTest {
        unbookmarkChapters.await(emptyList(), clearNotes = true)
        coVerify(exactly = 0) { chapterRepository.updateAll(any()) }
    }

    @Test
    fun `unbookmarks chapters while keeping memo intact when clearNotes is false`() = runTest {
        val chapter = Chapter.create().copy(
            id = 1L,
            bookmark = true,
            memo = JsonObject(
                mapOf(
                    "bookmark_note" to JsonPrimitive("Important scene"),
                    "extra" to JsonPrimitive("keep"),
                ),
            ),
        )
        val updatesSlot = slot<List<ChapterUpdate>>()
        coEvery { chapterRepository.updateAll(capture(updatesSlot)) } returns Unit

        unbookmarkChapters.await(listOf(chapter), clearNotes = false)

        val update = updatesSlot.captured.single()
        update.id shouldBe 1L
        update.bookmark shouldBe false
        update.memo.shouldBeNull()
    }

    @Test
    fun `unbookmarks chapters and clears bookmark_note while preserving other memo keys when clearNotes is true`() =
        runTest {
            val notedChapter = Chapter.create().copy(
                id = 2L,
                bookmark = true,
                memo = JsonObject(
                    mapOf(
                        "bookmark_note" to JsonPrimitive("Delete this"),
                        "remote_flag" to JsonPrimitive("keep"),
                    ),
                ),
            )
            val plainChapter = Chapter.create().copy(
                id = 3L,
                bookmark = true,
                memo = JsonObject(emptyMap()),
            )
            val updatesSlot = slot<List<ChapterUpdate>>()
            coEvery { chapterRepository.updateAll(capture(updatesSlot)) } returns Unit

            unbookmarkChapters.await(listOf(notedChapter, plainChapter), clearNotes = true)

            updatesSlot.captured shouldHaveSize 2
            val firstUpdate = updatesSlot.captured[0]
            firstUpdate.id shouldBe 2L
            firstUpdate.bookmark shouldBe false
            firstUpdate.memo shouldBe JsonObject(mapOf("remote_flag" to JsonPrimitive("keep")))
            Chapter.create().copy(memo = firstUpdate.memo!!).bookmarkNote.shouldBeNull()

            val secondUpdate = updatesSlot.captured[1]
            secondUpdate.id shouldBe 3L
            secondUpdate.bookmark shouldBe false
            secondUpdate.memo.shouldBeNull()
        }
}
