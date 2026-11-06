package app.hikari.domain.manga.model

import app.hikari.domain.manga.model.Manga
import app.hikari.source.model.SManga

fun SManga.toDomainManga(sourceId: Long): Manga {
    return Manga.create().copy(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy,
        initialized = initialized,
        memo = memo,
        source = sourceId,
    )
}
