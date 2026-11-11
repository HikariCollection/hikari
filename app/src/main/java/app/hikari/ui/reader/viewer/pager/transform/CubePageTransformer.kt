package app.hikari.ui.reader.viewer.pager.transform

import android.view.View
import androidx.viewpager.widget.ViewPager

/**
 * 3D cube rotation [ViewPager.PageTransformer] rotating pages around their adjacent edge.
 */
class CubePageTransformer(
    private val isHorizontal: Boolean = true,
) : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        if (position < -1f || position > 1f) {
            page.resetPageTransform()
            page.alpha = 0f
            return
        }

        page.applyPerspectiveCameraDistance()
        page.translationX = 0f
        page.translationY = 0f
        page.scaleX = 1f
        page.scaleY = 1f
        page.alpha = 1f

        if (isHorizontal) {
            page.pivotX = if (position < 0f) page.width.toFloat() else 0f
            page.pivotY = page.height * 0.5f
            page.rotationY = 90f * position
            page.rotationX = 0f
        } else {
            page.pivotX = page.width * 0.5f
            page.pivotY = if (position < 0f) page.height.toFloat() else 0f
            page.rotationX = -90f * position
            page.rotationY = 0f
        }
    }
}
