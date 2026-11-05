package app.hikari.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.hikari.ui.base.delegate.SecureActivityDelegate
import app.hikari.ui.base.delegate.SecureActivityDelegateImpl
import app.hikari.ui.base.delegate.ThemingDelegate
import app.hikari.ui.base.delegate.ThemingDelegateImpl
import app.hikari.util.system.prepareTabletUiContext
import app.hikari.app.di.appGraph

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun attachBaseContext(newBase: Context) {
        val uiPreferences = newBase.appGraph.uiPreferences
        super.attachBaseContext(newBase.prepareTabletUiContext(uiPreferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
