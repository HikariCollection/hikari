package app.hikari.data.backup

import app.hikari.data.backup.create.creators.HiddenMangaBackupCreator
import app.hikari.data.backup.models.BackupHiddenManga
import app.hikari.data.backup.restore.restorers.HiddenMangaRestorer
import app.hikari.domain.manga.interactor.GetHiddenManga
import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

class HiddenMangaBackupRestoreTest {

    private val repository = mockk<HiddenMangaRepository>(relaxed = true)
    private val getHiddenManga = mockk<GetHiddenManga>(relaxed = true)

    @Test
    fun `HiddenMangaBackupCreator maps domain HiddenManga to BackupHiddenManga`() = runBlocking {
        val fixedInstant = Instant.fromEpochMilliseconds(1_700_000_000_000L)
        val domainList = listOf(
            HiddenManga(
                id = 1L,
                sourceId = 42L,
                url = "/manga/42",
                title = "Manga 42",
                thumbnailUrl = "https://example.org/cover.png",
                createdAt = fixedInstant,
            ),
            HiddenManga(
                id = 2L,
                sourceId = 43L,
                url = "/manga/43",
                title = "Manga 43",
                thumbnailUrl = null,
                createdAt = fixedInstant,
            ),
        )
        coEvery { getHiddenManga.awaitAll() } returns domainList

        val creator = HiddenMangaBackupCreator(getHiddenManga)
        val backupList = creator()

        assertEquals(2, backupList.size)
        assertEquals(42L, backupList[0].source)
        assertEquals("/manga/42", backupList[0].url)
        assertEquals("Manga 42", backupList[0].title)
        assertEquals("https://example.org/cover.png", backupList[0].thumbnailUrl)
        assertEquals(1_700_000_000_000L, backupList[0].createdAt)

        assertEquals(43L, backupList[1].source)
        assertEquals("/manga/43", backupList[1].url)
        assertEquals(null, backupList[1].thumbnailUrl)
    }

    @Test
    fun `HiddenMangaRestorer ignores blank URLs and preserves positive createdAt`() = runBlocking {
        val restorer = HiddenMangaRestorer(repository)

        val input = listOf(
            BackupHiddenManga(
                source = 10L,
                url = "/valid",
                title = "Valid Title",
                thumbnailUrl = "https://example.org/valid.jpg",
                createdAt = 1_700_000_123_000L,
            ),
            BackupHiddenManga(
                source = 10L,
                url = "   ",
                title = "Blank URL",
                thumbnailUrl = null,
                createdAt = 1_700_000_456_000L,
            ),
        )

        val slot = slot<List<HiddenManga>>()
        coEvery { repository.upsertAll(capture(slot)) } returns Unit

        restorer(input)

        coVerify(exactly = 1) { repository.upsertAll(any()) }
        val saved = slot.captured
        assertEquals(1, saved.size)
        assertEquals(10L, saved[0].sourceId)
        assertEquals("/valid", saved[0].url)
        assertEquals("Valid Title", saved[0].title)
        assertEquals("https://example.org/valid.jpg", saved[0].thumbnailUrl)
        assertEquals(Instant.fromEpochMilliseconds(1_700_000_123_000L), saved[0].createdAt)
    }

    @Test
    fun `HiddenMangaRestorer substitutes current timestamp when createdAt is zero or negative`() = runBlocking {
        val fixedClock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1_750_000_000_000L)
        }
        val restorer = HiddenMangaRestorer(repository, clock = fixedClock)

        val input = listOf(
            BackupHiddenManga(
                source = 20L,
                url = "/zero-created-at",
                title = "Zero Timestamp",
                createdAt = 0L,
            ),
            BackupHiddenManga(
                source = 20L,
                url = "/negative-created-at",
                title = "Negative Timestamp",
                createdAt = -500L,
            ),
        )

        val slot = slot<List<HiddenManga>>()
        coEvery { repository.upsertAll(capture(slot)) } returns Unit

        restorer(input)

        coVerify(exactly = 1) { repository.upsertAll(any()) }
        val saved = slot.captured
        assertEquals(2, saved.size)
        assertEquals(Instant.fromEpochMilliseconds(1_750_000_000_000L), saved[0].createdAt)
        assertEquals(Instant.fromEpochMilliseconds(1_750_000_000_000L), saved[1].createdAt)
    }

    @Test
    fun `HiddenMangaRestorer does nothing when input is empty or all entries are blank`() = runBlocking {
        val restorer = HiddenMangaRestorer(repository)

        restorer(emptyList())
        restorer(listOf(BackupHiddenManga(source = 1L, url = "", title = "Empty")))
        restorer(listOf(BackupHiddenManga(source = 1L, url = "  \t  ", title = "Whitespace")))

        coVerify(exactly = 0) { repository.upsertAll(any()) }
    }
}
