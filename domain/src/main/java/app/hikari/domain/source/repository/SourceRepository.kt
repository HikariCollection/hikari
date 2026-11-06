package app.hikari.domain.source.repository

import androidx.paging.PagingSource
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.model.Source
import app.hikari.domain.source.model.SourceWithCount
import app.hikari.source.model.FilterList
import kotlinx.coroutines.flow.Flow

typealias SourcePagingSource = PagingSource<Long, Manga>

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    fun search(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource

    fun getPopular(sourceId: Long): SourcePagingSource

    fun getLatest(sourceId: Long): SourcePagingSource
}
