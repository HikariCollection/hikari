package app.hikari.ui.stats

import app.hikari.domain.history.model.ReadActivityEntry
import app.hikari.domain.library.model.LibraryManga
import app.hikari.presentation.more.stats.data.StatsData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object StatsInsightsCalculator {

    private const val TREND_WINDOW_DAYS = 30

    fun computeActivity(
        entries: List<ReadActivityEntry>,
        today: LocalDate,
        timeZone: TimeZone,
    ): StatsData.Activity {
        val groupedByDate = entries.groupBy { it.readAt.toLocalDateTime(timeZone).date }
        val activeDates = groupedByDate.keys.sorted()

        val currentStreak = computeCurrentStreak(groupedByDate.keys, today)
        val longestStreak = computeLongestStreak(activeDates)

        val startDate = today.minus(TREND_WINDOW_DAYS - 1, DateTimeUnit.DAY)
        val buckets = (0 until TREND_WINDOW_DAYS).map { offset ->
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            val dayEntries = groupedByDate[date].orEmpty()
            StatsData.DailyReadingBucket(
                date = date,
                durationMs = dayEntries.sumOf { it.readDuration },
                chaptersRead = dayEntries.size,
            )
        }

        return StatsData.Activity(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            activeDaysCount = activeDates.size,
            dailyBuckets30Days = buckets,
        )
    }

    fun computeBreakdown(
        libraryManga: List<LibraryManga>,
        sourceNameResolver: (Long) -> String,
        maxItems: Int = 5,
    ): StatsData.Breakdown {
        if (libraryManga.isEmpty()) {
            return StatsData.Breakdown(
                topSources = emptyList(),
                topGenres = emptyList(),
            )
        }

        val total = libraryManga.size.toFloat()

        val sourceCounts = libraryManga
            .groupingBy { it.manga.source }
            .eachCount()
            .entries
            .map { (sourceId, count) ->
                StatsData.BreakdownItem(
                    label = sourceNameResolver(sourceId),
                    count = count,
                    fraction = count / total,
                )
            }
            .sortedWith(compareByDescending<StatsData.BreakdownItem> { it.count }.thenBy { it.label })
            .take(maxItems)

        val genreCounts = mutableMapOf<String, Int>()
        libraryManga.forEach { item ->
            item.manga.genre
                ?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.distinct()
                ?.forEach { genre ->
                    genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                }
        }

        val topGenres = genreCounts.entries
            .map { (genre, count) ->
                StatsData.BreakdownItem(
                    label = genre,
                    count = count,
                    fraction = count / total,
                )
            }
            .sortedWith(compareByDescending<StatsData.BreakdownItem> { it.count }.thenBy { it.label })
            .take(maxItems)

        return StatsData.Breakdown(
            topSources = sourceCounts,
            topGenres = topGenres,
        )
    }

    private fun computeCurrentStreak(activeDates: Set<LocalDate>, today: LocalDate): Int {
        if (activeDates.isEmpty()) return 0
        var cursor = when {
            today in activeDates -> today
            today.minus(1, DateTimeUnit.DAY) in activeDates -> today.minus(1, DateTimeUnit.DAY)
            else -> return 0
        }
        var streak = 0
        while (cursor in activeDates) {
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun computeLongestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].plus(1, DateTimeUnit.DAY)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }
}
