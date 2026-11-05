package app.hikari.domain.extension.model

import app.hikari.extension.model.Extension

data class Extensions(
    val updates: List<Extension.Installed>,
    val loaded: List<Extension.Loaded>,
    val available: List<Extension.Available>,
    val notLoaded: List<Extension.NotLoaded>,
)
