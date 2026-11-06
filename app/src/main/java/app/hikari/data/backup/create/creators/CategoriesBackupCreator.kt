package app.hikari.data.backup.create.creators

import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.models.backupCategoryMapper
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category
import dev.zacsweers.metro.Inject

@Inject
class CategoriesBackupCreator(
    private val getCategories: GetCategories,
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return getCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }
}
