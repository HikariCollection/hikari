package app.hikari.domain.track.interactor

import app.hikari.data.track.Tracker
import app.hikari.data.track.TrackerManager
import app.hikari.domain.track.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import app.hikari.data.database.models.Track as DbTrack

class RefreshTracksTest {

    private val getTracks = mockk<GetTracks>()
    private val trackerManager = mockk<TrackerManager>()
    private val upsertTrack = mockk<UpsertTrack>(relaxed = true)
    private val syncChapterProgressWithTrack = mockk<SyncChapterProgressWithTrack>(relaxed = true)

    private lateinit var refreshTracks: RefreshTracks

    @BeforeEach
    fun setUp() {
        refreshTracks = RefreshTracks(
            getTracks = getTracks,
            trackerManager = trackerManager,
            upsertTrack = upsertTrack,
            syncChapterProgressWithTrack = syncChapterProgressWithTrack,
        )
    }

    private fun testTrack(id: Long, trackerId: Long): Track {
        return Track(
            id = id,
            mangaId = 100L,
            trackerId = trackerId,
            remoteId = 1000L,
            libraryId = null,
            title = "Test Manga",
            lastChapterRead = 5.0,
            totalChapters = 20L,
            status = 1L,
            score = 8.0,
            remoteUrl = "https://example.com/track",
            startDate = null,
            finishDate = null,
            private = false,
        )
    }

    @Test
    fun `await refreshes logged in trackers and ignores logged out trackers`() = runBlocking {
        val loggedInTracker = mockk<Tracker> {
            every { isLoggedIn } returns true
            coEvery { refresh(any()) } answers { firstArg() }
        }
        val loggedOutTracker = mockk<Tracker> {
            every { isLoggedIn } returns false
        }

        every { trackerManager.get(1L) } returns loggedInTracker
        every { trackerManager.get(2L) } returns loggedOutTracker

        val track1 = testTrack(id = 10L, trackerId = 1L)
        val track2 = testTrack(id = 20L, trackerId = 2L)
        coEvery { getTracks.await(100L) } returns listOf(track1, track2)

        val failures = refreshTracks.await(100L)

        assertTrue(failures.isEmpty())
        coVerify(exactly = 1) { loggedInTracker.refresh(any()) }
        coVerify(exactly = 1) { upsertTrack.await(any()) }
        coVerify(exactly = 1) { syncChapterProgressWithTrack.await(100L, any(), loggedInTracker) }
        coVerify(exactly = 0) { loggedOutTracker.refresh(any()) }
    }

    @Test
    fun `await captures failures without throwing exception`() = runBlocking {
        val failingTracker = mockk<Tracker> {
            every { isLoggedIn } returns true
            coEvery { refresh(any()) } throws RuntimeException("Auth expired")
        }
        every { trackerManager.get(1L) } returns failingTracker

        val track = testTrack(id = 10L, trackerId = 1L)
        coEvery { getTracks.await(100L) } returns listOf(track)

        val failures = refreshTracks.await(100L)

        assertEquals(1, failures.size)
        assertEquals(failingTracker, failures[0].first)
        assertEquals("Auth expired", failures[0].second.message)
    }
}
