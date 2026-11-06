package app.hikari.domain.extension.interactor

import app.hikari.domain.extension.model.ExtensionStore
import app.hikari.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend fun get(): List<ExtensionStore> = repository.getAll()

    fun subscribe(): Flow<List<ExtensionStore>> = repository.getAllAsFlow()
}
