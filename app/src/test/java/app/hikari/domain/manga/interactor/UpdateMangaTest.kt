package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.CUSTOM_METADATA_MEMO_KEY
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.hasCustomMetadata
import app.hikari.domain.manga.model.memoWithCustomMetadataMarker
import app.hikari.domain.manga.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateMangaTest {

    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val updateManga = UpdateManga(mangaRepository)

    @Test
    fun `awaitUpdateCustomMetadata rejects blank title without calling repository`() = runBlocking {
        val manga = Manga.create().copy(id = 1L, title = "Original")

        val updated = updateManga.awaitUpdateCustomMetadata(
            manga = manga,
            title = "   ",
            author = "Author",
            artist = "Artist",
            description = "Desc",
            genresInput = "Action",
        )

        assertFalse(updated)
        coVerify(exactly = 0) {
            mangaRepository.updateCustomMetadata(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `awaitUpdateCustomMetadata normalizes fields and writes custom metadata marker`() = runBlocking {
        val manga = Manga.create().copy(
            id = 42L,
            title = "Original Title",
            author = "Original Author",
            genre = listOf("Action"),
        )
        val memoSlot = slot<JsonObject>()
        coEvery {
            mangaRepository.updateCustomMetadata(
                id = 42L,
                title = "Edited Title",
                author = null,
                artist = "Edited Artist",
                description = "Edited Desc",
                genre = listOf("Action", "Drama"),
                memo = capture(memoSlot),
            )
        } returns true

        val updated = updateManga.awaitUpdateCustomMetadata(
            manga = manga,
            title = "  Edited Title ",
            author = "   ",
            artist = " Edited Artist ",
            description = " Edited Desc ",
            genresInput = "Action, Drama, action",
        )

        assertTrue(updated)
        assertTrue(manga.copy(memo = memoSlot.captured).hasCustomMetadata())
    }

    @Test
    fun `awaitResetCustomMetadata restores snapshot and preserves unrelated memo keys`() = runBlocking {
        val original = Manga.create().copy(
            id = 7L,
            title = "Original Title",
            author = "Original Author",
            artist = "Original Artist",
            description = "Original Desc",
            genre = listOf("Fantasy"),
            memo = buildJsonObject {
                put("keep_key", JsonPrimitive("keep_value"))
            },
        )
        val overridden = original.copy(
            title = "Custom Title",
            author = "Custom Author",
            artist = null,
            description = null,
            genre = emptyList(),
            memo = original.memoWithCustomMetadataMarker(),
        )
        val memoSlot = slot<JsonObject>()
        coEvery {
            mangaRepository.updateCustomMetadata(
                id = 7L,
                title = "Original Title",
                author = "Original Author",
                artist = "Original Artist",
                description = "Original Desc",
                genre = listOf("Fantasy"),
                memo = capture(memoSlot),
            )
        } returns true

        val reset = updateManga.awaitResetCustomMetadata(overridden)

        assertTrue(reset)
        assertFalse(memoSlot.captured.containsKey(CUSTOM_METADATA_MEMO_KEY))
        assertEquals(JsonPrimitive("keep_value"), memoSlot.captured["keep_key"])
        assertNull(memoSlot.captured["missing"])
    }
}
