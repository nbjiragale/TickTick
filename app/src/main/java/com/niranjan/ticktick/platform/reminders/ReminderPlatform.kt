package com.niranjan.ticktick.platform.reminders

import android.media.AudioAttributes
import java.security.MessageDigest
import android.Manifest
import android.app.AlarmManager
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.niranjan.ticktick.MainActivity
import com.niranjan.ticktick.R
import com.niranjan.ticktick.domain.model.AppSound
import com.niranjan.ticktick.platform.sounds.AppSounds
import com.niranjan.ticktick.domain.repository.ReminderEffect
import java.time.Instant

data class ReminderCapability(val notifications: Boolean = false, val channelEnabled: Boolean = false,
    val exactAlarms: Boolean = false, val habitChannelEnabled: Boolean = true,
    val overOtherApps: Boolean = false, val fullScreen: Boolean = false,
    val taskHeadsUp: Boolean = false, val habitHeadsUp: Boolean = false,
    val taskPopupHeadsUp: Boolean = false, val habitPopupHeadsUp: Boolean = false,
    val interruptionsAllowed: Boolean = false,
    val habitChannelId: String = "", val taskChannelId: String = REMINDER_CHANNEL,
    val taskPopupChannelId: String = "", val habitPopupChannelId: String = "", val error: String? = null)

internal const val REMINDER_CHANNEL = "task_reminders"
internal const val EXTRA_DELIVERY = "reminder_delivery"
internal const val EXTRA_REVISION = "reminder_revision"
internal const val EXTRA_PAGE = "reminder_page"

internal class ReminderPlatform(private val context: Context, private val sounds: AppSounds) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Due tasks and snoozed reminders"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            })
    }

    private fun reminderChannel(habit: Boolean, popup: Boolean = false, badge: Boolean = false): String {
        val sound = sounds.current(if (popup) AppSound.Popup else AppSound.Notification).uri
        val hash = MessageDigest.getInstance("SHA-256").digest("${sound ?: "silent"}|$badge".toByteArray())
            .take(8).joinToString("") { "%02x".format(it) }
        // Preserve the original default channel and its Android overrides where possible.
        val id = if (!habit && !popup && sound == "") REMINDER_CHANNEL
            else "${if (habit) "habit" else "task"}_${if (popup) "popups" else "reminders"}_$hash"
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(
            NotificationChannel(id, "${if (habit) "Habit" else "Task"} ${if (popup) "popups" else "reminders"}", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sound selected in TickTick Settings"
                setSound(sounds.uri(sound), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                setShowBadge(if (habit) badge else true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            })
        return id
    }
    fun capability(badge: Boolean = false): ReminderCapability {
        val taskChannel = reminderChannel(false)
        val habitChannel = reminderChannel(true, badge = badge)
        val taskPopupChannel = reminderChannel(false, popup = true)
        val habitPopupChannel = reminderChannel(true, popup = true, badge = badge)
        return ReminderCapability(
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED),
            channelEnabled = channelEnabled(taskChannel),
            exactAlarms = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms(),
            habitChannelEnabled = channelEnabled(habitChannel),
            overOtherApps = Settings.canDrawOverlays(context),
            fullScreen = Build.VERSION.SDK_INT < 34 || notifications.canUseFullScreenIntent(),
            taskHeadsUp = channelCanInterrupt(taskChannel),
            habitHeadsUp = channelCanInterrupt(habitChannel),
            taskPopupHeadsUp = channelCanInterrupt(taskPopupChannel),
            habitPopupHeadsUp = channelCanInterrupt(habitPopupChannel),
            interruptionsAllowed = notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL,
            habitChannelId = habitChannel, taskChannelId = taskChannel,
            taskPopupChannelId = taskPopupChannel,
            habitPopupChannelId = habitPopupChannel,
        )
    }

    private fun channelEnabled(id: String): Boolean = Build.VERSION.SDK_INT < 26 ||
        (notifications.getNotificationChannel(id)?.importance ?: 0) > NotificationManager.IMPORTANCE_NONE

    private fun channelCanInterrupt(id: String): Boolean = Build.VERSION.SDK_INT < 26 ||
        (notifications.getNotificationChannel(id)?.importance ?: 0) >= NotificationManager.IMPORTANCE_HIGH

    private fun popupIntent(effect: ReminderEffect) = Intent(context, ReminderPopupActivity::class.java)
        .setData(Uri.Builder().scheme("ticktick").authority("popup").appendPath(effect.id).build())
        .putExtra(EXTRA_DELIVERY, effect.id).putExtra(EXTRA_REVISION, effect.revision)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun fullScreenIntent(effect: ReminderEffect): PendingIntent {
        // This immutable, explicit intent is handed only to the notification system.
        // A due alarm must be launchable when our app has no visible activity.
        val options = if (Build.VERSION.SDK_INT >= 35) ActivityOptions.makeBasic().apply {
            @Suppress("DEPRECATION")
            setPendingIntentCreatorBackgroundActivityStartMode(
                if (Build.VERSION.SDK_INT >= 36) ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }.toBundle() else null
        return PendingIntent.getActivity(context, 0, popupIntent(effect),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE, options)
    }

    private fun broadcast(id: String, revision: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
            .setData(Uri.Builder().scheme("ticktick").authority("reminder").appendPath(action).appendPath(id).build())
            .putExtra(EXTRA_DELIVERY, id).putExtra(EXTRA_REVISION, revision)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun open(id: String?, revision: Long = 0, page: String = "reminder"): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(Uri.Builder().scheme("ticktick").authority("open-reminder").appendPath(id ?: "all").appendPath(page).build())
            .putExtra(EXTRA_DELIVERY, id).putExtra(EXTRA_REVISION, revision).putExtra(EXTRA_PAGE, page)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun schedule(at: Instant?, intent: PendingIntent) {
        alarms.cancel(intent)
        if (at == null) return
        if (capability().exactAlarms) {
            try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent); return }
            catch (_: SecurityException) { /* Access may change before registration; use the visible degraded mode. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
    }

    fun resumeAt(at: Instant?) = schedule(at, broadcast("resume", 0, "reconcile"))

    fun apply(effect: ReminderEffect, foreground: Boolean): Boolean {
        schedule(effect.alarmAt, broadcast(effect.id, effect.revision, "alarm"))
        if (!effect.showNotification || foreground && (effect.habit == null || effect.autoPopUp) || effect.missed) {
            notifications.cancel(effect.id, 1)
            if (foreground && effect.showNotification && !effect.missed && effect.alertAgain &&
                effect.habit == null) sounds.play(AppSound.Popup)
            return true
        }
        val capability = capability(effect.showBadge)
        if (!capability.notifications || if (effect.habit == null) !capability.channelEnabled else !capability.habitChannelEnabled) return false
        val notificationChannel = reminderChannel(effect.habit != null, badge = effect.showBadge)
        // Maintenance/permission refreshes must not reopen an already delivered popup.
        // Respect quiet channels and Do Not Disturb even when overlay access is granted.
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
            !context.getSystemService(PowerManager::class.java).isInteractive
        val popupChannel = reminderChannel(effect.habit != null, popup = true, badge = effect.showBadge)
        val popupEligible = (effect.habit == null || effect.autoPopUp) && channelCanInterrupt(notificationChannel) &&
            channelCanInterrupt(popupChannel) && capability.interruptionsAllowed &&
            (capability.fullScreen || !locked && capability.overOtherApps)
        val popup = effect.alertAgain && popupEligible
        // The notification owns background audio, including popup audio: never play both.
        val channel = if (popupEligible) popupChannel else notificationChannel
        val sound = sounds.current(if (popupEligible) AppSound.Popup else AppSound.Notification).uri
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_reminder_notification)
            .setContentTitle(effect.title).setContentText(effect.dueLabel)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(!effect.alertAgain).setAutoCancel(false)
            .setContentIntent(open(effect.id, effect.revision))
            .setDeleteIntent(broadcast(effect.id, effect.revision, "dismiss"))
        // A newly posted notification after leaving the foreground is still an update,
        // not a new alarm. ONLY_ALERT_ONCE alone does not silence its first post.
        if (!effect.alertAgain) builder.setSilent(true)
        if (Build.VERSION.SDK_INT < 26) builder.setSound(sounds.uri(sound))
        if (popup && capability.fullScreen) {
            // Android chooses full-screen vs heads-up at presentation time. A lock-state
            // snapshot here can already be stale when SystemUI handles the notification.
            builder.setFullScreenIntent(fullScreenIntent(effect), true)
        }
        if (effect.habit == null) builder
            .addAction(0, "Done", broadcast(effect.id, effect.revision, "done"))
            .addAction(0, "Snooze 15 min", broadcast(effect.id, effect.revision, "snooze"))
            .addAction(0, "Change date", open(effect.id, effect.revision, "change_date"))
        else {
            if (effect.quickCheckIn) builder.addAction(0, "Check in", broadcast(effect.id, effect.revision, "done"))
            builder.addAction(0, "Open habit", open(effect.id, effect.revision))
            builder.addAction(0, "Dismiss", broadcast(effect.id, effect.revision, "dismiss"))
            builder.setBadgeIconType(if (effect.showBadge) NotificationCompat.BADGE_ICON_SMALL else NotificationCompat.BADGE_ICON_NONE)
        }
        val notification = builder.build()
        try { notifications.notify(effect.id, 1, notification) } catch (_: SecurityException) { return false }
        if (popup && !locked && capability.overOtherApps) {
            // User-granted SYSTEM_ALERT_WINDOW is Android's documented background-activity
            // exception. A translucent activity reuses the complete card/Snooze UI safely.
            try { context.startActivity(popupIntent(effect)) }
            catch (_: SecurityException) { /* OEM/access restrictions: the notification remains usable. */ }
            catch (_: android.content.ActivityNotFoundException) { /* Keep the notification fallback. */ }
        }
        return true
    }

    fun showMissedSummary(count: Int, foreground: Boolean) {
        if (count == 0 || foreground) { notifications.cancel("missed", 2); return }
        val capability = capability()
        if (!capability.notifications || !capability.channelEnabled) return
        // Catch-up is one silent summary; opening it shows the existing card queue.
        val notification = NotificationCompat.Builder(context, reminderChannel(false))
            .setSmallIcon(R.drawable.ic_reminder_notification).setContentTitle("Missed reminders")
            .setContentText("$count reminders to review").setNumber(count).setSilent(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open(null)).build()
        try { notifications.notify("missed", 2, notification) } catch (_: SecurityException) { }
    }

    fun showMissedHabits(count: Int, badge: Boolean) {
        if (count == 0) { notifications.cancel("habit_missed", 2); return }
        val capability = capability(badge)
        if (!capability.notifications || !capability.habitChannelEnabled) return
        val notification = NotificationCompat.Builder(context, reminderChannel(true, badge = badge))
            .setSmallIcon(R.drawable.ic_reminder_notification).setContentTitle("Missed habit reminders")
            .setContentText("$count reminders to review").setSilent(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open(null, page = "habit_summary")).build()
        try { notifications.notify("habit_missed", 2, notification) } catch (_: SecurityException) { }
    }
}
