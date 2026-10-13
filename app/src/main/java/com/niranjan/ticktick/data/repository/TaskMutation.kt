package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskSnapshot
import com.niranjan.ticktick.domain.model.TaskSchedule
import com.niranjan.ticktick.domain.model.TaskListView
import com.niranjan.ticktick.domain.model.TaskTag
import com.niranjan.ticktick.domain.model.SavedTaskFilter
import com.niranjan.ticktick.domain.model.FilterRule
import com.niranjan.ticktick.domain.repository.DeletedTasks
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.TaskConflictException
import java.time.Instant
import java.time.Clock
import java.util.UUID

internal class TaskMutation(initial: TaskSnapshot, private val clock: Clock = Clock.systemDefaultZone()) {
    private val seedTime = clock.instant()
    private var currentSnapshot = initial.copy(tasks = initial.tasks.mapIndexed { index, task ->
        val created = task.createdAt ?: seedTime.minusMillis((initial.tasks.size - index).toLong())
        normalize(task.copy(createdAt = created, modifiedAt = task.modifiedAt ?: created), task)
    })
    val snapshot get() = currentSnapshot
    var completedSomething = false
        private set
    private fun update(transform: (TaskSnapshot) -> TaskSnapshot) { currentSnapshot = transform(currentSnapshot) }
    private fun getAndUpdate(transform: (TaskSnapshot) -> TaskSnapshot): TaskSnapshot = currentSnapshot.also { update(transform) }

    fun save(task: Task): Task {
        saveTasks(listOf(task))
        return snapshot.tasks.first { it.id == task.id }
    }

    fun saveTasks(tasks: List<Task>) {
        require(tasks.all { it.title.isNotBlank() })
        require(tasks.map { it.id }.distinct().size == tasks.size)
        update { current ->
            require(tasks.all { task -> current.lists.any { it.id == task.listId } })
            val previous = current.tasks.associateBy { it.id }
            val history = current.occurrenceHistory.toMutableList()
            val saved = tasks.associate { task ->
                val old = previous[task.id]
                if ((old == null && task.revision != 0L) || (old != null && old.revision != task.revision))
                    throw com.niranjan.ticktick.domain.repository.TaskConflictException()
                require(task.checklist.map { it.id }.distinct().size == task.checklist.size) { "Checklist item IDs must be unique." }
                require(task.attachments.map { it.id }.distinct().size == task.attachments.size) { "Attachment IDs must be unique." }
                task.schedule.validationError(clock, requireFutureReminders = old == null || old.schedule != task.schedule)?.let { throw IllegalArgumentException(it) }
                var normalized = normalize(task.copy(createdAt = old?.createdAt ?: clock.instant(), modifiedAt = old?.modifiedAt), old)
                if (old?.isActive == true && task.completed && !task.isNote) completedSomething = true
                if (old?.isActive == true && task.completed && !task.isNote && task.repeat.isRepeating) {
                    normalized = advance(normalized, OccurrenceOutcome.Completed, history)
                }
                task.id to if (old != null && normalized == old) old else normalized.copy(modifiedAt = clock.instant(), revision = (old?.revision ?: 0) + 1)
            }
            val invalidSnoozes = tasks.filter { !it.isActive || it.isNote || previous[it.id]?.schedule != it.schedule }.map { it.id }.toSet()
            current.copy(tasks = current.tasks.map { saved[it.id] ?: it } + saved.values.filter { it.id !in previous },
                snoozedUntil = current.snoozedUntil - invalidSnoozes, occurrenceHistory = history)
        }
    }

    private fun normalize(task: Task, old: Task?): Task {
        if (!task.repeat.isRepeating || task.dueDate == null || task.isNote) return task.copy(recurrence = null)
        val previous = old?.recurrence
        val identity = if (previous != null && old.schedule == task.schedule) {
            if (!old.isActive && task.isActive) previous.copy(occurrenceId = UUID.randomUUID().toString()) else previous
        } else TaskRecurrence(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            previous?.anchor?.takeIf { old.dueDate == task.dueDate && old.repeat == task.repeat } ?: task.dueDate)
        return task.copy(recurrence = identity)
    }

    private fun advance(task: Task, outcome: OccurrenceOutcome, history: MutableList<TaskOccurrenceHistory>): Task {
        val state = requireNotNull(task.recurrence)
        check(history.none { it.id == state.occurrenceId }) { "This occurrence was already handled." }
        val due = requireNotNull(task.dueDate)
        val resolved = requireNotNull(task.schedule.resolvedDue(clock.zone))
        history += TaskOccurrenceHistory(state.occurrenceId, task.id, state.seriesRevision, due, task.dueTime,
            task.duration, task.title, outcome, clock.instant(), clock.zone.id, resolved.offset.totalSeconds)
        val next = TaskRecurrences.nextDue(task, clock, skipped = outcome == OccurrenceOutcome.Skipped)
            ?: return task.copy(completed = outcome == OccurrenceOutcome.Completed, declined = outcome == OccurrenceOutcome.Skipped)
        val schedule = TaskRecurrences.scheduleOn(task.schedule, next)
        return task.copy(dueDate = next, duration = schedule.duration, completed = false, declined = false,
            checklist = task.checklist.map { it.copy(completed = false) },
            recurrence = state.copy(occurrenceId = UUID.randomUUID().toString()))
    }

    fun setCompleted(id: String, completed: Boolean, expectedRevision: Long) {
        val task = snapshot.tasks.find { it.id == id } ?: throw TaskConflictException()
        if (task.revision != expectedRevision) throw TaskConflictException()
        saveTasks(listOf(task.copy(completed = completed, declined = false)))
    }

    fun skipOccurrences(expectedRevisions: Map<String, Long>) {
        require(expectedRevisions.isNotEmpty())
        val selected = expectedRevisions.map { (id, revision) ->
            snapshot.tasks.find { it.id == id && it.revision == revision } ?: throw TaskConflictException()
        }
        require(selected.all { it.isActive && !it.isNote && it.recurrence != null }) { "Select active repeating tasks to skip." }
        update { current ->
            val history = current.occurrenceHistory.toMutableList()
            val advanced = selected.associate { task -> task.id to advance(task, OccurrenceOutcome.Skipped, history)
                .copy(revision = task.revision + 1, modifiedAt = clock.instant()) }
            current.copy(tasks = current.tasks.map { advanced[it.id] ?: it },
                snoozedUntil = current.snoozedUntil - expectedRevisions.keys, occurrenceHistory = history)
        }
    }

    fun setDeclined(id: String, declined: Boolean) {
        update { current ->
            require(current.tasks.any { it.id == id }) { "This task was removed." }
            current.copy(tasks = current.tasks.map { if (it.id == id) normalize(it.copy(declined = declined, completed = false, modifiedAt = clock.instant(), revision = it.revision + 1), it) else it },
                snoozedUntil = current.snoozedUntil - id)
        }
    }

    fun snooze(id: String, until: Instant) {
        update { current ->
            require(current.tasks.any { it.id == id && it.isActive && !it.isNote && it.dueDate != null })
            current.copy(snoozedUntil = current.snoozedUntil + (id to until),
                tasks = current.tasks.map { if (it.id == id) it.copy(revision = it.revision + 1) else it })
        }
    }

    fun changeSchedule(id: String, schedule: TaskSchedule) {
        val task = snapshot.tasks.first { it.id == id && it.isActive && !it.isNote }
        saveTasks(listOf(task.copy(dueDate = schedule.date, dueTime = schedule.time,
            duration = schedule.duration, reminders = schedule.reminders, repeat = schedule.repeat)))
    }

    fun addList(name: String, color: Int?, view: TaskListView): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Enter a list name." }
        val id = UUID.randomUUID().toString()
        update { current ->
            require(current.lists.none { it.name.equals(trimmed, ignoreCase = true) }) { "A list with this name already exists." }
            current.copy(lists = current.lists + TaskList(id, trimmed, ListSymbol.Custom, color, view))
        }
        return id
    }

    fun addTag(name: String, color: Int?) {
        val trimmed = name.trim().removePrefix("#")
        require(Regex("[\\p{L}\\p{N}_-]+").matches(trimmed)) { "Use letters, numbers, underscores or hyphens." }
        update { current ->
            require(current.knownTags.none { it.name.equals(trimmed, true) }) { "A tag with this name already exists." }
            current.copy(tags = current.tags + TaskTag(trimmed, color))
        }
    }

    private fun validateRules(rules: List<FilterRule>, current: TaskSnapshot) {
        require(rules.isNotEmpty()) { "Add at least one condition." }
        rules.forEach { rule -> when (rule.field) {
            FilterField.List -> require(current.lists.any { it.id == rule.value }) { "A selected list was removed. Choose another condition." }
            FilterField.Tag -> require(rule.value.isNotBlank()) { "Choose a tag." }
            FilterField.Date -> FilterDate.valueOf(rule.value)
            FilterField.Priority -> TaskPriority.valueOf(rule.value)
        } }
    }

    fun addFilter(name: String, rules: List<FilterRule>, matchAll: Boolean): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && rules.isNotEmpty()) { "Enter a name and add at least one condition." }
        val id = UUID.randomUUID().toString()
        update { current ->
            validateRules(rules, current)
            require(current.filters.none { it.name.equals(trimmed, true) }) { "A filter with this name already exists." }
            current.copy(filters = current.filters + SavedTaskFilter(id, trimmed, rules.toList(), matchAll))
        }
        return id
    }

    private fun listToken(list: TaskList): String = "~${when (list.symbol) {
        ListSymbol.Inbox -> "📥"; ListSymbol.Work -> "💼"; ListSymbol.Personal -> "🏠"
        ListSymbol.Welcome -> "👋"; ListSymbol.Custom -> "☰"
    }}${list.name}"
    private fun Task.moveList(old: TaskList, new: TaskList): Task = copy(listId = new.id,
        title = Regex("(?<!\\S)${Regex.escape(listToken(old))}(?!\\S)").replace(title) { listToken(new) },
        modifiedAt = clock.instant(), revision = revision + 1)

    fun editList(id: String, name: String, color: Int?, view: TaskListView) {
        require(id != "inbox") { "Inbox cannot be edited." }
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Enter a list name." }
        update { current ->
            val old = current.lists.firstOrNull { it.id == id } ?: error("This list was removed.")
            require(current.lists.none { it.id != id && it.name.equals(trimmed, true) }) { "A list with this name already exists." }
            val new = old.copy(name = trimmed, color = color, view = view)
            current.copy(lists = current.lists.map { if (it.id == id) new else it },
                tasks = current.tasks.map { if (it.listId == id && old.name != new.name) it.moveList(old, new) else it })
        }
    }
    fun deleteList(id: String) {
        require(id != "inbox") { "Inbox cannot be deleted." }
        update { current ->
            val old = current.lists.firstOrNull { it.id == id } ?: error("This list was removed.")
            val inbox = current.lists.first { it.id == "inbox" }
            current.copy(lists = current.lists.filterNot { it.id == id },
                tasks = current.tasks.map { if (it.listId == id) it.moveList(old, inbox) else it },
                filters = current.filters.map { filter -> filter.copy(rules = filter.rules.map {
                    if (it.field == FilterField.List && it.value == id) it.copy(value = "inbox") else it
                }) })
        }
    }
    private fun tagPattern(name: String) = Regex("(?<![\\p{L}\\p{N}_])#${Regex.escape(name)}(?![\\p{L}\\p{N}_-])", RegexOption.IGNORE_CASE)
    private fun replaceTagInTitle(task: Task, current: TaskSnapshot, name: String, replacement: String): String {
        // List names can contain #tag-looking text. Those characters belong to the
        // list token, not the task's tags; keep source offsets while masking them.
        val source = task.title.toCharArray()
        current.lists.find { it.id == task.listId }?.let { list ->
            Regex("(?<!\\S)${Regex.escape(listToken(list))}(?!\\S)").findAll(task.title).forEach { token ->
                token.range.forEach { source[it] = ' ' }
            }
        }
        return tagPattern(name).findAll(source.concatToString()).toList().asReversed()
            .fold(task.title) { text, match -> text.replaceRange(match.range, replacement) }
    }

    fun editTag(oldName: String, name: String, color: Int?) {
        val trimmed = name.trim().removePrefix("#")
        require(Regex("[\\p{L}\\p{N}_-]+").matches(trimmed)) { "Use letters, numbers, underscores or hyphens." }
        update { current ->
            require(current.knownTags.any { it.name.equals(oldName, true) }) { "This tag was removed." }
            require(current.knownTags.none { !it.name.equals(oldName, true) && it.name.equals(trimmed, true) }) { "A tag with this name already exists." }
            current.copy(tags = current.tags.filterNot { it.name.equals(oldName, true) } + TaskTag(trimmed, color),
                tasks = current.tasks.map { task ->
                    if (task.tags.none { it.equals(oldName, true) }) task else task.copy(
                        tags = task.tags.map { if (it.equals(oldName, true)) trimmed else it },
                        title = replaceTagInTitle(task, current, oldName, "#$trimmed"),
                        modifiedAt = clock.instant(), revision = task.revision + 1)
                }, filters = current.filters.map { filter -> filter.copy(rules = filter.rules.map {
                    if (it.field == FilterField.Tag && it.value.equals(oldName, true)) it.copy(value = trimmed) else it
                }) })
        }
    }
    fun deleteTag(name: String) {
        update { current ->
            require(current.knownTags.any { it.name.equals(name, true) }) { "This tag was removed." }
            current.copy(tags = current.tags.filterNot { it.name.equals(name, true) }, tasks = current.tasks.map { task ->
                if (task.tags.none { it.equals(name, true) }) task else task.copy(
                    tags = task.tags.filterNot { it.equals(name, true) },
                    title = replaceTagInTitle(task, current, name, "").trim().ifBlank { name },
                    modifiedAt = clock.instant(), revision = task.revision + 1)
            })
        }
    }
    fun editFilter(id: String, name: String, rules: List<FilterRule>, matchAll: Boolean) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank() && rules.isNotEmpty()) { "Enter a name and add at least one condition." }
        update { current ->
            validateRules(rules, current)
            require(current.filters.any { it.id == id }) { "This filter was removed." }
            require(current.filters.none { it.id != id && it.name.equals(trimmed, true) }) { "A filter with this name already exists." }
            current.copy(filters = current.filters.map { if (it.id == id) it.copy(name = trimmed, rules = rules.toList(), matchAll = matchAll) else it })
        }
    }
    fun deleteFilter(id: String) {
        update { current -> current.copy(filters = current.filters.filterNot { it.id == id }) }
    }

    fun deleteTask(id: String) {
        deleteTasks(setOf(id))
    }

    fun deleteTasks(ids: Set<String>): DeletedTasks {
        val before = getAndUpdate { current ->
            current.copy(tasks = current.tasks.filterNot { it.id in ids }, snoozedUntil = current.snoozedUntil - ids,
                occurrenceHistory = current.occurrenceHistory.filterNot { it.taskId in ids })
        }
        return DeletedTasks(before.tasks.mapIndexedNotNull { index, task -> if (task.id in ids) index to task else null },
            before.snoozedUntil.filterKeys { it in ids }, before.occurrenceHistory.filter { it.taskId in ids })
    }

    fun restoreTasks(deleted: DeletedTasks) {
        update { current ->
            val restored = deleted.tasks.filter { (_, task) -> current.tasks.none { it.id == task.id } }
            val tasks = current.tasks.toMutableList()
            restored.sortedBy { it.first }.forEach { (index, task) -> tasks.add(index.coerceIn(0, tasks.size), task.copy(revision = task.revision + 1, listId = task.listId.takeIf { id -> current.lists.any { it.id == id } } ?: "inbox")) }
            current.copy(tasks = tasks, snoozedUntil = current.snoozedUntil + deleted.snoozedUntil.filterKeys { id -> restored.any { it.second.id == id } },
                occurrenceHistory = current.occurrenceHistory + deleted.history.filter { row -> restored.any { it.second.id == row.taskId } })
        }
    }
}
