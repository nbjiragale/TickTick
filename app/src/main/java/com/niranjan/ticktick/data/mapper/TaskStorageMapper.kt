package com.niranjan.ticktick.data.mapper

import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.domain.model.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal fun String.storageKey() = lowercase(Locale.ROOT)

// Versioned JSON only for bounded schedule value objects; aggregate children have relational tables.
private fun record() = JSONObject().put("version", 1)
private fun String.record(): JSONObject = JSONObject(this).also { require(it.getInt("version") == 1) }
private fun <T> JSONArray.values(map: (String) -> T): List<T> = (0 until length()).map { map(getString(it)) }
private fun ReminderConfig.encode() = record().put("offsets", JSONArray(offsetsMinutes))
    .put("constant", constant).put("dateOnlyTime", dateOnlyTime.toString()).toString()
private fun String.reminders(): ReminderConfig = record().let { obj -> ReminderConfig(
    obj.getJSONArray("offsets").values(String::toLong), obj.getBoolean("constant"), LocalTime.parse(obj.getString("dateOnlyTime"))) }
private fun RepeatRule.encode() = record().put("unit", unit.name).put("interval", interval)
    .put("basis", basis.name).put("weekdays", JSONArray(weekdays.sortedBy { it.value }.map { it.name }))
    .put("dates", JSONArray(dates.map { it.toString() })).toString()
private fun String.repeatRule(): RepeatRule = record().let { obj -> RepeatRule(
    RepeatUnit.valueOf(obj.getString("unit")), obj.getInt("interval"),
    obj.getJSONArray("weekdays").values(DayOfWeek::valueOf).toSet(), RepeatBasis.valueOf(obj.getString("basis")),
    obj.getJSONArray("dates").values(LocalDate::parse)) }
private fun TaskDuration.encode() = record().put("startDate", startDate.toString()).put("endDate", endDate.toString())
    .put("startTime", startTime.toString()).put("endTime", endTime.toString()).put("allDay", allDay).toString()
private fun String.duration(): TaskDuration = record().let { obj -> TaskDuration(
    LocalDate.parse(obj.getString("startDate")), LocalDate.parse(obj.getString("endDate")),
    LocalTime.parse(obj.getString("startTime")), LocalTime.parse(obj.getString("endTime")), obj.getBoolean("allDay")) }

internal fun Task.entity(position: Int) = TaskEntity(id, title, listId, dueDate?.toString(), dueTime?.toString(),
    completed, description, priority.name, isNote, reminders.encode(), repeat.encode(), duration?.encode(), declined,
    requireNotNull(createdAt).toString(), requireNotNull(modifiedAt).toString(), pinned, revision, position)

internal suspend fun TaskDao.readSnapshot(): TaskSnapshot {
    val recurrences = recurrences().associateBy { it.taskId }
    val links = links().groupBy { it.taskId }
    val checklist = checklist().groupBy { it.taskId }
    val attachments = attachments().groupBy { it.taskId }
    val rules = rules().groupBy { it.filterId }
    return TaskSnapshot(
        tasks = tasks().map { row ->
            require(row.zonePolicy == "device_local")
            Task(id = row.id, title = row.title, listId = row.listId, dueDate = row.dueDate?.let(LocalDate::parse),
                dueTime = row.dueTime?.let(LocalTime::parse), completed = row.completed, description = row.description,
                priority = TaskPriority.valueOf(row.priority), tags = links[row.id].orEmpty().map { it.displayName },
                checklist = checklist[row.id].orEmpty().map { ChecklistItem(it.id, it.text, it.completed) },
                attachments = attachments[row.id].orEmpty().map { TaskAttachment(it.id, it.name, it.uri, it.mimeType, it.sizeBytes) },
                isNote = row.isNote, reminders = row.reminders.reminders(), repeat = row.repeat.repeatRule(),
                duration = row.duration?.duration(), declined = row.declined, createdAt = Instant.parse(row.createdAt),
                modifiedAt = Instant.parse(row.modifiedAt), pinned = row.pinned, revision = row.revision,
                recurrence = recurrences[row.id]?.let { TaskRecurrence(it.seriesRevision, it.occurrenceId, LocalDate.parse(it.anchor)) })
        },
        lists = lists().map { TaskList(it.id, it.name, ListSymbol.valueOf(it.symbol), it.color, TaskListView.valueOf(it.view)) },
        tags = tags().filter { it.explicit }.map { TaskTag(it.name, it.color) },
        filters = filters().map { row -> SavedTaskFilter(row.id, row.name,
            rules[row.id].orEmpty().map { FilterRule(FilterField.valueOf(it.field), it.value) }, row.matchAll) },
        occurrenceHistory = history().map { TaskOccurrenceHistory(it.id, it.taskId, it.seriesRevision, LocalDate.parse(it.date),
            it.time?.let(LocalTime::parse), it.duration?.duration(), it.title, OccurrenceOutcome.valueOf(it.outcome),
            Instant.parse(it.recordedAt), it.zoneId, it.offsetSeconds) },
        snoozedUntil = snoozes().associate { it.taskId to Instant.parse(it.untilInstant) },
    )
}

internal suspend fun TaskDao.writeChanges(before: TaskSnapshot, after: TaskSnapshot) {
    putLists(after.lists.mapIndexedNotNull { index, row ->
        if (before.lists.getOrNull(index) == row) null else TaskListEntity(row.id, row.name, row.name.storageKey(), row.symbol.name, row.color, row.view.name, index)
    })
    putTags(after.knownTags.mapIndexed { index, row -> TaskTagEntity(row.name.storageKey(), row.name, row.color,
        after.tags.any { it.name.equals(row.name, true) }, index) })
    val previous = before.tasks.associateBy { it.id }
    val currentIds = after.tasks.map { it.id }.toSet()
    deleteTasks(previous.keys.filterNot { it in currentIds })
    putTasks(after.tasks.mapIndexedNotNull { index, task -> if (before.tasks.getOrNull(index) == task) null else task.entity(index) })
    deleteRecurrences(after.tasks.filter { it.recurrence == null }.map { it.id })
    putRecurrences(after.tasks.filter { previous[it.id]?.recurrence != it.recurrence }.mapNotNull { task ->
        task.recurrence?.let { TaskRecurrenceEntity(task.id, it.seriesRevision, it.occurrenceId, it.anchor.toString()) }
    })
    val historyIds = before.occurrenceHistory.map { it.id }.toSet()
    putHistory(after.occurrenceHistory.filter { it.id !in historyIds }.map {
        TaskOccurrenceHistoryEntity(it.id, it.taskId, it.seriesRevision, it.date.toString(), it.time?.toString(),
            it.duration?.encode(), it.title, it.outcome.name, it.recordedAt.toString(), it.zoneId, it.offsetSeconds)
    })
    after.tasks.filter { previous[it.id] != it }.forEach { task ->
        clearLinks(task.id); clearChecklist(task.id); clearAttachments(task.id)
        putLinks(task.tags.distinctBy { it.storageKey() }.mapIndexed { index, name -> TaskTagLinkEntity(task.id, name.storageKey(), name, index) })
        putChecklist(task.checklist.mapIndexed { index, item -> ChecklistEntity(task.id, item.id, item.text, item.completed, index) })
        putAttachments(task.attachments.mapIndexed { index, item -> AttachmentEntity(task.id, item.id, item.name, item.uri, item.mimeType, item.sizeBytes, index) })
    }
    deleteUnusedTags()
    if (before.snoozedUntil != after.snoozedUntil) {
        clearSnoozes()
        putSnoozes(after.snoozedUntil.map { TaskSnoozeEntity(it.key, it.value.toString()) })
    }
    putFilters(after.filters.mapIndexedNotNull { index, row ->
        if (before.filters.getOrNull(index) == row) null else SavedFilterEntity(row.id, row.name, row.name.storageKey(), row.matchAll, index)
    })
    after.filters.filter { it !in before.filters }.forEach { filter ->
        clearRules(filter.id)
        putRules(filter.rules.mapIndexed { index, rule -> FilterRuleEntity(filter.id, index, rule.field.name, rule.value) })
    }
}
