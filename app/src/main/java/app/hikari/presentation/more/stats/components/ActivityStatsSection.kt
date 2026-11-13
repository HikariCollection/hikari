package app.hikari.presentation.more.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hikari.i18n.MR
import app.hikari.presentation.core.components.SectionCard
import app.hikari.presentation.core.components.material.SECONDARY_ALPHA
import app.hikari.presentation.core.components.material.padding
import app.hikari.presentation.core.i18n.stringResource
import app.hikari.presentation.more.stats.data.StatsData
import app.hikari.presentation.util.toDurationString
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private enum class ActivityRange(val days: Int) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
}

@Composable
fun LazyItemScope.ActivityStatsSection(
    data: StatsData.Activity,
) {
    var selectedRange by remember { mutableStateOf(ActivityRange.SEVEN_DAYS) }
    val buckets = remember(data.dailyBuckets30Days, selectedRange) {
        data.dailyBuckets30Days.takeLast(selectedRange.days)
    }
    var selectedIndex by remember(buckets) {
        mutableIntStateOf(
            buckets.indexOfLast { it.durationMs > 0L }.takeIf { it >= 0 } ?: (buckets.lastIndex.coerceAtLeast(0)),
        )
    }

    SectionCard(MR.strings.label_reading_activity_section) {
        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatsItem(
                    title = stringResource(MR.strings.label_days_count, data.currentStreakDays),
                    subtitle = stringResource(MR.strings.label_current_streak),
                )
                StatsItem(
                    title = stringResource(MR.strings.label_days_count, data.longestStreakDays),
                    subtitle = stringResource(MR.strings.label_longest_streak),
                )
                StatsItem(
                    title = data.activeDaysCount.toString(),
                    subtitle = stringResource(MR.strings.label_active_days),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                FilterChip(
                    selected = selectedRange == ActivityRange.SEVEN_DAYS,
                    onClick = { selectedRange = ActivityRange.SEVEN_DAYS },
                    label = { Text(stringResource(MR.strings.label_last_7_days)) },
                )
                FilterChip(
                    selected = selectedRange == ActivityRange.THIRTY_DAYS,
                    onClick = { selectedRange = ActivityRange.THIRTY_DAYS },
                    label = { Text(stringResource(MR.strings.label_last_30_days)) },
                )
            }

            if (buckets.isEmpty()) {
                return@Column
            }

            val maxDuration = remember(buckets) {
                buckets.maxOfOrNull { it.durationMs } ?: 0L
            }
            val selectedBucket = buckets.getOrNull(selectedIndex)
            val context = LocalContext.current
            val none = stringResource(MR.strings.none)

            if (maxDuration == 0L) {
                Text(
                    text = stringResource(MR.strings.label_no_reading_activity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_ALPHA),
                )
            } else if (selectedBucket != null) {
                val durationText = remember(selectedBucket.durationMs) {
                    selectedBucket.durationMs
                        .toDuration(DurationUnit.MILLISECONDS)
                        .toDurationString(context, fallback = none)
                }
                val dateLabel = "${selectedBucket.date.month.ordinal + 1}/${selectedBucket.date.day}"
                Text(
                    text = "$dateLabel · $durationText (${selectedBucket.chaptersRead})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val barSpacing = if (selectedRange == ActivityRange.SEVEN_DAYS) 8.dp else 3.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(barSpacing),
                verticalAlignment = Alignment.Bottom,
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val fraction = if (maxDuration > 0L && bucket.durationMs > 0L) {
                        (bucket.durationMs.toFloat() / maxDuration.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        0.06f
                    }
                    val isSelected = index == selectedIndex
                    val barColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        bucket.durationMs > 0L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedIndex = index },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val first = buckets.first().date
                val last = buckets.last().date
                Text(
                    text = "${first.month.ordinal + 1}/${first.day}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_ALPHA),
                )
                Text(
                    text = "${last.month.ordinal + 1}/${last.day}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_ALPHA),
                )
            }
        }
    }
}
