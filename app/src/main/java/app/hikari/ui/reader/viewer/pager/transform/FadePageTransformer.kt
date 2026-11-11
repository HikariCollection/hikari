package app.hikari.ui.reader.viewer.pager.transform

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

/**
 * Cross-fade [ViewPager.PageTransformer] that dissolves between consecutive pages in place.
 */
class FadePageTransformer(
    private val isHorizontal: Boolean = true,
) : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.resetPageTransform()
        if (position <= -1f || position >= 1f) {
            page.alpha = 0f
            return
        }

        if (isHorizontal) {
            page.translationX = -position * page.width
        } else {
            page.translationY = -position * page.height
        }
        page.alpha = (1f - abs(position)).coerceIn(0f, 1f)
    }
}
