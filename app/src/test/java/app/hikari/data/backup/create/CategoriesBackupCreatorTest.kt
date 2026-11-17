package app.hikari.data.backup.create

import app.hikari.data.backup.create.creators.CategoriesBackupCreator
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CategoriesBackupCreatorTest {

    private val getCategories = mockk<GetCategories>()
    private val creator = CategoriesBackupCreator(getCategories)

    @Test
    fun `excludes system categories and maps user categories`() = runTest {
        val categories = listOf(
            Category(id = Category.UNCATEGORIZED_ID, name = "Default", order = 0, flags = 0),
            Category(id = 1L, name = "Reading", order = 1, flags = 2),
            Category(id = 2L, name = "Completed", order = 2, flags = 4),
        )
        coEvery { getCategories.await() } returns categories

        val backupCategories = creator()

        backupCategories shouldHaveSize 2
        backupCategories[0].id shouldBe 1L
        backupCategories[0].name shouldBe "Reading"
        backupCategories[0].order shouldBe 1L
        backupCategories[0].flags shouldBe 2L

        backupCategories[1].id shouldBe 2L
        backupCategories[1].name shouldBe "Completed"
        backupCategories[1].order shouldBe 2L
        backupCategories[1].flags shouldBe 4L
    }
}
