package app.hikari.domain.extension.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.extension.repository.ExtensionStoreRepository

@Inject
class UpdateExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke() {
        repository.refreshAll()
    }
}
