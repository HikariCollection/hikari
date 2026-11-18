package app.hikari.domain.history.interactor

import app.hikari.domain.chapter.interactor.GetChaptersByMangaId
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.history.model.HistoryWithRelations
import app.hikari.domain.history.repository.HistoryRepository
import app.hikari.domain.manga.interactor.GetManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaCover
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GetNextChaptersTest {

    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val getManga = mockk<GetManga>()
    private val historyRepository = mockk<HistoryRepository>()
    private val getNextChapters = GetNextChapters(getChaptersByMangaId, getManga, historyRepository)

    private fun testManga(id: Long = 1L) = Manga.create().copy(
        id = id,
        chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC,
    )

    private fun testChapter(
        id: Long,
        number: Double,
        read: Boolean = false,
        sourceOrder: Long = id,
    ) = Chapter.create().copy(
        id = id,
        mangaId = 1L,
        name = "Chapter $number",
        chapterNumber = number,
        read = read,
        sourceOrder = sourceOrder,
    )

    @Test
    fun `returns empty list when no history or manga is missing`() = runTest {
        coEvery { historyRepository.getLastHistory() } returns null
        getNextChapters.await().shouldBeEmpty()

        coEvery { getManga.await(99L) } returns null
        getNextChapters.await(99L).shouldBeEmpty()
    }

    @Test
    fun `returns unread chapters sorted ascending when onlyUnread is true`() = runTest {
        val manga = testManga()
        val ch1 = testChapter(1L, 1.0, read = true)
        val ch2 = testChapter(2L, 2.0, read = false)
        val ch3 = testChapter(3L, 3.0, read = false)

        coEvery { getManga.await(1L) } returns manga
        coEvery { getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns listOf(ch3, ch1, ch2)

        val result = getNextChapters.await(1L, onlyUnread = true)
        result.map { it.id } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `onlyUnread false keeps unread anchor chapter and drops read anchor chapter`() = runTest {
        val manga = testManga()
        val ch1 = testChapter(1L, 1.0, read = true)
        val ch2 = testChapter(2L, 2.0, read = false)
        val ch3 = testChapter(3L, 3.0, read = false)

        coEvery { getManga.await(1L) } returns manga
        coEvery { getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns listOf(ch1, ch2, ch3)

        val fromUnread = getNextChapters.await(1L, fromChapterId = 2L, onlyUnread = false)
        fromUnread.map { it.id } shouldBe listOf(2L, 3L)

        val fromRead = getNextChapters.await(1L, fromChapterId = 1L, onlyUnread = false)
        fromRead.map { it.id } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `onlyUnread false does not drop first chapter when anchor chapter is missing`() = runTest {
        val manga = testManga()
        val ch1 = testChapter(1L, 1.0, read = false)
        val ch2 = testChapter(2L, 2.0, read = false)

        coEvery { historyRepository.getLastHistory() } returns HistoryWithRelations(
            id = 1L,
            chapterId = 999L,
            mangaId = 1L,
            title = "Manga",
            chapterNumber = 5.0,
            readAt = null,
            readDuration = 0L,
            coverData = MangaCover(1L, 1L, false, null, null),
        )
        coEvery { getManga.await(1L) } returns manga
        coEvery { getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns listOf(ch1, ch2)

        val result = getNextChapters.await(onlyUnread = false)
        result.map { it.id } shouldBe listOf(1L, 2L)
    }
}
