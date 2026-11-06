package app.hikari.domain.source.service

import app.hikari.domain.source.model.StubSource
import app.hikari.source.Source
import app.hikari.source.online.HttpSource
import kotlinx.coroutines.flow.Flow

interface SourceManager {

    val sources: Flow<List<Source>>

    suspend fun get(sourceKey: Long): Source?

    suspend fun getOrStub(sourceKey: Long): Source

    suspend fun getAll(): List<Source>

    suspend fun getOnlineSources(): List<HttpSource>

    suspend fun getStubSources(): List<StubSource>
}
