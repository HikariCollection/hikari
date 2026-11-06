package app.hikari.presentation.widget.di

import app.hikari.presentation.widget.BaseUpdatesGridGlanceWidget
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
interface PresentationWidgetGraph {
    fun inject(widget: BaseUpdatesGridGlanceWidget)
}
