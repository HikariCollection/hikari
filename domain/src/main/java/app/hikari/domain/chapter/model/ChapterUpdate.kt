package app.hikari.domain.chapter.model

import app.hikari.domain.common.PartialUpdate
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

class ChapterUpdate(val id: Long, block: ChapterUpdate.() -> Unit) : PartialUpdate() {
    var read: Boolean? by field(null)
    var bookmark: Boolean? by field(null)
    var lastPageRead: Int? by field(null)
    var dateFetch: Instant? by field(null)
    var memo: JsonObject? by field(null)

    init {
        block()
    }
}
