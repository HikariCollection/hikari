package app.hikari.domain.bookmark.interactor

import app.hikari.domain.bookmark.model.BookmarksAndNotesFilter
import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.bookmark.model.BookmarksAndNotesQuery
import app.hikari.domain.bookmark.model.BookmarksAndNotesSort
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.chapter.model.bookmarkNote
import app.hikari.domain.chapter.model.matchesSearchQuery
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

@Inject
class GetBookmarksAndNotes(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    fun subscribe(
        queryFlow: Flow<BookmarksAndNotesQuery>,
        sourceNameResolver: suspend (Long) -> String,
    ): Flow<List<BookmarksAndNotesGroup>> {
        return combine(
            mangaRepository.getMangaWithBookmarksOrNotesAsFlow(),
            chapterRepository.getBookmarkedAndNotedChaptersAsFlow(),
            queryFlow,
        ) { mangaList, chapters, query ->
            val sourceNames = resolveSourceNames(mangaList, sourceNameResolver)
            buildGroups(
                mangaList = mangaList,
                chapters = chapters,
                sourceNamesById = sourceNames,
                query = query,
            )
        }
    }

    suspend fun awaitAll(sourceNameResolver: suspend (Long) -> String): List<BookmarksAndNotesGroup> {
        val mangaList = mangaRepository.getMangaWithBookmarksOrNotesAsFlow().first()
        val chapters = chapterRepository.getBookmarkedAndNotedChaptersAsFlow().first()
        val sourceNames = resolveSourceNames(mangaList, sourceNameResolver)
        return buildGroups(
            mangaList = mangaList,
            chapters = chapters,
            sourceNamesById = sourceNames,
            query = BookmarksAndNotesQuery(),
        )
    }

    private suspend fun resolveSourceNames(
        mangaList: List<Manga>,
        sourceNameResolver: suspend (Long) -> String,
    ): Map<Long, String> {
        return mangaList
            .asSequence()
            .map { it.source }
            .distinct()
            .associateWith { sourceId -> sourceNameResolver(sourceId) }
    }

    fun buildGroups(
        mangaList: List<Manga>,
        chapters: List<BookmarkedChapterEntry>,
        sourceNamesById: Map<Long, String>,
        query: BookmarksAndNotesQuery,
    ): List<BookmarksAndNotesGroup> {
        val chaptersByMangaId = chapters
            .asSequence()
            .filter { it.chapter.bookmark || !it.chapter.bookmarkNote.isNullOrBlank() }
            .groupBy { it.chapter.mangaId }

        val trimmedSearch = query.searchQuery?.trim().orEmpty()

        val groups = mangaList.mapNotNull { manga ->
            if (query.libraryOnly && !manga.favorite) return@mapNotNull null

            val hasMangaNote = manga.notes.isNotBlank()
            val rawChapters = chaptersByMangaId[manga.id].orEmpty()
            val chipChapters = when (query.filter) {
                BookmarksAndNotesFilter.ALL -> rawChapters
                BookmarksAndNotesFilter.BOOKMARKS -> rawChapters.filter { it.chapter.bookmark }
                BookmarksAndNotesFilter.CHAPTER_NOTES -> rawChapters.filter { !it.chapter.bookmarkNote.isNullOrBlank() }
                BookmarksAndNotesFilter.MANGA_NOTES -> emptyList()
            }

            val passesFilter = when (query.filter) {
                BookmarksAndNotesFilter.ALL -> hasMangaNote || chipChapters.isNotEmpty()
                BookmarksAndNotesFilter.BOOKMARKS -> chipChapters.isNotEmpty()
                BookmarksAndNotesFilter.CHAPTER_NOTES -> chipChapters.isNotEmpty()
                BookmarksAndNotesFilter.MANGA_NOTES -> hasMangaNote
            }
            if (!passesFilter) return@mapNotNull null

            val sourceName = sourceNamesById[manga.source].orEmpty()
            val visibleChapters = if (trimmedSearch.isEmpty()) {
                chipChapters
            } else {
                val mangaMatches = manga.title.contains(trimmedSearch, ignoreCase = true) ||
                    sourceName.contains(trimmedSearch, ignoreCase = true) ||
                    (hasMangaNote && manga.notes.contains(trimmedSearch, ignoreCase = true))
                val matchedChapters = if (mangaMatches) {
                    chipChapters
                } else {
                    chipChapters.filter { it.chapter.matchesSearchQuery(trimmedSearch) }
                }
                if (!mangaMatches && matchedChapters.isEmpty()) return@mapNotNull null
                matchedChapters
            }

            val sortedChapters = when (query.sort) {
                BookmarksAndNotesSort.RECENT -> visibleChapters.sortedWith(
                    compareByDescending<BookmarkedChapterEntry> { it.activityAt }
                        .thenBy { it.chapter.sourceOrder }
                        .thenBy { it.chapter.id },
                )
                BookmarksAndNotesSort.TITLE -> visibleChapters.sortedWith(
                    compareBy<BookmarkedChapterEntry> { it.chapter.sourceOrder }
                        .thenBy { it.chapter.chapterNumber }
                        .thenBy { it.chapter.id },
                )
            }

            val lastActivityAt = (sortedChapters.map { it.activityAt } + listOfNotNull(manga.favoriteAt)).maxOrNull()

            BookmarksAndNotesGroup(
                manga = manga,
                sourceName = sourceName,
                chapters = sortedChapters,
                lastActivityAt = lastActivityAt,
            )
        }

        return when (query.sort) {
            BookmarksAndNotesSort.RECENT -> groups.sortedWith(
                compareByDescending<BookmarksAndNotesGroup> { it.lastActivityAt ?: Instant.DISTANT_PAST }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title },
            )
            BookmarksAndNotesSort.TITLE -> groups.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title },
            )
        }
    }
}
