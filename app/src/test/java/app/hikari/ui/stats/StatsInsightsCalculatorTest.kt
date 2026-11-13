package app.hikari.ui.stats

import app.hikari.domain.history.model.ReadActivityEntry
import app.hikari.domain.library.model.LibraryManga
import app.hikari.domain.manga.model.Manga
import app.hikari.source.model.UpdateStrategy
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsInsightsCalculatorTest {

    private val timeZone = TimeZone.UTC
    private val today = LocalDate(2026, 11, 13)

    private fun activityOn(date: LocalDate, hour: Int = 12, durationMs: Long): ReadActivityEntry {
        return ReadActivityEntry(
            readAt = date.atTime(hour, 0).toInstant(timeZone),
            readDuration = durationMs,
        )
    }

    private fun testLibraryManga(
        id: Long,
        source: Long,
        genres: List<String>? = null,
    ): LibraryManga {
        return LibraryManga(
            manga = Manga(
                id = id,
                source = source,
                lastUpdate = null,
                favoriteAt = null,
                viewerFlags = 0L,
                chapterFlags = 0L,
                coverLastModified = null,
                url = "/manga/$id",
                title = "Manga $id",
                artist = null,
                author = null,
                description = null,
                genre = genres,
                status = 0L,
                thumbnailUrl = null,
                updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
                initialized = false,
                notes = "",
                memo = JsonObject(emptyMap()),
            ),
            categories = emptyList(),
            totalChapters = 10L,
            readCount = 0L,
            bookmarkCount = 0L,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = null,
        )
    }

    @Test
    fun `empty history produces zero streaks and 30 zeroed daily buckets`() {
        val result = StatsInsightsCalculator.computeActivity(
            entries = emptyList(),
            today = today,
            timeZone = timeZone,
        )

        assertEquals(0, result.currentStreakDays)
        assertEquals(0, result.longestStreakDays)
        assertEquals(0, result.activeDaysCount)
        assertEquals(30, result.dailyBuckets30Days.size)
        assertEquals(LocalDate(2026, 10, 15), result.dailyBuckets30Days.first().date)
        assertEquals(today, result.dailyBuckets30Days.last().date)
        assertEquals(0L, result.dailyBuckets30Days.sumOf { it.durationMs })
        assertEquals(0, result.dailyBuckets30Days.sumOf { it.chaptersRead })
    }

    @Test
    fun `current streak counts through today or yesterday and tracks longest streak`() {
        val entries = listOf(
            // Older 4-day streak: Nov 1..4
            activityOn(LocalDate(2026, 11, 1), durationMs = 1_000L),
            activityOn(LocalDate(2026, 11, 2), durationMs = 1_000L),
            activityOn(LocalDate(2026, 11, 3), durationMs = 1_000L),
            activityOn(LocalDate(2026, 11, 4), durationMs = 1_000L),
            // Active 2-day streak ending yesterday: Nov 11..12
            activityOn(LocalDate(2026, 11, 11), durationMs = 2_000L),
            activityOn(LocalDate(2026, 11, 12), durationMs = 3_000L),
        )

        val endingYesterday = StatsInsightsCalculator.computeActivity(entries, today, timeZone)
        assertEquals(2, endingYesterday.currentStreakDays)
        assertEquals(4, endingYesterday.longestStreakDays)
        assertEquals(6, endingYesterday.activeDaysCount)

        // Adding today extends current streak to 3
        val withToday = StatsInsightsCalculator.computeActivity(
            entries + activityOn(today, durationMs = 5_000L),
            today,
            timeZone,
        )
        assertEquals(3, withToday.currentStreakDays)
        assertEquals(4, withToday.longestStreakDays)
        assertEquals(7, withToday.activeDaysCount)

        // Gap before yesterday resets current streak to 0
        val broken = StatsInsightsCalculator.computeActivity(
            listOf(activityOn(LocalDate(2026, 11, 10), durationMs = 1_000L)),
            today,
            timeZone,
        )
        assertEquals(0, broken.currentStreakDays)
        assertEquals(1, broken.longestStreakDays)
    }

    @Test
    fun `multiple chapters on same day aggregate into one bucket and one active day`() {
        val entries = listOf(
            activityOn(today, hour = 9, durationMs = 12_000L),
            activityOn(today, hour = 21, durationMs = 18_000L),
        )

        val result = StatsInsightsCalculator.computeActivity(entries, today, timeZone)
        assertEquals(1, result.activeDaysCount)
        assertEquals(1, result.currentStreakDays)
        val todayBucket = result.dailyBuckets30Days.last()
        assertEquals(today, todayBucket.date)
        assertEquals(30_000L, todayBucket.durationMs)
        assertEquals(2, todayBucket.chaptersRead)
    }

    @Test
    fun `computeBreakdown ranks top sources and deduplicates genres per manga`() {
        val library = listOf(
            testLibraryManga(1L, source = 100L, genres = listOf("Action", "Action", " ", "Fantasy")),
            testLibraryManga(2L, source = 100L, genres = listOf("Action", "Drama")),
            testLibraryManga(3L, source = 200L, genres = listOf("Fantasy")),
        )
        val sourceNames = mapOf(100L to "Source A", 200L to "Source B")

        val breakdown = StatsInsightsCalculator.computeBreakdown(
            libraryManga = library,
            sourceNameResolver = { sourceNames[it] ?: "Unknown" },
            maxItems = 5,
        )

        assertEquals(2, breakdown.topSources.size)
        assertEquals("Source A", breakdown.topSources[0].label)
        assertEquals(2, breakdown.topSources[0].count)
        assertEquals(2f / 3f, breakdown.topSources[0].fraction, 0.0001f)

        assertEquals("Source B", breakdown.topSources[1].label)
        assertEquals(1, breakdown.topSources[1].count)

        assertEquals(3, breakdown.topGenres.size)
        assertEquals("Action", breakdown.topGenres[0].label)
        assertEquals(2, breakdown.topGenres[0].count)
        assertEquals("Fantasy", breakdown.topGenres[1].label)
        assertEquals(2, breakdown.topGenres[1].count)
        assertEquals("Drama", breakdown.topGenres[2].label)
        assertEquals(1, breakdown.topGenres[2].count)
    }
}
