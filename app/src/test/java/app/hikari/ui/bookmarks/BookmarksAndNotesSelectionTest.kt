package app.hikari.ui.bookmarks

import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.manga.model.Manga
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BookmarksAndNotesSelectionTest {

    private fun group(mangaId: Long, vararg chapterIds: Long): BookmarkedChapterGroupFixture {
        val manga = Manga.create().copy(id = mangaId, title = "Manga $mangaId")
        val entries = chapterIds.map { cid ->
            BookmarkedChapterEntry(
                chapter = Chapter.create().copy(id = cid, mangaId = mangaId, bookmark = true),
                lastReadAt = null,
            )
        }
        return BookmarkedChapterGroupFixture(
            BookmarksAndNotesGroup(
                manga = manga,
                sourceName = "Source",
                chapters = entries,
                lastActivityAt = null,
            ),
        )
    }

    @JvmInline
    private value class BookmarkedChapterGroupFixture(val value: BookmarksAndNotesGroup)

    @Test
    fun `pruneSelection drops chapter IDs no longer present in visible groups`() {
        val groups = listOf(group(1L, 10L, 11L).value, group(2L, 20L).value)
        val pruned = pruneSelection(setOf(10L, 20L, 999L), groups)
        pruned shouldContainExactlyInAnyOrder setOf(10L, 20L)
    }

    @Test
    fun `selectAllVisible and invertVisibleSelection operate strictly on visible chapters`() {
        val groups = listOf(group(1L, 10L, 11L).value, group(2L, 20L).value)
        selectAllVisible(groups) shouldContainExactlyInAnyOrder setOf(10L, 11L, 20L)
        invertVisibleSelection(setOf(10L, 999L), groups) shouldContainExactlyInAnyOrder setOf(11L, 20L)
    }

    @Test
    fun `requiresNoteClearPrompt returns true only when at least one chapter has a non-blank bookmark note`() {
        val plain = Chapter.create().copy(id = 1L, bookmark = true)
        val blankNote = Chapter.create().copy(id = 2L, bookmark = true).let {
            it.copy(memo = it.withBookmarkNote("   "))
        }
        val withNote = Chapter.create().copy(id = 3L, bookmark = true).let {
            it.copy(memo = it.withBookmarkNote("Favorite fight"))
        }

        requiresNoteClearPrompt(listOf(plain, blankNote)) shouldBe false
        requiresNoteClearPrompt(listOf(plain, withNote)) shouldBe true
    }
}
