package app.hikari.domain.ui.model

import app.hikari.i18n.MR
import app.hikari.i18n.StringResource

enum class TimeFormat(val titleRes: StringResource) {
    FOLLOW_LANGUAGE(MR.strings.follow_language),
    FOLLOW_SYSTEM(MR.strings.follow_system),
    HOURS_24(MR.strings.time_24_hours),
    HOURS_12(MR.strings.time_12_hours),
}
