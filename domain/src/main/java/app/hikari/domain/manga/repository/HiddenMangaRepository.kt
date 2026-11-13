package app.hikari.domain.manga.repository

import app.hikari.domain.manga.model.HiddenManga
import app.hikari.domain.manga.model.Manga
import kotlinx.coroutines.flow.Flow

interface HiddenMangaRepository {
    fun getHiddenUrlsBySource(sourceId: Long): Flow<Set<String>>
    fun getAllHiddenKeysAsFlow(): Flow<Set<Pair<Long, String>>>
    fun getAllHiddenMangaAsFlow(): Flow<List<HiddenManga>>
    fun getCountAsFlow(): Flow<Long>
    suspend fun getAll(): List<HiddenManga>
    suspend fun upsertAll(items: List<HiddenManga>)
    suspend fun hide(mangaList: List<Manga>)
    suspend fun unhide(mangaList: List<Manga>)
    suspend fun unhide(sourceId: Long, url: String)
    suspend fun unhideByIds(ids: Set<Long>)
}
