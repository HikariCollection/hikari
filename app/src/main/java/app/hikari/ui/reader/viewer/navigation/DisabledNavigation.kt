package app.hikari.ui.reader.viewer.navigation

import app.hikari.ui.reader.setting.ReaderPreferences.TappingInvertMode
import app.hikari.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | M | M | M |   P: Previous
 * +---+---+---+
 * | M | M | M |   M: Menu
 * +---+---+---+
 * | M | M | M |   N: Next
 * +---+---+---+
*/
class DisabledNavigation(
    invertMode: TappingInvertMode,
    isReversed: Boolean,
) : ViewerNavigation(invertMode, isReversed) {

    override val regionList: List<Region> = emptyList()
}
