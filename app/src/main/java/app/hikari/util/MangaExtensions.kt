package app.hikari.util

import app.hikari.domain.manga.interactor.UpdateManga
import app.hikari.domain.manga.model.toSManga
import app.hikari.data.cache.CoverCache
import app.hikari.domain.manga.model.Manga
import app.hikari.source.local.image.LocalCoverManager
import app.hikari.source.local.isLocal
import java.io.InputStream

fun Manga.removeCovers(coverCache: CoverCache) {
    if (isLocal()) return
    coverCache.deleteFromCache(this, true)
}

suspend fun Manga.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateManga: UpdateManga,
    coverCache: CoverCache,
) {
    if (isLocal()) {
        coverManager.update(toSManga(), stream)
        updateManga.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateManga.awaitUpdateCoverLastModified(id)
    }
}
