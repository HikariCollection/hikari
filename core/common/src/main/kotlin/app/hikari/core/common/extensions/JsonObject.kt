package app.hikari.core.common.extensions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val JsonObjectEmpty = JsonObject(emptyMap())

val JsonObjectEmptyBytes = byteArrayOf(0x7B, 0x7D)

val JsonObject.Companion.EMPTY: JsonObject
    inline get() = JsonObjectEmpty

fun JsonObject.toByteArray(): ByteArray = toString().encodeToByteArray()

fun ByteArray.toJsonObject(): JsonObject =
    if (isEmpty()) {
        JsonObjectEmpty
    } else {
        runCatching { Json.decodeFromString<JsonObject>(decodeToString()) }
            .getOrDefault(JsonObjectEmpty)
    }
