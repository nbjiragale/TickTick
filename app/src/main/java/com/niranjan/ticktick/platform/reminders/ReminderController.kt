package com.niranjan.ticktick.platform.reminders

import android.content.Context
import com.niranjan.ticktick.domain.repository.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.niranjan.ticktick.platform.sounds.AppSounds
import com.niranjan.ticktick.domain.model.AppSound
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReminderController(private val context: Context, private val tasks: TaskRepository,
    private val repository: ReminderRepository, private val habits: HabitRepository, private val clock: Clock, private val scope: CoroutineScope,
    private val sounds: AppSounds) {
    private val platform = ReminderPlatform(context, sounds)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val forceRequested = AtomicBoolean(false)
    private val reconcileLock = Mutex()
    private val capabilityState = MutableStateFlow(platform.capability(habits.snapshot.value.settings.countInAppBadge))
    val capability = capabilityState.asStateFlow()
    val reminderState = repository.reminderState
    @Volatile private var foreground = false
    private val mainVisibleState = MutableStateFlow(false)
    val mainVisible = mainVisibleState.asStateFlow()

    init {
        ReminderMaintenance.ensurePeriodic(context)
        scope.launch {
            for (request in requests) {
                try { reconcile(forceRequested.getAndSet(false)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    capabilityState.value = platform.capability(habits.snapshot.value.settings.countInAppBadge).copy(error = "Reminders need to be rescheduled. Tap Retry.")
                    ReminderMaintenance.retry(context)
                }
            }
        }
        requestReconcile(force = true)
        scope.launch {
            sounds.choices.map { it[AppSound.Notification] to it[AppSound.Popup] }.distinctUntilChanged()
                .collect { requestReconcile(force = true) }
        }
        scope.launch {
            var date = LocalDate.now(clock)
            var zone = clock.zone
            while (true) {
                delay(30_000)
                val currentDate = LocalDate.now(clock)
                val currentZone = clock.zone
                // In-app cards must not depend on a delayed inexact system alarm.
                if (foreground) requestReconcile(force = date != currentDate || zone != currentZone)
                date = currentDate
                zone = currentZone
            }
        }
    }

    fun requestReconcile(force: Boolean = false) {
        if (force) forceRequested.set(true)
        requests.trySend(Unit)
    }

    fun setForeground(value: Boolean) { foreground = value; mainVisibleState.value = value; requestReconcile(force = true) }

    private suspend fun awaitStore() {
        check(tasks.storeState.first { it != TaskStoreState.Loading } == TaskStoreState.Ready)
    }

    suspend fun reconcile(force: Boolean = false): Boolean = reconcileLock.withLock {
        awaitStore()
        sounds.awaitReady()
        val batch = repository.reconcileReminders(force)
        var applied = true
        // Retire obsolete alarms/notifications before registering replacements.
        batch.effects.sortedBy { if (it.alarmAt == null && !it.showNotification) 0 else 1 }.forEach { effect ->
            if (!repository.applyReminderEffect(effect.id, effect.revision) { platform.apply(effect, foreground) }) applied = false
        }
        platform.resumeAt(batch.resumeAt)
        platform.showMissedSummary(batch.missedCount, foreground)
        platform.showMissedHabits(batch.missedHabitCount, habits.snapshot.value.settings.countInAppBadge)
        capabilityState.value = platform.capability(habits.snapshot.value.settings.countInAppBadge)
        applied
    }

    suspend fun handle(action: String, id: String, revision: Long, snoozeUntil: Long = 0) {
        awaitStore()
        when (action) {
            "alarm" -> repository.fireReminder(id, revision)
            "done" -> repository.actOnReminder(id, revision, ReminderAction.Done)
            "dismiss" -> repository.actOnReminder(id, revision, ReminderAction.Dismiss)
            "snooze" -> {
                // A deferred retry must retain the time selected by the user, never start a fresh interval.
                if (snoozeUntil > 0) repository.actOnReminder(id, revision, ReminderAction.Snooze(Instant.ofEpochMilli(snoozeUntil), replay = true))
            }
        }
        reconcile(force = action == "reconcile")
    }
}
