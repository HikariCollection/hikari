package app.hikari.util.chapter

import app.hikari.data.download.DownloadCache
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.manga.model.Manga
import app.hikari.source.local.isLocal

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Chapter>.filterDownloaded(manga: Manga, downloadCache: DownloadCache): List<Chapter> {
    if (manga.isLocal()) return this

    return filter { downloadCache.isChapterDownloaded(it.name, it.scanlator, it.url, manga.title, manga.source) }
}
