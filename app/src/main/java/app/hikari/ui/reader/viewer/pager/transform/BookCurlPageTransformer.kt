package app.hikari.ui.reader.viewer.pager.transform

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

/**
 * Interactive 3D page-curl [ViewPager.PageTransformer] that pivots turning pages around the binding spine.
 */
class BookCurlPageTransformer(
    private val isReversed: Boolean,
    private val isHorizontal: Boolean = true,
) : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        if (position < -1f || position > 1f) {
            page.resetPageTransform()
            page.alpha = 0f
            return
        }

        val progress = abs(position)
        val isLeaving = if (isHorizontal && isReversed) position >= 0f else position <= 0f

        if (isLeaving) {
            page.applyPerspectiveCameraDistance()
            page.scaleX = 1f
            page.scaleY = 1f
            if (isHorizontal) {
                page.translationX = -position * page.width
                page.translationY = 0f
                page.pivotX = if (isReversed) page.width.toFloat() else 0f
                page.pivotY = page.height * 0.5f
                page.rotationY = 90f * position
                page.rotationX = 0f
            } else {
                page.translationX = 0f
                page.translationY = -position * page.height
                page.pivotX = page.width * 0.5f
                page.pivotY = 0f
                page.rotationX = -90f * position
                page.rotationY = 0f
            }
            page.alpha = if (progress >= FADE_OUT_THRESHOLD) {
                ((1f - progress) / (1f - FADE_OUT_THRESHOLD)).coerceIn(0f, 1f)
            } else {
                1f
            }
        } else {
            page.resetPageTransform()
            if (isHorizontal) {
                page.translationX = -position * page.width
            } else {
                page.translationY = -position * page.height
            }
            page.alpha = (MIN_UNDERLAY_ALPHA + (1f - MIN_UNDERLAY_ALPHA) * (1f - progress)).coerceIn(0f, 1f)
        }
    }

    private companion object {
        const val FADE_OUT_THRESHOLD = 0.75f
        const val MIN_UNDERLAY_ALPHA = 0.45f
    }
}
