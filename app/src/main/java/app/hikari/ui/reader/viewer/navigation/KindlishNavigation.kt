package app.hikari.ui.reader.viewer.navigation

import androidx.compose.ui.geometry.Rect
import app.hikari.ui.reader.setting.ReaderPreferences.TappingInvertMode
import app.hikari.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | M | M | M |   P: Previous
 * +---+---+---+
 * | P | N | N |   M: Menu
 * +---+---+---+
 * | P | N | N |   N: Next
 * +---+---+---+
*/
class KindlishNavigation(
    invertMode: TappingInvertMode,
    isReversed: Boolean,
) : ViewerNavigation(invertMode, isReversed) {

    override val regionList: List<Region> = listOf(
        Region(
            rect = Rect(0.33f, 0.33f, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
        Region(
            rect = Rect(0f, 0.33f, 0.33f, 1f),
            type = NavigationRegion.PREV,
        ),
    )
}
