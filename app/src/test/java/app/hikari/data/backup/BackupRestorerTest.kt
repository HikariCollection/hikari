package app.hikari.data.backup

import android.content.Context
import android.net.Uri
import app.hikari.data.backup.models.Backup
import app.hikari.data.backup.restore.BackupRestorer
import app.hikari.data.backup.restore.RestoreOptions
import app.hikari.data.backup.restore.restorers.CategoriesRestorer
import app.hikari.data.backup.restore.restorers.ExtensionStoreRestorer
import app.hikari.data.backup.restore.restorers.HiddenMangaRestorer
import app.hikari.data.backup.restore.restorers.MangaRestorer
import app.hikari.data.backup.restore.restorers.PreferenceRestorer
import app.hikari.data.download.DownloadCache
import app.hikari.domain.source.repository.StubSourceRepository
import app.hikari.domain.source.service.SourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupRestorerTest {

    private fun createRestorer(
        notifier: BackupNotifier = mockk(relaxed = true),
    ): BackupRestorer {
        val backupDecoder = mockk<BackupDecoder>()
        every { backupDecoder.decodeMetadata(any()) } returns BackupDecoder.Metadata(0, Backup())
        every { backupDecoder.decodeEntries(any()) } returns emptyFlow()

        return BackupRestorer(
            notifier = notifier,
            isSync = false,
            context = mockk<Context>(relaxed = true),
            downloadCache = mockk<DownloadCache>(relaxed = true),
            categoriesRestorer = mockk<CategoriesRestorer>(relaxed = true),
            preferenceRestorer = mockk<PreferenceRestorer>(relaxed = true),
            extensionStoreRestorer = mockk<ExtensionStoreRestorer>(relaxed = true),
            hiddenMangaRestorer = mockk<HiddenMangaRestorer>(relaxed = true),
            mangaRestorer = mockk<MangaRestorer>(relaxed = true),
            backupDecoder = backupDecoder,
            sourceManager = mockk<SourceManager>(relaxed = true),
            stubSourceRepository = mockk<StubSourceRepository>(relaxed = true),
        )
    }

    @Test
    fun `triggers library update after restore complete when libraryEntries and updateLibraryAfterRestore are true`() =
        runBlocking {
            val events = mutableListOf<String>()
            val notifier = mockk<BackupNotifier>(relaxed = true)
            every { notifier.showRestoreComplete(any(), any(), any(), any(), any()) } answers {
                events.add("complete")
            }

            val restorer = createRestorer(notifier).apply {
                startLibraryUpdate = {
                    events.add("update")
                    true
                }
            }

            restorer.restore(
                uri = mockk<Uri>(relaxed = true),
                options = RestoreOptions(
                    libraryEntries = true,
                    categories = false,
                    appSettings = false,
                    extensionStores = false,
                    sourceSettings = false,
                    hiddenManga = false,
                    updateLibraryAfterRestore = true,
                ),
            )

            verify(exactly = 1) { notifier.showRestoreComplete(any(), any(), any(), any(), any()) }
            assertEquals(listOf("complete", "update"), events)
        }

    @Test
    fun `does not trigger library update when updateLibraryAfterRestore is false`() = runBlocking {
        var updateStarted = false
        val restorer = createRestorer().apply {
            startLibraryUpdate = {
                updateStarted = true
                true
            }
        }

        restorer.restore(
            uri = mockk<Uri>(relaxed = true),
            options = RestoreOptions(
                libraryEntries = true,
                updateLibraryAfterRestore = false,
            ),
        )

        assertEquals(false, updateStarted)
    }

    @Test
    fun `does not trigger library update when libraryEntries is false even if updateLibraryAfterRestore is true`() =
        runBlocking {
            var updateStarted = false
            val restorer = createRestorer().apply {
                startLibraryUpdate = {
                    updateStarted = true
                    true
                }
            }

            restorer.restore(
                uri = mockk<Uri>(relaxed = true),
                options = RestoreOptions(
                    libraryEntries = false,
                    categories = true,
                    updateLibraryAfterRestore = true,
                ),
            )

            assertEquals(false, updateStarted)
        }
}
