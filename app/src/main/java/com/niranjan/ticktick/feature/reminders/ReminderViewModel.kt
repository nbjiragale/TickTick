package com.niranjan.ticktick.feature.reminders

import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.domain.repository.ReminderRepository
import com.niranjan.ticktick.domain.repository.ReminderAction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

enum class SnoozePage { Reminder, Options, Custom, ChangeDate }
enum class SnoozeChoice(val label: String, val minutes: Long? = null) {
    FifteenMinutes("15 mins", 15), ThirtyMinutes("30 mins", 30), OneHour("1 hour", 60), ThreeHours("3 hours", 180),
    Tomorrow("Tomorrow"), TodayNight("Today Night"), NextHour("Next Hour"),
}

private data class ReminderDelivery(val id: String, val taskId: String, val schedule: TaskSchedule, val snoozedUntil: Instant?, val revision: Long? = null, val taskRevision: Long = 0)

data class SnoozeConfirmation(val id: String, val message: String)

private data class ReminderSession(
    val deliveries: List<ReminderDelivery> = emptyList(),
    val page: SnoozePage = SnoozePage.Reminder,
    val hours: Int = 0,
    val minutes: Int = 30,
    val keyboard: Boolean = false,
    val error: String? = null,
)

data class ReminderUiState(
    val active: Boolean = false,
    val alertId: String? = null,
    val task: Task? = null,
    val list: TaskList? = null,
    val page: SnoozePage = SnoozePage.Reminder,
    val hours: Int = 0,
    val minutes: Int = 30,
    val keyboard: Boolean = false,
    val now: Instant = Instant.EPOCH,
    val error: String? = null,
) {
    val customMinutes get() = hours * 60L + minutes
    val customUntil get() = now.plus(customMinutes, ChronoUnit.MINUTES)
}

/** Shared card queue for durable deliveries and explicit debug previews. */
class ReminderViewModel(
    private val repository: TaskRepository,
    val clock: Clock,
    private val savedState: SavedStateHandle,
    private val reminders: ReminderRepository,
    private val requestedOnly: Boolean = false,
) : ViewModel() {
    private val session = MutableStateFlow(ReminderSession(
        hours = (savedState.get<Int>("snoozeHours") ?: 0).coerceIn(0, 23),
        minutes = (savedState.get<Int>("snoozeMinutes") ?: 30).coerceIn(0, 59),
        keyboard = savedState["snoozeKeyboard"] ?: false,
    ))
    private val delayedPreviews = mutableMapOf<String, Job>()
    private val confirmationState = MutableStateFlow<SnoozeConfirmation?>(null)
    val confirmation = confirmationState.asStateFlow()
    private var confirmationDismissal: Job? = null
    private val now = flow { while (true) { emit(clock.instant()); delay(1_000) } }
    val uiState = combine(repository.snapshot, session, now) { snapshot, current, instant ->
        val delivery = current.deliveries.firstOrNull()
        val task = delivery?.let { snapshot.eligibleTask(it.taskId) }
        ReminderUiState(active = delivery != null, alertId = delivery?.id, task = task,
            list = snapshot.lists.find { it.id == task?.listId }, page = current.page,
            hours = current.hours, minutes = current.minutes, keyboard = current.keyboard, now = instant, error = current.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderUiState(now = clock.instant()))

    init {
        viewModelScope.launch {
            combine(repository.snapshot, reminders.reminderState) { _, _ -> Unit }.collect { refreshQueue() }
        }
    }

    private fun refreshQueue() {
        val snapshot = repository.snapshot.value
        val durable = reminders.reminderState.value.alerts.mapNotNull { alert ->
            if (requestedOnly && (alert.missed || alert.id !in savedState.get<ArrayList<String>>("popupDeliveries").orEmpty())) return@mapNotNull null
            snapshot.eligibleTask(alert.taskId)?.let { task -> ReminderDelivery(alert.id, task.id, task.schedule,
                snapshot.snoozedUntil[task.id], alert.revision, task.revision) }
        }
        val previews = session.value.deliveries.filter { delivery -> delivery.revision == null &&
            durable.none { it.taskId == delivery.taskId } && snapshot.eligibleTask(delivery.taskId)?.schedule == delivery.schedule &&
            snapshot.snoozedUntil[delivery.taskId] == delivery.snoozedUntil }
        val all = durable + previews
        val order = session.value.deliveries.map { it.id }
        replaceQueue(all.sortedBy { order.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
    }

    /** Add a platform popup without interrupting another card's in-progress Snooze input. */
    fun enqueueDelivery(id: String, revision: Long): Boolean {
        val alert = reminders.reminderState.value.alerts.firstOrNull { it.id == id && it.revision == revision } ?: return false
        if (repository.snapshot.value.eligibleTask(alert.taskId) == null || requestedOnly && alert.missed) return false
        if (requestedOnly) savedState["popupDeliveries"] = ArrayList((savedState.get<ArrayList<String>>("popupDeliveries").orEmpty() + id).distinct())
        refreshQueue()
        return true
    }

    fun openDelivery(id: String?, revision: Long, changeDate: Boolean): Boolean {
        if (requestedOnly && id != null && !enqueueDelivery(id, revision)) return false
        refreshQueue()
        val target = if (id == null) session.value.deliveries.firstOrNull() else
            session.value.deliveries.find { it.id == id && it.revision == revision }
        if (target == null) return false
        replaceQueue(listOf(target) + session.value.deliveries.filterNot { it.id == target.id })
        page(if (changeDate) SnoozePage.ChangeDate else SnoozePage.Reminder)
        return true
    }

    private fun TaskSnapshot.eligibleTask(id: String) = tasks.find { it.id == id && it.isActive && !it.isNote && it.dueDate != null }

    private fun replaceQueue(deliveries: List<ReminderDelivery>) {
        val current = session.value
        publish(if (current.deliveries.firstOrNull()?.id != deliveries.firstOrNull()?.id)
            ReminderSession(deliveries = deliveries) else current.copy(deliveries = deliveries))
    }

    private fun publish(value: ReminderSession) {
        session.value = value
        savedState["snoozeHours"] = value.hours
        savedState["snoozeMinutes"] = value.minutes
        savedState["snoozeKeyboard"] = value.keyboard
    }

    fun deliver(id: String) {
        val snapshot = repository.snapshot.value
        val task = snapshot.eligibleTask(id) ?: return
        if (session.value.deliveries.any { it.taskId == id }) return
        val delivery = ReminderDelivery(UUID.randomUUID().toString(), id, task.schedule, snapshot.snoozedUntil[id], taskRevision = task.revision)
        replaceQueue(session.value.deliveries + delivery)
    }

    fun previewAfterDelay(id: String) {
        delayedPreviews.remove(id)?.cancel()
        delayedPreviews[id] = viewModelScope.launch {
            delay(10_000)
            delayedPreviews.remove(id)
            deliver(id)
        }
    }

    private fun removeDelivery(id: String) { replaceQueue(session.value.deliveries.filterNot { it.id == id }) }
    fun dismiss(alertId: String?) {
        val delivery = deliveryForAction(alertId) ?: return
        perform { taskWriteResult { act(delivery, ReminderAction.Dismiss) }.fold(
            onSuccess = { removeDelivery(delivery.id) },
            onFailure = { publish(session.value.copy(error = it.message)) },
        ) }
    }
    fun dismissOverlay() {
        if (session.value.page == SnoozePage.Reminder) dismiss(session.value.deliveries.firstOrNull()?.id)
        else page(SnoozePage.Reminder)
    }
    fun back() {
        when (session.value.page) {
            SnoozePage.Reminder -> dismissOverlay()
            SnoozePage.Options -> page(SnoozePage.Reminder)
            else -> page(SnoozePage.Options)
        }
    }
    fun page(page: SnoozePage) = publish(session.value.copy(page = page, error = null))
    fun setHours(value: Int) = publish(session.value.copy(hours = value.coerceIn(0, 23), error = null))
    fun setMinutes(value: Int) = publish(session.value.copy(minutes = value.coerceIn(0, 59), error = null))
    fun toggleKeyboard() = publish(session.value.copy(keyboard = !session.value.keyboard))

    private fun deliveryForAction(alertId: String?): ReminderDelivery? {
        val delivery = session.value.deliveries.firstOrNull()?.takeIf { it.id == alertId } ?: return null
        val snapshot = repository.snapshot.value
        val task = snapshot.eligibleTask(delivery.taskId) ?: return null
        return delivery.takeIf { task.schedule == it.schedule && snapshot.snoozedUntil[it.taskId] == it.snoozedUntil }
    }

    private var writing = false
    private fun perform(action: suspend () -> Unit) {
        if (writing) return
        writing = true
        viewModelScope.launch { try { action() } finally { writing = false } }
    }

    private suspend fun act(delivery: ReminderDelivery, action: ReminderAction) {
        if (delivery.revision != null) {
            require(reminders.actOnReminder(delivery.id, delivery.revision, action)) { "This reminder is no longer active." }
        } else when (action) {
            ReminderAction.Done -> repository.setCompleted(delivery.taskId, true, delivery.taskRevision)
            ReminderAction.Dismiss -> Unit
            is ReminderAction.Snooze -> repository.snooze(delivery.taskId, action.until)
            is ReminderAction.Reschedule -> repository.changeSchedule(delivery.taskId, action.schedule)
        }
    }

    fun complete(alertId: String?) {
        val delivery = deliveryForAction(alertId) ?: return
        perform { taskWriteResult { act(delivery, ReminderAction.Done) }.fold(
            onSuccess = { removeDelivery(delivery.id) },
            onFailure = { publish(session.value.copy(error = "Couldn't complete this task. Please try again.")) },
        ) }
    }

    fun choose(choice: SnoozeChoice, alertId: String?) {
        val delivery = deliveryForAction(alertId) ?: return
        val task = repository.snapshot.value.eligibleTask(delivery.taskId) ?: return
        commitSnooze(choice.deadline(clock.instant(), clock, task.dueTime ?: task.reminders.dateOnlyTime), delivery, choice.minutes)
    }
    fun confirmCustom(alertId: String?) {
        val delivery = deliveryForAction(alertId) ?: return
        val current = session.value
        val minutes = current.hours * 60L + current.minutes
        if (minutes == 0L) publish(current.copy(error = "Choose at least 1 minute."))
        else commitSnooze(clock.instant().plus(minutes, ChronoUnit.MINUTES), delivery, minutes)
    }
    private fun commitSnooze(until: Instant, delivery: ReminderDelivery, durationMinutes: Long?) {
        if (!until.isAfter(clock.instant())) {
            publish(session.value.copy(error = "That time has passed. Choose a later time.")); return
        }
        perform { taskWriteResult { act(delivery, ReminderAction.Snooze(until)) }.fold(
            onSuccess = {
                removeDelivery(delivery.id)
                showConfirmation(until.confirmationMessage(durationMinutes, clock))
            },
            onFailure = { publish(session.value.copy(error = "This task can no longer be snoozed.")) },
        ) }
    }
    private fun showConfirmation(message: String) {
        confirmationDismissal?.cancel()
        val notice = SnoozeConfirmation(UUID.randomUUID().toString(), message)
        confirmationState.value = notice
        confirmationDismissal = viewModelScope.launch {
            delay(2_000)
            if (confirmationState.value?.id == notice.id) confirmationState.value = null
        }
    }
    fun applySchedule(schedule: TaskSchedule, alertId: String?) {
        val delivery = deliveryForAction(alertId) ?: return
        schedule.validationError(clock)?.let { publish(session.value.copy(error = it)); return }
        perform { taskWriteResult { act(delivery, ReminderAction.Reschedule(schedule)) }.fold(
            onSuccess = { removeDelivery(delivery.id) },
            onFailure = { publish(session.value.copy(error = "This task can no longer be rescheduled.")) },
        ) }
    }
}

private fun Instant.confirmationMessage(durationMinutes: Long?, clock: Clock): String {
    if (durationMinutes != null) {
        val duration = buildList {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            if (hours > 0) add("$hours ${if (hours == 1L) "hour" else "hours"}")
            if (minutes > 0) add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
        }.joinToString(" and ")
        return "We'll remind you in $duration."
    }
    val local = atZone(clock.zone)
    val today = LocalDate.now(clock)
    val day = when (local.toLocalDate()) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> "on ${local.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH))}"
    }
    val time = local.format(java.time.format.DateTimeFormatter.ofPattern("h:mma", java.util.Locale.ENGLISH))
    return "We'll remind you $day at $time."
}

internal fun SnoozeChoice.deadline(now: Instant, clock: Clock, originalTime: LocalTime): Instant {
    minutes?.let { return now.plus(it, ChronoUnit.MINUTES) }
    val local = now.atZone(clock.zone)
    return when (this) {
        SnoozeChoice.Tomorrow -> local.toLocalDate().plusDays(1).atTime(originalTime).atZone(clock.zone).toInstant()
        SnoozeChoice.TodayNight -> local.toLocalDate().atTime(21, 0).atZone(clock.zone).toInstant()
        SnoozeChoice.NextHour -> local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant()
        else -> error("Snooze duration missing")
    }
}

internal fun Instant.snoozeLabel(clock: Clock): String {
    val local = atZone(clock.zone)
    val today = LocalDate.now(clock)
    val day = when (local.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> local.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH))
    }
    return "$day ${local.format(java.time.format.DateTimeFormatter.ofPattern("h:mma", java.util.Locale.ENGLISH))}"
}
