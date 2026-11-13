package app.hikari.presentation.more.stats.data

import kotlinx.datetime.LocalDate

sealed interface StatsData {

    data class Overview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData

    data class Titles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData

    data class DailyReadingBucket(
        val date: LocalDate,
        val durationMs: Long,
        val chaptersRead: Int,
    )

    data class Activity(
        val currentStreakDays: Int,
        val longestStreakDays: Int,
        val activeDaysCount: Int,
        val dailyBuckets30Days: List<DailyReadingBucket>,
    ) : StatsData

    data class BreakdownItem(
        val label: String,
        val count: Int,
        val fraction: Float,
    )

    data class Breakdown(
        val topSources: List<BreakdownItem>,
        val topGenres: List<BreakdownItem>,
    ) : StatsData
}
