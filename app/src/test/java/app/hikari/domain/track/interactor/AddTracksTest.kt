package app.hikari.domain.track.interactor

import app.hikari.data.track.EnhancedTracker
import app.hikari.data.track.Tracker
import app.hikari.data.track.TrackerManager
import app.hikari.data.track.model.TrackSearch
import app.hikari.domain.chapter.interactor.GetChaptersByMangaId
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.history.interactor.GetHistory
import app.hikari.domain.history.model.History
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.track.model.Track
import app.hikari.source.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Instant
import app.hikari.data.database.models.Track as DbTrack

class AddTracksTest {

    private val upsertTrack = mockk<UpsertTrack>(relaxed = true)
    private val syncChapterProgressWithTrack = mockk<SyncChapterProgressWithTrack>(relaxed = true)
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val trackerManager = mockk<TrackerManager>()
    private val getHistory = mockk<GetHistory>()

    private lateinit var addTracks: AddTracks

    @BeforeEach
    fun setUp() {
        addTracks = AddTracks(
            upsertTrack = upsertTrack,
            syncChapterProgressWithTrack = syncChapterProgressWithTrack,
            getChaptersByMangaId = getChaptersByMangaId,
            trackerManager = trackerManager,
            getHistory = getHistory,
        )
    }

    private fun testChapter(id: Long, number: Double, read: Boolean): Chapter {
        return Chapter.create().copy(
            id = id,
            mangaId = 100L,
            chapterNumber = number,
            read = read,
        )
    }

    private fun testDbTrack(id: Long = 1L, trackerId: Long = 10L, lastChapterRead: Double = 0.0): DbTrack {
        return DbTrack.create(trackerId).apply {
            this.id = id
            this.manga_id = 100L
            this.last_chapter_read = lastChapterRead
            this.title = "Test Manga"
        }
    }

    @Test
    fun `bind ignores unnumbered chapters when computing continuous read prefix`() = runBlocking {
        val tracker = mockk<Tracker>(relaxed = true)
        val dbTrack = testDbTrack(lastChapterRead = 0.0)

        // Chapter -1.0 is unread (special), chapters 1.0 and 2.0 are read, chapter 3.0 is unread
        val chapters = listOf(
            testChapter(1L, -1.0, false),
            testChapter(2L, 1.0, true),
            testChapter(3L, 2.0, true),
            testChapter(4L, 3.0, false),
        )
        coEvery { getChaptersByMangaId.await(100L) } returns chapters
        coEvery { getHistory.await(100L) } returns emptyList()

        val capturedTrack = slot<Track>()
        coEvery { upsertTrack.await(capture(capturedTrack)) } returns Unit

        addTracks.bind(tracker, dbTrack, 100L)

        coVerify(exactly = 1) { tracker.bind(dbTrack, true) }
        coVerify(exactly = 1) { tracker.setRemoteLastChapterRead(any(), 2) }
        coVerify(exactly = 1) { syncChapterProgressWithTrack.await(100L, any(), tracker) }
        assertEquals(2.0, capturedTrack.captured.lastChapterRead)
    }

    @Test
    fun `bind sets remote start date from history when track start date is null`() = runBlocking {
        val tracker = mockk<Tracker>(relaxed = true)
        val dbTrack = testDbTrack(lastChapterRead = 1.0)
        dbTrack.started_reading_date = 0L

        val chapters = listOf(testChapter(1L, 1.0, true))
        val readInstant = Instant.fromEpochMilliseconds(1_700_000_000_000L)
        val historyList = listOf(History.create().copy(readAt = readInstant))

        coEvery { getChaptersByMangaId.await(100L) } returns chapters
        coEvery { getHistory.await(100L) } returns historyList

        val capturedTrack = slot<Track>()
        coEvery { upsertTrack.await(capture(capturedTrack)) } returns Unit

        addTracks.bind(tracker, dbTrack, 100L)

        coVerify(exactly = 1) { tracker.setRemoteStartDate(any(), 1_700_000_000_000L) }
        assertEquals(readInstant, capturedTrack.captured.startDate)
    }

    private interface EnhancedTrackerWithTracker : Tracker, EnhancedTracker

    @Test
    fun `bindEnhancedTrackers matches and binds enhanced trackers safely`() = runBlocking {
        val source = mockk<Source>()
        val manga = Manga.create().copy(id = 100L, title = "Matched Manga")

        val matchedTrack = TrackSearch.create(10L).apply {
            title = "Matched Manga"
            tracking_url = "https://example.com/matched"
        }
        val enhancedTracker = mockk<EnhancedTrackerWithTracker>(relaxed = true) {
            every { accept(source) } returns true
            coEvery { match(manga) } returns matchedTrack
        }
        every { trackerManager.loggedInTrackers() } returns listOf(enhancedTracker)

        addTracks.bindEnhancedTrackers(manga, source)

        coVerify(exactly = 1) { enhancedTracker.bind(any(), false) }
        coVerify(exactly = 1) { upsertTrack.await(any()) }
        coVerify(exactly = 1) { syncChapterProgressWithTrack.await(100L, any(), enhancedTracker) }
    }
}
