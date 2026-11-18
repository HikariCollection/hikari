package app.hikari.domain.chapter.interactor

import app.hikari.core.common.preference.InMemoryPreferenceStore
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.download.interactor.DeleteDownload
import app.hikari.domain.download.service.DownloadPreferences
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetReadStatusTest {

    private val preferenceStore = InMemoryPreferenceStore()
    private val downloadPreferences = DownloadPreferences(preferenceStore)
    private val deleteDownload = mockk<DeleteDownload>(relaxed = true)
    private val mangaRepository = mockk<MangaRepository>()
    private val chapterRepository = mockk<ChapterRepository>(relaxed = true)

    private val setReadStatus = SetReadStatus(
        downloadPreferences = downloadPreferences,
        deleteDownload = deleteDownload,
        mangaRepository = mangaRepository,
        chapterRepository = chapterRepository,
    )

    private fun testChapter(
        id: Long,
        mangaId: Long = 10L,
        read: Boolean = false,
        lastPageRead: Int = 0,
    ) = Chapter.create().copy(
        id = id,
        mangaId = mangaId,
        read = read,
        lastPageRead = lastPageRead,
    )

    @Test
    fun `returns NoChapters when all chapters already match target read state`() = runTest {
        val readChapter = testChapter(1L, read = true, lastPageRead = 5)
        val unreadCleanChapter = testChapter(2L, read = false, lastPageRead = 0)

        setReadStatus.await(read = true, readChapter) shouldBe SetReadStatus.Result.NoChapters
        setReadStatus.await(read = false, unreadCleanChapter) shouldBe SetReadStatus.Result.NoChapters
        coVerify(exactly = 0) { chapterRepository.updateAll(any()) }
    }

    @Test
    fun `marking unread resets lastPageRead and does not delete downloads`() = runTest {
        downloadPreferences.removeAfterMarkedAsRead.set(true)
        val ch1 = testChapter(1L, read = true, lastPageRead = 10)
        val ch2 = testChapter(2L, read = false, lastPageRead = 4)
        val updatesSlot = slot<List<ChapterUpdate>>()
        coEvery { chapterRepository.updateAll(capture(updatesSlot)) } returns Unit

        val result = setReadStatus.await(read = false, ch1, ch2)

        result shouldBe SetReadStatus.Result.Success
        updatesSlot.captured.size shouldBe 2
        updatesSlot.captured.all { it.read == false && it.lastPageRead == 0 } shouldBe true
        coVerify(exactly = 0) { deleteDownload.awaitAll(any(), *anyVararg()) }
    }

    @Test
    fun `marking read deletes downloads with read flag set when preference enabled`() = runTest {
        downloadPreferences.removeAfterMarkedAsRead.set(true)
        val manga = Manga.create().copy(id = 10L)
        val ch1 = testChapter(1L, mangaId = 10L, read = false)
        val ch2 = testChapter(2L, mangaId = 10L, read = true)

        coEvery { mangaRepository.getMangaById(10L) } returns manga

        val result = setReadStatus.await(read = true, ch1, ch2)

        result shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 1) {
            deleteDownload.awaitAll(
                manga = manga,
                ch1.copy(read = true),
            )
        }
    }

    @Test
    fun `returns InternalError when chapterRepository throws`() = runTest {
        val ch1 = testChapter(1L, read = false)
        coEvery { chapterRepository.updateAll(any()) } throws IllegalStateException("db failure")

        val result = setReadStatus.await(read = true, ch1)

        result.shouldBeInstanceOf<SetReadStatus.Result.InternalError>()
    }
}
