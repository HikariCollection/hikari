package app.hikari.presentation.widget.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import app.hikari.presentation.widget.BaseUpdatesGridGlanceWidget

@ContributesTo(AppScope::class)
interface PresentationWidgetGraph {
    fun inject(widget: BaseUpdatesGridGlanceWidget)
}
