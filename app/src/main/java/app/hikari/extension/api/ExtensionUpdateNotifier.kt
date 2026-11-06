package app.hikari.extension.api

import android.content.Context
import androidx.core.app.NotificationCompat
import app.hikari.core.common.i18n.pluralStringResource
import app.hikari.core.security.SecurityPreferences
import app.hikari.data.notification.NotificationReceiver
import app.hikari.data.notification.Notifications
import app.hikari.i18n.MR
import app.hikari.util.system.cancelNotification
import app.hikari.util.system.notify
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import app.hikari.icons.materialsymbols.R as MaterialSymbolsR

@Inject
@SingleIn(AppScope::class)
class ExtensionUpdateNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences,
) {
    fun promptUpdates(names: List<String>) {
        context.notify(
            Notifications.ID_UPDATES_TO_EXTS,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(
                context.pluralStringResource(
                    MR.plurals.update_check_notification_ext_updates,
                    names.size,
                    names.size,
                ),
            )
            if (!securityPreferences.hideNotificationContent.get()) {
                val extNames = names.joinToString(", ")
                setContentText(extNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
            }
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_extension)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }

    fun dismiss() {
        context.cancelNotification(Notifications.ID_UPDATES_TO_EXTS)
    }
}
