package app.hikari.domain.track.interactor

import android.content.Context
import app.hikari.data.track.Tracker
import app.hikari.data.track.TrackerManager
import app.hikari.domain.track.model.Track
import app.hikari.domain.track.store.DelayedTrackingStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import app.hikari.data.database.models.Track as DbTrack

class TrackChapterTest {

    private val getTracks = mockk<GetTracks>()
    private val trackerManager = mockk<TrackerManager>()
    private val upsertTrack = mockk<UpsertTrack>(relaxed = true)
    private val delayedTrackingStore = mockk<DelayedTrackingStore>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private lateinit var trackChapter: TrackChapter
    private var scheduledTasks = mutableListOf<Context>()

    @BeforeEach
    fun setUp() {
        trackChapter = TrackChapter(
            getTracks = getTracks,
            trackerManager = trackerManager,
            upsertTrack = upsertTrack,
            delayedTrackingStore = delayedTrackingStore,
        ).apply {
            scheduleDelayedTask = { scheduledTasks.add(it) }
        }
    }

    private fun testTrack(id: Long, trackerId: Long, lastChapterRead: Double): Track {
        return Track(
            id = id,
            mangaId = 100L,
            trackerId = trackerId,
            remoteId = 1000L,
            libraryId = null,
            title = "Test Manga",
            lastChapterRead = lastChapterRead,
            totalChapters = 50L,
            status = 1L,
            score = 0.0,
            remoteUrl = "https://example.com/track",
            startDate = null,
            finishDate = null,
            private = false,
        )
    }

    @Test
    fun `await skips track when tracker is logged out or chapterNumber is not newer`() = runBlocking {
        val loggedOutTracker = mockk<Tracker> {
            every { isLoggedIn } returns false
        }
        val loggedInTracker = mockk<Tracker> {
            every { isLoggedIn } returns true
        }

        every { trackerManager.get(1L) } returns loggedOutTracker
        every { trackerManager.get(2L) } returns loggedInTracker

        val track1 = testTrack(id = 10L, trackerId = 1L, lastChapterRead = 2.0)
        val track2 = testTrack(id = 20L, trackerId = 2L, lastChapterRead = 5.0)

        coEvery { getTracks.await(100L) } returns listOf(track1, track2)

        // chapterNumber = 4.0 -> track1 skipped because logged out; track2 skipped because 4.0 <= 5.0
        trackChapter.await(context, 100L, 4.0)

        coVerify(exactly = 0) { loggedInTracker.update(any(), any()) }
        verify(exactly = 0) { delayedTrackingStore.add(any(), any()) }
    }

    @Test
    fun `await updates track remotely and removes from delayed tracking store on success`() = runBlocking {
        val tracker = mockk<Tracker> {
            every { isLoggedIn } returns true
            coEvery { refresh(any()) } answers { firstArg() }
            coEvery { update(any(), true) } answers { firstArg() }
        }
        every { trackerManager.get(1L) } returns tracker

        val track = testTrack(id = 10L, trackerId = 1L, lastChapterRead = 2.0)
        coEvery { getTracks.await(100L) } returns listOf(track)

        trackChapter.await(context, 100L, 5.0)

        coVerify(exactly = 1) { tracker.refresh(any()) }
        coVerify(exactly = 1) { tracker.update(any(), true) }
        coVerify(exactly = 1) { upsertTrack.await(any()) }
        verify(exactly = 1) { delayedTrackingStore.remove(10L) }
        verify(exactly = 0) { delayedTrackingStore.add(any(), any()) }
    }

    @Test
    fun `await queues item in delayed tracking store and triggers task on failure`() = runBlocking {
        val tracker = mockk<Tracker> {
            every { isLoggedIn } returns true
            coEvery { refresh(any()) } answers { firstArg() }
            coEvery { update(any(), true) } throws RuntimeException("Network down")
        }
        every { trackerManager.get(1L) } returns tracker

        val track = testTrack(id = 10L, trackerId = 1L, lastChapterRead = 2.0)
        coEvery { getTracks.await(100L) } returns listOf(track)

        trackChapter.await(context, 100L, 5.0, setupJobOnFailure = true)

        verify(exactly = 1) { delayedTrackingStore.add(10L, 5.0) }
        verify(exactly = 0) { delayedTrackingStore.remove(10L) }
        assert(scheduledTasks.contains(context))
    }
}
