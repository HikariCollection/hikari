package app.hikari.domain.history.interactor

import app.hikari.domain.history.model.HistoryExportItem
import app.hikari.domain.history.repository.HistoryRepository
import dev.zacsweers.metro.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Inject
class ExportHistory(
    private val repository: HistoryRepository,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun getItems(): List<HistoryExportItem> {
        return repository.getAllHistoryForExport()
    }

    fun toCsv(items: List<HistoryExportItem>): String {
        val builder = StringBuilder()
        builder.appendLine("Manga Title,Chapter Number,Chapter Name,Read At,Read Duration (Seconds)")

        for (item in items) {
            val title = escapeCsv(item.mangaTitle)
            val chapterNum = if (item.chapterNumber < 0) {
                ""
            } else if (item.chapterNumber % 1.0 == 0.0) {
                item.chapterNumber.toLong().toString()
            } else {
                item.chapterNumber.toString()
            }
            val chapterName = escapeCsv(item.chapterName)
            val readAt = item.readAt?.toString() ?: ""
            val duration = item.readDuration.coerceAtLeast(0).toString()

            builder.appendLine("$title,$chapterNum,$chapterName,$readAt,$duration")
        }

        return builder.toString()
    }

    fun toJson(items: List<HistoryExportItem>): String {
        return json.encodeToString(items)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
