package app.hikari.domain.history.interactor

import app.hikari.domain.history.model.HistoryExportItem
import app.hikari.domain.history.repository.HistoryRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Instant

@Execution(ExecutionMode.CONCURRENT)
class ExportHistoryTest {

    private val repository: HistoryRepository = mockk(relaxed = true)
    private val exportHistory = ExportHistory(repository)

    @Test
    fun `getItems delegates to repository`() = runTest {
        val expected = listOf(
            HistoryExportItem(
                mangaTitle = "One Piece",
                chapterName = "Romance Dawn",
                chapterNumber = 1.0,
                readAt = Instant.parse("2024-05-10T12:00:00Z"),
                readDuration = 120L,
            ),
        )
        coEvery { repository.getAllHistoryForExport() } returns expected

        exportHistory.getItems() shouldBe expected
    }

    @Test
    fun `toCsv produces valid header for empty list`() {
        val csv = exportHistory.toCsv(emptyList())
        csv.trim() shouldBe "Manga Title,Chapter Number,Chapter Name,Read At,Read Duration (Seconds)"
    }

    @Test
    fun `toCsv formats standard items and escapes commas quotes and newlines`() {
        val items = listOf(
            HistoryExportItem(
                mangaTitle = "One Piece",
                chapterName = "Chapter 1",
                chapterNumber = 1.0,
                readAt = Instant.parse("2024-05-10T12:00:00Z"),
                readDuration = 150L,
            ),
            HistoryExportItem(
                mangaTitle = "Spy, x Family",
                chapterName = "Mission \"Zero\"\nSpecial",
                chapterNumber = 0.5,
                readAt = null,
                readDuration = -5L,
            ),
            HistoryExportItem(
                mangaTitle = "Prologue Manga",
                chapterName = "Prologue",
                chapterNumber = -1.0,
                readAt = Instant.parse("2024-05-11T08:30:00Z"),
                readDuration = 60L,
            ),
        )

        val csv = exportHistory.toCsv(items)

        csv shouldContain "Manga Title,Chapter Number,Chapter Name,Read At,Read Duration (Seconds)"
        csv shouldContain "One Piece,1,Chapter 1,2024-05-10T12:00:00Z,150"
        csv shouldContain "\"Spy, x Family\",0.5,\"Mission \"\"Zero\"\"\nSpecial\",,0"
        csv shouldContain "Prologue Manga,,Prologue,2024-05-11T08:30:00Z,60"
    }

    @Test
    fun `toJson serializes items to valid JSON array`() {
        val items = listOf(
            HistoryExportItem(
                mangaTitle = "Berserk",
                chapterName = "The Black Swordsman",
                chapterNumber = 1.0,
                readAt = Instant.parse("2024-01-01T00:00:00Z"),
                readDuration = 300L,
            ),
        )

        val jsonString = exportHistory.toJson(items)
        jsonString shouldContain "\"mangaTitle\": \"Berserk\""
        jsonString shouldContain "\"chapterNumber\": 1.0"
        jsonString shouldContain "\"readDuration\": 300"

        val parsed = Json.decodeFromString<List<HistoryExportItem>>(jsonString)
        parsed shouldBe items
    }
}
