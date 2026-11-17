package app.hikari.data.download

import android.content.Context
import android.content.SharedPreferences
import app.hikari.data.download.model.Download
import app.hikari.domain.chapter.interactor.GetChapter
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.manga.interactor.GetManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.online.HttpSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DownloadStoreTest {

    private val context = mockk<Context>(relaxed = true)
    private val sourceManager = mockk<SourceManager>()
    private val json = Json
    private val getManga = mockk<GetManga>()
    private val getChapter = mockk<GetChapter>()
    private val fakePreferences = FakeSharedPreferences()

    private lateinit var store: DownloadStore

    @BeforeEach
    fun setUp() {
        store = DownloadStore(
            context = context,
            sourceManager = sourceManager,
            json = json,
            getManga = getManga,
            getChapter = getChapter,
        ).apply {
            sharedPreferences = fakePreferences
        }
    }

    private fun testManga(id: Long, sourceId: Long = 100L): Manga =
        Manga.create().copy(id = id, source = sourceId, title = "Manga $id")

    private fun testChapter(id: Long, mangaId: Long): Chapter =
        Chapter.create().copy(id = id, mangaId = mangaId, name = "Chapter $id", url = "/chap/$id")

    @Test
    fun `addAll persists downloads and remove deletes by chapter ID`() = runTest {
        val source = mockk<HttpSource>()
        val manga = testManga(1L)
        val chapter1 = testChapter(101L, 1L)
        val chapter2 = testChapter(102L, 1L)

        val dl1 = Download(source, manga, chapter1)
        val dl2 = Download(source, manga, chapter2)

        store.addAll(listOf(dl1, dl2))
        fakePreferences.map.keys shouldBe setOf("101", "102")

        store.remove(dl1)
        fakePreferences.map.keys shouldBe setOf("102")

        store.clear()
        fakePreferences.map.isEmpty() shouldBe true
    }

    @Test
    fun `restore restores downloads in order and clears store`() = runTest {
        val source = mockk<HttpSource>()
        coEvery { sourceManager.get(100L) } returns source

        val manga = testManga(1L, sourceId = 100L)
        val chapter1 = testChapter(101L, 1L)
        val chapter2 = testChapter(102L, 1L)

        coEvery { getManga.await(1L) } returns manga
        coEvery { getChapter.await(101L) } returns chapter1
        coEvery { getChapter.await(102L) } returns chapter2

        val dl1 = Download(source, manga, chapter1)
        val dl2 = Download(source, manga, chapter2)

        store.addAll(listOf(dl1, dl2))

        val restored = store.restore()

        restored shouldHaveSize 2
        restored[0].chapter.id shouldBe 101L
        restored[1].chapter.id shouldBe 102L

        // Store should be cleared after restore
        fakePreferences.map.isEmpty() shouldBe true
    }

    @Test
    fun `restore skips mismatched chapter where chapter mangaId does not match manga id`() = runTest {
        val source = mockk<HttpSource>()
        coEvery { sourceManager.get(100L) } returns source

        val manga1 = testManga(1L, sourceId = 100L)
        // Chapter 200 points to manga 999 instead of manga 1
        val corruptedChapter = testChapter(200L, mangaId = 999L)

        coEvery { getManga.await(1L) } returns manga1
        coEvery { getChapter.await(200L) } returns corruptedChapter

        val dl = Download(source, manga1, corruptedChapter)
        store.addAll(listOf(dl))

        val restored = store.restore()
        restored.shouldBeEmpty()
    }

    @Test
    fun `restore ignores malformed JSON payloads and missing entities`() = runTest {
        fakePreferences.map["101"] = "{ invalid json payload"
        fakePreferences.map["102"] = """{"mangaId":1,"chapterId":102,"order":1}"""

        val source = mockk<HttpSource>()
        coEvery { sourceManager.get(100L) } returns source
        coEvery { getManga.await(1L) } returns null // Manga missing

        val restored = store.restore()
        restored.shouldBeEmpty()
        fakePreferences.map.isEmpty() shouldBe true
    }

    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? Set<*>)?.mapNotNull { it?.toString() }?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = key in map
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {}
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {}

        inner class Editor : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) temp[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removed.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) map.clear()
                removed.forEach { map.remove(it) }
                map.putAll(temp)
            }
        }
    }
}
