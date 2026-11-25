package app.hikari.domain.chapter.interactor

import app.hikari.core.common.preference.InMemoryPreferenceStore
import app.hikari.data.download.DownloadManager
import app.hikari.data.download.DownloadProvider
import app.hikari.data.download.model.Download
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterRemoteUpdate
import app.hikari.domain.chapter.model.NoChaptersException
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.library.service.LibraryPreferences
import app.hikari.domain.manga.interactor.GetExcludedScanlators
import app.hikari.domain.manga.interactor.UpdateManga
import app.hikari.domain.manga.model.Manga
import app.hikari.source.Source
import app.hikari.source.local.LocalSource
import app.hikari.source.model.SChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class SyncChaptersWithSourceTest {

    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private val downloadProvider = mockk<DownloadProvider>(relaxed = true)
    private val chapterRepository = mockk<ChapterRepository>(relaxed = true)
    private val shouldUpdateDbChapter = ShouldUpdateDbChapter()
    private val updateManga = mockk<UpdateManga>(relaxed = true)
    private val getExcludedScanlators = mockk<GetExcludedScanlators>()
    private val preferenceStore = InMemoryPreferenceStore()
    private val libraryPreferences = LibraryPreferences(preferenceStore)

    private val fixedNow = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private lateinit var syncChaptersWithSource: SyncChaptersWithSource

    @BeforeEach
    fun setUp() {
        coEvery { getExcludedScanlators.await(any()) } returns emptySet()
        coEvery { chapterRepository.updateFromRemote(any(), any(), any()) } answers {
            secondArg<List<Chapter>>().mapIndexed { index, ch -> ch.copy(id = 1000L + index) }
        }
        syncChaptersWithSource = SyncChaptersWithSource(
            downloadManager = downloadManager,
            downloadProvider = downloadProvider,
            chapterRepository = chapterRepository,
            shouldUpdateDbChapter = shouldUpdateDbChapter,
            updateManga = updateManga,
            getExcludedScanlators = getExcludedScanlators,
            libraryPreferences = libraryPreferences,
        ).apply {
            now = { fixedNow }
        }
    }

    private fun testManga(id: Long = 1L, sourceId: Long = 100L, title: String = "One Piece") =
        Manga.create().copy(id = id, source = sourceId, title = title)

    private fun sChapter(
        url: String,
        name: String,
        scanlator: String? = null,
        dateUpload: Long = 0L,
    ): SChapter = SChapter.create().apply {
        this.url = url
        this.name = name
        this.scanlator = scanlator
        this.date_upload = dateUpload
    }

    private fun remoteSource(id: Long = 100L): Source = mockk<Source>().apply {
        every { this@apply.id } returns id
    }

    @Test
    fun `throws NoChaptersException for empty remote source but allows empty local source`() = runTest {
        val remote = remoteSource(100L)
        val local = remoteSource(LocalSource.ID)
        val manga = testManga()

        shouldThrow<NoChaptersException> {
            syncChaptersWithSource.await(emptyList(), manga, remote)
        }

        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns emptyList()
        syncChaptersWithSource.await(emptyList(), manga.copy(source = LocalSource.ID), local).shouldBeEmpty()
    }

    @Test
    fun `deduplicates incoming chapters sanitizes names and falls back upload date`() = runTest {
        val manga = testManga(title = "One Piece")
        val source = remoteSource()
        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns emptyList()

        val addedSlot = slot<List<Chapter>>()
        coEvery { chapterRepository.updateFromRemote(any(), capture(addedSlot), any()) } answers {
            addedSlot.captured.mapIndexed { idx, c -> c.copy(id = idx + 1L) }
        }

        val ch2 = sChapter("/ch/2", "One Piece - Chapter 2", dateUpload = 2_000_000L)
        val ch2Dup = sChapter("/ch/2", "Duplicate", dateUpload = 9_999_999L)
        val ch1NoDate = sChapter("/ch/1", "One Piece - Chapter 1", dateUpload = 0L)

        val result = syncChaptersWithSource.await(listOf(ch2, ch2Dup, ch1NoDate), manga, source)

        result shouldHaveSize 2
        val added = addedSlot.captured
        added[0].name shouldBe "Chapter 2"
        added[0].chapterNumber shouldBe 2.0
        added[0].dateUpload shouldBe Instant.fromEpochMilliseconds(2_000_000L)
        added[0].dateFetch shouldBe fixedNow + 2.milliseconds

        added[1].name shouldBe "Chapter 1"
        added[1].chapterNumber shouldBe 1.0
        added[1].dateUpload shouldBe Instant.fromEpochMilliseconds(2_000_000L)
        added[1].dateFetch shouldBe fixedNow + 1.milliseconds

        coVerify { updateManga.awaitUpdateLastUpdate(manga.id, fixedNow) }
    }

    @Test
    fun `updates changed chapters renames downloaded chapter dir and cancels removed queued downloads`() = runTest {
        val manga = testManga()
        val source = remoteSource()
        val existingDb = Chapter.create().copy(
            id = 10L,
            mangaId = manga.id,
            url = "/ch/1",
            name = "Chapter 1 Old",
            chapterNumber = 1.0,
            sourceOrder = 0L,
        )
        val removedDb = Chapter.create().copy(
            id = 20L,
            mangaId = manga.id,
            url = "/ch/removed",
            name = "Removed",
            chapterNumber = 99.0,
        )
        val queuedDownload = mockk<Download>()

        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns listOf(existingDb, removedDb)
        every { downloadProvider.isChapterDirNameChanged(existingDb, any()) } returns true
        every {
            downloadManager.isChapterDownloaded(
                existingDb.name,
                existingDb.scanlator,
                existingDb.url,
                manga.title,
                manga.source,
            )
        } returns true
        every { downloadManager.getQueuedDownloadOrNull(20L) } returns queuedDownload
        every { downloadManager.getQueuedDownloadOrNull(10L) } returns null

        val updatedSlot = slot<List<ChapterRemoteUpdate>>()
        val removedSlot = slot<List<Long>>()
        coEvery {
            chapterRepository.updateFromRemote(capture(removedSlot), any(), capture(updatedSlot))
        } returns emptyList()

        val result = syncChaptersWithSource.await(
            listOf(sChapter("/ch/1", "Chapter 1 New", dateUpload = 1_000L)),
            manga,
            source,
        )

        result.shouldBeEmpty()
        removedSlot.captured shouldBe listOf(20L)
        updatedSlot.captured shouldHaveSize 1
        updatedSlot.captured.first().name shouldBe "Chapter 1 New"
        coVerify { downloadManager.renameChapter(source, manga, existingDb, any()) }
        coVerify { downloadManager.cancelQueuedDownloads(listOf(queuedDownload)) }
    }

    @Test
    fun `preserves markDuplicateAsRead when an unread removed chapter shares the same number`() = runTest {
        libraryPreferences.markDuplicateReadChapterAsRead.set(setOf(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_NEW))
        val manga = testManga()
        val source = remoteSource()

        val keptRead = Chapter.create().copy(
            id = 1L,
            mangaId = manga.id,
            url = "/ch/5/groupA",
            name = "Chapter 5",
            scanlator = "Group A",
            chapterNumber = 5.0,
            read = true,
            sourceOrder = 0L,
        )
        val removedUnread = Chapter.create().copy(
            id = 2L,
            mangaId = manga.id,
            url = "/ch/5/groupB-old",
            name = "Chapter 5",
            scanlator = "Group B",
            chapterNumber = 5.0,
            read = false,
            sourceOrder = 1L,
        )
        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns listOf(keptRead, removedUnread)

        val addedSlot = slot<List<Chapter>>()
        coEvery { chapterRepository.updateFromRemote(any(), capture(addedSlot), any()) } answers {
            addedSlot.captured.mapIndexed { idx, c -> c.copy(id = 100L + idx) }
        }

        val result = syncChaptersWithSource.await(
            listOf(
                sChapter("/ch/5/groupA", "Chapter 5", scanlator = "Group A"),
                sChapter("/ch/5/groupB-new", "Chapter 5", scanlator = "Group B"),
            ),
            manga,
            source,
        )

        addedSlot.captured shouldHaveSize 1
        addedSlot.captured.first().read shouldBe true
        result.shouldBeEmpty()
    }

    @Test
    fun `preserves latest dateFetch for removed duplicates and filters excluded scanlators`() = runTest {
        val manga = testManga()
        val source = remoteSource()

        val olderRemoved = Chapter.create().copy(
            id = 1L,
            mangaId = manga.id,
            url = "/ch/3/old1",
            name = "Chapter 3",
            chapterNumber = 3.0,
            read = true,
            bookmark = true,
            dateFetch = Instant.fromEpochMilliseconds(100_000L),
        )
        val newerRemoved = Chapter.create().copy(
            id = 2L,
            mangaId = manga.id,
            url = "/ch/3/old2",
            name = "Chapter 3",
            chapterNumber = 3.0,
            read = false,
            bookmark = false,
            dateFetch = Instant.fromEpochMilliseconds(500_000L),
        )
        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns listOf(olderRemoved, newerRemoved)
        coEvery { getExcludedScanlators.await(manga.id) } returns setOf("Blocked Group")

        val addedSlot = slot<List<Chapter>>()
        coEvery { chapterRepository.updateFromRemote(any(), capture(addedSlot), any()) } answers {
            addedSlot.captured.mapIndexed { idx, c -> c.copy(id = 200L + idx) }
        }

        val result = syncChaptersWithSource.await(
            listOf(
                sChapter("/ch/3/new", "Chapter 3", scanlator = "Good Group"),
                sChapter("/ch/4/blocked", "Chapter 4", scanlator = "Blocked Group"),
                sChapter("/ch/5/good", "Chapter 5", scanlator = "Good Group"),
            ),
            manga,
            source,
        )

        val restoredCh3 = addedSlot.captured.first { it.url == "/ch/3/new" }
        restoredCh3.read shouldBe true
        restoredCh3.bookmark shouldBe true
        restoredCh3.dateFetch shouldBe Instant.fromEpochMilliseconds(500_000L)

        result.map { it.url } shouldBe listOf("/ch/5/good")
    }

    @Test
    fun `preserves local bookmark note across remote update and chapter re-upload`() = runTest {
        val manga = testManga()
        val source = remoteSource()

        val dbUpdatedChapter = Chapter.create().let { base ->
            base.copy(
                id = 10L,
                mangaId = manga.id,
                url = "/ch/1",
                name = "Chapter 1",
                chapterNumber = 1.0,
                bookmark = true,
                memo = base.withBookmarkNote("Favorite panel on page 8"),
            )
        }
        val dbRemovedChapter = Chapter.create().let { base ->
            base.copy(
                id = 11L,
                mangaId = manga.id,
                url = "/ch/2/old",
                name = "Chapter 2",
                chapterNumber = 2.0,
                bookmark = true,
                dateFetch = Instant.fromEpochMilliseconds(400_000L),
                memo = base.withBookmarkNote("Re-upload note"),
            )
        }
        coEvery { chapterRepository.getChapterByMangaId(manga.id) } returns listOf(
            dbUpdatedChapter,
            dbRemovedChapter,
        )

        val addedSlot = slot<List<Chapter>>()
        val updatedSlot = slot<List<ChapterRemoteUpdate>>()
        coEvery {
            chapterRepository.updateFromRemote(any(), capture(addedSlot), capture(updatedSlot))
        } answers {
            addedSlot.captured.mapIndexed { idx, c -> c.copy(id = 300L + idx) }
        }

        syncChaptersWithSource.await(
            listOf(
                sChapter("/ch/1", "Chapter 1 (V2)"),
                sChapter("/ch/2/new", "Chapter 2"),
            ),
            manga,
            source,
        )

        updatedSlot.captured.single().let { update ->
            update.id shouldBe 10L
            update.name shouldBe "Chapter 1 (V2)"
            Chapter.create().copy(memo = update.memo).bookmarkNote shouldBe "Favorite panel on page 8"
        }

        addedSlot.captured.single().let { added ->
            added.url shouldBe "/ch/2/new"
            added.bookmark shouldBe true
            added.bookmarkNote shouldBe "Re-upload note"
        }
    }
}
