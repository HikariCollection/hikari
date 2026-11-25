package app.hikari.domain.chapter.model

import kotlin.time.Instant

data class BookmarkedChapterEntry(
    val chapter: Chapter,
    val lastReadAt: Instant?,
) {
    val activityAt: Instant
        get() = lastReadAt ?: chapter.dateFetch
}
