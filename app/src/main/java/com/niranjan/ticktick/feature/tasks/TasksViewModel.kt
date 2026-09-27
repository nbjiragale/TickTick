package com.niranjan.ticktick.feature.tasks

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.domain.repository.ReminderRepository
import com.niranjan.ticktick.domain.repository.AlertPause
import com.niranjan.ticktick.domain.repository.UiStateRepository
import org.json.JSONObject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.model.TaskListView
import com.niranjan.ticktick.domain.model.FilterRule
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HomeTaskSection(val label: String) {
    Overdue("Overdue"), Today("Today"), Upcoming("Upcoming");

    fun includes(date: LocalDate?, today: LocalDate): Boolean = date != null && when (this) {
        Overdue -> date < today
        Today -> date == today
        Upcoming -> date > today
    }
}

data class TasksUiState(
    val title: String = "Today",
    val filter: TaskFilter = TaskFilter.Today,
    val tasks: List<Task> = emptyList(),
    val snapshot: TaskSnapshot = TaskSnapshot(),
    val today: LocalDate = LocalDate.of(2026, 9, 5),
    val showCompleted: Boolean = false,
    val sortByTime: Boolean = false,
    val homeSection: HomeTaskSection = HomeTaskSection.Today,
    val presentation: TaskPresentation = TaskPresentation(),
    val groups: List<TaskGroup> = emptyList(),
    val dailyAlertsEnabled: Boolean = false,
    val alertsResumeDate: LocalDate? = null,
    val calendarShowCompleted: Boolean = false,
    val zoneId: String = "",
) {
    val todayCount get() = snapshot.tasks.count { it.isActive && it.dueDate == today }
    fun listCount(id: String) = snapshot.tasks.count { it.isActive && it.listId == id }
}

class TasksViewModel(
    private val repository: TaskRepository,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
    private val reminders: ReminderRepository,
    private val preferences: UiStateRepository,
) : ViewModel() {
    private val navigation = preferences.snapshot.value.preferences["taskNavigation"]?.let(::JSONObject)
    private val selectedList = savedState.getStateFlow("taskFilter", navigation?.optString("filter", "today") ?: "today")
    private val homeSection = savedState.getStateFlow("homeTaskSection", navigation?.optString("section", HomeTaskSection.Today.name) ?: HomeTaskSection.Today.name)
    private val selection = combine(selectedList, homeSection) { list, section -> list to section }
    private val date = MutableStateFlow(LocalDate.now(clock) to clock.zone)
    fun refreshTime() { date.value = LocalDate.now(clock) to clock.zone }
    private val dailyPreferences = combine(date, reminders.reminderState) { today, reminder -> today to reminder.pause.resumeDate }

    val uiState = combine(repository.snapshot, selection, preferences.snapshot, dailyPreferences) { snapshot, selected, stored, daily ->
        val options = taskPresentation(stored.preferences["tasks:${selected.first}"], defaultPresentation(selected.first))
        makeState(snapshot, selected.first, options.showCompleted, daily.first.first, selected.second, options)
            .withAlertPause(daily.second).copy(calendarShowCompleted = stored.preferences["calendar"]?.let(::JSONObject)?.optBoolean("showCompleted") ?: false)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        taskPresentation(preferences.snapshot.value.preferences["tasks:${selectedList.value}"], defaultPresentation(selectedList.value)).let { options ->
            makeState(repository.snapshot.value, selectedList.value, options.showCompleted, LocalDate.now(clock), homeSection.value, options)
                .withAlertPause(reminders.reminderState.value.pause.resumeDate)
                .copy(calendarShowCompleted = preferences.snapshot.value.preferences["calendar"]?.let(::JSONObject)?.optBoolean("showCompleted") ?: false)
        },
    )

    private fun TasksUiState.withAlertPause(storedDate: LocalDate?): TasksUiState {
        val resumeDate = storedDate?.takeIf { it > today }
        return copy(dailyAlertsEnabled = resumeDate == null, alertsResumeDate = resumeDate)
    }

    private fun defaultPresentation(selection: String) = TaskPresentation(
        layout = if (repository.snapshot.value.lists.find { it.id == selection }?.view == TaskListView.Kanban) TaskLayout.Kanban else TaskLayout.List,
        grouping = if (selection == "today") TaskGrouping.Date else TaskGrouping.None)

    private fun makeState(snapshot: TaskSnapshot, selection: String, completed: Boolean, today: LocalDate, sectionName: String, options: TaskPresentation): TasksUiState {
        val filter = when {
            selection == "today" -> TaskFilter.Today
            selection.startsWith("tag:") -> TaskFilter.TagFilter(selection.removePrefix("tag:"))
            selection.startsWith("filter:") -> TaskFilter.SavedFilter(selection.removePrefix("filter:"))
            else -> TaskFilter.ListFilter(selection)
        }
        val section = HomeTaskSection.entries.find { it.name == sectionName } ?: HomeTaskSection.Today
        val title = when (filter) {
            TaskFilter.Today -> "Today"
            is TaskFilter.ListFilter -> snapshot.lists.find { it.id == filter.listId }?.name ?: "Inbox"
            is TaskFilter.TagFilter -> "#${filter.name}"
            is TaskFilter.SavedFilter -> snapshot.filters.find { it.id == filter.id }?.name ?: "Filter"
        }
        var tasks = snapshot.tasks.filter {
            (completed || it.isActive) && when (filter) {
                TaskFilter.Today -> section.includes(it.dueDate, today)
                is TaskFilter.ListFilter -> it.listId == filter.listId
                is TaskFilter.TagFilter -> it.tags.any { tag -> tag.equals(filter.name, true) }
                is TaskFilter.SavedFilter -> snapshot.filters.find { saved -> saved.id == filter.id }?.matches(it, today) == true
            }
        }
        tasks = sortTasks(tasks, options)
        return TasksUiState(title, filter, tasks, snapshot, today, completed, options.sorting == TaskSorting.Date, section,
            options, groupTasks(tasks, snapshot, options, today, clock.zone), zoneId = clock.zone.id)
    }

    fun selectToday() {
        savedState["homeTaskSection"] = HomeTaskSection.Today.name
        savedState["taskFilter"] = "today"
        persistNavigation()
    }
    fun selectHomeSection(section: HomeTaskSection) { savedState["homeTaskSection"] = section.name; persistNavigation() }
    fun selectList(id: String) { savedState["taskFilter"] = id; persistNavigation() }
    fun selectTag(name: String) { savedState["taskFilter"] = "tag:$name"; persistNavigation() }
    fun selectFilter(id: String) { savedState["taskFilter"] = "filter:$id"; persistNavigation() }
    fun toggleShowCompleted() = updatePresentation { it.copy(showCompleted = !it.showCompleted) }
    fun toggleCalendarCompleted() = writePreference {
        preferences.updatePreference("calendar") { old -> (old?.let(::JSONObject) ?: JSONObject()).let { it.put("showCompleted", !it.optBoolean("showCompleted")).toString() } }
    }
    private fun persistNavigation() {
        val json = JSONObject().put("filter", selectedList.value).put("section", homeSection.value).toString()
        writePreference { preferences.updatePreference("taskNavigation") { json } }
    }
    fun setDailyAlertsEnabled(enabled: Boolean) = writeTask {
        reminders.setAlertPause(AlertPause(if (enabled) null else LocalDate.now(clock).plusDays(1)))
    }
    suspend fun pauseAlertsForDays(days: Int): Result<Unit> = taskWriteResult {
        require(days in 1..3650) { "Enter a whole number from 1 to 3650 days." }
        reminders.setAlertPause(AlertPause(LocalDate.now(clock).plusDays(days.toLong())))
    }
    fun toggleSort() = updatePresentation { it.copy(sorting = if (it.sorting == TaskSorting.Date) TaskSorting.Created else TaskSorting.Date) }
    fun updatePresentation(transform: (TaskPresentation) -> TaskPresentation) {
        val id = selectedList.value
        writePreference { preferences.updatePreference("tasks:$id") { transform(taskPresentation(it, defaultPresentation(id))).encode() } }
    }
    private fun writePreference(action: suspend () -> Unit) {
        viewModelScope.launch { taskWriteResult(action).onFailure { error.value = it.message ?: "Couldn't save this preference." } }
    }
    private val error = MutableStateFlow<String?>(null)
    val writeError = error.asStateFlow()
    private var writing = false
    init {
        viewModelScope.launch {
            while (true) {
                refreshTime()
                val midnight = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).toInstant()
                delay(java.time.Duration.between(clock.instant(), midnight).toMillis().coerceIn(1L, 30_000L))
            }
        }
        viewModelScope.launch {
            val legacy = when {
                savedState.contains("alertsResumeDate") -> AlertPause(savedState.get<String>("alertsResumeDate")?.takeIf { it.isNotBlank() }?.let(LocalDate::parse))
                savedState.contains("enabledAlertsDate") -> AlertPause(if (savedState.get<String>("enabledAlertsDate") == LocalDate.now(clock).toString()) null else LocalDate.now(clock).plusDays(1))
                else -> null
            }
            taskWriteResult { reminders.importAlertPause(legacy) }.onFailure { error.value = it.message }
        }
    }
    fun clearWriteError() { error.value = null }
    private fun writeTask(action: suspend () -> Unit) {
        if (writing) return
        writing = true
        viewModelScope.launch {
            try { taskWriteResult(action).onFailure { error.value = it.message } }
            finally { writing = false }
        }
    }
    fun toggleTask(task: Task) = writeTask { repository.setCompleted(task.id, task.isActive, task.revision) }
    fun moveToToday(task: Task) = writeTask {
        val today = uiState.value.today
        val range = task.duration?.let {
            val days = java.time.temporal.ChronoUnit.DAYS.between(it.startDate, it.endDate)
            it.copy(startDate = today, endDate = today.plusDays(days))
        }
        repository.save(task.copy(dueDate = today, duration = range))
    }
    fun deleteTask(id: String) = writeTask { repository.deleteTask(id) }

    suspend fun addList(name: String, color: Int?, view: TaskListView) = taskWriteResult { selectList(repository.addList(name, color, view)) }
    suspend fun addTag(name: String, color: Int?) = taskWriteResult { repository.addTag(name, color); selectTag(name.trim().removePrefix("#")) }
    suspend fun addFilter(name: String, rules: List<FilterRule>, matchAll: Boolean) = taskWriteResult { selectFilter(repository.addFilter(name, rules, matchAll)) }
}
