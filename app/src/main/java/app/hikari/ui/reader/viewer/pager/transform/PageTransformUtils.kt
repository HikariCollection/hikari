package app.hikari.ui.reader.viewer.pager.transform

import android.view.View

private const val DEFAULT_CAMERA_DISTANCE_MULTIPLIER = 8000f

internal fun View.resetPageTransform() {
    alpha = 1f
    translationX = 0f
    translationY = 0f
    scaleX = 1f
    scaleY = 1f
    rotationX = 0f
    rotationY = 0f
    pivotX = width * 0.5f
    pivotY = height * 0.5f
}

internal fun View.applyPerspectiveCameraDistance(multiplier: Float = DEFAULT_CAMERA_DISTANCE_MULTIPLIER) {
    val density = resources?.displayMetrics?.density ?: 1f
    cameraDistance = density.coerceAtLeast(1f) * multiplier
}
