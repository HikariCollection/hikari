package app.hikari.data.backup.create

import app.hikari.core.common.extensions.toByteArray
import app.hikari.data.backup.create.creators.MangaBackupCreator
import app.hikari.domain.category.model.Category
import app.hikari.domain.category.repository.CategoryRepository
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.history.model.History
import app.hikari.domain.history.repository.HistoryRepository
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import app.hikari.domain.track.model.Track
import app.hikari.domain.track.repository.TrackRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class MangaBackupCreatorTest {

    private val mangaRepository = mockk<MangaRepository>()
    private val chapterRepository = mockk<ChapterRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val historyRepository = mockk<HistoryRepository>()
    private val trackRepository = mockk<TrackRepository>()

    private lateinit var creator: MangaBackupCreator

    @BeforeEach
    fun setUp() {
        creator = MangaBackupCreator(
            mangaRepository = mangaRepository,
            chapterRepository = chapterRepository,
            categoryRepository = categoryRepository,
            historyRepository = historyRepository,
            trackRepository = trackRepository,
        )
    }

    private fun testManga(id: Long, title: String = "Manga $id"): Manga {
        return Manga.create().copy(
            id = id,
            url = "/manga/$id",
            title = title,
            source = 100L,
            memo = buildJsonObject { put("customNote", "memo-$id") },
        )
    }

    private fun testChapter(id: Long, mangaId: Long, url: String = "/chap/$id"): Chapter {
        return Chapter.create().copy(
            id = id,
            mangaId = mangaId,
            url = url,
            name = "Chapter $id",
            chapterNumber = 1.0,
        )
    }

    @Test
    fun `chunks manga list by 100 and streams all entries`() = runTest {
        val mangas = (1L..150L).map { testManga(it) }

        coEvery { mangaRepository.getExcludedScanlatorsByMangaIds(any()) } returns emptyMap()

        val options = BackupOptions(
            chapters = false,
            categories = false,
            tracking = false,
            history = false,
        )

        val result = creator(mangas, options).toList()

        result shouldHaveSize 150
        coVerify(exactly = 2) { mangaRepository.getExcludedScanlatorsByMangaIds(any()) }
    }

    @Test
    fun `options filter disabled components`() = runTest {
        val manga = testManga(1L)
        val chapter = testChapter(10L, 1L)
        val category = Category(id = 2L, name = "Reading", order = 5, flags = 0)
        val track = Track(
            id = 1L,
            mangaId = 1L,
            trackerId = 10L,
            remoteId = 100L,
            libraryId = null,
            title = "Track Title",
            lastChapterRead = 1.0,
            totalChapters = 10,
            status = 1,
            score = 8.5,
            remoteUrl = "https://example.org",
            startDate = null,
            finishDate = null,
            private = false,
        )

        coEvery { mangaRepository.getExcludedScanlatorsByMangaIds(listOf(1L)) } returns emptyMap()
        coEvery { chapterRepository.getChaptersByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(chapter))
        coEvery { categoryRepository.getCategoriesByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(category))
        coEvery { trackRepository.getTracksByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(track))

        val optionsDisabled = BackupOptions(
            chapters = false,
            categories = false,
            tracking = false,
            history = false,
        )

        val resultDisabled = creator(listOf(manga), optionsDisabled).toList()
        resultDisabled shouldHaveSize 1
        val backupMangaDisabled = resultDisabled.first()
        backupMangaDisabled.chapters.shouldBeEmpty()
        backupMangaDisabled.categories.shouldBeEmpty()
        backupMangaDisabled.tracking.shouldBeEmpty()
        backupMangaDisabled.history.shouldBeEmpty()

        coVerify(exactly = 0) { chapterRepository.getChaptersByMangaIds(any()) }
        coVerify(exactly = 0) { categoryRepository.getCategoriesByMangaIds(any()) }
        coVerify(exactly = 0) { trackRepository.getTracksByMangaIds(any()) }
    }

    @Test
    fun `options enable chapters, categories, and tracking`() = runTest {
        val manga = testManga(1L)
        val chapter = testChapter(10L, 1L)
        val category = Category(id = 2L, name = "Reading", order = 5, flags = 0)
        val track = Track(
            id = 1L,
            mangaId = 1L,
            trackerId = 10L,
            remoteId = 100L,
            libraryId = null,
            title = "Track Title",
            lastChapterRead = 1.0,
            totalChapters = 10,
            status = 1,
            score = 8.5,
            remoteUrl = "https://example.org",
            startDate = null,
            finishDate = null,
            private = false,
        )

        coEvery { mangaRepository.getExcludedScanlatorsByMangaIds(listOf(1L)) } returns mapOf(1L to listOf("ScanGroup"))
        coEvery { chapterRepository.getChaptersByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(chapter))
        coEvery { categoryRepository.getCategoriesByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(category))
        coEvery { trackRepository.getTracksByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(track))

        val optionsEnabled = BackupOptions(
            chapters = true,
            categories = true,
            tracking = true,
            history = false,
        )

        val result = creator(listOf(manga), optionsEnabled).toList()
        result shouldHaveSize 1
        val backupManga = result.first()

        backupManga.chapters shouldHaveSize 1
        backupManga.chapters.first().url shouldBe "/chap/10"

        backupManga.categories shouldContainExactly listOf(5L)

        backupManga.tracking shouldHaveSize 1
        backupManga.tracking.first().title shouldBe "Track Title"

        backupManga.excludedScanlators shouldContainExactly listOf("ScanGroup")
        backupManga.memo shouldBe manga.memo.toByteArray()
    }

    @Test
    fun `history maps to chapter url and drops missing chapters`() = runTest {
        val manga = testManga(1L)
        val chapter1 = testChapter(10L, 1L, url = "/chap/10")
        val timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000L)

        val historyExisting = History(id = 1L, chapterId = 10L, readAt = timestamp, readDuration = 120L)
        val historyDeleted = History(id = 2L, chapterId = 999L, readAt = timestamp, readDuration = 60L)

        coEvery { mangaRepository.getExcludedScanlatorsByMangaIds(listOf(1L)) } returns emptyMap()
        coEvery { chapterRepository.getChaptersByMangaIds(listOf(1L)) } returns mapOf(1L to listOf(chapter1))
        coEvery { historyRepository.getHistoryByMangaIds(listOf(1L)) } returns mapOf(
            1L to listOf(historyExisting, historyDeleted),
        )

        val options = BackupOptions(
            chapters = false,
            categories = false,
            tracking = false,
            history = true,
        )

        val result = creator(listOf(manga), options).toList()
        result shouldHaveSize 1
        val backupManga = result.first()

        // Chapters option is false, so chapters list is empty, but history fetched chapters for url mapping
        backupManga.chapters.shouldBeEmpty()
        backupManga.history shouldHaveSize 1
        backupManga.history.first().url shouldBe "/chap/10"
        backupManga.history.first().lastRead shouldBe 1_700_000_000_000L
        backupManga.history.first().readDuration shouldBe 120L
    }
}
