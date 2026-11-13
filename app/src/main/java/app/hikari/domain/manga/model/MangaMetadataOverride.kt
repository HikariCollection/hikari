package app.hikari.domain.manga.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val CUSTOM_METADATA_MEMO_KEY = "hikari_custom_metadata"

private const val KEY_TITLE = "title"
private const val KEY_AUTHOR = "author"
private const val KEY_ARTIST = "artist"
private const val KEY_DESCRIPTION = "description"
private const val KEY_GENRE = "genre"

data class NormalizedCustomMetadata(
    val title: String,
    val author: String?,
    val artist: String?,
    val description: String?,
    val genres: List<String>?,
)

fun Manga.hasCustomMetadata(): Boolean {
    return memo.containsKey(CUSTOM_METADATA_MEMO_KEY)
}

fun Manga.memoWithCustomMetadataMarker(): JsonObject {
    val existingSnapshot = memo[CUSTOM_METADATA_MEMO_KEY] as? JsonObject
    val snapshot = existingSnapshot ?: buildJsonObject {
        put(KEY_TITLE, JsonPrimitive(title))
        put(KEY_AUTHOR, author?.let(::JsonPrimitive) ?: JsonNull)
        put(KEY_ARTIST, artist?.let(::JsonPrimitive) ?: JsonNull)
        put(KEY_DESCRIPTION, description?.let(::JsonPrimitive) ?: JsonNull)
        put(KEY_GENRE, genre?.let { list -> JsonArray(list.map(::JsonPrimitive)) } ?: JsonNull)
    }
    return buildJsonObject {
        memo.forEach { (key, value) ->
            put(key, value)
        }
        put(CUSTOM_METADATA_MEMO_KEY, snapshot)
    }
}

fun Manga.extractOriginalMetadata(): NormalizedCustomMetadata? {
    val snapshot = runCatching { memo[CUSTOM_METADATA_MEMO_KEY]?.jsonObject }.getOrNull() ?: return null
    val originalTitle = snapshot[KEY_TITLE]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: return null
    val originalAuthor = snapshot[KEY_AUTHOR]?.jsonPrimitive?.contentOrNull
    val originalArtist = snapshot[KEY_ARTIST]?.jsonPrimitive?.contentOrNull
    val originalDescription = snapshot[KEY_DESCRIPTION]?.jsonPrimitive?.contentOrNull
    val originalGenres = runCatching {
        snapshot[KEY_GENRE]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
    }.getOrNull()
    return NormalizedCustomMetadata(
        title = originalTitle,
        author = originalAuthor,
        artist = originalArtist,
        description = originalDescription,
        genres = originalGenres,
    )
}

fun Manga.memoWithoutCustomMetadataMarker(): JsonObject {
    if (!memo.containsKey(CUSTOM_METADATA_MEMO_KEY)) return memo
    return buildJsonObject {
        memo.forEach { (key, value) ->
            if (key != CUSTOM_METADATA_MEMO_KEY) {
                put(key, value)
            }
        }
    }
}

fun mergeCustomMetadataMarker(remoteMemo: JsonObject, localMemo: JsonObject): JsonObject {
    val marker = localMemo[CUSTOM_METADATA_MEMO_KEY] ?: JsonPrimitive(true)
    return buildJsonObject {
        remoteMemo.forEach { (key, value) ->
            put(key, value)
        }
        put(CUSTOM_METADATA_MEMO_KEY, marker)
    }
}

fun normalizeCustomMetadata(
    title: String,
    author: String?,
    artist: String?,
    description: String?,
    genresInput: String?,
): NormalizedCustomMetadata {
    val seenGenres = HashSet<String>()
    val genres = genresInput
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.filter { seenGenres.add(it.lowercase()) }
        ?.takeIf { it.isNotEmpty() }

    return NormalizedCustomMetadata(
        title = title.trim(),
        author = author?.trim()?.takeIf { it.isNotEmpty() },
        artist = artist?.trim()?.takeIf { it.isNotEmpty() },
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        genres = genres,
    )
}
