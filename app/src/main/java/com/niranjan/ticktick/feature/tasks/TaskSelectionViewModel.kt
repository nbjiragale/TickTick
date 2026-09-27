package com.niranjan.ticktick.feature.tasks

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import com.niranjan.ticktick.domain.repository.taskWriteResult
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.DeletedTasks
import com.niranjan.ticktick.domain.repository.TaskRepository
import com.niranjan.ticktick.domain.nlp.TaskDateParser
import com.niranjan.ticktick.feature.taskeditor.editorToken
import com.niranjan.ticktick.feature.taskeditor.tokenSpans
import com.niranjan.ticktick.feature.taskeditor.titleWithoutListTokens
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TaskSelectionState(val active: Boolean = false, val tasks: List<Task> = emptyList(), val canUndo: Boolean = false) {
    val ids get() = tasks.map { it.id }.toSet()
}

class TaskSelectionViewModel(private val repository: TaskRepository, val clock: Clock, private val saved: SavedStateHandle) : ViewModel() {
    private val active = saved.getStateFlow("selectingTasks", false)
    private val ids = saved.getStateFlow("selectedTaskIds", arrayListOf<String>())
    private val deleted = MutableStateFlow<DeletedTasks?>(null)
    val uiState = combine(active, ids, repository.snapshot, deleted) { selecting, selected, snapshot, undo ->
        TaskSelectionState(selecting, snapshot.tasks.filter { it.id in selected }, undo?.tasks?.isNotEmpty() == true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskSelectionState())

    private val error = MutableStateFlow<String?>(null)
    private val writing = MutableStateFlow(false)
    val writeError = error.asStateFlow()
    val busy = writing.asStateFlow()
    fun clearWriteError() { error.value = null }
    private fun perform(closeAfter: Boolean = true, action: suspend () -> Unit) {
        if (writing.value) return
        writing.value = true
        viewModelScope.launch {
            try {
                val result = taskWriteResult(action)
                writing.value = false
                result.fold(onSuccess = { if (closeAfter) close() }, onFailure = { error.value = it.message })
            } finally { writing.value = false }
        }
    }

    fun start(id: String? = null) {
        if (writing.value) return
        saved["selectedTaskIds"] = arrayListOf<String>().apply { if (id != null) add(id) }
        saved["selectingTasks"] = true
    }
    fun toggle(id: String) {
        if (writing.value) return
        saved["selectedTaskIds"] = ArrayList(ids.value.let { if (id in it) it - id else it + id })
    }
    fun selectAll(tasks: List<Task>) { if (!writing.value) saved["selectedTaskIds"] = ArrayList(tasks.map { it.id }.distinct()) }
    fun close() { if (writing.value) return; saved["selectingTasks"] = false; saved["selectedTaskIds"] = arrayListOf<String>() }
    private fun selected() = repository.snapshot.value.tasks.filter { it.id in ids.value }
    fun update(transform: (Task) -> Task) = perform { repository.saveTasks(selected().map(transform)) }
    fun setDate(date: LocalDate?) = update { task ->
        if (date == null) task.copy(dueDate = null, dueTime = null, duration = null, reminders = ReminderConfig(), repeat = RepeatRule())
        else task.copy(dueDate = date, duration = task.duration?.let {
            it.copy(startDate = date, endDate = date.plusDays(ChronoUnit.DAYS.between(it.startDate, it.endDate)))
        })
    }
    fun setSchedule(schedule: TaskSchedule) = update { it.copy(dueDate = schedule.date, dueTime = schedule.time,
        duration = schedule.duration, reminders = schedule.reminders, repeat = schedule.repeat) }
    fun move(listId: String) = update { task ->
        val lists = repository.snapshot.value.lists
        val target = lists.first { it.id == listId }
        val source = lists.find { it.id == task.listId }
        val title = source?.tokenSpans(task.title).orEmpty().asReversed().fold(task.title) { text, span ->
            text.replaceRange(span.start, span.end, target.editorToken)
        }
        task.copy(listId = listId, title = title)
    }
    suspend fun createList(name: String, color: Int?, view: TaskListView): Result<Unit> = taskWriteResult {
        repository.addList(name, color, view)
        Unit
    }
    fun skipRecurrence() {
        val expected = uiState.value.tasks.associate { it.id to it.revision }
        perform { repository.skipOccurrences(expected) }
    }
    fun done() {
        val tasks = uiState.value.tasks
        perform { repository.saveTasks(tasks.map { it.copy(completed = true, declined = false) }) }
    }
    fun pin() {
        val pin = !selected().all { it.pinned }
        update { it.copy(pinned = pin) }
    }
    fun priority(priority: TaskPriority) = update { it.copy(priority = priority) }
    fun tags(changes: Map<String, Boolean>, additional: String) = perform {
        val names = additional.split(',', ' ', '\n').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
        require(names.all { Regex("[\\p{L}\\p{N}_-]+").matches(it) }) { "Use letters, numbers, underscores or hyphens for tags." }
        repository.saveTasks(selected().map { task ->
            val retained = task.tags.filter { tag -> changes.entries.none { !it.value && it.key.equals(tag, true) } }
            val source = titleWithoutListTokens(task.title, repository.snapshot.value.lists.find { it.id == task.listId })
            val removed = TaskDateParser(clock).tags(source).filter { tag -> changes.entries.any { !it.value && it.key.equals(tag.name, true) } }
            val title = removed.asReversed().fold(task.title) { text, tag -> text.removeRange(tag.span.start, tag.span.end) }.trim()
                .ifBlank { task.title.replace("#", "") }
            task.copy(title = title, tags = (retained + changes.filterValues { it }.keys + names).distinctBy { it.lowercase(java.util.Locale.ROOT) })
        })
    }
    fun convert() {
        val toNote = !selected().all { it.isNote }
        update { it.copy(isNote = toNote) }
    }
    fun duplicate() = perform {
        repository.saveTasks(selected().map { it.copy(id = UUID.randomUUID().toString(), completed = false, declined = false,
            pinned = false, createdAt = null, modifiedAt = null, revision = 0,
            checklist = it.checklist.map { item -> item.copy(id = UUID.randomUUID().toString(), completed = false) },
            attachments = it.attachments.map { attachment -> attachment.copy(id = UUID.randomUUID().toString()) }) })
    }
    fun delete() = perform {
        val batch = repository.deleteTasks(selected().map { it.id }.toSet())
        if (batch.tasks.isNotEmpty()) deleted.value = batch
    }
    fun undo() = perform(closeAfter = false) {
        deleted.value?.let { repository.restoreTasks(it) }
        deleted.value = null
    }
}
