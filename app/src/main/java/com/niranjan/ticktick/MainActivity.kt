package com.niranjan.ticktick

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.niranjan.ticktick.platform.reminders.*
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.niranjan.ticktick.app.TickTickApp
import com.niranjan.ticktick.app.TickTickApplication
import com.niranjan.ticktick.core.designsystem.TickTickTheme

class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableStateOf<ReminderOpenRequest?>(null)
    private val container get() = (application as TickTickApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.getBoolean("reminderRequestHandled") != true) readReminderIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TickTickTheme {
                TickTickApp(container, reminderRequest) {
                    reminderRequest = null
                    intent.removeExtra(EXTRA_PAGE)
                    intent.removeExtra(EXTRA_DELIVERY)
                    intent.removeExtra(EXTRA_REVISION)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readReminderIntent(intent)
    }

    private fun readReminderIntent(intent: Intent) {
        if (intent.hasExtra(EXTRA_PAGE)) reminderRequest = ReminderOpenRequest(
            intent.getStringExtra(EXTRA_DELIVERY), intent.getLongExtra(EXTRA_REVISION, -1), intent.getStringExtra(EXTRA_PAGE) == "change_date",
            habitSummary = intent.getStringExtra(EXTRA_PAGE) == "habit_summary")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("reminderRequestHandled", reminderRequest == null)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        container.reminderController.setForeground(true)
    }

    override fun onPause() {
        container.reminderController.setForeground(false)
        super.onPause()
    }
}
