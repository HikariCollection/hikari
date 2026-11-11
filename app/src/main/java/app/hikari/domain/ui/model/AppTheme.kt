package app.hikari.domain.ui.model

import app.hikari.i18n.MR
import app.hikari.i18n.StringResource

enum class AppTheme(val titleRes: StringResource) {
    DEFAULT(MR.strings.label_default),
    MONET(MR.strings.theme_monet),
}
