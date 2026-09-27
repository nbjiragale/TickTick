package com.niranjan.ticktick.feature.tasks

import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskSnapshot
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TaskLayout(val label: String) { List("List View"), Kanban("Kanban View") }
enum class TaskGrouping(val label: String) { List("List"), Date("Date"), Created("Created Time"), Tag("Tag"), Priority("Priority"), None("None") }
enum class TaskSorting(val label: String) { Date("Date"), Created("Created Time"), Modified("Modified Time"), Title("Title"), Tag("Tag"), Priority("Priority") }
enum class TaskBackdrop(val label: String) { None("None"), Color("Color"), Gradient("Gradient"), Image("Image") }

data class TaskPresentation(
    val layout: TaskLayout = TaskLayout.List,
    val details: Boolean = false,
    val grouping: TaskGrouping = TaskGrouping.Date,
    val sorting: TaskSorting = TaskSorting.Date,
    val descending: Boolean = false,
    val backdrop: TaskBackdrop = TaskBackdrop.None,
    val swatch: Int = 0,
    val imageUri: String? = null,
    val showCompleted: Boolean = false,
) : Serializable

data class TaskGroup(val key: String, val label: String, val tasks: List<Task>)

fun sortTasks(tasks: List<Task>, options: TaskPresentation): List<Task> {
    val comparator = when (options.sorting) {
        TaskSorting.Date -> compareBy<Task, LocalDate?>(nullsLast()) { it.dueDate }
            .thenBy(nullsLast<LocalTime>()) { it.dueTime }
        TaskSorting.Created -> compareBy(nullsLast()) { task: Task -> task.createdAt }
        TaskSorting.Modified -> compareBy(nullsLast()) { task: Task -> task.modifiedAt }
        TaskSorting.Title -> compareBy<Task> { it.title.lowercase(Locale.ROOT) }
        TaskSorting.Tag -> compareBy(nullsLast<String>()) { task: Task -> task.tags.minOfOrNull { it.lowercase(Locale.ROOT) } }
        TaskSorting.Priority -> compareBy<Task> { it.priority.ordinal }
    }
    // Undated/untagged tasks stay last in either direction.
    val (missing, present) = tasks.partition { task -> when (options.sorting) {
        TaskSorting.Date -> task.dueDate == null
        TaskSorting.Tag -> task.tags.isEmpty()
        TaskSorting.Priority -> task.priority == com.niranjan.ticktick.domain.model.TaskPriority.None
        else -> false
    } }
    return (present.sortedWith((if (options.descending) comparator.reversed() else comparator).thenBy { it.id }) + missing)
        .sortedByDescending { it.pinned }
}

fun groupTasks(tasks: List<Task>, snapshot: TaskSnapshot, options: TaskPresentation, today: LocalDate, zone: ZoneId): List<TaskGroup> {
    val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    fun dateLabel(date: LocalDate) = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(dateFormat)
    }
    val groups = linkedMapOf<String, MutableList<Task>>()
    tasks.forEach { task ->
        val keys = when (options.grouping) {
            TaskGrouping.None -> listOf("all")
            TaskGrouping.List -> listOf(task.listId)
            TaskGrouping.Date -> listOf(when {
                task.dueDate == null -> "none"
                task.dueDate < today -> "overdue"
                else -> task.dueDate.toString()
            })
            TaskGrouping.Created -> listOf(task.createdAt?.atZone(zone)?.toLocalDate()?.toString() ?: "none")
            TaskGrouping.Tag -> task.tags.map { it.lowercase(Locale.ROOT) }.distinct().ifEmpty { listOf("") }
            TaskGrouping.Priority -> listOf(task.priority.name)
        }
        keys.forEach { groups.getOrPut(it) { mutableListOf() }.add(task) }
    }
    val orderedKeys = when (options.grouping) {
        TaskGrouping.Date -> groups.keys.sortedBy { when (it) { "overdue" -> "0"; "none" -> "9"; else -> "1$it" } }
        TaskGrouping.Created -> groups.keys.sorted()
        TaskGrouping.Priority -> groups.keys.sortedBy { com.niranjan.ticktick.domain.model.TaskPriority.valueOf(it).ordinal }
        TaskGrouping.List -> groups.keys.sortedBy { id -> snapshot.lists.indexOfFirst { it.id == id } }
        TaskGrouping.Tag -> groups.keys.sortedBy { it.ifEmpty { "\uffff" } }
        TaskGrouping.None -> groups.keys.toList()
    }
    return orderedKeys.map { key ->
        val label = when (options.grouping) {
            TaskGrouping.None -> "Tasks"
            TaskGrouping.List -> snapshot.lists.find { it.id == key }?.name ?: "Inbox"
            TaskGrouping.Date -> when (key) { "none" -> "No Date"; "overdue" -> "Overdue"; else -> dateLabel(LocalDate.parse(key)) }
            TaskGrouping.Created -> if (key == "none") "Unknown creation date" else dateLabel(LocalDate.parse(key))
            TaskGrouping.Tag -> if (key.isEmpty()) "No Tag" else "#$key"
            TaskGrouping.Priority -> if (key == "None") "No Priority" else "$key Priority"
        }
        TaskGroup(key, label, sortTasks(groups.getValue(key), options))
    }
}
