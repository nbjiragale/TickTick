package com.niranjan.ticktick.platform.focus

import android.app.*
import android.content.*
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.niranjan.ticktick.MainActivity
import com.niranjan.ticktick.R
import com.niranjan.ticktick.app.TickTickApplication
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class FocusController(private val context: Context, private val repository: FocusRepository, private val scope: CoroutineScope, private val restoring: () -> Boolean = { false }) {
    private val lock = Mutex()
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val pending get() = PendingIntent.getBroadcast(context, 401,
        Intent(context, FocusReceiver::class.java).setAction("focus-complete"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private val error = MutableStateFlow<String?>(null)
    val deliveryError = error.asStateFlow()
    init {
        scope.launch { repository.session.collect {
            if (repository.storeState.value == TaskStoreState.Ready) {
                try { apply() } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error.value = "Focus completion needs rescheduling. Tap Retry." }
            }
        } }
        scope.launch {
            repository.storeState.first { it == TaskStoreState.Ready }
            while (true) {
                try { reconcile() } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error.value = "Focus completion needs rescheduling. Tap Retry." }
                delay(30_000)
            }
        }
    }
    fun retry() { scope.launch {
        try { reconcile() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error.value = "Focus completion needs rescheduling. Tap Retry." }
    } }
    suspend fun reconcile() {
        if (restoring()) return
        check(repository.storeState.first { it != TaskStoreState.Loading } == TaskStoreState.Ready)
        repository.reconcile(); apply()
    }
    private suspend fun apply() = lock.withLock {
        if (restoring()) return@withLock
        val value = repository.session.value
        alarms.cancel(pending)
        if (value.phase == FocusPhase.Running && value.mode == FocusMode.Pomo) {
            val now = SystemClock.elapsedRealtime()
            val deadline = now + (value.durationMinutes * 60_000L - value.elapsed(now)).coerceAtLeast(1)
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                try { alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending) }
                catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending) }
            } else alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pending)
        }
        if (value.completionPending) {
            val manager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("focus_completion", "Focus completion", NotificationManager.IMPORTANCE_DEFAULT))
            val intent = PendingIntent.getActivity(context, 402, Intent(context, MainActivity::class.java).putExtra("open_focus", true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (manager.areNotificationsEnabled()) {
                try {
                    manager.notify("focus", 401, NotificationCompat.Builder(context, "focus_completion")
                        .setSmallIcon(R.drawable.ic_reminder_notification).setContentTitle("Focus session complete")
                        .setContentText("${value.durationMinutes} minutes focused").setContentIntent(intent)
                        .setAutoCancel(true).setOnlyAlertOnce(true).build())
                } catch (_: SecurityException) { /* Completion history remains available without notification access. */ }
            }
            repository.acknowledgeCompletion(value.id)
        } else if (value.phase != FocusPhase.Finished) NotificationManagerCompat.from(context).cancel("focus", 401)
        error.value = null
    }
}
class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                try { WorkManager.getInstance(context).enqueueUniqueWork("focus-recovery", ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<FocusWorker>().setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()).result.get(2, TimeUnit.SECONDS) }
                catch (_: Exception) { /* A slow work queue must not suppress immediate handling. */ }
                withTimeout(6_000) { (context.applicationContext as TickTickApplication).container.focusController.reconcile() }
            } catch (_: Exception) { /* Durable worker, periodic maintenance and app resume recover. */ }
            finally { result.finish() }
        }
    }
}
class FocusWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        (applicationContext as TickTickApplication).container.focusController.reconcile(); Result.success()
    } catch (e: CancellationException) { throw e } catch (_: Exception) { Result.retry() }
}
