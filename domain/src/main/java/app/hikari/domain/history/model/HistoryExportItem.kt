package app.hikari.domain.history.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class HistoryExportItem(
    val mangaTitle: String,
    val chapterName: String,
    val chapterNumber: Double,
    val readAt: Instant?,
    val readDuration: Long,
)
