package app.hikari.domain.category.interactor

import app.hikari.domain.library.model.LibraryDisplayMode
import app.hikari.domain.library.service.LibraryPreferences
import dev.zacsweers.metro.Inject

@Inject
class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    fun await(display: LibraryDisplayMode) {
        preferences.displayMode.set(display)
    }
}
