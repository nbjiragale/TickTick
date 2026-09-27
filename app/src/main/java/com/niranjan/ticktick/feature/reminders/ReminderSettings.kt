package com.niranjan.ticktick.feature.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.platform.reminders.ReminderController

@Composable
fun ReminderSettings(controller: ReminderController) {
    val context = LocalContext.current
    val capability by controller.capability.collectAsStateWithLifecycle()
    val reminderState by controller.reminderState.collectAsStateWithLifecycle()
    var openError by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { controller.requestReconcile(force = true) }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { controller.requestReconcile(force = true) }
    fun open(intent: Intent) {
        try { openError = null; settings.launch(intent) }
        catch (_: Exception) { openError = "Open Android Settings and find TickTick under Apps or Special app access to change this permission." }
    }
    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        Text("Task reminders", style = MaterialTheme.typography.titleSmall)
        reminderState.pause.resumeDate?.takeIf { it > java.time.LocalDate.now() }?.let { date ->
            Text("Reminders are paused until $date. Turn on the drawer bell to resume them now.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text(when {
            !capability.notifications -> "Notifications are blocked. Allow them for reminders outside the app."
            !capability.channelEnabled -> "The task reminder notification channel is off."
            !capability.exactAlarms -> "Notifications are on. Reminder timing may be delayed."
            else -> "Notifications and exact reminder timing are available."
        }, style = MaterialTheme.typography.bodySmall)
        if (!capability.habitChannelEnabled) Text("Habit notifications are blocked in Android notification settings.", style = MaterialTheme.typography.bodySmall)
        Text(if (capability.overOtherApps) "Reminder popups over other apps are allowed."
            else "Notifications can arrive without a popup. Allow display over other apps to show the reminder card while using another app.",
            style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }) { Text(if (capability.overOtherApps) "Popup permission settings" else "Allow display over other apps") }
        if (Build.VERSION.SDK_INT >= 34) {
            Text(if (capability.fullScreen) "Full-screen lock-screen alerts are allowed."
                else "Lock-screen popups need full-screen alert access. Notifications still work without it.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
            }) { Text("Lock-screen popup settings") }
        }
        Text("For habits, also enable Auto pop-up of habit log in the habit editor. Silent channels, Do Not Disturb and phone settings can prevent popups.",
            style = MaterialTheme.typography.bodySmall)
        if (!capability.interruptionsAllowed) Text("Do Not Disturb is preventing automatic reminder popups.",
            style = MaterialTheme.typography.bodySmall)
        if (Build.VERSION.SDK_INT >= 26) {
            if (!capability.taskHeadsUp || !capability.habitHeadsUp) Text(
                "A reminder channel is silent or has low importance. Choose Alerting and enable Pop on screen where available.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, capability.taskChannelId))
            }) { Text("Task notification channel") }
            TextButton(onClick = { open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, capability.habitChannelId))
            }) { Text("Habit notification channel") }
        }
        if (Build.VERSION.SDK_INT >= 26) {
            if (!capability.taskPopupHeadsUp) Text("The task popup channel is off or has low importance. Enable it and choose Alerting / Pop on screen in Task popup channel.",
                style = MaterialTheme.typography.bodySmall)
            if (!capability.habitPopupHeadsUp) Text("The habit popup channel is off or has low importance. Enable it and choose Alerting / Pop on screen in Habit popup channel.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, capability.taskPopupChannelId))
            }) { Text("Task popup channel") }
            TextButton(onClick = { open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, capability.habitPopupChannelId))
            }) { Text("Habit popup channel") }
        }
        Text("Constant reminders repeat every 15 minutes, up to four follow-ups. Missed reminders are grouped silently.",
            style = MaterialTheme.typography.bodySmall)
        if (Build.VERSION.SDK_INT >= 33 && !capability.notifications) TextButton(onClick = {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }) { Text("Allow notifications") }
        TextButton(onClick = {
            open(if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Notification settings") }
        if (Build.VERSION.SDK_INT >= 31 && !capability.exactAlarms) TextButton(onClick = {
            open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }) { Text("Allow exact reminders") }
        capability.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { controller.requestReconcile(force = true) }) { Text("Retry") }
        }
        openError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
