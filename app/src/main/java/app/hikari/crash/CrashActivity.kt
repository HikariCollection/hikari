package app.hikari.crash

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import app.hikari.presentation.crash.CrashScreen
import app.hikari.ui.base.activity.BaseActivity
import app.hikari.ui.main.MainActivity
import app.hikari.util.view.setComposeContent

class CrashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setComposeContent {
            CrashScreen(
                exception = exception,
                onRestartClick = {
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                },
            )
        }
    }
}
