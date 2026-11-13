package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import app.hikari.source.model.UpdateStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class HiddenMangaInteractorsTest {

    private val repository = mockk<HiddenMangaRepository>(relaxed = true)
    private val getHiddenManga = GetHiddenManga(repository)
    private val hideManga = HideManga(repository)
    private val unhideManga = UnhideManga(repository)

    private fun testManga(id: Long, source: Long = 1L, url: String = "/manga/$id"): Manga {
        return Manga(
            id = id,
            source = source,
            lastUpdate = null,
            favoriteAt = null,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = null,
            url = url,
            title = "Title $id",
            artist = null,
            author = null,
            description = null,
            genre = null,
            status = 0L,
            thumbnailUrl = null,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            notes = "",
            memo = JsonObject(emptyMap()),
        )
    }

    @Test
    fun `HideManga skips empty lists and delegates non-empty lists`() = runBlocking {
        hideManga.await(emptyList())
        coVerify(exactly = 0) { repository.hide(any()) }

        val list = listOf(testManga(1L), testManga(2L))
        hideManga.await(list)
        coVerify(exactly = 1) { repository.hide(list) }

        val single = testManga(3L)
        hideManga.await(single)
        coVerify(exactly = 1) { repository.hide(listOf(single)) }
    }

    @Test
    fun `UnhideManga skips empty collections and delegates non-empty calls`() = runBlocking {
        unhideManga.await(emptyList())
        coVerify(exactly = 0) { repository.unhide(any<List<Manga>>()) }

        unhideManga.awaitByIds(emptySet())
        coVerify(exactly = 0) { repository.unhideByIds(any()) }

        val list = listOf(testManga(1L))
        unhideManga.await(list)
        coVerify(exactly = 1) { repository.unhide(list) }

        unhideManga.await(1L, "/manga/1")
        coVerify(exactly = 1) { repository.unhide(1L, "/manga/1") }

        val ids = setOf(10L, 20L)
        unhideManga.awaitByIds(ids)
        coVerify(exactly = 1) { repository.unhideByIds(ids) }
    }

    @Test
    fun `GetHiddenManga exposes repository flows`() = runBlocking {
        every { repository.getHiddenUrlsBySource(5L) } returns flowOf(setOf("/a", "/b"))
        every { repository.getAllHiddenKeysAsFlow() } returns flowOf(setOf(5L to "/a"))
        every { repository.getCountAsFlow() } returns flowOf(2L)

        assertEquals(setOf("/a", "/b"), getHiddenManga.subscribeUrlsBySource(5L).first())
        assertEquals(setOf(5L to "/a"), getHiddenManga.subscribeAllKeys().first())
        assertEquals(2L, getHiddenManga.subscribeCount().first())

        verify(exactly = 1) { repository.getHiddenUrlsBySource(5L) }
        verify(exactly = 1) { repository.getAllHiddenKeysAsFlow() }
        verify(exactly = 1) { repository.getCountAsFlow() }
    }

    @Test
    fun `GetHiddenManga awaitAll delegates to repository`() = runBlocking {
        val expected = listOf(
            HiddenManga(
                id = 1L,
                sourceId = 5L,
                url = "/manga/a",
                title = "Title A",
                thumbnailUrl = "https://example.org/a.jpg",
                createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            ),
        )
        coEvery { repository.getAll() } returns expected

        assertEquals(expected, getHiddenManga.awaitAll())
        coVerify(exactly = 1) { repository.getAll() }
    }
}
