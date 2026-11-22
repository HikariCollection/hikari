package app.hikari.feature.library

import app.hikari.domain.library.model.LibraryManga
import app.hikari.domain.library.model.search.AndNode
import app.hikari.domain.library.model.search.Comparator
import app.hikari.domain.library.model.search.ComparisonField
import app.hikari.domain.library.model.search.ComparisonQueryNode
import app.hikari.domain.library.model.search.EmptyQueryNode
import app.hikari.domain.library.model.search.FieldQueryNode
import app.hikari.domain.library.model.search.GeneralQueryNode
import app.hikari.domain.library.model.search.MangaField
import app.hikari.domain.library.model.search.NotNode
import app.hikari.domain.library.model.search.OrNode
import app.hikari.domain.library.model.search.QueryNode
import app.hikari.domain.manga.model.Manga
import app.hikari.source.local.LocalSource
import app.hikari.ui.library.LibraryItem
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Instant

@Execution(ExecutionMode.CONCURRENT)
class QueryNodeExtensionsTest {

    private fun createItem(
        id: Long = 1L,
        title: String = "One Piece",
        author: String? = "Eiichiro Oda",
        artist: String? = "Eiichiro Oda",
        description: String? = "Pirate adventure on the Grand Line",
        genre: List<String>? = listOf("Action", "Adventure", "Comedy"),
        sourceId: Long = 100L,
        sourceName: String = "MangaDex",
        sourceLanguage: String = "en",
        notes: String = "Masterpiece series",
        favoriteAt: Instant? = Instant.parse("2024-05-10T12:00:00Z"),
        totalChapters: Long = 1100L,
        readCount: Long = 1050L,
        unreadCount: Long = 50L,
    ): LibraryItem {
        val manga = Manga.create().copy(
            id = id,
            title = title,
            author = author,
            artist = artist,
            description = description,
            genre = genre,
            source = sourceId,
            notes = notes,
            favoriteAt = favoriteAt,
        )
        val libraryManga = LibraryManga(
            manga = manga,
            categories = listOf(1L),
            totalChapters = totalChapters,
            readCount = readCount,
            bookmarkCount = 5L,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = null,
        )
        return LibraryItem(
            libraryManga = libraryManga,
            downloadCount = 0,
            unreadCount = unreadCount,
            isLocal = sourceId == LocalSource.ID,
            sourceName = sourceName,
            sourceLanguage = sourceLanguage,
            badges = LibraryItem.Badges(
                downloadCount = 0,
                unreadCount = unreadCount,
                isLocal = sourceId == LocalSource.ID,
                sourceLanguage = sourceLanguage,
            ),
        )
    }

    @Test
    fun `EmptyQueryNode matches any item`() {
        val item = createItem()
        EmptyQueryNode.matches(item) shouldBe true
    }

    @Test
    fun `GeneralQueryNode matches across any general field`() {
        val item = createItem()

        GeneralQueryNode("piece", negated = false).matches(item) shouldBe true
        GeneralQueryNode("oda", negated = false).matches(item) shouldBe true
        GeneralQueryNode("pirate", negated = false).matches(item) shouldBe true
        GeneralQueryNode("comedy", negated = false).matches(item) shouldBe true
        GeneralQueryNode("mangadex", negated = false).matches(item) shouldBe true
        GeneralQueryNode("masterpiece", negated = false).matches(item) shouldBe true

        GeneralQueryNode("naruto", negated = false).matches(item) shouldBe false
        GeneralQueryNode("piece", negated = true).matches(item) shouldBe false
        GeneralQueryNode("naruto", negated = true).matches(item) shouldBe true
    }

    @Test
    fun `GeneralQueryNode matches local source keyword`() {
        val localItem = createItem(sourceId = LocalSource.ID, sourceName = "Local source")
        GeneralQueryNode("local", negated = false).matches(localItem) shouldBe true
    }

    @Test
    fun `FieldQueryNode matches specific fields`() {
        val item = createItem()

        FieldQueryNode(MangaField.TITLE, "One", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.TITLE, "Naruto", negated = false).matches(item) shouldBe false

        FieldQueryNode(MangaField.AUTHOR, "Oda", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.ARTIST, "Oda", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.DESCRIPTION, "Grand Line", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.GENRE, "Action", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.GENRE, "Horror", negated = false).matches(item) shouldBe false

        FieldQueryNode(MangaField.SOURCE, "MangaDex", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.SOURCE_ID, "100", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.SOURCE_ID, "999", negated = false).matches(item) shouldBe false
        FieldQueryNode(MangaField.SOURCE_ID, "invalid", negated = false).matches(item) shouldBe false

        FieldQueryNode(MangaField.LANGUAGE, "en", negated = false).matches(item) shouldBe true
        FieldQueryNode(MangaField.LANGUAGE, "ja", negated = false).matches(item) shouldBe false

        FieldQueryNode(MangaField.NOTES, "Masterpiece", negated = false).matches(item) shouldBe true
    }

    @Test
    fun `FieldQueryNode handles empty string queries for null or empty values`() {
        val emptyItem = createItem(
            author = null,
            artist = "",
            description = null,
            genre = emptyList(),
            notes = "",
        )

        FieldQueryNode(MangaField.AUTHOR, "", negated = false).matches(emptyItem) shouldBe true
        FieldQueryNode(MangaField.ARTIST, "", negated = false).matches(emptyItem) shouldBe true
        FieldQueryNode(MangaField.DESCRIPTION, "", negated = false).matches(emptyItem) shouldBe true
        FieldQueryNode(MangaField.GENRE, "", negated = false).matches(emptyItem) shouldBe true
        FieldQueryNode(MangaField.NOTES, "", negated = false).matches(emptyItem) shouldBe true

        val filledItem = createItem()
        FieldQueryNode(MangaField.AUTHOR, "", negated = false).matches(filledItem) shouldBe false
        FieldQueryNode(MangaField.GENRE, "", negated = false).matches(filledItem) shouldBe false
    }

    @Test
    fun `ComparisonQueryNode matches numeric metrics`() {
        val item = createItem(
            id = 42L,
            unreadCount = 50L,
            readCount = 1050L,
            totalChapters = 1100L,
        )

        ComparisonQueryNode(ComparisonField.ID, "42", Comparator.EQ, negated = false).matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.ID, "10", Comparator.GT, negated = false).matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.ID, "100", Comparator.LT, negated = false).matches(item) shouldBe true

        ComparisonQueryNode(ComparisonField.UNREAD, "50", Comparator.GTE, negated = false).matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.UNREAD, "51", Comparator.GTE, negated = false).matches(item) shouldBe false

        ComparisonQueryNode(ComparisonField.READ, "1000", Comparator.GT, negated = false).matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.READ, "1050", Comparator.LTE, negated = false).matches(item) shouldBe true

        ComparisonQueryNode(ComparisonField.TOTAL, "1100", Comparator.EQ, negated = false).matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.TOTAL, "1200", Comparator.LT, negated = false).matches(item) shouldBe true

        // Invalid numeric value returns false safely
        ComparisonQueryNode(ComparisonField.UNREAD, "abc", Comparator.GT, negated = false).matches(item) shouldBe false
        ComparisonQueryNode(ComparisonField.UNREAD, "abc", Comparator.GT, negated = true).matches(item) shouldBe true
    }

    @Test
    fun `ComparisonQueryNode matches ISO date added`() {
        val item = createItem(favoriteAt = Instant.parse("2024-05-10T12:00:00Z"))

        ComparisonQueryNode(ComparisonField.DATE_ADDED, "2024-05-10", Comparator.EQ, negated = false)
            .matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.DATE_ADDED, "2024-05-01", Comparator.GT, negated = false)
            .matches(item) shouldBe true
        ComparisonQueryNode(ComparisonField.DATE_ADDED, "2024-05-20", Comparator.LT, negated = false)
            .matches(item) shouldBe true

        // Item with null favoriteAt returns false
        val noDateItem = createItem(favoriteAt = null)
        ComparisonQueryNode(ComparisonField.DATE_ADDED, "2024-05-10", Comparator.EQ, negated = false)
            .matches(noDateItem) shouldBe false

        // Invalid date string returns false safely
        ComparisonQueryNode(ComparisonField.DATE_ADDED, "invalid-date", Comparator.EQ, negated = false)
            .matches(item) shouldBe false
    }

    @Test
    fun `Composite Boolean nodes evaluate correctly`() {
        val item = createItem(unreadCount = 10L)

        val andQuery = AndNode(
            listOf(
                FieldQueryNode(MangaField.TITLE, "One", negated = false),
                ComparisonQueryNode(ComparisonField.UNREAD, "5", Comparator.GT, negated = false),
            ),
        )
        andQuery.matches(item) shouldBe true

        val orQuery = OrNode(
            listOf(
                FieldQueryNode(MangaField.TITLE, "Bleach", negated = false),
                FieldQueryNode(MangaField.GENRE, "Action", negated = false),
            ),
        )
        orQuery.matches(item) shouldBe true

        val notQuery = NotNode(FieldQueryNode(MangaField.TITLE, "One", negated = false))
        notQuery.matches(item) shouldBe false
    }

    @Test
    fun `End to end parsed query matches item`() {
        val item = createItem(unreadCount = 20L)

        QueryNode.from("tag:action && unread >= 10").matches(item) shouldBe true
        QueryNode.from("(tag:romance || tag:comedy) && -tag:horror").matches(item) shouldBe true
        QueryNode.from("tag:action && unread > 50").matches(item) shouldBe false
        QueryNode.from("").matches(item) shouldBe true
    }
}
