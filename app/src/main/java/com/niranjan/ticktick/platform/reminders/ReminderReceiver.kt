package com.niranjan.ticktick.platform.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niranjan.ticktick.app.TickTickApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val system = intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED, NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED)
        val action = if (system) "reconcile" else intent.action ?: return
        if (action !in setOf("alarm", "done", "dismiss", "snooze", "reconcile")) return
        val id = intent.getStringExtra(EXTRA_DELIVERY).orEmpty()
        val revision = intent.getLongExtra(EXTRA_REVISION, -1)
        val snoozeUntil = if (action == "snooze") System.currentTimeMillis() + 15 * 60_000L else 0
        // Persist retry work before handling the event. A repeated command is harmless by delivery revision.
        val pending = goAsync()
        receiverScope.launch {
            try {
                // Wait off the main thread for WorkManager's durable enqueue acknowledgement.
                try { ReminderMaintenance.event(context, action, id, revision, snoozeUntil).result.get(3, TimeUnit.SECONDS) }
                catch (_: Exception) { /* A slow/unavailable work queue must not prevent immediate alarm delivery. */ }
                withTimeout(5_000) {
                    (context.applicationContext as TickTickApplication).container.reminderController.handle(action, id, revision, snoozeUntil)
                }
            } catch (_: Exception) { /* Enqueued work (or periodic reconciliation) recovers interrupted delivery. */ }
            finally { pending.finish() }
        }
    }

    companion object { private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
