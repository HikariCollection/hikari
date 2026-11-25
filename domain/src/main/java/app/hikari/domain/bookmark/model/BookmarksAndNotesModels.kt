package app.hikari.domain.bookmark.model

import androidx.compose.runtime.Immutable
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.manga.model.Manga
import kotlinx.serialization.Serializable
import kotlin.time.Instant

enum class BookmarksAndNotesFilter {
    ALL,
    BOOKMARKS,
    CHAPTER_NOTES,
    MANGA_NOTES,
}

enum class BookmarksAndNotesSort {
    RECENT,
    TITLE,
}

@Immutable
data class BookmarksAndNotesQuery(
    val filter: BookmarksAndNotesFilter = BookmarksAndNotesFilter.ALL,
    val libraryOnly: Boolean = false,
    val sort: BookmarksAndNotesSort = BookmarksAndNotesSort.RECENT,
    val searchQuery: String? = null,
)

@Immutable
data class BookmarksAndNotesGroup(
    val manga: Manga,
    val sourceName: String,
    val chapters: List<BookmarkedChapterEntry>,
    val lastActivityAt: Instant?,
)

@Serializable
data class BookmarksAndNotesExportPayload(
    val exportedAt: Instant,
    val entries: List<BookmarksAndNotesExportEntry>,
)

@Serializable
data class BookmarksAndNotesExportEntry(
    val mangaTitle: String,
    val sourceName: String,
    val inLibrary: Boolean,
    val mangaNotes: String,
    val chapters: List<BookmarksAndNotesExportChapter>,
)

@Serializable
data class BookmarksAndNotesExportChapter(
    val chapterName: String,
    val chapterNumber: Double,
    val scanlator: String?,
    val bookmarked: Boolean,
    val bookmarkNote: String?,
    val lastPageRead: Int,
    val read: Boolean,
)
