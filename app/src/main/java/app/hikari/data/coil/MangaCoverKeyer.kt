package app.hikari.data.coil

import coil3.key.Keyer
import coil3.request.Options
import app.hikari.domain.manga.model.hasCustomCover
import app.hikari.data.cache.CoverCache
import app.hikari.domain.manga.model.MangaCover
import app.hikari.domain.manga.model.Manga as DomainManga

class MangaKeyer : Keyer<DomainManga> {
    override fun key(data: DomainManga, options: Options): String {
        return if (data.favorite && data.hasCustomCover()) {
            "${data.id};${data.coverLastModified}"
        } else {
            "${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class MangaCoverKeyer(
    private val coverCache: CoverCache,
) : Keyer<MangaCover> {
    override fun key(data: MangaCover, options: Options): String {
        return if (data.isMangaFavorite && coverCache.hasCustomCover(data.mangaId)) {
            "${data.mangaId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
