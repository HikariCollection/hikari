package app.hikari.feature.migration.list.search

import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.toDomainManga
import app.hikari.source.Source
import app.hikari.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartSourceSearchEngine(extraSearchParams: String?) : BaseSmartSearchEngine<SManga>(extraSearchParams) {

    override fun getTitle(result: SManga) = result.title

    suspend fun regularSearch(source: Source, title: String): Manga? {
        return regularSearch(makeSearchAction(source), title).let {
            it?.toDomainManga(source.id)
        }
    }

    suspend fun deepSearch(source: Source, title: String): Manga? {
        return deepSearch(makeSearchAction(source), title).let {
            it?.toDomainManga(source.id)
        }
    }

    // Sources aren't main-safe: their default implementations block on the network
    private fun makeSearchAction(source: Source): SearchAction<SManga> = { query ->
        withContext(Dispatchers.IO) { source.getSearchManga(1, query, source.getFilterList()).mangas }
    }
}
