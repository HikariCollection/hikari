package app.hikari.data.backup.restore

import app.hikari.core.common.preference.InMemoryPreferenceStore
import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.restore.restorers.CategoriesRestorer
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category
import app.hikari.domain.category.model.NewCategory
import app.hikari.domain.category.repository.CategoryRepository
import app.hikari.domain.library.service.LibraryPreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CategoriesRestorerTest {

    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val getCategories = mockk<GetCategories>()
    private val preferenceStore = InMemoryPreferenceStore()
    private val libraryPreferences = LibraryPreferences(preferenceStore)

    private lateinit var restorer: CategoriesRestorer

    @BeforeEach
    fun setUp() {
        restorer = CategoriesRestorer(
            categoryRepository = categoryRepository,
            getCategories = getCategories,
            libraryPreferences = libraryPreferences,
        )
    }

    @Test
    fun `empty backup categories does nothing`() = runTest {
        restorer(emptyList())

        coVerify(exactly = 0) { getCategories.await() }
        coVerify(exactly = 0) { categoryRepository.insertAll(any()) }
    }

    @Test
    fun `inserts new categories skipping existing DB categories`() = runTest {
        val dbCategories = listOf(
            Category(id = 1L, name = "Reading", order = 1, flags = 0),
        )
        coEvery { getCategories.await() } returns dbCategories

        val backupCategories = listOf(
            BackupCategory(name = "Reading", order = 1, flags = 0),
            BackupCategory(name = "Completed", order = 2, flags = 0),
            BackupCategory(name = "Plan to Read", order = 3, flags = 0),
        )

        val insertedSlot = slot<List<NewCategory>>()
        coEvery { categoryRepository.insertAll(capture(insertedSlot)) } returns Unit

        restorer(backupCategories)

        insertedSlot.isCaptured shouldBe true
        val inserted = insertedSlot.captured
        inserted.map { it.name } shouldBe listOf("Completed", "Plan to Read")
        libraryPreferences.categorizedDisplaySettings.get() shouldBe false
    }

    @Test
    fun `deduplicates duplicate category names in backup`() = runTest {
        coEvery { getCategories.await() } returns emptyList()

        val backupCategories = listOf(
            BackupCategory(name = "Favorites", order = 1, flags = 1),
            BackupCategory(name = "Favorites", order = 2, flags = 1),
            BackupCategory(name = "Action", order = 3, flags = 2),
        )

        val insertedSlot = slot<List<NewCategory>>()
        coEvery { categoryRepository.insertAll(capture(insertedSlot)) } returns Unit

        restorer(backupCategories)

        insertedSlot.isCaptured shouldBe true
        val inserted = insertedSlot.captured
        inserted.map { it.name } shouldBe listOf("Favorites", "Action")
        libraryPreferences.categorizedDisplaySettings.get() shouldBe true
    }
}
