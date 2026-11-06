package app.hikari.data.source

import app.hikari.data.Database
import app.hikari.data.subscribeToList
import app.hikari.domain.manga.interactor.NetworkToLocalManga
import app.hikari.domain.source.model.SourceWithCount
import app.hikari.domain.source.model.StubSource
import app.hikari.domain.source.repository.SourcePagingSource
import app.hikari.domain.source.repository.SourceRepository
import app.hikari.domain.source.service.SourceManager
import app.hikari.source.Source
import app.hikari.source.model.FilterList
import app.hikari.source.online.HttpSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import app.hikari.domain.source.model.Source as DomainSource

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val database: Database,
    private val networkToLocalManga: NetworkToLocalManga,
) : SourceRepository {

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources
                .filterIsInstance<HttpSource>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        val sourceIdWithFavoriteCountFlow = database.mangaQueries
            .getSourceIdWithFavoriteCount()
            .subscribeToList()
        return combine(sourceIdWithFavoriteCountFlow, sourceManager.sources) { sourceIdWithFavoriteCount, _ ->
            sourceIdWithFavoriteCount
        }
            .map {
                it.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubSource,
                    )
                    domainSource to count
                }
            }
    }

    override fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>> {
        return database.mangaQueries
            .getSourceIdsWithNonLibraryManga()
            .subscribeToList()
            .map { sourceId ->
                sourceId.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubSource,
                    )
                    SourceWithCount(domainSource, count)
                }
            }
    }

    override fun search(
        sourceId: Long,
        query: String,
        filterList: FilterList,
    ): SourcePagingSource {
        return SourceSearchPagingSource(
            { sourceManager.getOrStub(sourceId) },
            query,
            filterList,
            networkToLocalManga,
        )
    }

    override fun getPopular(sourceId: Long): SourcePagingSource {
        return SourcePopularPagingSource({ sourceManager.getOrStub(sourceId) }, networkToLocalManga)
    }

    override fun getLatest(sourceId: Long): SourcePagingSource {
        return SourceLatestPagingSource({ sourceManager.getOrStub(sourceId) }, networkToLocalManga)
    }

    private fun mapSourceToDomainSource(source: Source): DomainSource = DomainSource(
        id = source.id,
        lang = source.lang,
        name = source.name,
        supportsLatest = false,
        isStub = false,
    )
}
