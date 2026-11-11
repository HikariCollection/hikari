package app.hikari.ui.reader.viewer.pager.transform

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

/**
 * Depth-reveal [ViewPager.PageTransformer] where the underlying page scales up as the active page slides away.
 */
class DepthPageTransformer(
    private val isReversed: Boolean,
    private val isHorizontal: Boolean = true,
) : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        if (position < -1f || position > 1f) {
            page.resetPageTransform()
            page.alpha = 0f
            return
        }

        val isLeaving = if (isHorizontal && isReversed) position >= 0f else position <= 0f
        page.resetPageTransform()

        if (isLeaving) {
            page.alpha = 1f
        } else {
            val progress = abs(position)
            val scale = MIN_SCALE + (1f - MIN_SCALE) * (1f - progress)
            if (isHorizontal) {
                page.translationX = -position * page.width
            } else {
                page.translationY = -position * page.height
            }
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = (1f - progress).coerceIn(0f, 1f)
        }
    }

    private companion object {
        const val MIN_SCALE = 0.75f
    }
}
