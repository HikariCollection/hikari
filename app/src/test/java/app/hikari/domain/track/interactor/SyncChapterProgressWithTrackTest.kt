package app.hikari.domain.track.interactor

import app.hikari.data.track.EnhancedTracker
import app.hikari.data.track.Tracker
import app.hikari.domain.chapter.interactor.GetChaptersByMangaId
import app.hikari.domain.chapter.interactor.UpdateChapter
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterUpdate
import app.hikari.domain.track.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncChapterProgressWithTrackTest {

    private val updateChapter = mockk<UpdateChapter>(relaxed = true)
    private val upsertTrack = mockk<UpsertTrack>(relaxed = true)
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()

    private lateinit var syncProgress: SyncChapterProgressWithTrack

    @BeforeEach
    fun setUp() {
        syncProgress = SyncChapterProgressWithTrack(
            updateChapter = updateChapter,
            upsertTrack = upsertTrack,
            getChaptersByMangaId = getChaptersByMangaId,
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

    private fun testTrack(lastChapterRead: Double): Track {
        return Track(
            id = 1L,
            mangaId = 100L,
            trackerId = 10L,
            remoteId = 1000L,
            libraryId = null,
            title = "Test Manga",
            lastChapterRead = lastChapterRead,
            totalChapters = 20L,
            status = 1L,
            score = 0.0,
            remoteUrl = "https://example.com/track",
            startDate = null,
            finishDate = null,
            private = false,
        )
    }

    private interface EnhancedTrackerWithTracker : Tracker, EnhancedTracker

    @Test
    fun `await exits early when tracker is not an EnhancedTracker`() = runBlocking {
        val plainTracker = mockk<Tracker>(relaxed = true)
        val track = testTrack(lastChapterRead = 5.0)

        syncProgress.await(100L, track, plainTracker)

        coVerify(exactly = 0) { getChaptersByMangaId.await(any()) }
        coVerify(exactly = 0) { upsertTrack.await(any()) }
    }

    @Test
    fun `await syncs local read chapters and updates remote track to continuous read progress`() = runBlocking {
        val enhancedTracker = mockk<EnhancedTrackerWithTracker>(relaxed = true)
        val track = testTrack(lastChapterRead = 2.0)

        // Chapters 1..3 read locally, chapter 4 unread. Remote has read 2.0.
        // Should mark unread chapters <= remote read (if any) as read, and update remote to continuous local (3.0).
        val chapters = listOf(
            testChapter(1L, 1.0, true),
            testChapter(2L, 2.0, true),
            testChapter(3L, 3.0, true),
            testChapter(4L, 4.0, false),
        )
        coEvery { getChaptersByMangaId.await(100L) } returns chapters

        val capturedTrack = slot<Track>()
        coEvery { upsertTrack.await(capture(capturedTrack)) } returns Unit

        syncProgress.await(100L, track, enhancedTracker)

        coVerify(exactly = 1) { enhancedTracker.update(any()) }
        coVerify(exactly = 1) { upsertTrack.await(any()) }
        assertEquals(3.0, capturedTrack.captured.lastChapterRead)
    }

    @Test
    fun `await marks local chapters read when remote track has higher progress`() = runBlocking {
        val enhancedTracker = mockk<EnhancedTrackerWithTracker>(relaxed = true)
        val track = testTrack(lastChapterRead = 3.0)

        // Chapter 1 is read locally, chapters 2 and 3 are unread. Remote has 3.0.
        val chapters = listOf(
            testChapter(1L, 1.0, true),
            testChapter(2L, 2.0, false),
            testChapter(3L, 3.0, false),
            testChapter(4L, 4.0, false),
        )
        coEvery { getChaptersByMangaId.await(100L) } returns chapters

        val chapterUpdatesSlot = slot<List<ChapterUpdate>>()
        coEvery { updateChapter.awaitAll(capture(chapterUpdatesSlot)) } returns Unit

        syncProgress.await(100L, track, enhancedTracker)

        coVerify(exactly = 1) { updateChapter.awaitAll(any()) }
        val updatedIds = chapterUpdatesSlot.captured.map { it.id }
        assertEquals(listOf(2L, 3L), updatedIds)
    }
}
