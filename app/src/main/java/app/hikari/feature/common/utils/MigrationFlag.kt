package app.hikari.feature.common.utils

import app.hikari.domain.migration.models.MigrationFlag
import app.hikari.i18n.MR
import app.hikari.i18n.StringResource

fun MigrationFlag.getLabel(): StringResource {
    return when (this) {
        MigrationFlag.CHAPTER -> MR.strings.chapters
        MigrationFlag.CATEGORY -> MR.strings.categories
        MigrationFlag.CUSTOM_COVER -> MR.strings.custom_cover
        MigrationFlag.NOTES -> MR.strings.action_notes
        MigrationFlag.REMOVE_DOWNLOAD -> MR.strings.delete_downloaded
    }
}
