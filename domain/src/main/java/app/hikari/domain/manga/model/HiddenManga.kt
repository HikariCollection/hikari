package app.hikari.domain.manga.model

import kotlin.time.Instant

data class HiddenManga(
    val id: Long,
    val sourceId: Long,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val createdAt: Instant,
)
