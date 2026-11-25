package app.hikari.domain.chapter.interactor

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.chapter.model.matchesSearchQuery
import app.hikari.domain.chapter.model.mergeLocalMemoInto
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
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
class SetChapterBookmarkMemoTest {

    private val chapterRepository: ChapterRepository = mockk(relaxed = true)
    private val setChapterBookmarkMemo = SetChapterBookmarkMemo(chapterRepository)

    @Test
    fun `returns false when chapter does not exist`() = runTest {
        coEvery { chapterRepository.getChapterById(1L) } returns null

        val result = setChapterBookmarkMemo(1L, "My note")

        result shouldBe false
        coVerify(exactly = 0) { chapterRepository.update(any()) }
    }

    @Test
    fun `sets note on unbookmarked chapter and marks it bookmarked`() = runTest {
        val chapter = Chapter.create().copy(id = 10L, bookmark = false)
        coEvery { chapterRepository.getChapterById(10L) } returns chapter

        val updateSlot = slot<ChapterUpdate>()
        coEvery { chapterRepository.update(capture(updateSlot)) } returns Unit

        val result = setChapterBookmarkMemo(10L, "Plot twist at page 12")

        result shouldBe true
        updateSlot.captured.id shouldBe 10L
        updateSlot.captured.bookmark shouldBe true
        updateSlot.captured.memo shouldBe JsonObject(mapOf("bookmark_note" to JsonPrimitive("Plot twist at page 12")))
    }

    @Test
    fun `updates note preserving other memo keys`() = runTest {
        val existingMemo = JsonObject(
            mapOf(
                "custom_key" to JsonPrimitive("preserved_value"),
                "bookmark_note" to JsonPrimitive("Old note"),
            ),
        )
        val chapter = Chapter.create().copy(id = 20L, bookmark = true, memo = existingMemo)
        coEvery { chapterRepository.getChapterById(20L) } returns chapter

        val updateSlot = slot<ChapterUpdate>()
        coEvery { chapterRepository.update(capture(updateSlot)) } returns Unit

        val result = setChapterBookmarkMemo(20L, "New updated note")

        result shouldBe true
        val memo = updateSlot.captured.memo!!
        memo["custom_key"] shouldBe JsonPrimitive("preserved_value")
        memo["bookmark_note"] shouldBe JsonPrimitive("New updated note")
    }

    @Test
    fun `removes note when blank or null without unbookmarking`() = runTest {
        val existingMemo = JsonObject(
            mapOf(
                "bookmark_note" to JsonPrimitive("Existing note"),
            ),
        )
        val chapter = Chapter.create().copy(id = 30L, bookmark = true, memo = existingMemo)
        coEvery { chapterRepository.getChapterById(30L) } returns chapter

        val updateSlot = slot<ChapterUpdate>()
        coEvery { chapterRepository.update(capture(updateSlot)) } returns Unit

        val result = setChapterBookmarkMemo(30L, "   ")

        result shouldBe true
        updateSlot.captured.memo shouldBe JsonObject(emptyMap())
        updateSlot.captured.bookmark.shouldBeNull()
    }

    @Test
    fun `Chapter bookmarkNote extension reads note correctly`() {
        val emptyChapter = Chapter.create()
        emptyChapter.bookmarkNote.shouldBeNull()

        val notedChapter = emptyChapter.copy(
            memo = emptyChapter.withBookmarkNote("Chapter cliffhanger"),
        )
        notedChapter.bookmarkNote shouldBe "Chapter cliffhanger"
    }

    @Test
    fun `mergeLocalMemoInto preserves local bookmark note on remote chapter`() {
        val localChapter = Chapter.create().copy(
            memo = JsonObject(
                mapOf(
                    "bookmark_note" to JsonPrimitive("Keep me"),
                    "stale_remote_key" to JsonPrimitive("old"),
                ),
            ),
        )
        val remoteChapter = Chapter.create().copy(
            memo = JsonObject(mapOf("remote_key" to JsonPrimitive("new"))),
        )

        val merged = localChapter.mergeLocalMemoInto(remoteChapter)
        merged["remote_key"] shouldBe JsonPrimitive("new")
        merged["bookmark_note"] shouldBe JsonPrimitive("Keep me")
        merged.containsKey("stale_remote_key") shouldBe false
    }

    @Test
    fun `matchesSearchQuery matches name, chapter number, scanlator, and bookmark note`() {
        val chapter = Chapter.create().copy(
            name = "The Final Battle",
            chapterNumber = 42.5,
            scanlator = "Hikari Scans",
            memo = JsonObject(mapOf("bookmark_note" to JsonPrimitive("Plot twist"))),
        )

        chapter.matchesSearchQuery("   ") shouldBe true
        chapter.matchesSearchQuery("final") shouldBe true
        chapter.matchesSearchQuery("42.5") shouldBe true
        chapter.matchesSearchQuery("hikari") shouldBe true
        chapter.matchesSearchQuery("twist") shouldBe true
        chapter.matchesSearchQuery("nonexistent") shouldBe false

        val integerChapter = chapter.copy(chapterNumber = 12.0, memo = JsonObject(emptyMap()))
        integerChapter.matchesSearchQuery("12") shouldBe true
    }
}
