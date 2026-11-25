package app.hikari.domain.bookmark.interactor

import app.hikari.domain.bookmark.model.BookmarksAndNotesFilter
import app.hikari.domain.bookmark.model.BookmarksAndNotesQuery
import app.hikari.domain.bookmark.model.BookmarksAndNotesSort
import app.hikari.domain.chapter.model.BookmarkedChapterEntry
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.withBookmarkNote
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.MangaRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Instant

@Execution(ExecutionMode.CONCURRENT)
class GetBookmarksAndNotesTest {

    private val mangaRepository: MangaRepository = mockk()
    private val chapterRepository: ChapterRepository = mockk()
    private val getBookmarksAndNotes = GetBookmarksAndNotes(mangaRepository, chapterRepository)

    private fun manga(
        id: Long,
        title: String,
        sourceId: Long = 10L,
        favoriteAt: Instant? = Instant.fromEpochMilliseconds(1_000L),
        notes: String = "",
    ): Manga = Manga.create().copy(
        id = id,
        source = sourceId,
        title = title,
        favoriteAt = favoriteAt,
        notes = notes,
    )

    private fun chapterEntry(
        id: Long,
        mangaId: Long,
        name: String = "Chapter $id",
        number: Double = id.toDouble(),
        bookmark: Boolean = true,
        note: String? = null,
        scanlator: String? = null,
        dateFetchMs: Long = 1_000L,
        lastReadMs: Long? = null,
    ): BookmarkedChapterEntry {
        val base = Chapter.create().copy(
            id = id,
            mangaId = mangaId,
            name = name,
            chapterNumber = number,
            bookmark = bookmark,
            scanlator = scanlator,
            dateFetch = Instant.fromEpochMilliseconds(dateFetchMs),
        )
        return BookmarkedChapterEntry(
            chapter = base.copy(memo = base.withBookmarkNote(note)),
            lastReadAt = lastReadMs?.let(Instant::fromEpochMilliseconds),
        )
    }

    @Test
    fun `drops chapters with only unrelated remote memo and drops empty parent manga`() = runTest {
        val orphanedManga = manga(id = 1L, title = "Unrelated Memo Only", notes = "   ")
        val remoteOnlyChapter = chapterEntry(id = 10L, mangaId = 1L, bookmark = false, note = null).let { entry ->
            entry.copy(
                chapter = entry.chapter.copy(
                    memo = JsonObject(mapOf("remote_id" to JsonPrimitive("abc"))),
                ),
            )
        }
        val activeManga = manga(id = 2L, title = "Active Manga", notes = "")
        val activeChapter = chapterEntry(id = 20L, mangaId = 2L, bookmark = true, note = null)

        every { mangaRepository.getMangaWithBookmarksOrNotesAsFlow() } returns
            flowOf(listOf(orphanedManga, activeManga))
        every { chapterRepository.getBookmarkedAndNotedChaptersAsFlow() } returns flowOf(
            listOf(remoteOnlyChapter, activeChapter),
        )

        val result = getBookmarksAndNotes.subscribe(
            queryFlow = flowOf(BookmarksAndNotesQuery()),
            sourceNameResolver = { "Source $it" },
        ).first()

        result.map { it.manga.id } shouldContainExactly listOf(2L)
        result.single().chapters.map { it.chapter.id } shouldContainExactly listOf(20L)
    }

    @Test
    fun `applies filter chips including unbookmarked chapters with preserved notes and libraryOnly`() = runTest {
        val libWithNote = manga(id = 1L, title = "Alpha", notes = "Great worldbuilding")
        val nonLibBookmarked = manga(id = 2L, title = "Beta", favoriteAt = null, notes = "")

        val bookmarkedNoNote = chapterEntry(id = 11L, mangaId = 1L, bookmark = true, note = null)
        val unbookmarkedWithNote = chapterEntry(
            id = 12L,
            mangaId = 1L,
            bookmark = false,
            note = "Preserved plot twist",
        )
        val nonLibChapter = chapterEntry(id = 21L, mangaId = 2L, bookmark = true, note = "Non-lib note")

        every { mangaRepository.getMangaWithBookmarksOrNotesAsFlow() } returns
            flowOf(listOf(libWithNote, nonLibBookmarked))
        every { chapterRepository.getBookmarkedAndNotedChaptersAsFlow() } returns flowOf(
            listOf(bookmarkedNoNote, unbookmarkedWithNote, nonLibChapter),
        )

        val bookmarksOnly = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(filter = BookmarksAndNotesFilter.BOOKMARKS)),
        ) { "Src" }.first()
        bookmarksOnly.flatMap { g -> g.chapters.map { it.chapter.id } } shouldContainExactly listOf(11L, 21L)

        val chapterNotesOnly = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(filter = BookmarksAndNotesFilter.CHAPTER_NOTES)),
        ) { "Src" }.first()
        chapterNotesOnly.flatMap { g -> g.chapters.map { it.chapter.id } } shouldContainExactly listOf(12L, 21L)

        val mangaNotesOnly = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(filter = BookmarksAndNotesFilter.MANGA_NOTES)),
        ) { "Src" }.first()
        mangaNotesOnly.map { it.manga.id } shouldContainExactly listOf(1L)
        mangaNotesOnly.single().chapters.shouldBeEmpty()

        val libraryOnly = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(libraryOnly = true)),
        ) { "Src" }.first()
        libraryOnly.map { it.manga.id } shouldContainExactly listOf(1L)
    }

    @Test
    fun `search query narrows chapter matches on chapter-specific hit and keeps all on manga-level hit`() = runTest {
        val berserk = manga(id = 1L, title = "Berserk", sourceId = 99L, notes = "Dark fantasy epic")
        val ch1 = chapterEntry(id = 101L, mangaId = 1L, name = "Black Swordsman", note = "Intro")
        val ch2 = chapterEntry(id = 102L, mangaId = 1L, name = "Golden Age", note = "Eclipse twist")

        every { mangaRepository.getMangaWithBookmarksOrNotesAsFlow() } returns flowOf(listOf(berserk))
        every { chapterRepository.getBookmarkedAndNotedChaptersAsFlow() } returns flowOf(listOf(ch1, ch2))

        val noteHit = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(searchQuery = "eclipse")),
        ) { "MangaDex" }.first()
        noteHit.single().chapters.map { it.chapter.id } shouldContainExactly listOf(102L)

        val titleHit = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(searchQuery = "berserk")),
        ) { "MangaDex" }.first()
        titleHit.single().chapters.map { it.chapter.id } shouldContainExactly listOf(101L, 102L)

        val seriesNoteHit = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(searchQuery = "fantasy")),
        ) { "MangaDex" }.first()
        seriesNoteHit.single().chapters.map { it.chapter.id } shouldContainExactly listOf(101L, 102L)

        val sourceHit = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(searchQuery = "mangadex")),
        ) { "MangaDex" }.first()
        sourceHit.single().chapters.map { it.chapter.id } shouldContainExactly listOf(101L, 102L)
    }

    @Test
    fun `sorts groups by recent activity descending or title ascending`() = runTest {
        val zeta = manga(id = 1L, title = "Zeta", favoriteAt = Instant.fromEpochMilliseconds(5_000L))
        val alpha = manga(id = 2L, title = "Alpha", favoriteAt = Instant.fromEpochMilliseconds(1_000L))
        val zetaChapter = chapterEntry(id = 10L, mangaId = 1L, dateFetchMs = 2_000L, lastReadMs = 9_000L)
        val alphaChapter = chapterEntry(id = 20L, mangaId = 2L, dateFetchMs = 3_000L, lastReadMs = null)

        every { mangaRepository.getMangaWithBookmarksOrNotesAsFlow() } returns flowOf(listOf(alpha, zeta))
        every { chapterRepository.getBookmarkedAndNotedChaptersAsFlow() } returns
            flowOf(listOf(alphaChapter, zetaChapter))

        val recent = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(sort = BookmarksAndNotesSort.RECENT)),
        ) { "Src" }.first()
        recent.map { it.manga.title } shouldContainExactly listOf("Zeta", "Alpha")
        recent.first().lastActivityAt shouldBe Instant.fromEpochMilliseconds(9_000L)

        val byTitle = getBookmarksAndNotes.subscribe(
            flowOf(BookmarksAndNotesQuery(sort = BookmarksAndNotesSort.TITLE)),
        ) { "Src" }.first()
        byTitle.map { it.manga.title } shouldContainExactly listOf("Alpha", "Zeta")
    }
}
