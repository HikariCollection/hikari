package app.hikari.domain.category.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.library.model.LibraryDisplayMode
import app.hikari.domain.library.service.LibraryPreferences

@Inject
class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    fun await(display: LibraryDisplayMode) {
        preferences.displayMode.set(display)
    }
}
