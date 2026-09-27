package com.niranjan.ticktick.platform.reminders

import android.content.Context
import androidx.work.*
import com.niranjan.ticktick.app.TickTickApplication
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/** WorkManager repairs scheduling; AlarmManager owns reminder deadlines. */
object ReminderMaintenance {
    fun ensurePeriodic(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("reminder-maintenance", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build())
    }

    fun retry(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-reconcile", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderWorker>().setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
    }

    fun event(context: Context, action: String, id: String, revision: Long, snoozeUntil: Long): Operation {
        val data = workDataOf("action" to action, "id" to id, "revision" to revision, "snoozeUntil" to snoozeUntil)
        return WorkManager.getInstance(context).enqueueUniqueWork("reminder-$action-$id-$revision", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(data).build())
    }
}

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val controller = (applicationContext as TickTickApplication).container.reminderController
        val action = inputData.getString("action")
        if (action != null) controller.handle(action, inputData.getString("id").orEmpty(),
            inputData.getLong("revision", -1), inputData.getLong("snoozeUntil", 0))
        else controller.reconcile(force = true)
        // Missing user permissions are not a transient worker failure. Resume refresh retries them.
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { Result.retry() }
}
