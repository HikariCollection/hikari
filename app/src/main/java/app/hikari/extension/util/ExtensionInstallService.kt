package app.hikari.extension.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import app.hikari.R
import app.hikari.core.common.i18n.stringResource
import app.hikari.data.notification.Notifications
import app.hikari.extension.installer.Installer
import app.hikari.extension.installer.PackageInstallerInstaller
import app.hikari.extension.util.ExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import app.hikari.i18n.MR
import app.hikari.util.system.notificationBuilder

class ExtensionInstallService : Service() {

    private var installer: Installer? = null

    override fun onCreate() {
        val notification = notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.ic_hikari)
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setContentTitle(stringResource(MR.strings.ext_install_service_notif))
            setProgress(100, 100, true)
        }.build()
        startForeground(Notifications.ID_EXTENSION_INSTALLER, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.takeIf { it != -1L }
        if (uri == null || id == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (installer == null) {
            installer = PackageInstallerInstaller(this)
        }
        installer!!.addToQueue(id, uri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
        ): Intent {
            return Intent(context, ExtensionInstallService::class.java)
                .setDataAndType(uri, ExtensionInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
    }
}
