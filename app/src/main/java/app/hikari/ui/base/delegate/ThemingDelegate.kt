package app.hikari.ui.base.delegate

import android.app.Activity
import app.hikari.domain.ui.model.AppTheme
import app.hikari.R
import app.hikari.app.di.appGraph

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
    AppTheme.CATPPUCCIN to R.style.Theme_Hikari_Catppuccin,
    AppTheme.TOKYONIGHT to R.style.Theme_Hikari_TokyoNight,
    AppTheme.GREEN_APPLE to R.style.Theme_Hikari_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_Hikari_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_Hikari_MidnightDusk,
    AppTheme.MONOCHROME to R.style.Theme_Hikari_Monochrome,
    AppTheme.NORD to R.style.Theme_Hikari_Nord,
    AppTheme.STRAWBERRY_DAIQUIRI to R.style.Theme_Hikari_StrawberryDaiquiri,
    AppTheme.TAKO to R.style.Theme_Hikari_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_Hikari_TealTurquoise,
    AppTheme.YINYANG to R.style.Theme_Hikari_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_Hikari_Yotsuba,
    AppTheme.TIDAL_WAVE to R.style.Theme_Hikari_TidalWave,
)
