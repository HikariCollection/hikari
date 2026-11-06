package app.hikari.data.backup.create.creators

import app.hikari.data.backup.models.BackupExtensionStore
import app.hikari.data.backup.models.backupExtensionStoreMapper
import app.hikari.domain.extension.interactor.GetExtensionStores
import dev.zacsweers.metro.Inject

@Inject
class ExtensionStoresBackupCreator(
    private val getExtensionStores: GetExtensionStores,
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupExtensionStoreMapper)
    }
}
