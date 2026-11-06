package app.hikari.domain.category.interactor

import app.hikari.domain.category.repository.CategoryRepository
import app.hikari.domain.library.model.plus
import app.hikari.domain.library.service.LibraryPreferences
import dev.zacsweers.metro.Inject

@Inject
class ResetCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await() {
        val sort = preferences.sortingMode.get()
        categoryRepository.updateAllFlags(sort.type + sort.direction)
    }
}
