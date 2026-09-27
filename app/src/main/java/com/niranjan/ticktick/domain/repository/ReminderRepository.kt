package com.niranjan.ticktick.domain.repository

import com.niranjan.ticktick.domain.model.TaskSchedule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

data class AlertPause(val resumeDate: LocalDate? = null)
data class ReminderAlert(val id: String, val taskId: String, val occurrenceId: String,
    val revision: Long, val missed: Boolean)
data class ReminderState(val pause: AlertPause = AlertPause(), val alerts: List<ReminderAlert> = emptyList())
data class ReminderEffect(val id: String, val taskId: String, val revision: Long, val alarmAt: Instant?,
    val showNotification: Boolean, val missed: Boolean, val title: String, val dueLabel: String,
    val alertAgain: Boolean, val habit: com.niranjan.ticktick.domain.model.HabitReminderTarget? = null,
    val soundUri: String? = "", val autoPopUp: Boolean = false, val quickCheckIn: Boolean = true, val showBadge: Boolean = false)
data class ReminderBatch(val effects: List<ReminderEffect>, val resumeAt: Instant?, val missedCount: Int, val missedHabitCount: Int = 0)
sealed interface ReminderAction {
    data object Done : ReminderAction
    data object Dismiss : ReminderAction
    data class Snooze(val until: Instant, val replay: Boolean = false) : ReminderAction
    data class Reschedule(val schedule: TaskSchedule) : ReminderAction
}

interface ReminderRepository {
    val reminderState: StateFlow<ReminderState>
    suspend fun importAlertPause(legacy: AlertPause?)
    suspend fun setAlertPause(pause: AlertPause)
    suspend fun reconcileReminders(force: Boolean = false): ReminderBatch
    suspend fun applyReminderEffect(id: String, revision: Long, apply: () -> Boolean): Boolean
    suspend fun fireReminder(id: String, revision: Long)
    suspend fun actOnReminder(id: String, revision: Long, action: ReminderAction): Boolean
}
