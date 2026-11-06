package app.hikari.ui.base.delegate

import android.app.Activity
import app.hikari.R
import app.hikari.app.di.appGraph
import app.hikari.domain.ui.model.AppTheme

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_Hikari))
                if (isAmoled) add(R.style.ThemeOverlay_Hikari_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = activity.appGraph.uiPreferences
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme.get(), uiPreferences.themeDarkAmoled.get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Hikari_Monet,
)
