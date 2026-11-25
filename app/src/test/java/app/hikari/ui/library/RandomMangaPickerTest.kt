package app.hikari.ui.library

import app.hikari.domain.library.model.LibraryManga
import app.hikari.domain.manga.model.Manga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RandomMangaPickerTest {

    private fun item(
        id: Long,
        totalChapters: Long = 10L,
        readCount: Long = 0L,
    ): LibraryItem {
        val libraryManga = LibraryManga(
            manga = Manga.create().copy(id = id, title = "Manga $id"),
            categories = listOf(1L),
            totalChapters = totalChapters,
            readCount = readCount,
            bookmarkCount = 0L,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = null,
        )
        return LibraryItem(
            libraryManga = libraryManga,
            downloadCount = 0,
            unreadCount = libraryManga.unreadCount,
            isLocal = false,
            sourceName = "Source",
            sourceLanguage = "en",
            badges = LibraryItem.Badges(0, libraryManga.unreadCount, false, "en"),
        )
    }

    @Test
    fun `filterRandomCandidates deduplicates across categories and applies status filters`() {
        val unreadUnstarted = item(id = 1L, totalChapters = 10L, readCount = 0L)
        val unreadStarted = item(id = 2L, totalChapters = 10L, readCount = 4L)
        val completed = item(id = 3L, totalChapters = 10L, readCount = 10L)
        val duplicate = item(id = 2L, totalChapters = 10L, readCount = 4L)

        val allItems = listOf(unreadUnstarted, unreadStarted, completed, duplicate)

        filterRandomCandidates(allItems, RandomMangaFilter()).map { it.id } shouldContainExactly listOf(1L, 2L, 3L)
        filterRandomCandidates(
            allItems,
            RandomMangaFilter(unreadOnly = true),
        ).map { it.id } shouldContainExactly listOf(1L, 2L)
        filterRandomCandidates(
            allItems,
            RandomMangaFilter(startedOnly = true),
        ).map { it.id } shouldContainExactly listOf(2L, 3L)
        filterRandomCandidates(
            allItems,
            RandomMangaFilter(unreadOnly = true, startedOnly = true),
        ).map { it.id } shouldContainExactly listOf(2L)
    }

    @Test
    fun `pickRandomCandidate handles empty, single, and non-repeating rerolls`() {
        pickRandomCandidate(emptyList()).shouldBeNull()

        val sole = item(id = 1L)
        pickRandomCandidate(listOf(sole), excludeMangaId = 1L) shouldBe sole

        val two = listOf(item(id = 1L), item(id = 2L))
        val random = Random(42)
        repeat(20) {
            pickRandomCandidate(two, excludeMangaId = 1L, random = random)?.id shouldBe 2L
            pickRandomCandidate(two, excludeMangaId = 2L, random = random)?.id shouldBe 1L
        }

        val three = listOf(item(id = 1L), item(id = 2L), item(id = 3L))
        repeat(20) {
            pickRandomCandidate(three, excludeMangaId = 2L, random = random)?.id shouldNotBe 2L
        }
    }
}
