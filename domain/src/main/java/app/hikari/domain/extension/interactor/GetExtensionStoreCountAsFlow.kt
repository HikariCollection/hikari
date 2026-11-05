package app.hikari.domain.extension.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.extension.repository.ExtensionStoreRepository

@Inject
class GetExtensionStoreCountAsFlow(
    private val repository: ExtensionStoreRepository,
) {
    operator fun invoke() = repository.getCountAsFlow()
}
