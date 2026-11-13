package app.hikari.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.hikari.data.Database
import app.hikari.data.subscribeToList
import app.hikari.data.subscribeToOne
import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class HiddenMangaRepositoryImpl(
    private val database: Database,
) : HiddenMangaRepository {

    override fun getHiddenUrlsBySource(sourceId: Long): Flow<Set<String>> {
        return database.hidden_mangaQueries
            .getHiddenUrlsBySource(sourceId)
            .subscribeToList()
            .map { it.toSet() }
    }

    override fun getAllHiddenKeysAsFlow(): Flow<Set<Pair<Long, String>>> {
        return database.hidden_mangaQueries
            .getAllHiddenKeys { sourceId, remoteUrl -> sourceId to remoteUrl }
            .subscribeToList()
            .map { it.toSet() }
    }

    override fun getAllHiddenMangaAsFlow(): Flow<List<HiddenManga>> {
        return database.hidden_mangaQueries
            .getAllHiddenManga(::mapHiddenManga)
            .subscribeToList()
    }

    override fun getCountAsFlow(): Flow<Long> {
        return database.hidden_mangaQueries
            .getCount()
            .subscribeToOne()
    }

    override suspend fun getAll(): List<HiddenManga> {
        return database.hidden_mangaQueries
            .getAllHiddenManga(::mapHiddenManga)
            .awaitAsList()
    }

    override suspend fun upsertAll(items: List<HiddenManga>) {
        if (items.isEmpty()) return
        database.transaction {
            items.forEach { item ->
                database.hidden_mangaQueries.insert(
                    sourceId = item.sourceId,
                    remoteUrl = item.url,
                    remoteTitle = item.title,
                    remoteCover = item.thumbnailUrl,
                    createdAt = item.createdAt,
                )
            }
        }
    }

    override suspend fun hide(mangaList: List<Manga>) {
        if (mangaList.isEmpty()) return
        val now = Clock.System.now()
        database.transaction {
            mangaList.forEach { manga ->
                database.hidden_mangaQueries.insert(
                    sourceId = manga.source,
                    remoteUrl = manga.url,
                    remoteTitle = manga.title,
                    remoteCover = manga.thumbnailUrl,
                    createdAt = now,
                )
            }
        }
    }

    override suspend fun unhide(mangaList: List<Manga>) {
        if (mangaList.isEmpty()) return
        database.transaction {
            mangaList.forEach { manga ->
                database.hidden_mangaQueries.delete(
                    sourceId = manga.source,
                    remoteUrl = manga.url,
                )
            }
        }
    }

    override suspend fun unhide(sourceId: Long, url: String) {
        database.hidden_mangaQueries.delete(
            sourceId = sourceId,
            remoteUrl = url,
        )
    }

    override suspend fun unhideByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        database.hidden_mangaQueries.deleteByIds(ids)
    }

    private fun mapHiddenManga(
        id: Long,
        sourceId: Long,
        remoteUrl: String,
        remoteTitle: String,
        remoteCover: String?,
        createdAt: Instant,
    ): HiddenManga {
        return HiddenManga(
            id = id,
            sourceId = sourceId,
            url = remoteUrl,
            title = remoteTitle,
            thumbnailUrl = remoteCover,
            createdAt = createdAt,
        )
    }
}
