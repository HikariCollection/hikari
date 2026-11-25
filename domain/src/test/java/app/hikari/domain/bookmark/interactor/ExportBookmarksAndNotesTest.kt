package app.hikari.domain.bookmark.interactor

import app.hikari.domain.bookmark.model.BookmarksAndNotesExportPayload
import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.manga.model.Manga
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Instant

@Execution(ExecutionMode.CONCURRENT)
class ExportBookmarksAndNotesTest {

    private val exportInteractor = ExportBookmarksAndNotes()

    private fun sampleGroups(): List<BookmarksAndNotesGroup> {
        val manga1 = Manga.create().copy(
            id = 1L,
            source = 10L,
            title = "Berserk",
            favoriteAt = Instant.fromEpochMilliseconds(5_000L),
            notes = "Re-read Golden Age arc.",
        )
        val ch1Base = Chapter.create().copy(
            id = 101L,
            mangaId = 1L,
            name = "Chapter 100: Eclipse",
            chapterNumber = 100.0,
            scanlator = "Band of Hawks",
            bookmark = true,
            read = false,
            lastPageRead = 14,
        )
        val ch2Base = Chapter.create().copy(
            id = 102L,
            mangaId = 1L,
            name = "Bonus Side Story",
            chapterNumber = -1.0,
            scanlator = null,
            bookmark = false,
            read = true,
            lastPageRead = 0,
        )
        return listOf(
            BookmarksAndNotesGroup(
                manga = manga1,
                sourceName = "Local source",
                chapters = listOf(
                    BookmarkedChapterEntry(
                        chapter = ch1Base.copy(memo = ch1Base.withBookmarkNote("Key turning point")),
                        lastReadAt = Instant.fromEpochMilliseconds(9_000L),
                    ),
                    BookmarkedChapterEntry(
                        chapter = ch2Base.copy(memo = ch2Base.withBookmarkNote("Kept note only")),
                        lastReadAt = null,
                    ),
                ),
                lastActivityAt = Instant.fromEpochMilliseconds(9_000L),
            ),
        )
    }

    @Test
    fun `toMarkdown formats header, manga notes, and chapters cleanly`() {
        val md = exportInteractor.toMarkdown(sampleGroups())

        md shouldContain "# Hikari Bookmarks & Notes"
        md shouldContain "## Berserk"
        md shouldContain "Source: Local source | In library"
        md shouldContain "Re-read Golden Age arc."
        md shouldContain "Ch. 100 - Chapter 100: Eclipse [Band of Hawks] (Bookmarked, Page 15)"
        md shouldContain "Note: Key turning point"
        md shouldContain "Bonus Side Story (Note only, Read)"
        md shouldContain "Note: Kept note only"
        md shouldNotContain "-1"
    }

    @Test
    fun `toJson serializes structured payload matching input groups`() {
        val exportedAt = Instant.parse("2026-11-25T10:00:00Z")
        val rawJson = exportInteractor.toJson(sampleGroups(), exportedAt = exportedAt)

        val payload = Json.decodeFromString<BookmarksAndNotesExportPayload>(rawJson)
        payload.exportedAt shouldBe exportedAt
        payload.entries shouldHaveSize 1

        val entry = payload.entries.single()
        entry.mangaTitle shouldBe "Berserk"
        entry.sourceName shouldBe "Local source"
        entry.inLibrary shouldBe true
        entry.mangaNotes shouldBe "Re-read Golden Age arc."
        entry.chapters shouldHaveSize 2
        entry.chapters[0].chapterNumber shouldBe 100.0
        entry.chapters[0].bookmarkNote shouldBe "Key turning point"
        entry.chapters[1].bookmarked shouldBe false
        entry.chapters[1].bookmarkNote shouldBe "Kept note only"
    }
}
