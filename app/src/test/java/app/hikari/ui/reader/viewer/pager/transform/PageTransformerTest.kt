package app.hikari.ui.reader.viewer.pager.transform

import android.view.View
import app.hikari.ui.reader.setting.ReaderPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageTransformerTest {

    private class ViewState(
        val width: Int = 1000,
        val height: Int = 2000,
        var alpha: Float = 1f,
        var translationX: Float = 0f,
        var translationY: Float = 0f,
        var scaleX: Float = 1f,
        var scaleY: Float = 1f,
        var rotationX: Float = 0f,
        var rotationY: Float = 0f,
        var pivotX: Float = 0f,
        var pivotY: Float = 0f,
        var cameraDistance: Float = 0f,
    )

    private fun createTrackedView(state: ViewState = ViewState()): Pair<View, ViewState> {
        val view = mockk<View>(relaxed = true)
        every { view.width } returns state.width
        every { view.height } returns state.height
        every { view.resources } returns null

        every { view.alpha = any() } answers { state.alpha = firstArg() }
        every { view.alpha } answers { state.alpha }
        every { view.translationX = any() } answers { state.translationX = firstArg() }
        every { view.translationX } answers { state.translationX }
        every { view.translationY = any() } answers { state.translationY = firstArg() }
        every { view.translationY } answers { state.translationY }
        every { view.scaleX = any() } answers { state.scaleX = firstArg() }
        every { view.scaleX } answers { state.scaleX }
        every { view.scaleY = any() } answers { state.scaleY = firstArg() }
        every { view.scaleY } answers { state.scaleY }
        every { view.rotationX = any() } answers { state.rotationX = firstArg() }
        every { view.rotationX } answers { state.rotationX }
        every { view.rotationY = any() } answers { state.rotationY = firstArg() }
        every { view.rotationY } answers { state.rotationY }
        every { view.pivotX = any() } answers { state.pivotX = firstArg() }
        every { view.pivotX } answers { state.pivotX }
        every { view.pivotY = any() } answers { state.pivotY = firstArg() }
        every { view.pivotY } answers { state.pivotY }
        every { view.cameraDistance = any() } answers { state.cameraDistance = firstArg() }
        every { view.cameraDistance } answers { state.cameraDistance }

        return view to state
    }

    @Test
    fun `book curl anchors left spine for L2R leaving page`() {
        val transformer = BookCurlPageTransformer(isReversed = false, isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, -0.5f)

        assertEquals(0f, state.pivotX)
        assertEquals(1000f, state.pivotY)
        assertEquals(500f, state.translationX)
        assertEquals(-45f, state.rotationY)
        assertEquals(1f, state.alpha)
        assertTrue(state.cameraDistance > 0f)
    }

    @Test
    fun `book curl anchors right spine for R2L leaving page`() {
        val transformer = BookCurlPageTransformer(isReversed = true, isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, 0.5f)

        assertEquals(1000f, state.pivotX)
        assertEquals(1000f, state.pivotY)
        assertEquals(-500f, state.translationX)
        assertEquals(45f, state.rotationY)
        assertEquals(1f, state.alpha)
    }

    @Test
    fun `book curl fades out smoothly near end of turn and hides offscreen`() {
        val transformer = BookCurlPageTransformer(isReversed = false, isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, -0.875f)
        assertEquals(0.5f, state.alpha, 0.001f)

        transformer.transformPage(view, -1.5f)
        assertEquals(0f, state.alpha)
        assertEquals(0f, state.rotationY)
    }

    @Test
    fun `book curl adapts to vertical pager axis`() {
        val transformer = BookCurlPageTransformer(isReversed = false, isHorizontal = false)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, -0.5f)

        assertEquals(500f, state.pivotX)
        assertEquals(0f, state.pivotY)
        assertEquals(1000f, state.translationY)
        assertEquals(45f, state.rotationX)
        assertEquals(0f, state.rotationY)
    }

    @Test
    fun `depth transformer scales and translates incoming page`() {
        val transformer = DepthPageTransformer(isReversed = false, isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, 0.5f)

        assertEquals(-500f, state.translationX)
        assertEquals(0.875f, state.scaleX, 0.001f)
        assertEquals(0.875f, state.scaleY, 0.001f)
        assertEquals(0.5f, state.alpha, 0.001f)
    }

    @Test
    fun `depth transformer adapts incoming side for R2L`() {
        val transformer = DepthPageTransformer(isReversed = true, isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, -0.5f)

        assertEquals(500f, state.translationX)
        assertEquals(0.875f, state.scaleX, 0.001f)
        assertEquals(0.5f, state.alpha, 0.001f)
    }

    @Test
    fun `fade transformer cancels slide and dissolves alpha`() {
        val transformer = FadePageTransformer(isHorizontal = true)
        val (view, state) = createTrackedView()

        transformer.transformPage(view, 0.4f)
        assertEquals(-400f, state.translationX)
        assertEquals(0.6f, state.alpha, 0.001f)

        transformer.transformPage(view, 1f)
        assertEquals(0f, state.alpha)
    }

    @Test
    fun `cube transformer rotates around touching edge`() {
        val transformer = CubePageTransformer(isHorizontal = true)
        val (leftView, leftState) = createTrackedView()
        val (rightView, rightState) = createTrackedView()

        transformer.transformPage(leftView, -0.5f)
        assertEquals(1000f, leftState.pivotX)
        assertEquals(-45f, leftState.rotationY)
        assertEquals(1f, leftState.alpha)

        transformer.transformPage(rightView, 0.5f)
        assertEquals(0f, rightState.pivotX)
        assertEquals(45f, rightState.rotationY)
        assertEquals(1f, rightState.alpha)
    }

    @Test
    fun `pager transition enum exposes all expected styles`() {
        assertEquals(
            listOf(
                ReaderPreferences.PagerPageTransition.DEFAULT,
                ReaderPreferences.PagerPageTransition.CURL,
                ReaderPreferences.PagerPageTransition.DEPTH,
                ReaderPreferences.PagerPageTransition.FADE,
                ReaderPreferences.PagerPageTransition.CUBE,
            ),
            ReaderPreferences.PagerPageTransition.entries,
        )
    }
}
