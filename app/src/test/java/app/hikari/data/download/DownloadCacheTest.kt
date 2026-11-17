package app.hikari.data.download

import android.content.Context
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.service.SourceManager
import app.hikari.domain.storage.service.StorageManager
import app.hikari.source.Source
import com.hippo.unifile.UniFile
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DownloadCacheTest {

    @TempDir
    lateinit var tempDir: File

    private val context = mockk<Context>()
    private val sourceManager = mockk<SourceManager>()
    private val storageManager = mockk<StorageManager>()
    private val storageChanges = MutableSharedFlow<Unit>()
    private val provider = mockk<DownloadProvider>()
    private val testScope = TestScope()

    private lateinit var cache: DownloadCache

    @BeforeEach
    fun setUp() {
        every { context.cacheDir } returns tempDir
        every { storageManager.changes } returns storageChanges
        every { storageManager.getDownloadsDirectory() } returns UniFile.fromFile(tempDir)

        every { provider.getMangaDirName(any()) } answers { firstArg() }
        every { provider.getValidChapterDirNames(any(), any(), any()) } answers { listOf(firstArg()) }
        every { provider.getSourceDirName(any()) } answers { "source_${(firstArg() as Source).id}" }
        coEvery { provider.findSourceDir(any()) } answers {
            val source = firstArg<Source>()
            val dir = File(tempDir, "source_${source.id}").apply { mkdirs() }
            UniFile.fromFile(dir)
        }

        cache = DownloadCache(
            scope = testScope,
            context = context,
            provider = provider,
            sourceManager = sourceManager,
            storageManager = storageManager,
        )
    }

    private fun testManga(id: Long = 1L, sourceId: Long = 100L, title: String = "One Piece"): Manga =
        Manga.create().copy(id = id, source = sourceId, title = title)

    private fun testChapter(
        id: Long,
        mangaId: Long = 1L,
        name: String = "Chapter 1",
        url: String = "/chap/1",
    ): Chapter = Chapter.create().copy(id = id, mangaId = mangaId, name = name, url = url)

    @Test
    fun `addChapter registers chapter and getDownloadedChapterIds matches it`() = runTest {
        val source = mockk<Source>()
        every { source.id } returns 100L
        coEvery { sourceManager.get(100L) } returns source

        val sourceDir = File(tempDir, "source_100").apply { mkdirs() }
        val mangaDir = File(sourceDir, "One Piece").apply { mkdirs() }
        val mangaUniFile = UniFile.fromFile(mangaDir)!!

        every { storageManager.getDownloadsDirectory() } returns UniFile.fromFile(tempDir)

        val manga = testManga()
        val chapter1 = testChapter(101L, name = "Chapter 1", url = "/chap/1")
        val chapter2 = testChapter(102L, name = "Chapter 2", url = "/chap/2")

        val chapterDirName = "Chapter 1"

        cache.addChapter(chapterDirName, mangaUniFile, manga)

        cache.isChapterDownloaded("Chapter 1", null, "/chap/1", manga.title, manga.source) shouldBe true
        cache.isChapterDownloaded("Chapter 2", null, "/chap/2", manga.title, manga.source) shouldBe false

        val downloadedIds = cache.getDownloadedChapterIds(listOf(chapter1, chapter2), manga.title, manga.source)
        downloadedIds shouldContainExactly setOf(101L)

        cache.getDownloadCount(manga) shouldBe 1

        cache.removeChapter(chapter1, manga)
        cache.isChapterDownloaded("Chapter 1", null, "/chap/1", manga.title, manga.source) shouldBe false
        cache.getDownloadCount(manga) shouldBe 0
    }

    @Test
    fun `removeManga removes manga and its downloaded chapters from cache`() = runTest {
        val source = mockk<Source>()
        every { source.id } returns 100L
        coEvery { sourceManager.get(100L) } returns source

        val mangaDir = File(tempDir, "One Piece").apply { mkdirs() }
        val mangaUniFile = UniFile.fromFile(mangaDir)!!

        val manga = testManga()
        val chapterDirName = "Chapter 1"

        cache.addChapter(chapterDirName, mangaUniFile, manga)
        cache.getDownloadCount(manga) shouldBe 1

        cache.removeManga(manga)
        cache.getDownloadCount(manga) shouldBe 0
    }
}
