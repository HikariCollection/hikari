package app.hikari.domain.manga.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaMetadataOverrideTest {

    @Test
    fun `hasCustomMetadata returns false by default and true when marker is present`() {
        val plain = Manga.create().copy(title = "Original Title")
        assertFalse(plain.hasCustomMetadata())

        val overridden = plain.copy(
            memo = plain.memoWithCustomMetadataMarker(),
        )
        assertTrue(overridden.hasCustomMetadata())
    }

    @Test
    fun `memoWithCustomMetadataMarker preserves original source metadata across repeated edits`() {
        val original = Manga.create().copy(
            title = "Original Title",
            author = "Original Author",
            artist = "Original Artist",
            description = "Original Desc",
            genre = listOf("Action", "Adventure"),
        )
        val firstMemo = original.memoWithCustomMetadataMarker()
        val editedOnce = original.copy(
            title = "Edited Once",
            author = null,
            memo = firstMemo,
        )
        val secondMemo = editedOnce.memoWithCustomMetadataMarker()
        val restored = editedOnce.copy(memo = secondMemo).extractOriginalMetadata()

        assertNotNull(restored)
        assertEquals("Original Title", restored!!.title)
        assertEquals("Original Author", restored.author)
        assertEquals("Original Artist", restored.artist)
        assertEquals("Original Desc", restored.description)
        assertEquals(listOf("Action", "Adventure"), restored.genres)
    }

    @Test
    fun `normalizeCustomMetadata trims fields and deduplicates genres`() {
        val result = normalizeCustomMetadata(
            title = "  My Custom Title  ",
            author = "   ",
            artist = "  Artist Name ",
            description = "",
            genresInput = " Action , Drama, , action , Fantasy ",
        )

        assertEquals("My Custom Title", result.title)
        assertNull(result.author)
        assertEquals("Artist Name", result.artist)
        assertNull(result.description)
        assertEquals(listOf("Action", "Drama", "Fantasy"), result.genres)
    }

    @Test
    fun `withoutCustomMetadataMarker preserves unrelated memo keys`() {
        val memo = buildJsonObject {
            put("source_key", JsonPrimitive("keep_me"))
            put(CUSTOM_METADATA_MEMO_KEY, JsonPrimitive(true))
        }
        val manga = Manga.create().copy(memo = memo)

        val cleaned = manga.memoWithoutCustomMetadataMarker()
        assertFalse(cleaned.containsKey(CUSTOM_METADATA_MEMO_KEY))
        assertEquals(JsonPrimitive("keep_me"), cleaned["source_key"])
    }

    @Test
    fun `mergeCustomMetadataMarker preserves original snapshot when remote memo updates`() {
        val original = Manga.create().copy(title = "Original Title")
        val localMemo = original.memoWithCustomMetadataMarker()
        val remoteMemo = buildJsonObject {
            put("remote_key", JsonPrimitive("v2"))
        }
        val merged = mergeCustomMetadataMarker(remoteMemo, localMemo)
        assertTrue(merged.containsKey(CUSTOM_METADATA_MEMO_KEY))
        assertEquals(JsonPrimitive("v2"), merged["remote_key"])
        assertEquals(
            "Original Title",
            original.copy(memo = merged).extractOriginalMetadata()?.title,
        )
    }
}
