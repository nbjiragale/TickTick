package com.niranjan.ticktick.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskFilter
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class TasksUiState(
    val title: String = "Today",
    val filter: TaskFilter = TaskFilter.Today,
    val tasks: List<Task> = emptyList(),
    val snapshot: TaskSnapshot = TaskSnapshot(),
    val today: LocalDate = LocalDate.of(2026, 9, 5),
    val showCompleted: Boolean = false,
    val sortByTime: Boolean = false,
) {
    val todayCount get() = snapshot.tasks.count { !it.completed && it.dueDate == today }
    fun listCount(id: String) = snapshot.tasks.count { !it.completed && it.listId == id }
}

class TasksViewModel(
    private val repository: TaskRepository,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val selectedList = savedState.getStateFlow("taskFilter", "today")
    private val showCompleted = savedState.getStateFlow("showCompleted", false)
    private val sortByTime = savedState.getStateFlow("sortByTime", false)
    private val date = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(30_000)
        }
    }

    val uiState = combine(repository.snapshot, selectedList, showCompleted, sortByTime, date) { snapshot, list, completed, sort, today ->
        makeState(snapshot, list, completed, sort, today)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        makeState(repository.snapshot.value, selectedList.value, showCompleted.value, sortByTime.value, LocalDate.now(clock)),
    )

    private fun makeState(snapshot: TaskSnapshot, selection: String, completed: Boolean, sort: Boolean, today: LocalDate): TasksUiState {
        val filter = if (selection == "today") TaskFilter.Today else TaskFilter.ListFilter(selection)
        val title = if (filter == TaskFilter.Today) "Today" else snapshot.lists.find { it.id == selection }?.name ?: "Inbox"
        var tasks = snapshot.tasks.filter {
            (completed || !it.completed) && when (filter) {
                TaskFilter.Today -> it.dueDate == today
                is TaskFilter.ListFilter -> it.listId == filter.listId
            }
        }
        if (sort) tasks = tasks.sortedWith(compareBy<Task> { it.dueDate ?: LocalDate.MAX }.thenBy { it.dueTime ?: LocalTime.MAX })
        return TasksUiState(title, filter, tasks, snapshot, today, completed, sort)
    }

    fun selectToday() { savedState["taskFilter"] = "today" }
    fun selectList(id: String) { savedState["taskFilter"] = id }
    fun toggleShowCompleted() { savedState["showCompleted"] = !showCompleted.value }
    fun toggleSort() { savedState["sortByTime"] = !sortByTime.value }
    fun toggleTask(task: Task) = repository.setCompleted(task.id, !task.completed)
    fun moveToToday(task: Task) = repository.save(task.copy(dueDate = uiState.value.today))
    fun deleteTask(id: String) = repository.deleteTask(id)

    fun saveTask(id: String?, title: String, listId: String, dueDate: LocalDate?, dueTime: LocalTime?) {
        if (title.isBlank()) return
        val existing = repository.snapshot.value.tasks.find { it.id == id }
        repository.save(Task(id ?: UUID.randomUUID().toString(), title.trim(), listId, dueDate, dueTime, existing?.completed ?: false))
    }

    fun addList(name: String) {
        if (name.isNotBlank()) selectList(repository.addList(name))
    }
}
