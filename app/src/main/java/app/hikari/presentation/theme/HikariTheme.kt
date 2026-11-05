package app.hikari.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.hikari.domain.ui.model.AppTheme
import app.hikari.presentation.theme.colorscheme.BaseColorScheme
import app.hikari.presentation.theme.colorscheme.CatppuccinColorScheme
import app.hikari.presentation.theme.colorscheme.GreenAppleColorScheme
import app.hikari.presentation.theme.colorscheme.LavenderColorScheme
import app.hikari.presentation.theme.colorscheme.MidnightDuskColorScheme
import app.hikari.presentation.theme.colorscheme.MonetColorScheme
import app.hikari.presentation.theme.colorscheme.MonochromeColorScheme
import app.hikari.presentation.theme.colorscheme.NordColorScheme
import app.hikari.presentation.theme.colorscheme.StrawberryColorScheme
import app.hikari.presentation.theme.colorscheme.TachiyomiColorScheme
import app.hikari.presentation.theme.colorscheme.TakoColorScheme
import app.hikari.presentation.theme.colorscheme.TealTurqoiseColorScheme
import app.hikari.presentation.theme.colorscheme.TidalWaveColorScheme
import app.hikari.presentation.theme.colorscheme.TokyoNightColorScheme
import app.hikari.presentation.theme.colorscheme.YinYangColorScheme
import app.hikari.presentation.theme.colorscheme.YotsubaColorScheme
import app.hikari.app.di.appGraph

@Composable
fun HikariTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    BaseHikariTheme(
        appTheme = appTheme ?: uiPreferences.appTheme.get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
        content = content,
    )
}

@Composable
fun HikariPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseHikariTheme(appTheme, isAmoled, content)

@Composable
private fun BaseHikariTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled) {
            getThemeColorScheme(
                context = context,
                appTheme = appTheme,
                isDark = isDark,
                isAmoled = isAmoled,
            )
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
