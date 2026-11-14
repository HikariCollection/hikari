package app.hikari.ui.browse.source.globalsearch

import app.hikari.core.common.preference.Preference
import app.hikari.domain.manga.interactor.GetManga
import app.hikari.domain.manga.interactor.NetworkToLocalManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.service.SourceManager
import app.hikari.domain.source.service.SourcePreferences
import app.hikari.extension.ExtensionManager
import app.hikari.source.Source
import app.hikari.source.model.MangasPage
import app.hikari.source.model.SManga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var pinnedSource: Source
    private lateinit var unpinnedSource: Source
    private lateinit var sourceManager: SourceManager
    private lateinit var sourcePreferences: SourcePreferences
    private lateinit var extensionManager: ExtensionManager
    private lateinit var networkToLocalManga: NetworkToLocalManga
    private lateinit var getManga: GetManga

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        pinnedSource = mockk<Source>(relaxed = true).apply {
            every { id } returns 1L
            every { name } returns "Pinned"
            every { lang } returns "en"
        }
        unpinnedSource = mockk<Source>(relaxed = true).apply {
            every { id } returns 2L
            every { name } returns "Unpinned"
            every { lang } returns "en"
        }

        sourceManager = mockk {
            coEvery { getAll() } returns listOf(pinnedSource, unpinnedSource)
        }
        extensionManager = mockk(relaxed = true)
        getManga = mockk(relaxed = true)
        networkToLocalManga = mockk {
            coEvery { this@mockk.invoke(any<List<Manga>>()) } answers { firstArg() }
        }

        val enabledLangsPref = mockk<Preference<Set<String>>> {
            every { get() } returns setOf("en")
        }
        val disabledSourcesPref = mockk<Preference<Set<String>>> {
            every { get() } returns emptySet()
        }
        val pinnedSourcesPref = mockk<Preference<Set<String>>> {
            every { get() } returns setOf("1")
        }
        val filterStatePref = mockk<Preference<Boolean>>(relaxed = true) {
            every { changes() } returns flowOf(false)
        }

        sourcePreferences = mockk {
            every { enabledLanguages } returns enabledLangsPref
            every { disabledSources } returns disabledSourcesPref
            every { pinnedSources } returns pinnedSourcesPref
            every { globalSearchFilterState } returns filterStatePref
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        initialQuery: String = "",
        initialExtensionFilter: String? = null,
    ): GlobalSearchViewModel {
        return GlobalSearchViewModel(
            initialQuery = initialQuery,
            initialExtensionFilter = initialExtensionFilter,
            sourcePreferences = sourcePreferences,
            sourceManager = sourceManager,
            extensionManager = extensionManager,
            networkToLocalManga = networkToLocalManga,
            getManga = getManga,
            getHiddenManga = mockk(relaxed = true) {
                every { subscribeAllKeys() } returns flowOf(emptySet())
            },
            preferences = sourcePreferences.also {
                every { it.showHiddenItems } returns mockk {
                    every { changes() } returns flowOf(false)
                }
            },
        )
    }

    private fun stubSearchResult(source: Source, query: String, title: String) {
        val smanga = SManga.create().apply {
            url = "/manga/$title"
            this.title = title
        }
        coEvery { source.getSearchManga(1, query, any()) } returns MangasPage(listOf(smanga), false)
    }

    private suspend fun awaitSearchSettled(viewModel: SearchViewModel, expectedCount: Int) {
        withTimeout(5_000) {
            viewModel.state.first { state ->
                state.total == expectedCount && state.progress == expectedCount
            }
        }
    }

    @Test
    fun `toggling All to PinnedOnly and back to All preserves unpinned results without re-fetching`() = runBlocking {
        stubSearchResult(pinnedSource, "naruto", "Naruto Pinned")
        stubSearchResult(unpinnedSource, "naruto", "Naruto Unpinned")

        val viewModel = createViewModel()
        viewModel.setSourceFilter(SourceFilter.All)
        viewModel.updateSearchQuery("naruto")
        viewModel.search()

        awaitSearchSettled(viewModel, expectedCount = 2)
        coVerify(exactly = 1) { pinnedSource.getSearchManga(1, "naruto", any()) }
        coVerify(exactly = 1) { unpinnedSource.getSearchManga(1, "naruto", any()) }

        viewModel.setSourceFilter(SourceFilter.PinnedOnly)
        awaitSearchSettled(viewModel, expectedCount = 1)
        assertTrue(pinnedSource in viewModel.state.value.items)
        assertFalse(unpinnedSource in viewModel.state.value.items)

        viewModel.setSourceFilter(SourceFilter.All)
        awaitSearchSettled(viewModel, expectedCount = 2)
        assertTrue(pinnedSource in viewModel.state.value.items)
        assertTrue(unpinnedSource in viewModel.state.value.items)
        coVerify(exactly = 1) { pinnedSource.getSearchManga(1, "naruto", any()) }
        coVerify(exactly = 1) { unpinnedSource.getSearchManga(1, "naruto", any()) }
    }

    @Test
    fun `toggling PinnedOnly to All fetches missing unpinned sources for active query`() = runBlocking {
        stubSearchResult(pinnedSource, "naruto", "Naruto Pinned")
        stubSearchResult(unpinnedSource, "naruto", "Naruto Unpinned")

        val viewModel = createViewModel(initialQuery = "naruto")
        awaitSearchSettled(viewModel, expectedCount = 1)
        coVerify(exactly = 1) { pinnedSource.getSearchManga(1, "naruto", any()) }
        coVerify(exactly = 0) { unpinnedSource.getSearchManga(any(), any(), any()) }

        viewModel.setSourceFilter(SourceFilter.All)
        awaitSearchSettled(viewModel, expectedCount = 2)
        coVerify(exactly = 1) { pinnedSource.getSearchManga(1, "naruto", any()) }
        coVerify(exactly = 1) { unpinnedSource.getSearchManga(1, "naruto", any()) }
    }

    @Test
    fun `toggling source filter with unsubmitted search bar draft applies to last searched query`() = runBlocking {
        stubSearchResult(pinnedSource, "naruto", "Naruto Pinned")
        stubSearchResult(unpinnedSource, "naruto", "Naruto Unpinned")
        stubSearchResult(pinnedSource, "bleach", "Bleach Pinned")
        stubSearchResult(unpinnedSource, "bleach", "Bleach Unpinned")

        val viewModel = createViewModel(initialQuery = "naruto")
        awaitSearchSettled(viewModel, expectedCount = 1)

        viewModel.updateSearchQuery("bleach")
        viewModel.setSourceFilter(SourceFilter.All)
        awaitSearchSettled(viewModel, expectedCount = 2)

        assertEquals("bleach", viewModel.state.value.searchQuery)
        coVerify(exactly = 1) { unpinnedSource.getSearchManga(1, "naruto", any()) }
        coVerify(exactly = 0) { pinnedSource.getSearchManga(any(), "bleach", any()) }
        coVerify(exactly = 0) { unpinnedSource.getSearchManga(any(), "bleach", any()) }
    }

    @Test
    fun `toggling source filter before submitting any search updates filter without searching`() = runBlocking {
        val viewModel = createViewModel(initialQuery = "")
        viewModel.updateSearchQuery("draft")
        viewModel.setSourceFilter(SourceFilter.All)

        assertEquals(SourceFilter.All, viewModel.state.value.sourceFilter)
        assertEquals(0, viewModel.state.value.total)
        coVerify(exactly = 0) { pinnedSource.getSearchManga(any(), any(), any()) }
        coVerify(exactly = 0) { unpinnedSource.getSearchManga(any(), any(), any()) }
    }

    @Test
    fun `submitting a new search query clears cached results from previous query`() = runBlocking {
        stubSearchResult(pinnedSource, "naruto", "Naruto Pinned")
        stubSearchResult(unpinnedSource, "naruto", "Naruto Unpinned")
        stubSearchResult(pinnedSource, "bleach", "Bleach Pinned")
        stubSearchResult(unpinnedSource, "bleach", "Bleach Unpinned")

        val viewModel = createViewModel()
        viewModel.setSourceFilter(SourceFilter.All)
        viewModel.updateSearchQuery("naruto")
        viewModel.search()
        awaitSearchSettled(viewModel, expectedCount = 2)

        viewModel.setSourceFilter(SourceFilter.PinnedOnly)
        awaitSearchSettled(viewModel, expectedCount = 1)

        viewModel.updateSearchQuery("bleach")
        viewModel.search()
        awaitSearchSettled(viewModel, expectedCount = 1)

        viewModel.setSourceFilter(SourceFilter.All)
        awaitSearchSettled(viewModel, expectedCount = 2)

        val unpinnedBleachResult = viewModel.state.value.items[unpinnedSource] as SearchItemResult.Success
        assertEquals("Bleach Unpinned", unpinnedBleachResult.result.single().title)
        coVerify(exactly = 1) { unpinnedSource.getSearchManga(1, "bleach", any()) }
    }
}
