package app.hikari.domain.manga.interactor

import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.repository.HiddenMangaRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetHiddenManga(
    private val repository: HiddenMangaRepository,
) {
    fun subscribeUrlsBySource(sourceId: Long): Flow<Set<String>> = repository.getHiddenUrlsBySource(sourceId)

    fun subscribeAllKeys(): Flow<Set<Pair<Long, String>>> = repository.getAllHiddenKeysAsFlow()

    fun subscribeAll(): Flow<List<HiddenManga>> = repository.getAllHiddenMangaAsFlow()

    fun subscribeCount(): Flow<Long> = repository.getCountAsFlow()

    suspend fun awaitAll(): List<HiddenManga> = repository.getAll()
}
