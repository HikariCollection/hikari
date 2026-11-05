package app.hikari.data.backup.create.creators

import dev.zacsweers.metro.Inject
import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.models.backupCategoryMapper
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category

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
