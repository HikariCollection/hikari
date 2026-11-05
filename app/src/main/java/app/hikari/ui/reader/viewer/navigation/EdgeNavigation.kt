package app.hikari.ui.reader.viewer.navigation

import androidx.compose.ui.geometry.Rect
import app.hikari.ui.reader.setting.ReaderPreferences.TappingInvertMode
import app.hikari.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | N | N |   P: Previous
 * +---+---+---+
 * | N | M | N |   M: Menu
 * +---+---+---+
 * | N | P | N |   N: Next
 * +---+---+---+
*/
class EdgeNavigation(invertMode: TappingInvertMode, isReversed: Boolean) : ViewerNavigation(invertMode, isReversed) {

    override val regionList: List<Region> = listOf(
        Region(
            rect = Rect(0f, 0f, 0.33f, 1f),
            type = NavigationRegion.NEXT,
        ),
        Region(
            rect = Rect(0.33f, 0.66f, 0.66f, 1f),
            type = NavigationRegion.PREV,
        ),
        Region(
            rect = Rect(0.66f, 0f, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
    )
}
