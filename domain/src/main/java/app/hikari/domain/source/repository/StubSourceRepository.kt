package app.hikari.domain.source.repository

import app.hikari.domain.source.model.StubSource
import kotlinx.coroutines.flow.Flow

interface StubSourceRepository {
    fun subscribeAll(): Flow<List<StubSource>>

    suspend fun getStubSource(id: Long): StubSource?

    suspend fun upsertStubSource(id: Long, lang: String, name: String)
}
