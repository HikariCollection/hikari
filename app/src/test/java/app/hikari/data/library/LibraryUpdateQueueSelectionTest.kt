package app.hikari.data.library

import app.hikari.domain.library.model.LibraryManga
import app.hikari.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import app.hikari.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import app.hikari.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import app.hikari.domain.manga.model.Manga
import app.hikari.source.model.SManga
import app.hikari.source.model.UpdateStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibraryUpdateQueueSelectionTest {

    private fun libraryManga(
        id: Long,
        title: String,
        categories: List<Long> = emptyList(),
        totalChapters: Long = 10L,
        readCount: Long = 5L,
        status: Int = SManga.ONGOING,
        updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
    ): LibraryManga {
        return LibraryManga(
            manga = Manga.create().copy(
                id = id,
                title = title,
                status = status.toLong(),
                updateStrategy = updateStrategy,
            ),
            categories = categories,
            totalChapters = totalChapters,
            readCount = readCount,
            bookmarkCount = 0L,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = null,
        )
    }

    @Test
    fun `explicit categoryId filters strictly to that category and sorts by title`() {
        val items = listOf(
            libraryManga(id = 1L, title = "Zeta", categories = listOf(2L)),
            libraryManga(id = 2L, title = "Alpha", categories = listOf(2L)),
            libraryManga(id = 3L, title = "Beta", categories = listOf(3L)),
        )

        val result = selectLibraryMangaToUpdate(
            libraryManga = items,
            categoryId = 2L,
            includedCategories = setOf(3L),
            excludedCategories = setOf(2L),
            restrictions = emptySet(),
        )

        assertEquals(listOf("Alpha", "Zeta"), result.toUpdate.map { it.manga.title })
        assertEquals(emptyList<Pair<Manga, UpdateSkipReason>>(), result.skipped)
    }

    @Test
    fun `categoryId -1 applies included and excluded category filters`() {
        val items = listOf(
            libraryManga(id = 1L, title = "Included", categories = listOf(1L)),
            libraryManga(id = 2L, title = "Excluded", categories = listOf(1L, 2L)),
            libraryManga(id = 3L, title = "Other", categories = listOf(3L)),
        )

        val result = selectLibraryMangaToUpdate(
            libraryManga = items,
            categoryId = -1L,
            includedCategories = setOf(1L, 3L),
            excludedCategories = setOf(2L),
            restrictions = emptySet(),
        )

        assertEquals(listOf("Included", "Other"), result.toUpdate.map { it.manga.title })
    }

    @Test
    fun `applies all update restrictions and reports matching skip reasons`() {
        val once = libraryManga(
            id = 1L,
            title = "Fetch Once",
            totalChapters = 5L,
            readCount = 5L,
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
        )
        val completed = libraryManga(
            id = 2L,
            title = "Completed",
            totalChapters = 5L,
            readCount = 5L,
            status = SManga.COMPLETED,
        )
        val unread = libraryManga(
            id = 3L,
            title = "Has Unread",
            totalChapters = 5L,
            readCount = 3L,
        )
        val notStarted = libraryManga(
            id = 4L,
            title = "Not Started",
            totalChapters = 5L,
            readCount = 0L,
        )
        val eligible = libraryManga(
            id = 5L,
            title = "Eligible",
            totalChapters = 5L,
            readCount = 5L,
        )

        val result = selectLibraryMangaToUpdate(
            libraryManga = listOf(once, completed, unread, eligible),
            categoryId = -1L,
            includedCategories = emptySet(),
            excludedCategories = emptySet(),
            restrictions = setOf(MANGA_NON_COMPLETED, MANGA_HAS_UNREAD),
        )

        assertEquals(listOf("Eligible"), result.toUpdate.map { it.manga.title })
        assertEquals(
            listOf(
                once.manga to UpdateSkipReason.NOT_ALWAYS_UPDATE,
                completed.manga to UpdateSkipReason.COMPLETED,
                unread.manga to UpdateSkipReason.NOT_CAUGHT_UP,
            ),
            result.skipped,
        )

        val notStartedResult = selectLibraryMangaToUpdate(
            libraryManga = listOf(notStarted, eligible),
            categoryId = -1L,
            includedCategories = emptySet(),
            excludedCategories = emptySet(),
            restrictions = setOf(MANGA_NON_READ),
        )

        assertEquals(listOf("Eligible"), notStartedResult.toUpdate.map { it.manga.title })
        assertEquals(listOf(notStarted.manga to UpdateSkipReason.NOT_STARTED), notStartedResult.skipped)
    }
}
