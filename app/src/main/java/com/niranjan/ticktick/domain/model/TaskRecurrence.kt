package com.niranjan.ticktick.domain.model

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Backend-owned identity; retained across content edits and replaced on a schedule edit. */
data class TaskRecurrence(val seriesRevision: String, val occurrenceId: String, val anchor: LocalDate)
enum class OccurrenceOutcome { Completed, Skipped }
data class TaskOccurrenceHistory(val id: String, val taskId: String, val seriesRevision: String,
    val date: LocalDate, val time: LocalTime?, val duration: TaskDuration?, val title: String,
    val outcome: OccurrenceOutcome, val recordedAt: Instant, val zoneId: String, val offsetSeconds: Int)

val RepeatRule.isRepeating get() = basis == RepeatBasis.SpecificDates || unit != RepeatUnit.None

/** Gap: shift forward by its length. Overlap: choose the earlier offset consistently. */
fun TaskSchedule.resolvedDue(zone: ZoneId): ZonedDateTime? = date?.atTime(time ?: reminders.dateOnlyTime)
    ?.atZone(zone)?.withEarlierOffsetAtOverlap()

/** Pure, bounded recurrence math. No future database rows or dependency on the system clock. */
object TaskRecurrences {
    fun nextDue(task: Task, clock: Clock, skipped: Boolean): LocalDate? {
        val due = task.dueDate ?: return null
        val rule = task.repeat
        if (!rule.isRepeating) return null
        if (rule.basis != RepeatBasis.Completion) return nextAfter(rule, task.recurrence?.anchor ?: due, due)
        val base = if (skipped) due else LocalDate.now(clock)
        val interval = rule.interval.toLong()
        require(interval in 1..999)
        return when (rule.unit) {
            RepeatUnit.Day -> base.plusDays(interval)
            RepeatUnit.Week -> {
                val earliest = base.plusWeeks(interval)
                (0L..6L).map { earliest.plusDays(it) }.first { it.dayOfWeek in rule.weekdays }
            }
            RepeatUnit.Month -> base.plusMonths(interval)
            RepeatUnit.Year -> base.plusYears(interval)
            RepeatUnit.None -> null
        }
    }

    /** First date strictly after [after], calculated from the original anchor to avoid month-end drift. */
    fun nextAfter(rule: RepeatRule, anchor: LocalDate, after: LocalDate): LocalDate? {
        require(rule.interval in 1..999)
        if (rule.basis == RepeatBasis.SpecificDates) return rule.dates.filter { it > after && it >= anchor }.minOrNull()
        val interval = rule.interval.toLong()
        return when (rule.unit) {
            RepeatUnit.None -> null
            RepeatUnit.Day -> anchor.plusDays((Math.floorDiv(ChronoUnit.DAYS.between(anchor, after), interval) + 1).coerceAtLeast(0) * interval)
            RepeatUnit.Week -> {
                require(rule.weekdays.isNotEmpty())
                val week = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val block = Math.floorDiv(ChronoUnit.DAYS.between(week, after), 7 * interval).coerceAtLeast(0)
                (block..block + 1).flatMap { n -> rule.weekdays.map { week.plusWeeks(n * interval).plusDays((it.value - 1).toLong()) } }
                    .filter { it > after && it >= anchor }.minOrNull()
            }
            RepeatUnit.Month -> {
                val months = ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), after.withDayOfMonth(1))
                val block = Math.floorDiv(months, interval).coerceAtLeast(0)
                (block..block + 1).map { anchor.plusMonths(it * interval) }.first { it > after }
            }
            RepeatUnit.Year -> {
                val block = Math.floorDiv((after.year - anchor.year).toLong(), interval).coerceAtLeast(0)
                (block..block + 1).map { anchor.plusYears(it * interval) }.first { it > after }
            }
        }
    }

    fun scheduleOn(schedule: TaskSchedule, date: LocalDate): TaskSchedule {
        val old = requireNotNull(schedule.date)
        val shift = ChronoUnit.DAYS.between(old, date)
        return schedule.copy(date = date, duration = schedule.duration?.let {
            it.copy(startDate = it.startDate.plusDays(shift), endDate = it.endDate.plusDays(shift))
        })
    }

    /** At most 512 overlapping occurrences per query. Completion-based dates cannot be predicted. */
    fun onDate(task: Task, date: LocalDate): List<Task> {
        val due = task.dueDate ?: return emptyList()
        val span = task.duration?.let { ChronoUnit.DAYS.between(it.startDate, it.endDate) } ?: 0
        val earliest = date.minusDays(span.coerceAtLeast(0))
        val result = mutableListOf<Task>()
        fun add(day: LocalDate) {
            if (day in earliest..date) {
                val schedule = scheduleOn(task.schedule, day)
                result += task.copy(dueDate = day, duration = schedule.duration, revision = if (day == due) task.revision else -1)
            }
        }
        add(due)
        if (!task.isActive || task.isNote || !task.repeat.isRepeating || task.repeat.basis == RepeatBasis.Completion) return result
        var after = maxOf(due, earliest.minusDays(1))
        repeat(512 - result.size) {
            val next = nextAfter(task.repeat, task.recurrence?.anchor ?: due, after) ?: return result
            if (next > date) return result
            add(next)
            after = next
        }
        return result
    }
}
