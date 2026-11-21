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
        setContent { HyruleApp(model) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_update_notice", false)) model.checkForUpdates(manual = true)
    }
}
