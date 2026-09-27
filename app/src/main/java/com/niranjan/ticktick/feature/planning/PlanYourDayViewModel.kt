package com.niranjan.ticktick.feature.planning

import kotlinx.coroutines.launch
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.domain.repository.UiStateRepository
import com.niranjan.ticktick.domain.repository.PreferenceCommit
import org.json.JSONObject
import org.json.JSONArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class PlanningMenu { Today, Later }
enum class TodayPeriod(val label: String, val hour: Int) {
    Morning("Morning", 10), Afternoon("Afternoon", 13), Evening("Evening", 17), Night("Night", 21),
}
enum class LaterDay(val label: String, val days: Long) {
    Tomorrow("Tomorrow", 1), ThreeDays("3 days later", 3), NextWeek("Next week", 7),
}

private data class PlanningSession(
    val active: Boolean = false,
    val date: LocalDate? = null,
    val candidates: List<String> = emptyList(),
    val reviewed: Set<String> = emptySet(),
    val occurrences: Map<String, String> = emptyMap(),
    val selectedId: String? = null,
    val menuTaskId: String? = null,
    val menu: PlanningMenu? = null,
    val deleteId: String? = null,
    val error: String? = null,
)

data class PlanYourDayState(
    val active: Boolean = false,
    val cards: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val selectedId: String? = null,
    val total: Int = 0,
    val resolved: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val hour: Int = 9,
    val menuTaskId: String? = null,
    val menu: PlanningMenu? = null,
    val deleteId: String? = null,
    val error: String? = null,
) {
    val selectedIndex get() = cards.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
}

/** Session review order is stable; task values always come from the shared repository. */
class PlanYourDayViewModel(private val repository: TaskRepository, val clock: Clock, private val preferences: UiStateRepository) : ViewModel() {
    private val session = MutableStateFlow(restorePlanning(preferences.snapshot.value.preferences["planning"], LocalDate.now(clock)))
    private fun publish(next: PlanningSession, store: Boolean = true) {
        val previous = session.value
        session.value = next
        val encoded = next.encode()
        if (store && encoded != previous.encode()) persist(encoded)
    }
    private fun persist(encoded: String) {
        viewModelScope.launch {
            taskWriteResult { preferences.updatePreference("planning") { encoded } }
                .onFailure { session.value = session.value.copy(error = "Couldn't save planning progress. Tap OK to retry.") }
        }
    }
    private val time = MutableStateFlow(LocalDateTime.now(clock))
    fun refreshTime() {
        if (session.value.active && session.value.date != LocalDate.now(clock)) restart()
        time.value = LocalDateTime.now(clock)
    }
    init { viewModelScope.launch { while (true) { refreshTime(); delay(30_000) } } }
    val uiState = combine(repository.snapshot, session, time, ::makeState).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        makeState(repository.snapshot.value, session.value, LocalDateTime.now(clock)),
    )

    private fun pending(snapshot: TaskSnapshot, current: PlanningSession): List<Task> {
        val tasks = snapshot.tasks.associateBy { it.id }
        return current.candidates.filterNot { it in current.reviewed }.mapNotNull { tasks[it] }
            .filter { it.isActive && !it.isNote && (current.occurrences[it.id] == null || current.occurrences[it.id] == it.recurrence?.occurrenceId) }
    }

    private fun makeState(snapshot: TaskSnapshot, current: PlanningSession, now: LocalDateTime): PlanYourDayState {
        val cards = pending(snapshot, current)
        val originalIndex = current.candidates.indexOf(current.selectedId)
        val selected = cards.find { it.id == current.selectedId }
            ?: cards.firstOrNull { current.candidates.indexOf(it.id) >= originalIndex } ?: cards.lastOrNull()
        return PlanYourDayState(current.active, cards, snapshot.lists, selected?.id,
            current.candidates.size, current.candidates.size - cards.size, now.toLocalDate(), now.hour,
            current.menuTaskId?.takeIf { id -> cards.any { it.id == id } },
            current.menu?.takeIf { cards.any { it.id == current.menuTaskId } },
            current.deleteId?.takeIf { id -> cards.any { it.id == id } }, current.error)
    }

    fun open() {
        if (session.value.date == LocalDate.now(clock)) publish(session.value.copy(active = true))
        else restart()
    }

    fun restart() {
        if (writing) return
        val ids = repository.snapshot.value.tasks.filter { it.isActive && !it.isNote }
            .sortedWith(compareBy<Task> { it.dueDate ?: LocalDate.MAX }.thenBy { it.dueTime ?: LocalTime.MAX })
            .map { it.id }
        publish(PlanningSession(active = true, date = LocalDate.now(clock), candidates = ids, occurrences = repository.snapshot.value.tasks.mapNotNull { task -> task.recurrence?.occurrenceId?.let { task.id to it } }.toMap(), selectedId = ids.firstOrNull()))
    }

    fun close() { if (writing) return; publish(session.value.copy(active = false, menuTaskId = null, menu = null, deleteId = null, error = null)) }
    fun select(id: String) {
        if (writing) return
        if (taskForAction(id) == null) return
        if (session.value.selectedId != id) cancelMenu()
        publish(session.value.copy(selectedId = id))
    }
    fun clearError() { publish(session.value.copy(error = null)); if (!writing) persist(session.value.encode()) }
    fun cancelMenu() { publish(session.value.copy(menuTaskId = null, menu = null, error = null)) }
    fun back() { if (session.value.menu != null) cancelMenu() else close() }
    fun cancelDelete() { publish(session.value.copy(deleteId = null)) }

    private fun taskForAction(id: String): Task? = repository.snapshot.value.tasks.find {
        session.value.active && session.value.date == LocalDate.now(clock) &&
            (session.value.occurrences[id] == null || session.value.occurrences[id] == it.recurrence?.occurrenceId) && it.id == id && id in session.value.candidates && id !in session.value.reviewed && it.isActive && !it.isNote
    }

    private var writing = false
    private fun commit(id: String, operation: suspend (Task, PreferenceCommit) -> Unit) {
        if (writing) return
        val task = taskForAction(id) ?: return
        val cards = pending(repository.snapshot.value, session.value)
        val index = cards.indexOfFirst { it.id == id }
        writing = true
        val remaining = cards.filterNot { it.id == id }
        val next = session.value.copy(reviewed = session.value.reviewed + id,
            selectedId = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex).coerceAtLeast(0))?.id,
            menuTaskId = null, menu = null, deleteId = null, error = null)
        viewModelScope.launch {
        try { taskWriteResult {
            // Drain earlier navigation writes before committing the task and review checkpoint together.
            preferences.updatePreference("planning") { session.value.encode() }
            operation(task, PreferenceCommit("planning", next.encode()))
        }.fold(
            onSuccess = { publish(next, store = false) },
            onFailure = { publish(session.value.copy(error = "Couldn't update this task. Please try again.")) },
        ) } finally { writing = false }
        }
    }

    fun done(id: String) = commit(id) { task, checkpoint -> repository.setCompleted(task.id, true, task.revision, checkpoint) }
    fun wontDo(id: String) = commit(id) { task, checkpoint -> repository.setDeclined(task.id, true, checkpoint) }
    fun requestDelete(id: String) { if (taskForAction(id) != null) publish(session.value.copy(deleteId = id)) }
    fun confirmDelete() { session.value.deleteId?.let { id -> commit(id) { task, checkpoint -> repository.deleteTask(task.id, checkpoint = checkpoint) } } }

    fun today(id: String) = showMenu(id, PlanningMenu.Today)
    fun later(id: String) = showMenu(id, PlanningMenu.Later)

    private fun showMenu(id: String, menu: PlanningMenu) {
        if (taskForAction(id) != null) publish(session.value.copy(selectedId = id, menuTaskId = id, menu = menu, error = null))
    }

    fun chooseToday(id: String, period: TodayPeriod) {
        if (session.value.menu != PlanningMenu.Today || session.value.menuTaskId != id) return
        val task = taskForAction(id) ?: return
        applySchedule(id, task.scheduleOn(LocalDate.now(clock), LocalTime.of(period.hour, 0)))
    }

    fun chooseLater(id: String, day: LaterDay) {
        if (session.value.menu != PlanningMenu.Later || session.value.menuTaskId != id) return
        val task = taskForAction(id) ?: return
        applySchedule(id, task.scheduleOn(LocalDate.now(clock).plusDays(day.days)))
    }

    private fun applySchedule(id: String, schedule: TaskSchedule) {
        val error = schedule.validationError(clock)
        if (error != null) publish(session.value.copy(error = error))
        else commit(id) { task, checkpoint -> repository.changeSchedule(task.id, schedule, checkpoint) }
    }
}

private fun Task.scheduleOn(date: LocalDate, time: LocalTime? = null): TaskSchedule {
    val shifted = duration?.let {
        if (time == null) it.copy(startDate = date, endDate = date.plusDays(ChronoUnit.DAYS.between(it.startDate, it.endDate)))
        else {
            // A named Today time becomes the interval start; preserve its elapsed length.
            val length = if (it.allDay) Duration.ofDays(ChronoUnit.DAYS.between(it.startDate, it.endDate) + 1)
                else Duration.between(it.startDate.atTime(it.startTime), it.endDate.atTime(it.endTime))
            val end = date.atTime(time).plus(length)
            it.copy(startDate = date, startTime = time, endDate = end.toLocalDate(), endTime = end.toLocalTime(), allDay = false)
        }
    }
    return TaskSchedule(date, time ?: dueTime, shifted, reminders, repeat)
}

private fun Task.withSchedule(schedule: TaskSchedule) = copy(dueDate = schedule.date, dueTime = schedule.time,
    duration = schedule.duration, reminders = schedule.reminders, repeat = schedule.repeat)

private fun PlanningSession.encode(): String = JSONObject().put("date", date?.toString())
    .put("candidates", JSONArray(candidates)).put("reviewed", JSONArray(reviewed.toList()))
    .put("selected", selectedId).put("occurrences", JSONObject(occurrences)).toString()

private fun restorePlanning(json: String?, today: LocalDate): PlanningSession {
    if (json == null) return PlanningSession()
    val j = JSONObject(json)
    if (j.optString("date") != today.toString()) return PlanningSession()
    fun strings(key: String): List<String> = j.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
    val occurrences = j.optJSONObject("occurrences") ?: JSONObject()
    return PlanningSession(date = today, candidates = strings("candidates"), reviewed = strings("reviewed").toSet(),
        selectedId = j.optString("selected").takeIf { it.isNotEmpty() },
        occurrences = occurrences.keys().asSequence().associateWith { occurrences.getString(it) })
}
