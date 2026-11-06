package app.hikari.domain.track.model

import app.hikari.i18n.MR
import app.hikari.i18n.StringResource

enum class AutoTrackState(val titleRes: StringResource) {
    ALWAYS(MR.strings.lock_always),
    ASK(MR.strings.default_category_summary),
    NEVER(MR.strings.lock_never),
}
