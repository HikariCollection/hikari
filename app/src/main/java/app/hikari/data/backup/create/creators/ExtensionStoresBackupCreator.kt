package app.hikari.data.backup.create.creators

import dev.zacsweers.metro.Inject
import app.hikari.data.backup.models.BackupExtensionStore
import app.hikari.data.backup.models.backupExtensionStoreMapper
import app.hikari.domain.extension.interactor.GetExtensionStores

@Inject
class ExtensionStoresBackupCreator(
    private val getExtensionStores: GetExtensionStores,
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupExtensionStoreMapper)
    }
}
