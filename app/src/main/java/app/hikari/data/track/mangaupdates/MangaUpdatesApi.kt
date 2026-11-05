package app.hikari.data.track.mangaupdates

import app.hikari.data.database.models.Track
import app.hikari.data.track.mangaupdates.MangaUpdates.Companion.READING_LIST
import app.hikari.data.track.mangaupdates.MangaUpdates.Companion.WISH_LIST
import app.hikari.data.track.mangaupdates.dto.MUContext
import app.hikari.data.track.mangaupdates.dto.MUCurrentUser
import app.hikari.data.track.mangaupdates.dto.MUListItem
import app.hikari.data.track.mangaupdates.dto.MULoginResponse
import app.hikari.data.track.mangaupdates.dto.MURating
import app.hikari.data.track.mangaupdates.dto.MURecord
import app.hikari.data.track.mangaupdates.dto.MUSearchResult
import app.hikari.network.DELETE
import app.hikari.network.GET
import app.hikari.network.POST
import app.hikari.network.PUT
import app.hikari.network.await
import app.hikari.network.awaitSuccess
import app.hikari.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
import app.hikari.domain.track.model.Track as DomainTrack

class MangaUpdatesApi(
    private val client: OkHttpClient,
    interceptor: MangaUpdatesInterceptor,
) {
    private val json: Json by injectLazy()

    private val authClient by lazy {
        client.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }

    suspend fun getSeriesListItem(track: Track): Pair<MUListItem, MURating?> {
        val listItem = withContext(Dispatchers.IO) {
            with(json) {
                authClient.newCall(GET("$BASE_URL/v1/lists/series/${track.remote_id}"))
                    .awaitSuccess()
                    .parseAs<MUListItem>()
            }
        }

        val rating = getSeriesRating(track)

        return listItem to rating
    }

    suspend fun addSeriesToList(track: Track, hasReadChapters: Boolean) {
        val status = if (hasReadChapters) READING_LIST else WISH_LIST
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remote_id)
                }
                put("list_id", status)
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
            .let {
                if (it.code == 200) {
                    track.status = status
                    track.last_chapter_read = 1.0
                }
            }
    }

    suspend fun updateSeriesListItem(track: Track) {
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remote_id)
                }
                put("list_id", track.status)
                putJsonObject("status") {
                    put("chapter", track.last_chapter_read.toInt())
                }
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/update",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess().close()

        updateSeriesRating(track)
    }

    suspend fun deleteSeriesFromList(track: DomainTrack) {
        val body = buildJsonArray {
            add(track.remoteId)
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/delete",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess().close()
    }

    private suspend fun getSeriesRating(track: Track): MURating? {
        return try {
            withContext(Dispatchers.IO) {
                with(json) {
                    authClient.newCall(GET("$BASE_URL/v1/series/${track.remote_id}/rating"))
                        .awaitSuccess()
                        .parseAs<MURating>()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun updateSeriesRating(track: Track) {
        if (track.score < 0.0) return
        if (track.score != 0.0) {
            val body = buildJsonObject {
                put("rating", track.score)
            }
            authClient.newCall(
                PUT(
                    url = "$BASE_URL/v1/series/${track.remote_id}/rating",
                    body = body.toString().toRequestBody(CONTENT_TYPE),
                ),
            )
                .awaitSuccess().close()
        } else {
            authClient.newCall(
                DELETE(url = "$BASE_URL/v1/series/${track.remote_id}/rating"),
            )
                .awaitSuccess().close()
        }
    }

    suspend fun search(query: String): List<MURecord> {
        val body = buildJsonObject {
            put("search", query)
            put(
                "filter_types",
                buildJsonArray {
                    add("drama cd")
                    add("novel")
                },
            )
        }

        return withContext(Dispatchers.IO) {
            with(json) {
                client.newCall(
                    POST(
                        url = "$BASE_URL/v1/series/search",
                        body = body.toString().toRequestBody(CONTENT_TYPE),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<MUSearchResult>()
                    .results
                    .map { it.record }
            }
        }
    }

    suspend fun getSeriesDetails(id: Long): MURecord? {
        return withContext(Dispatchers.IO) {
            val url = "$BASE_URL/v1/series/$id"

            with(json) {
                val response = client.newCall(GET(url))
                    .await()

                if (response.code == 404) {
                    null
                } else {
                    response.parseAs<MURecord>()
                }
            }
        }
    }

    suspend fun authenticate(username: String, password: String): MUContext {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }
        return withContext(Dispatchers.IO) {
            with(json) {
                client.newCall(
                    PUT(
                        url = "$BASE_URL/v1/account/login",
                        body = body.toString().toRequestBody(CONTENT_TYPE),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<MULoginResponse>()
                    .context
            }
        }
    }

    suspend fun getCurrentUser(): MUCurrentUser {
        return withContext(Dispatchers.IO) {
            with(json) {
                authClient.newCall(GET("$BASE_URL/v1/account/profile"))
                    .awaitSuccess()
                    .parseAs<MUCurrentUser>()
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://api.mangaupdates.com"

        private val CONTENT_TYPE = "application/json".toMediaType()
    }
}
