package app.hikari.ui.reader

import app.hikari.ui.reader.setting.ReaderPreferences.BrightnessSwipeEdge
import kotlin.math.roundToInt

internal const val BRIGHTNESS_MIN = -75
internal const val BRIGHTNESS_MAX = 100
private const val BRIGHTNESS_RANGE = BRIGHTNESS_MAX - BRIGHTNESS_MIN
private const val BRIGHTNESS_SWIPE_HEIGHT_FRACTION = 0.55f
private const val BRIGHTNESS_EDGE_MAX_WIDTH_FRACTION = 0.15f
private const val DEFAULT_INITIAL_BRIGHTNESS = 50

internal fun isInsideBrightnessEdge(
    x: Float,
    width: Float,
    edgeWidthPx: Float,
    edge: BrightnessSwipeEdge,
): Boolean {
    if (edge == BrightnessSwipeEdge.OFF || width <= 0f || edgeWidthPx <= 0f) return false
    if (x < 0f || x > width) return false

    val effectiveEdgeWidth = minOf(edgeWidthPx, width * BRIGHTNESS_EDGE_MAX_WIDTH_FRACTION)
    return when (edge) {
        BrightnessSwipeEdge.OFF -> false
        BrightnessSwipeEdge.LEFT -> x <= effectiveEdgeWidth
        BrightnessSwipeEdge.RIGHT -> x >= width - effectiveEdgeWidth
    }
}

internal fun calculateDragBrightness(
    startBrightness: Int,
    dragDy: Float,
    containerHeight: Float,
): Int {
    val clampedStart = startBrightness.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    if (containerHeight <= 0f) return clampedStart

    val fullTravelPx = containerHeight * BRIGHTNESS_SWIPE_HEIGHT_FRACTION
    val delta = (-dragDy / fullTravelPx) * BRIGHTNESS_RANGE
    return (clampedStart + delta).roundToInt().coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
}

internal fun brightnessProgress(value: Int): Float {
    val clamped = value.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    return (clamped - BRIGHTNESS_MIN) / BRIGHTNESS_RANGE.toFloat()
}

internal fun resolveInitialBrightness(
    currentBrightness: Int,
    windowScreenBrightness: Float,
    systemBrightness: Int?,
): Int {
    if (currentBrightness != 0) {
        return currentBrightness.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    }
    if (!windowScreenBrightness.isNaN() && windowScreenBrightness >= 0f) {
        return (windowScreenBrightness.coerceIn(0f, 1f) * BRIGHTNESS_MAX)
            .roundToInt()
            .coerceIn(1, BRIGHTNESS_MAX)
    }
    if (systemBrightness != null) {
        return ((systemBrightness.coerceIn(0, 255) / 255f) * BRIGHTNESS_MAX)
            .roundToInt()
            .coerceIn(1, BRIGHTNESS_MAX)
    }
    return DEFAULT_INITIAL_BRIGHTNESS
}
