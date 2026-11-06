package app.hikari.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.hikari.R
import app.hikari.data.coil.InstalledExtensionIcon
import app.hikari.data.coil.InstalledSourceIcon
import app.hikari.domain.source.model.Source
import app.hikari.extension.model.Extension
import app.hikari.icons.materialsymbols.MaterialSymbols
import app.hikari.icons.materialsymbols.roundedfilled.Dangerous
import app.hikari.icons.materialsymbols.roundedfilled.Warning
import app.hikari.presentation.util.rememberResourceBitmapPainter
import app.hikari.source.builtin.BuiltinSources
import app.hikari.source.local.isLocal
import coil3.compose.AsyncImage

private val defaultModifier = Modifier
    .height(40.dp)
    .aspectRatio(1f)

@Composable
fun SourceIcon(
    source: Source,
    modifier: Modifier = Modifier,
) {
    val builtinIconRes = when (source.id) {
        BuiltinSources.shinigami.id -> R.mipmap.ic_source_shinigami
        BuiltinSources.wurmz.id -> R.mipmap.ic_source_wurmz
        BuiltinSources.mgKomik.id -> R.mipmap.ic_source_mgkomik
        BuiltinSources.arenaScans.id -> R.mipmap.ic_source_arenascans
        else -> null
    }
    if (builtinIconRes != null) {
        Image(
            painter = painterResource(builtinIconRes),
            contentDescription = null,
            modifier = modifier.then(defaultModifier),
        )
        return
    }

    when {
        source.isStub -> {
            Image(
                imageVector = MaterialSymbols.RoundedFilled.Warning,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                modifier = modifier.then(defaultModifier),
            )
        }
        source.isLocal() -> {
            Image(
                painter = painterResource(R.mipmap.ic_local_source),
                contentDescription = null,
                modifier = modifier.then(defaultModifier),
            )
        }
        else -> {
            val defaultIcon = painterResource(R.mipmap.ic_default_source)
            AsyncImage(
                model = InstalledSourceIcon(source.id),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = modifier.then(defaultModifier),
            )
        }
    }
}

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
    density: Int = DisplayMetrics.DENSITY_DEFAULT,
) {
    if (extension is Extension.Loaded) {
        val builtinIconRes = when (extension.pkgName) {
            "app.hikari.extension.id.shinigami" -> R.mipmap.ic_source_shinigami
            "app.hikari.extension.id.wurmz" -> R.mipmap.ic_source_wurmz
            "app.hikari.extension.id.mgkomik" -> R.mipmap.ic_source_mgkomik
            "app.hikari.extension.en.arenascans" -> R.mipmap.ic_source_arenascans
            else -> null
        }
        if (builtinIconRes != null) {
            Image(
                painter = painterResource(builtinIconRes),
                contentDescription = null,
                modifier = modifier,
            )
            return
        }
    }

    when (extension) {
        is Extension.Available -> {
            AsyncImage(
                model = extension.iconUrl,
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
        is Extension.Loaded -> {
            val defaultIcon = painterResource(R.mipmap.ic_default_source)
            AsyncImage(
                model = InstalledExtensionIcon(extension.pkgName, extension.versionCode, density),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = modifier,
            )
        }
        is Extension.NotLoaded -> Image(
            imageVector = MaterialSymbols.RoundedFilled.Dangerous,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
            modifier = modifier.then(defaultModifier),
        )
    }
}
