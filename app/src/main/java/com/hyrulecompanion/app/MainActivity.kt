package com.hyrulecompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hyrulecompanion.app.updates.AppUpdateScheduler
import com.hyrulecompanion.app.ui.HyruleApp

class MainActivity : ComponentActivity() {
    private val model: HyruleViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUpdateScheduler.schedule(applicationContext)
        if (intent?.getBooleanExtra("open_update_notice", false) == true) model.checkForUpdates(manual = true)
        handleEmailConfirmation(intent)
        setContent { HyruleApp(model) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailConfirmation(intent)
        if (intent.getBooleanExtra("open_update_notice", false)) model.checkForUpdates(manual = true)
    }

    private fun handleEmailConfirmation(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != android.content.Intent.ACTION_VIEW ||
            uri.scheme != BuildConfig.APPLICATION_ID || uri.host != "auth" || uri.path != "/confirm") return
        val code = uri.getQueryParameter("code")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,256}")) } ?: return
        intent.data = null
        model.confirmEmail(code)
    }
}
