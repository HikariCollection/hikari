package app.hikari.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import app.hikari.app.di.appGraph
import app.hikari.data.track.TrackerManager
import app.hikari.presentation.core.screens.LoadingScreen
import app.hikari.ui.base.activity.BaseActivity
import app.hikari.ui.main.MainActivity
import app.hikari.util.view.setComposeContent
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject

@HasMemberInjections
abstract class BaseOAuthLoginActivity : BaseActivity() {

    @Inject protected lateinit var trackerManager: TrackerManager

    abstract fun handleResult(uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appGraph.inject(this)

        setComposeContent {
            LoadingScreen()
        }

        val data = intent.data
        if (data == null) {
            returnToSettings()
        } else {
            handleResult(data)
        }
    }

    internal fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
