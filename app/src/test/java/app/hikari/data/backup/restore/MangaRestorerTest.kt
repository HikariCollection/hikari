package app.hikari.data.backup.restore

import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.models.BackupChapter
import app.hikari.data.backup.models.BackupHistory
import app.hikari.data.backup.models.BackupManga
import app.hikari.data.backup.models.BackupTracking
import app.hikari.data.backup.restore.restorers.MangaRestorer
import app.hikari.domain.backup.model.RestoredManga
import app.hikari.domain.backup.repository.RestoreRepository
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MangaRestorerTest {

    private val restoreRepository = mockk<RestoreRepository>()
    private val getCategories = mockk<GetCategories>()

    private lateinit var restorer: MangaRestorer

    @BeforeEach
    fun setUp() {
        restorer = MangaRestorer(
            restoreRepository = restoreRepository,
            getCategories = getCategories,
        )
    }

    @Test
    fun `maps backup models and resolves category ids by name`() = runTest {
        val dbCategories = listOf(
            Category(id = 10L, name = "Reading", order = 1, flags = 0),
            Category(id = 20L, name = "Completed", order = 2, flags = 0),
        )
        coEvery { getCategories.await() } returns dbCategories

        val backupCategories = listOf(
            BackupCategory(name = "Reading", order = 100),
            BackupCategory(name = "Completed", order = 200),
        )

        val backupManga = BackupManga(
            url = "/manga/one-piece",
            title = "One Piece",
            source = 1L,
            categories = listOf(100L, 200L, 999L), // 999L has no matching backup category
            chapters = listOf(
                BackupChapter(url = "/chap/1", name = "Chapter 1", chapterNumber = 1f),
            ),
            tracking = listOf(
                BackupTracking(syncId = 1, mediaId = 123L, title = "One Piece"),
            ),
            history = listOf(
                BackupHistory(url = "/chap/1", lastRead = 1_700_000_000_000L, readDuration = 300L),
            ),
            excludedScanlators = listOf("BadScan"),
        )

        val entriesSlot = slot<List<RestoredManga>>()
        coEvery { restoreRepository.restoreManga(capture(entriesSlot), any()) } returns Unit

        restorer.restore(listOf(backupManga), backupCategories)

        entriesSlot.isCaptured shouldBe true
        val restoredList = entriesSlot.captured
        restoredList shouldHaveSize 1
        val entry = restoredList.first()

        entry.manga.title shouldBe "One Piece"
        entry.manga.url shouldBe "/manga/one-piece"
        entry.chapters shouldHaveSize 1
        entry.chapters.first().url shouldBe "/chap/1"
        entry.tracks shouldHaveSize 1
        entry.tracks.first().title shouldBe "One Piece"
        entry.history shouldHaveSize 1
        entry.history.first().chapterUrl shouldBe "/chap/1"
        entry.excludedScanlators shouldContainExactly listOf("BadScan")

        // 100L resolves to Reading (id 10), 200L resolves to Completed (id 20), 999L dropped
        entry.categoryIds shouldContainExactly listOf(10L, 20L)
    }

    @Test
    fun `deduplicates category ids when backup contains duplicate category orders`() = runTest {
        val dbCategories = listOf(
            Category(id = 10L, name = "Reading", order = 1, flags = 0),
        )
        coEvery { getCategories.await() } returns dbCategories

        val backupCategories = listOf(
            BackupCategory(name = "Reading", order = 100),
        )

        val backupManga = BackupManga(
            url = "/manga/bleach",
            title = "Bleach",
            categories = listOf(100L, 100L),
        )

        val entriesSlot = slot<List<RestoredManga>>()
        coEvery { restoreRepository.restoreManga(capture(entriesSlot), any()) } returns Unit

        restorer.restore(listOf(backupManga), backupCategories)

        entriesSlot.isCaptured shouldBe true
        entriesSlot.captured.first().categoryIds shouldContainExactly listOf(10L)
    }
}
