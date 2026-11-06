package app.hikari.ui.reader.setting

import androidx.compose.ui.graphics.vector.ImageVector
import app.hikari.app.di.appGraph
import app.hikari.i18n.MR
import app.hikari.i18n.StringResource
import app.hikari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.hikari.icons.custommaterialsymbols.rounded.ReaderLeftToRight
import app.hikari.icons.custommaterialsymbols.rounded.ReaderLongStrip
import app.hikari.icons.custommaterialsymbols.rounded.ReaderLongStripGaps
import app.hikari.icons.custommaterialsymbols.rounded.ReaderRightToLeft
import app.hikari.icons.custommaterialsymbols.rounded.ReaderVertical
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.rounded.MobileGear
import app.hikari.ui.reader.ReaderActivity
import app.hikari.ui.reader.viewer.Viewer
import app.hikari.ui.reader.viewer.pager.L2RPagerViewer
import app.hikari.ui.reader.viewer.pager.R2LPagerViewer
import app.hikari.ui.reader.viewer.pager.VerticalPagerViewer
import app.hikari.ui.reader.viewer.webgpu.WebGpuViewer
import app.hikari.ui.reader.viewer.webgpu.WebGpuViewerContinuous
import app.hikari.ui.reader.viewer.webtoon.WebtoonViewer

enum class ReadingMode(
    val stringRes: StringResource,
    val icon: ImageVector,
    val flagValue: Int,
    val direction: Direction? = null,
    val type: ViewerType? = null,
) {
    DEFAULT(MR.strings.label_default, MaterialSymbols.Rounded.MobileGear, 0x00000000),
    LEFT_TO_RIGHT(
        MR.strings.left_to_right_viewer,
        CustomMaterialSymbols.Rounded.ReaderLeftToRight,
        0x00000001,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    RIGHT_TO_LEFT(
        MR.strings.right_to_left_viewer,
        CustomMaterialSymbols.Rounded.ReaderRightToLeft,
        0x00000002,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    VERTICAL(
        MR.strings.vertical_viewer,
        CustomMaterialSymbols.Rounded.ReaderVertical,
        0x00000003,
        Direction.Vertical,
        ViewerType.Pager,
    ),
    WEBTOON(
        MR.strings.webtoon_viewer,
        CustomMaterialSymbols.Rounded.ReaderLongStrip,
        0x00000004,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    CONTINUOUS_VERTICAL(
        MR.strings.vertical_plus_viewer,
        CustomMaterialSymbols.Rounded.ReaderLongStripGaps,
        0x00000005,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    ;

    companion object {
        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingMode = entries.find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode.type is ViewerType.Pager
        }

        fun toViewer(preference: Int?, activity: ReaderActivity): Viewer {
            if (activity.appGraph.basePreferences.highQualityRenderer.get()) {
                return when (fromPreference(preference)) {
                    LEFT_TO_RIGHT -> WebGpuViewer(activity, isReversed = false, isVertical = false)
                    RIGHT_TO_LEFT -> WebGpuViewer(activity, isReversed = true, isVertical = false)
                    VERTICAL -> WebGpuViewer(activity, isReversed = false, isVertical = true)
                    WEBTOON -> WebGpuViewerContinuous(activity, useGap = false)
                    CONTINUOUS_VERTICAL -> WebGpuViewerContinuous(activity, useGap = true)
                    DEFAULT -> throw IllegalStateException("Preference value must be resolved: $preference")
                }
            }
            return when (fromPreference(preference)) {
                LEFT_TO_RIGHT -> L2RPagerViewer(activity)
                RIGHT_TO_LEFT -> R2LPagerViewer(activity)
                VERTICAL -> VerticalPagerViewer(activity)
                WEBTOON -> WebtoonViewer(activity)
                CONTINUOUS_VERTICAL -> WebtoonViewer(activity, isContinuous = false)
                DEFAULT -> throw IllegalStateException("Preference value must be resolved: $preference")
            }
        }
    }

    sealed interface Direction {
        data object Horizontal : Direction
        data object Vertical : Direction
    }

    sealed interface ViewerType {
        data object Pager : ViewerType
        data object Webtoon : ViewerType
    }
}
