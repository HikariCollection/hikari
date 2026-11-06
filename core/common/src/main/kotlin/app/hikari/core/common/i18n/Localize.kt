package app.hikari.core.common.i18n

import android.content.Context
import app.hikari.i18n.PluralsResource
import app.hikari.i18n.StringResource

fun Context.stringResource(resource: StringResource): String {
    return getString(resource.resourceId)
}

fun Context.stringResource(resource: StringResource, vararg args: Any): String {
    return getString(resource.resourceId, *args)
}

fun Context.pluralStringResource(resource: PluralsResource, count: Int): String {
    return resources.getQuantityString(resource.resourceId, count)
}

fun Context.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String {
    return resources.getQuantityString(resource.resourceId, count, *args)
}
