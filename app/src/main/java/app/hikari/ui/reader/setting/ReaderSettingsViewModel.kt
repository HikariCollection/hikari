package app.hikari.ui.reader.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hikari.core.common.preference.Preference
import app.hikari.domain.base.BasePreferences
import app.hikari.domain.manga.model.readingMode
import app.hikari.ui.reader.BRIGHTNESS_MAX
import app.hikari.ui.reader.BRIGHTNESS_MIN
import app.hikari.ui.reader.ReaderViewModel
import app.hikari.ui.reader.viewer.LongStripSettings
import app.hikari.ui.reader.viewer.NavigationSettings
import app.hikari.ui.reader.viewer.PagedSettings
import app.hikari.ui.reader.viewer.ViewerNavigation
import app.hikari.ui.reader.viewer.navigation.LNavigation
import app.hikari.ui.reader.viewer.navigation.RightAndLeftNavigation
import app.hikari.ui.reader.viewer.pager.SimplePagedSettings
import app.hikari.ui.reader.viewer.webgpu.WebGpuLongStripSettings
import app.hikari.ui.reader.viewer.webgpu.WebGpuPagedSettings
import app.hikari.ui.reader.viewer.webgpu.WebGpuSettings
import app.hikari.ui.reader.viewer.webtoon.SimpleLongStripSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

/**
 * The reader's settings, each as one value re-read whenever a preference it's made of changes, so the activity and
 * viewers apply a change in one place instead of each preference calling back.
 */
class ReaderSettingsViewModel(
    private val readerViewModel: ReaderViewModel,
    val preferences: ReaderPreferences,
    private val basePreferences: BasePreferences,
) : ViewModel() {

    val mangaFlow = readerViewModel.state
        .map { it.manga }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * The reading mode the viewer is made for, with the default resolved.
     */
    val readingMode = combine(mangaFlow, preferences.defaultReadingMode.changes()) { manga, default ->
        ReadingMode.fromPreference(manga?.readingMode?.toInt())
            .takeIf { it != ReadingMode.DEFAULT }
            ?: ReadingMode.fromPreference(default)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ReadingMode.fromPreference(readerViewModel.getMangaReadingMode()),
        )

    /**
     * Whether the viewers are the WebGPU ones.
     */
    val isWebGpu = basePreferences.highQualityRenderer

    val general = settings(
        preferences.readerTheme,
        preferences.keepScreenOn,
        preferences.grayscale,
        preferences.invertedColors,
        preferences.fullscreen,
        preferences.drawUnderCutout,
        preferences.readWithLongTap,
        preferences.pageTransitions,
        preferences.doubleTapAnimSpeed,
        preferences.readWithVolumeKeys,
        preferences.readWithVolumeKeysInverted,
        preferences.alwaysShowChapterTransition,
        preferences.showNavigationOverlayOnStart,
    ) {
        GeneralSettings(
            theme = ReaderPreferences.ReaderTheme.fromValue(readerTheme.get()),
            keepScreenOn = keepScreenOn.get(),
            grayscale = grayscale.get(),
            invertedColors = invertedColors.get(),
            fullscreen = fullscreen.get(),
            drawUnderCutout = drawUnderCutout.get(),
            longTapEnabled = readWithLongTap.get(),
            usePageTransitions = pageTransitions.get(),
            doubleTapAnimDuration = doubleTapAnimSpeed.get(),
            volumeKeysEnabled = readWithVolumeKeys.get(),
            volumeKeysInverted = readWithVolumeKeysInverted.get(),
            alwaysShowChapterTransition = alwaysShowChapterTransition.get(),
            navigationOverlayOnStart = showNavigationOverlayOnStart.get(),
        )
    }

    private val liveBrightness = MutableStateFlow<Int?>(null)

    @Volatile
    private var isDraggingBrightness = false

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val persistedBrightness = preferences.customBrightness.changes()
        .flatMapLatest { enabled ->
            if (enabled) preferences.customBrightnessValue.changes().sample(0.1.seconds) else flowOf(0)
        }
        .onEach {
            if (!isDraggingBrightness) {
                liveBrightness.value = null
            }
        }

    /**
     * The custom brightness, 0 when it's off.
     */
    val brightness: StateFlow<Int> = combine(persistedBrightness, liveBrightness) { persisted, live ->
        live ?: persisted
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            if (preferences.customBrightness.get()) preferences.customBrightnessValue.get() else 0,
        )

    fun updateLiveBrightness(value: Int) {
        isDraggingBrightness = true
        liveBrightness.value = value.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    }

    fun commitLiveBrightness(value: Int) {
        val clamped = value.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
        isDraggingBrightness = false
        liveBrightness.value = clamped
        preferences.customBrightnessValue.set(clamped)
        if (!preferences.customBrightness.get()) {
            preferences.customBrightness.set(true)
        }
    }

    /**
     * Settings every paged viewer uses.
     */
    val paged = settings(
        preferences.imageScaleType,
        preferences.zoomStart,
        preferences.cropBorders,
        preferences.landscapeZoom,
        preferences.navigateToPan,
        preferences.navigationModePager,
        preferences.pagerNavInverted,
    ) {
        PagedSettings(
            page = PagedSettings.Page(
                imageScaleType = ReaderPreferences.ScaleType.fromValue(imageScaleType.get()),
                zoomStart = ReaderPreferences.ZoomStartType.fromValue(zoomStart.get()),
                cropBorders = cropBorders.get(),
                landscapeZoom = landscapeZoom.get(),
            ),
            navigateToPan = navigateToPan.get(),
            navigation = NavigationSettings(
                ReaderPreferences.NavigationMode.fromValue(navigationModePager.get()),
                pagerNavInverted.get(),
            ),
        )
    }

    /**
     * Settings every long strip viewer uses.
     */
    val longStrip = settings(
        preferences.cropBordersWebtoon,
        preferences.webtoonDisableZoomOut,
    ) {
        LongStripSettings(
            cropBorders = cropBordersWebtoon.get(),
            zoomOutDisabled = webtoonDisableZoomOut.get(),
        )
    }

    /**
     * Settings only the pager viewers use.
     */
    val simplePaged = settings(
        preferences.dualPageSplitPaged,
        preferences.dualPageInvertPaged,
        preferences.dualPageRotateToFit,
        preferences.dualPageRotateToFitInvert,
        preferences.pagerPageTransition,
    ) {
        SimplePagedSettings(
            dualPageSplit = dualPageSplitPaged.get(),
            dualPageInvert = dualPageInvertPaged.get(),
            dualPageRotateToFit = dualPageRotateToFit.get(),
            dualPageRotateToFitInvert = dualPageRotateToFitInvert.get(),
            pageTransition = pagerPageTransition.get(),
        )
    }

    /**
     * Settings only the webtoon viewers use.
     */
    val simpleLongStrip = settings(
        preferences.webtoonSidePadding,
        preferences.dualPageSplitWebtoon,
        preferences.dualPageInvertWebtoon,
        preferences.dualPageRotateToFitWebtoon,
        preferences.dualPageRotateToFitInvertWebtoon,
        preferences.webtoonDoubleTapZoomEnabled,
        preferences.navigationModeWebtoon,
        preferences.webtoonNavInverted,
    ) {
        SimpleLongStripSettings(
            page = SimpleLongStripSettings.Page(
                sidePadding = webtoonSidePadding.get(),
                dualPageSplit = dualPageSplitWebtoon.get(),
                dualPageInvert = dualPageInvertWebtoon.get(),
                dualPageRotateToFit = dualPageRotateToFitWebtoon.get(),
                dualPageRotateToFitInvert = dualPageRotateToFitInvertWebtoon.get(),
            ),
            doubleTapZoom = webtoonDoubleTapZoomEnabled.get(),
            navigation = NavigationSettings(
                ReaderPreferences.NavigationMode.fromValue(navigationModeWebtoon.get()),
                webtoonNavInverted.get(),
            ),
        )
    }

    /**
     * Settings only the paged WebGPU viewers use.
     */
    val webgpuPaged = settings(
        preferences.dualPageView,
        preferences.transitionAnimation,
        preferences.transitionAnimationDual,
        preferences.cutoutMode,
        preferences.cutoutModeDual,
        preferences.disableDoubleTapGestures,
        preferences.imageFilters,
        preferences.tileFilters,
        preferences.upscaler,
        preferences.downscaler,
        preferences.fastRenderingOnly,
        preferences.holdUntilFinal,
        preferences.cropInsteadOfZoom,
    ) {
        WebGpuPagedSettings(
            page = WebGpuPagedSettings.Page(
                dualPageView = dualPageView.get(),
                transitionAnimation = transitionAnimation.get(),
                transitionAnimationDual = transitionAnimationDual.get(),
                cutoutMode = cutoutMode.get(),
                cutoutModeDual = cutoutModeDual.get(),
                cropInsteadOfZoom = cropInsteadOfZoom.get(),
            ),
            common = WebGpuSettings(
                doubleTapGesturesDisabled = disableDoubleTapGestures.get(),
                fastRenderingOnly = fastRenderingOnly.get(),
                holdUntilFinal = holdUntilFinal.get(),
                imageFilters = imageFilters.get(),
                tileFilters = tileFilters.get(),
                upscaler = upscaler.get().firstOrNull(),
                downscaler = downscaler.get().firstOrNull(),
            ),
        )
    }

    /**
     * Settings only the long strip WebGPU viewer uses.
     */
    val webgpuLongStrip = settings(
        preferences.continuousMinWidth,
        preferences.continuousGap,
        preferences.disableDoubleTapGestures,
        preferences.imageFilters,
        preferences.tileFilters,
        preferences.upscaler,
        preferences.downscaler,
        preferences.fastRenderingOnly,
        preferences.holdUntilFinal,
    ) {
        WebGpuLongStripSettings(
            page = WebGpuLongStripSettings.Page(
                continuousMinWidth = continuousMinWidth.get(),
                continuousGap = continuousGap.get(),
            ),
            common = WebGpuSettings(
                doubleTapGesturesDisabled = disableDoubleTapGestures.get(),
                fastRenderingOnly = fastRenderingOnly.get(),
                holdUntilFinal = holdUntilFinal.get(),
                imageFilters = imageFilters.get(),
                tileFilters = tileFilters.get(),
                upscaler = upscaler.get().firstOrNull(),
                downscaler = downscaler.get().firstOrNull(),
            ),
        )
    }

    private val viewerMode = MutableStateFlow<ViewerMode?>(null)

    /**
     * Tap zones of the open viewer. Null until a viewer is made.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val navigation: StateFlow<ViewerNavigation?> = viewerMode
        .flatMapLatest { viewerMode ->
            if (viewerMode == null) return@flatMapLatest flowOf(null)

            val usesWebtoonZones = viewerMode.readingMode.type == ReadingMode.ViewerType.Webtoon &&
                !viewerMode.isWebGpu
            val settings = if (usesWebtoonZones) {
                simpleLongStrip.map { it.navigation }
            } else {
                paged.map { it.navigation }
            }
            val isReversed = viewerMode.readingMode == ReadingMode.RIGHT_TO_LEFT
            val defaultNavigation: (ReaderPreferences.TappingInvertMode, Boolean) -> ViewerNavigation =
                if (viewerMode.readingMode.direction == ReadingMode.Direction.Vertical) {
                    ::LNavigation
                } else {
                    ::RightAndLeftNavigation
                }
            settings.distinctUntilChanged().map { it.navigator(isReversed, defaultNavigation) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Called when a viewer is made for [readingMode].
     */
    fun setViewerReadingMode(readingMode: ReadingMode) {
        viewerMode.value = ViewerMode(readingMode, isWebGpu.get())
    }

    fun setReadingMode(readingMode: ReadingMode) = readerViewModel.setMangaReadingMode(readingMode)

    fun setOrientation(orientation: ReaderOrientation) = readerViewModel.setMangaOrientationType(orientation)

    /**
     * Whether a viewer must show its tap zones because the reader is new to the user, which it is only once.
     */
    fun takeNavigationOverlayForNewUser(): Boolean {
        val show = preferences.showNavigationOverlayNewUser.get()
        if (show) {
            preferences.showNavigationOverlayNewUser.set(false)
        }
        return show
    }

    /**
     * A value [read] from [sources], kept current as any of them changes. Started eagerly, so a viewer reading it
     * before collecting gets what the preferences hold now.
     */
    private fun <T> settings(vararg sources: Preference<*>, read: ReaderPreferences.() -> T): StateFlow<T> {
        return sources.map { it.changes() }
            .merge()
            .map { preferences.read() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.read())
    }

    private data class ViewerMode(
        val readingMode: ReadingMode,
        val isWebGpu: Boolean,
    )
}
