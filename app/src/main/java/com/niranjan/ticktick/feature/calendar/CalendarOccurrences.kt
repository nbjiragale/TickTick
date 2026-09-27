package com.niranjan.ticktick.feature.calendar

import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import com.niranjan.ticktick.domain.model.Holiday
import com.niranjan.ticktick.domain.model.HolidaySnapshot
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.model.Task
import com.niranjan.ticktick.domain.model.habitsFor
import java.time.LocalDate
import java.time.LocalTime

/** One entry rendered on a specific calendar date. */
sealed interface CalendarItem {
    val date: LocalDate
    val title: String

    /** [start]/[end] are set when the task occupies the timeline on [date]; otherwise it is all-day there. */
    data class TaskItem(override val date: LocalDate, val task: Task, val start: LocalTime?, val end: LocalTime?) : CalendarItem {
        override val title: String get() = task.title
        val done: Boolean get() = !task.isActive
        val key get() = "${task.id}:${task.recurrence?.occurrenceId}:${task.dueDate}"
        val actionable get() = task.revision >= 0
    }

    data class HabitItem(override val date: LocalDate, val habit: Habit, val checkedIn: Boolean) : CalendarItem {
        override val title: String get() = habit.name
    }

    data class HolidayItem(override val date: LocalDate, val holiday: Holiday) : CalendarItem {
        override val title: String get() = holiday.name
    }
}

/** Everything appearing on one date, pre-sorted into the buckets the views render. */
data class CalendarDay(
    val date: LocalDate,
    val holidays: List<CalendarItem.HolidayItem>,
    val timedTasks: List<CalendarItem.TaskItem>,
    val allDayTasks: List<CalendarItem.TaskItem>,
    val habits: List<CalendarItem.HabitItem>,
) {
    val isEmpty: Boolean get() = holidays.isEmpty() && timedTasks.isEmpty() && allDayTasks.isEmpty() && habits.isEmpty()
    val totalCount: Int get() = holidays.size + timedTasks.size + allDayTasks.size + habits.size

    /** Content of the all-day strip above a timeline. */
    val allDayItems: List<CalendarItem> get() = holidays + allDayTasks + habits

    /** Agenda ordering: holidays, timed tasks by start, date-only tasks, habit check-ins. */
    val agendaItems: List<CalendarItem> get() = holidays + timedTasks + allDayTasks + habits
}

/** Session inputs shared by every view; derivation is pure and cheap at session data sizes. */
data class CalendarData(
    val tasks: List<Task>,
    val habits: HabitSnapshot,
    val holidays: HolidaySnapshot,
    val showCompleted: Boolean,
    val history: List<TaskOccurrenceHistory> = emptyList(),
) {
    fun dayFor(date: LocalDate): CalendarDay {
        val dated = tasks.filter { showCompleted || it.isActive }.flatMap { TaskRecurrences.onDate(it, date) }.toMutableList()
        if (showCompleted) history.forEach { row ->
            val source = tasks.find { it.id == row.taskId } ?: return@forEach
            // Exhausted series already display the final handled occurrence as their current record.
            if (!source.isActive && source.recurrence?.occurrenceId == row.id) return@forEach
            val historical = source.copy(title = row.title, dueDate = row.date, dueTime = row.time, duration = row.duration,
                repeat = RepeatRule(), completed = row.outcome == OccurrenceOutcome.Completed,
                declined = row.outcome == OccurrenceOutcome.Skipped, revision = -1,
                recurrence = TaskRecurrence(row.seriesRevision, row.id, row.date))
            dated += TaskRecurrences.onDate(historical, date)
        }
        val timed = dated.mapNotNull { task ->
            task.timedSlotOn(date)?.let { CalendarItem.TaskItem(date, task, it.first, it.second) }
        }.sortedWith(compareBy({ it.start }, { it.end }, { it.task.title }))
        val allDay = dated.filter { it.timedSlotOn(date) == null }.map { CalendarItem.TaskItem(date, it, null, null) }
        val habitItems = habits.habitsFor(date)
            .map { CalendarItem.HabitItem(date, it, date in habits.completedDates[it.id].orEmpty()) }
            .filter { showCompleted || !it.checkedIn }
        val holidayItems = holidays.holidays.filter { it.date == date }.map { CalendarItem.HolidayItem(date, it) }
        return CalendarDay(date, holidayItems, timed, allDay, habitItems)
    }
}

private const val DefaultEventMinutes = 60L
private const val MinBlockMinutes = 30L

/** Uses the same domain recurrence projection as every calendar view. */
fun Task.occursOn(date: LocalDate): Boolean = TaskRecurrences.onDate(this, date).isNotEmpty()

/**
 * Timeline slot on [date], or null when the task belongs in the all-day area that day:
 * date-only tasks, all-day durations, and the middle days of multi-day timed durations.
 */
fun Task.timedSlotOn(date: LocalDate): Pair<LocalTime, LocalTime>? {
    val range = duration
    if (range == null) {
        val start = dueTime ?: return null
        return start to if (start.hour >= 23) LocalTime.MAX else start.plusMinutes(DefaultEventMinutes)
    }
    if (range.allDay) return null
    return when {
        range.startDate == range.endDate -> range.startTime to maxOf(range.endTime, range.startTime)
        date == range.startDate -> range.startTime to LocalTime.MAX
        date == range.endDate -> LocalTime.MIN to range.endTime
        else -> null
    }
}

/** Sunday-start weeks, matching the existing schedule-picker month calendar. */
internal fun sundayStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

/** A timed task with its overlap-cluster column assignment. */
internal data class PositionedTask(val item: CalendarItem.TaskItem, val column: Int, val columns: Int)

/** End of the block as drawn, enforcing a minimum visual length. */
internal fun visualEnd(item: CalendarItem.TaskItem): LocalTime {
    val start = item.start ?: return LocalTime.MAX
    val floor = if (start.hour >= 23) LocalTime.MAX else start.plusMinutes(MinBlockMinutes)
    return maxOf(item.end ?: floor, floor)
}

/** Greedy column assignment inside overlap clusters so simultaneous blocks share width. */
internal fun positionTimed(items: List<CalendarItem.TaskItem>): List<PositionedTask> {
    val sorted = items.sortedWith(compareBy({ it.start }, { it.end }, { it.task.title }))
    val result = mutableListOf<PositionedTask>()
    val cluster = mutableListOf<Pair<CalendarItem.TaskItem, Int>>()
    val columnEnds = mutableListOf<LocalTime>()
    var clusterEnd: LocalTime? = null
    fun flush() {
        cluster.forEach { (item, column) -> result += PositionedTask(item, column, columnEnds.size) }
        cluster.clear()
        columnEnds.clear()
        clusterEnd = null
    }
    for (item in sorted) {
        val start = item.start ?: continue
        val end = visualEnd(item)
        val boundary = clusterEnd
        if (boundary != null && start >= boundary) flush()
        val free = columnEnds.indexOfFirst { it <= start }
        val column = if (free == -1) {
            columnEnds.add(end)
            columnEnds.lastIndex
        } else {
            columnEnds[free] = end
            free
        }
        cluster += item to column
        clusterEnd = maxOf(clusterEnd ?: end, end)
    }
    flush()
    return result
}
