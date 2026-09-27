package com.niranjan.ticktick.domain.model

import java.io.Serializable
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Schedule configuration; delivery and recurrence execution use shared domain/repository actions.
data class ReminderConfig(
    val offsetsMinutes: List<Long> = emptyList(),
    val constant: Boolean = false,
    val dateOnlyTime: LocalTime = LocalTime.of(9, 0),
) : Serializable

enum class RepeatUnit { None, Day, Week, Month, Year }
enum class RepeatBasis { DueDates, Completion, SpecificDates }

data class RepeatRule(
    val unit: RepeatUnit = RepeatUnit.None,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val basis: RepeatBasis = RepeatBasis.DueDates,
    val dates: List<LocalDate> = emptyList(),
) : Serializable

data class TaskDuration(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime = LocalTime.of(10, 0),
    val endTime: LocalTime = LocalTime.of(11, 0),
    val allDay: Boolean = false,
) : Serializable

data class TaskSchedule(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val duration: TaskDuration? = null,
    val reminders: ReminderConfig = ReminderConfig(),
    val repeat: RepeatRule = RepeatRule(),
) : Serializable {
    fun reminderIsFuture(offset: Long, clock: Clock): Boolean {
        if (date == null) return false
        return resolvedDue(clock.zone)!!.toInstant().minusSeconds(Math.multiplyExact(offset, 60L)).isAfter(clock.instant())
    }

    fun validationError(clock: Clock, requireFutureReminders: Boolean = true): String? = when {
        date == null && (time != null || duration != null || reminders.offsetsMinutes.isNotEmpty() || repeat != RepeatRule()) -> "Choose a date first."
        duration != null && duration.endDate < duration.startDate -> "End date must be on or after the start date."
        duration != null && !duration.allDay && duration.endDate.atTime(duration.endTime) <= duration.startDate.atTime(duration.startTime) -> "End time must be after the start time."
        reminders.offsetsMinutes.any { it < 0 || it > 5256000L } -> "Reminder offsets must be within ten years before the due time."
        reminders.offsetsMinutes.size > 32 -> "Choose at most 32 reminder offsets."
        repeat.dates.size > 366 -> "Choose at most 366 repeat dates."
        duration != null && duration.startDate != date -> "The duration must start on the task date."
        requireFutureReminders && reminders.offsetsMinutes.any { !reminderIsFuture(it, clock) } -> "A reminder is in the past. Choose a later date or time, or remove that reminder."
        repeat.basis == RepeatBasis.SpecificDates && repeat.dates.isEmpty() -> "Choose at least one repeat date."
        repeat.interval !in 1..999 -> "Repeat interval must be between 1 and 999."
        repeat.unit == RepeatUnit.Week && repeat.basis != RepeatBasis.SpecificDates && repeat.weekdays.isEmpty() -> "Choose at least one weekday."
        else -> null
    }
}
