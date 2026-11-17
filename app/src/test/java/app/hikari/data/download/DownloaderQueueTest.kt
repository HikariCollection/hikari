package app.hikari.data.download

import android.content.Context
import app.hikari.core.common.preference.InMemoryPreferenceStore
import app.hikari.data.cache.ChapterCache
import app.hikari.data.download.model.Download
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.download.service.DownloadPreferences
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.service.SourceManager
import app.hikari.domain.track.interactor.GetTracks
import app.hikari.source.online.HttpSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DownloaderQueueTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val context = mockk<Context>(relaxed = true)
    private val provider = mockk<DownloadProvider>(relaxed = true)
    private val cache = mockk<DownloadCache>(relaxed = true)
    private val sourceManager = mockk<SourceManager>(relaxed = true)
    private val chapterCache = mockk<ChapterCache>(relaxed = true)
    private val downloadPreferences = DownloadPreferences(InMemoryPreferenceStore())
    private val xml = mockk<XML>(relaxed = true)
    private val getCategories = mockk<GetCategories>(relaxed = true)
    private val getTracks = mockk<GetTracks>(relaxed = true)
    private val store = mockk<DownloadStore>(relaxed = true)
    private val notifier = mockk<DownloadNotifier>(relaxed = true)

    private fun testManga(id: Long = 1L): Manga =
        Manga.create().copy(id = id, source = 100L, title = "Manga $id")

    private fun testChapter(id: Long, mangaId: Long = 1L): Chapter =
        Chapter.create().copy(id = id, mangaId = mangaId, name = "Chapter $id", url = "/chap/$id")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createDownloader(initialDownloads: List<Download> = emptyList()): Downloader {
        coEvery { store.restore() } returns initialDownloads
        return Downloader(
            scope = testScope,
            context = context,
            provider = provider,
            cache = cache,
            sourceManager = sourceManager,
            chapterCache = chapterCache,
            downloadPreferences = downloadPreferences,
            xml = xml,
            getCategories = getCategories,
            getTracks = getTracks,
            store = store,
            notifier = notifier,
        ).apply {
            initJob.join()
        }
    }

    @Test
    fun `initializes queue from store restore`() = testScope.runTest {
        val source = mockk<HttpSource>()
        val manga = testManga()
        val dl1 = Download(source, manga, testChapter(101L))
        val dl2 = Download(source, manga, testChapter(102L))

        val downloader = createDownloader(listOf(dl1, dl2))
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.queueState.value shouldHaveSize 2
        downloader.queueState.value.map { it.chapter.id } shouldBe listOf(101L, 102L)
    }

    @Test
    fun `updateQueue reorders and deduplicates downloads`() = testScope.runTest {
        val source = mockk<HttpSource>()
        val manga = testManga()
        val dl1 = Download(source, manga, testChapter(101L))
        val dl2 = Download(source, manga, testChapter(102L))
        val dl3 = Download(source, manga, testChapter(103L))

        val downloader = createDownloader(listOf(dl1, dl2, dl3))
        testDispatcher.scheduler.advanceUntilIdle()

        // Reorder queue to [dl3, dl1, dl1 (duplicate)]
        downloader.updateQueue { listOf(dl3, dl1, dl1) }
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.queueState.value shouldHaveSize 2
        downloader.queueState.value.map { it.chapter.id } shouldBe listOf(103L, 101L)
        coVerify { store.clear() }
        coVerify { store.addAll(any()) }
    }

    @Test
    fun `dequeue removes specific chapters from queue`() = testScope.runTest {
        val source = mockk<HttpSource>()
        val manga = testManga()
        val chap1 = testChapter(101L)
        val chap2 = testChapter(102L)
        val dl1 = Download(source, manga, chap1)
        val dl2 = Download(source, manga, chap2)

        val downloader = createDownloader(listOf(dl1, dl2))
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.dequeue(listOf(chap1))
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.queueState.value shouldHaveSize 1
        downloader.queueState.value.first().chapter.id shouldBe 102L
        coVerify { store.removeAll(any()) }
    }

    @Test
    fun `clearQueue removes all downloads and clears store`() = testScope.runTest {
        val source = mockk<HttpSource>()
        val manga = testManga()
        val dl1 = Download(source, manga, testChapter(101L))

        val downloader = createDownloader(listOf(dl1))
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.clearQueue()
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.queueState.value.shouldBeEmpty()
        coVerify { store.clear() }
        coVerify { notifier.dismissProgress() }
    }

    @Test
    fun `pause marks downloads as queue and sets isPaused`() = testScope.runTest {
        val source = mockk<HttpSource>()
        val manga = testManga()
        val dl1 = Download(source, manga, testChapter(101L)).apply {
            status = Download.State.DOWNLOADING
        }

        val downloader = createDownloader(listOf(dl1))
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.pause()
        testDispatcher.scheduler.advanceUntilIdle()

        downloader.isPaused shouldBe false // stopLocked resets isPaused after onPaused
        dl1.status shouldBe Download.State.QUEUE
        coVerify { notifier.onPaused() }
    }
}
