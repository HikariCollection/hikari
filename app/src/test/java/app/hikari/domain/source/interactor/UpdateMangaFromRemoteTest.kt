package app.hikari.domain.source.interactor

import app.hikari.core.common.preference.InMemoryPreferenceStore
import app.hikari.data.cache.CoverCache
import app.hikari.data.download.DownloadManager
import app.hikari.domain.chapter.interactor.SyncChaptersWithSource
import app.hikari.domain.chapter.repository.ChapterRepository
import app.hikari.domain.library.service.LibraryPreferences
import app.hikari.domain.manga.model.CUSTOM_METADATA_MEMO_KEY
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaRemoteUpdate
import app.hikari.domain.manga.model.extractOriginalMetadata
import app.hikari.domain.manga.model.memoWithCustomMetadataMarker
import app.hikari.domain.manga.repository.MangaRepository
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.Source
import app.hikari.source.model.SManga
import app.hikari.source.model.SMangaUpdate
import app.hikari.source.model.UpdateStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class UpdateMangaFromRemoteTest {

    private val sourceManager = mockk<SourceManager>(relaxed = true)
    private val chapterRepository = mockk<ChapterRepository>(relaxed = true)
    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val syncChaptersWithSource = mockk<SyncChaptersWithSource>(relaxed = true)
    private val coverCache = mockk<CoverCache>(relaxed = true)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val interactor = UpdateMangaFromRemote(
        sourceManager = sourceManager,
        chapterRepository = chapterRepository,
        mangaRepository = mangaRepository,
        syncChaptersWithSource = syncChaptersWithSource,
        coverCache = coverCache,
        libraryPreferences = libraryPreferences,
        downloadManager = downloadManager,
    )

    @Test
    fun `preserves custom metadata overrides and merges remote memo`() = runBlocking {
        val original = Manga.create().copy(
            id = 10L,
            source = 1L,
            title = "Original Title",
            favoriteAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )
        val localManga = original.copy(
            title = "Custom Title",
            author = "Custom Author",
            memo = original.memoWithCustomMetadataMarker(),
        )
        val remoteSManga = SManga.create().apply {
            title = "Remote Title"
            author = "Remote Author"
            artist = "Remote Artist"
            description = "Remote Desc"
            genre = "Action, Comedy"
            status = SManga.ONGOING
            update_strategy = UpdateStrategy.ALWAYS_UPDATE
            memo = buildJsonObject {
                put("remote_key", JsonPrimitive("remote_val"))
            }
        }
        val source = mockk<Source>()
        coEvery { chapterRepository.getChapterByMangaId(10L) } returns emptyList()
        coEvery { source.getMangaUpdate(any(), any(), any(), any()) } returns SMangaUpdate(remoteSManga, emptyList())
        coEvery { syncChaptersWithSource.await(any(), any(), any(), any()) } returns emptyList()
        coEvery { mangaRepository.getMangaById(10L) } returns localManga

        val updateSlot = slot<MangaRemoteUpdate>()
        coEvery { mangaRepository.updateRemote(capture(updateSlot)) } returns true

        val result = interactor.invoke(
            source = source,
            manga = localManga,
            fetchDetails = true,
            fetchChapters = false,
            manualFetch = false,
        )

        assertTrue(result.isSuccess)
        val captured = updateSlot.captured
        assertNull(captured.title)
        assertNull(captured.author)
        assertNull(captured.artist)
        assertNull(captured.description)
        assertNull(captured.genre)
        assertTrue(captured.memo.containsKey(CUSTOM_METADATA_MEMO_KEY))
        assertEquals(JsonPrimitive("remote_val"), captured.memo["remote_key"])
        assertEquals("Original Title", localManga.copy(memo = captured.memo).extractOriginalMetadata()?.title)
        coVerify(exactly = 0) { downloadManager.renameManga(any(), any()) }
    }

    @Test
    fun `updates metadata normally and renames downloads when no custom metadata marker is set`() = runBlocking {
        libraryPreferences.updateMangaTitles.set(true)
        val localManga = Manga.create().copy(
            id = 20L,
            source = 1L,
            title = "Old Title",
            favoriteAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )
        val remoteSManga = SManga.create().apply {
            title = "New Remote Title"
            author = "Remote Author"
            artist = "Remote Artist"
            description = "Remote Desc"
            genre = "Drama"
            status = SManga.COMPLETED
            update_strategy = UpdateStrategy.ALWAYS_UPDATE
        }
        val source = mockk<Source>()
        every { coverCache.deleteFromCache(any(), any()) } returns 0
        coEvery { chapterRepository.getChapterByMangaId(20L) } returns emptyList()
        coEvery { source.getMangaUpdate(any(), any(), any(), any()) } returns SMangaUpdate(remoteSManga, emptyList())
        coEvery { syncChaptersWithSource.await(any(), any(), any(), any()) } returns emptyList()
        coEvery { mangaRepository.getMangaById(20L) } returns localManga.copy(title = "New Remote Title")

        val updateSlot = slot<MangaRemoteUpdate>()
        coEvery { mangaRepository.updateRemote(capture(updateSlot)) } returns true

        val result = interactor.invoke(
            source = source,
            manga = localManga,
            fetchDetails = true,
            fetchChapters = false,
            manualFetch = false,
        )

        assertTrue(result.isSuccess)
        val captured = updateSlot.captured
        assertEquals("New Remote Title", captured.title)
        assertEquals("Remote Author", captured.author)
        assertEquals("Remote Artist", captured.artist)
        assertEquals("Remote Desc", captured.description)
        assertEquals(listOf("Drama"), captured.genre)
        coVerify(exactly = 1) { downloadManager.renameManga(localManga, "New Remote Title") }
    }
}
