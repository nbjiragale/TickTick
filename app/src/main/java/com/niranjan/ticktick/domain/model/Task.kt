package com.niranjan.ticktick.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val listId: String = "inbox",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val completed: Boolean = false,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.None,
    val tags: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val attachments: List<TaskAttachment> = emptyList(),
    val isNote: Boolean = false,
    val reminders: ReminderConfig = ReminderConfig(),
    val repeat: RepeatRule = RepeatRule(),
    val duration: TaskDuration? = null,
    val declined: Boolean = false,
    val createdAt: Instant? = null,
    val modifiedAt: Instant? = null,
    val pinned: Boolean = false,
    val revision: Long = 0,
    val recurrence: TaskRecurrence? = null,
) {
    val isActive get() = !completed && !declined
    val hasReminder get() = dueDate != null && reminders.offsetsMinutes.isNotEmpty()
    val schedule get() = TaskSchedule(dueDate, dueTime, duration, reminders, repeat)
}

enum class TaskPriority { High, Medium, Low, None }
data class ChecklistItem(val id: String, val text: String, val completed: Boolean = false)
data class TaskAttachment(val id: String, val name: String, val uri: String, val mimeType: String, val sizeBytes: Long? = null)

enum class TaskListView { List, Kanban }
data class TaskList(val id: String, val name: String, val symbol: ListSymbol, val color: Int? = null, val view: TaskListView = TaskListView.List)
data class TaskTag(val name: String, val color: Int? = null)
enum class ListSymbol { Inbox, Work, Personal, Welcome, Custom }
data class TaskSnapshot(
    val tasks: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    // Durable reminder state is separate from editor drafts and due dates.
    val snoozedUntil: Map<String, Instant> = emptyMap(),
    val tags: List<TaskTag> = emptyList(),
    val filters: List<SavedTaskFilter> = emptyList(),
    val occurrenceHistory: List<TaskOccurrenceHistory> = emptyList(),
) {
    val knownTags get() = (tags + tasks.flatMap { it.tags }.map { TaskTag(it) }).distinctBy { it.name.lowercase(java.util.Locale.ROOT) }
}

sealed interface TaskFilter {
    data object Today : TaskFilter
    data class ListFilter(val listId: String) : TaskFilter
    data class TagFilter(val name: String) : TaskFilter
    data class SavedFilter(val id: String) : TaskFilter
}
