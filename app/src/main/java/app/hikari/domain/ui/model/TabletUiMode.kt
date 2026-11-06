package app.hikari.domain.ui.model

import app.hikari.i18n.MR
import app.hikari.i18n.StringResource

enum class TabletUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    ALWAYS(MR.strings.lock_always),
    LANDSCAPE(MR.strings.landscape),
    NEVER(MR.strings.lock_never),
}
