package app.hikari.domain.history.model

import kotlin.time.Instant

data class ReadActivityEntry(
    val readAt: Instant,
    val readDuration: Long,
)
