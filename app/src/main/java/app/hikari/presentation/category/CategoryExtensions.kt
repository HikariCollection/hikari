package app.hikari.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import app.hikari.core.common.i18n.stringResource
import app.hikari.domain.category.model.Category
import app.hikari.i18n.MR
import app.hikari.presentation.core.i18n.stringResource

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

fun Category.visualName(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }
