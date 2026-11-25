package app.hikari.domain.chapter.interactor

import app.hikari.core.common.extensions.JsonObjectEmpty
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.withBookmarkNote
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class ShouldUpdateDbChapterTest {

    private val shouldUpdate = ShouldUpdateDbChapter()

    private fun baseChapter() = Chapter.create().copy(
        id = 1L,
        mangaId = 10L,
        url = "/ch/1",
        name = "Chapter 1",
        scanlator = "Group A",
        dateUpload = Instant.fromEpochMilliseconds(1_000_000L),
        chapterNumber = 1.0,
        sourceOrder = 0L,
        memo = JsonObjectEmpty,
    )

    @Test
    fun `returns false when remote fields match and only local user state differs`() {
        val dbChapter = baseChapter().copy(
            read = true,
            bookmark = true,
            lastPageRead = 12,
            dateFetch = Instant.fromEpochMilliseconds(2_000_000L),
        )
        val sourceChapter = baseChapter().copy(
            read = false,
            bookmark = false,
            lastPageRead = 0,
            dateFetch = Instant.fromEpochMilliseconds(3_000_000L),
        )

        shouldUpdate.await(dbChapter, sourceChapter) shouldBe false
    }

    @Test
    fun `returns true when any remote metadata field changes`() {
        val dbChapter = baseChapter()

        shouldUpdate.await(dbChapter, dbChapter.copy(scanlator = "Group B")) shouldBe true
        shouldUpdate.await(dbChapter, dbChapter.copy(name = "Chapter 1 Revised")) shouldBe true
        shouldUpdate.await(
            dbChapter,
            dbChapter.copy(dateUpload = Instant.fromEpochMilliseconds(5_000_000L)),
        ) shouldBe true
        shouldUpdate.await(dbChapter, dbChapter.copy(chapterNumber = 1.5)) shouldBe true
        shouldUpdate.await(dbChapter, dbChapter.copy(sourceOrder = 2L)) shouldBe true
        shouldUpdate.await(
            dbChapter,
            dbChapter.copy(memo = buildJsonObject { put("extra", "1") }),
        ) shouldBe true
    }

    @Test
    fun `ignores local bookmark note when comparing memo`() {
        val remoteMemo = buildJsonObject { put("extra", "1") }
        val sourceChapter = baseChapter().copy(memo = remoteMemo)
        val dbChapterWithNote = sourceChapter.copy(
            memo = sourceChapter.withBookmarkNote("Remember this scene"),
        )

        shouldUpdate.await(dbChapterWithNote, sourceChapter) shouldBe false
        shouldUpdate.await(
            dbChapterWithNote,
            sourceChapter.copy(memo = buildJsonObject { put("extra", "2") }),
        ) shouldBe true
    }
}
