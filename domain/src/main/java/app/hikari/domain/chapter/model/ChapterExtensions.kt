package app.hikari.domain.chapter.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val BOOKMARK_NOTE_KEY = "bookmark_note"

val Chapter.bookmarkNote: String?
    get() = memo[BOOKMARK_NOTE_KEY]?.jsonPrimitive?.contentOrNull?.ifBlank { null }

fun Chapter.withBookmarkNote(note: String?): JsonObject {
    val mutable = memo.toMutableMap()
    val cleanNote = note?.trim()
    if (cleanNote.isNullOrEmpty()) {
        mutable.remove(BOOKMARK_NOTE_KEY)
    } else {
        mutable[BOOKMARK_NOTE_KEY] = JsonPrimitive(cleanNote)
    }
    return JsonObject(mutable)
}
