package app.hikari.domain.chapter.repository

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.ChapterRemoteUpdate
import app.hikari.domain.chapter.model.ChapterSummary
import app.hikari.domain.chapter.model.ChapterUpdate
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {

    suspend fun update(chapterUpdate: ChapterUpdate)

    suspend fun updateAll(chapterUpdates: List<ChapterUpdate>)

    suspend fun updateFromRemote(
        removedIds: List<Long>,
        added: List<Chapter>,
        updated: List<ChapterRemoteUpdate>,
    ): List<Chapter>

    suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter>

    suspend fun getChaptersByMangaIds(mangaIds: List<Long>): Map<Long, List<Chapter>>

    suspend fun getChapterSummaryByMangaId(mangaId: Long): ChapterSummary

    suspend fun getScanlatorsByMangaId(mangaId: Long): List<String>

    fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>>

    suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Chapter>

    suspend fun getChapterById(id: Long): Chapter?

    suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyScanlatorFilter: Boolean = false): Flow<List<Chapter>>

    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter?
}
