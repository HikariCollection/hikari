package app.hikari.data.backup.restore.restorers

import app.hikari.data.backup.models.BackupExtensionStore
import app.hikari.domain.extension.model.ExtensionStore
import app.hikari.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject

@Inject
class ExtensionStoreRestorer(
    private val extensionStoreRepository: ExtensionStoreRepository,
) {

    suspend operator fun invoke(
        backupStore: BackupExtensionStore,
    ) {
        extensionStoreRepository.upsert(
            ExtensionStore(
                indexUrl = backupStore.indexUrl,
                name = backupStore.name,
                badgeLabel = backupStore.badgeLabel ?: backupStore.name,
                signingKey = backupStore.signingKey,
                contact = ExtensionStore.Contact(
                    website = backupStore.contactWebsite,
                    discord = backupStore.contactDiscord,
                ),
                isLegacy = backupStore.isLegacy ?: true,
                extensionListUrl = backupStore.extensionListUrl,
            ),
        )
    }
}
