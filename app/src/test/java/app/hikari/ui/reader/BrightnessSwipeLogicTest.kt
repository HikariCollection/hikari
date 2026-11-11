package app.hikari.ui.reader

import app.hikari.ui.reader.setting.ReaderPreferences.BrightnessSwipeEdge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrightnessSwipeLogicTest {

    @Test
    fun `isInsideBrightnessEdge respects configured edge`() {
        val width = 1080f
        val edgeWidth = 144f

        assertFalse(isInsideBrightnessEdge(0f, width, edgeWidth, BrightnessSwipeEdge.OFF))
        assertFalse(isInsideBrightnessEdge(540f, width, edgeWidth, BrightnessSwipeEdge.OFF))
        assertFalse(isInsideBrightnessEdge(1080f, width, edgeWidth, BrightnessSwipeEdge.OFF))

        assertTrue(isInsideBrightnessEdge(0f, width, edgeWidth, BrightnessSwipeEdge.LEFT))
        assertTrue(isInsideBrightnessEdge(144f, width, edgeWidth, BrightnessSwipeEdge.LEFT))
        assertFalse(isInsideBrightnessEdge(145f, width, edgeWidth, BrightnessSwipeEdge.LEFT))
        assertFalse(isInsideBrightnessEdge(-1f, width, edgeWidth, BrightnessSwipeEdge.LEFT))

        assertTrue(isInsideBrightnessEdge(936f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))
        assertTrue(isInsideBrightnessEdge(1080f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))
        assertFalse(isInsideBrightnessEdge(935f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))
        assertFalse(isInsideBrightnessEdge(1081f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))
    }

    @Test
    fun `isInsideBrightnessEdge caps edge width on narrow viewports`() {
        val width = 200f
        val edgeWidth = 100f

        assertTrue(isInsideBrightnessEdge(30f, width, edgeWidth, BrightnessSwipeEdge.LEFT))
        assertFalse(isInsideBrightnessEdge(31f, width, edgeWidth, BrightnessSwipeEdge.LEFT))

        assertTrue(isInsideBrightnessEdge(170f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))
        assertFalse(isInsideBrightnessEdge(169f, width, edgeWidth, BrightnessSwipeEdge.RIGHT))

        assertFalse(isInsideBrightnessEdge(0f, 0f, edgeWidth, BrightnessSwipeEdge.LEFT))
        assertFalse(isInsideBrightnessEdge(0f, width, 0f, BrightnessSwipeEdge.LEFT))
    }

    @Test
    fun `calculateDragBrightness maps vertical travel over 55 percent of height and clamps`() {
        val height = 1000f
        val fullTravel = height * 0.55f

        assertEquals(100, calculateDragBrightness(-75, -fullTravel, height))
        assertEquals(-75, calculateDragBrightness(100, fullTravel, height))

        assertEquals(100, calculateDragBrightness(90, -400f, height))
        assertEquals(-75, calculateDragBrightness(-60, 400f, height))

        assertEquals(25, calculateDragBrightness(25, 0f, height))
        assertEquals(100, calculateDragBrightness(150, 0f, height))
        assertEquals(-75, calculateDragBrightness(-120, 0f, height))
        assertEquals(40, calculateDragBrightness(40, -100f, 0f))
    }

    @Test
    fun `brightnessProgress normalizes range to unit interval`() {
        assertEquals(0f, brightnessProgress(-75), 1e-4f)
        assertEquals(1f, brightnessProgress(100), 1e-4f)
        assertEquals(75f / 175f, brightnessProgress(0), 1e-4f)
        assertEquals(0f, brightnessProgress(-200), 1e-4f)
        assertEquals(1f, brightnessProgress(200), 1e-4f)
    }

    @Test
    fun `resolveInitialBrightness preserves active custom brightness and falls back smoothly`() {
        assertEquals(-40, resolveInitialBrightness(-40, 0.5f, 128))
        assertEquals(70, resolveInitialBrightness(70, -1f, 128))
        assertEquals(-75, resolveInitialBrightness(-90, -1f, 128))

        assertEquals(35, resolveInitialBrightness(0, 0.35f, 200))
        assertEquals(1, resolveInitialBrightness(0, 0.004f, 200))

        assertEquals(50, resolveInitialBrightness(0, -1f, 128))
        assertEquals(100, resolveInitialBrightness(0, -1f, 255))
        assertEquals(1, resolveInitialBrightness(0, -1f, 0))
        assertEquals(50, resolveInitialBrightness(0, -1f, null))
        assertEquals(50, resolveInitialBrightness(0, Float.NaN, null))
    }
}
