package com.niranjan.ticktick.domain.model

import java.time.Clock
import java.time.LocalDate

data class HabitReminderTarget(val habitId: String, val date: LocalDate)
fun habitReminderTarget(owner: String): HabitReminderTarget? {
    if (!owner.startsWith("habit:")) return null
    val day = owner.substringAfterLast(':').toLongOrNull() ?: return null
    return HabitReminderTarget(owner.removePrefix("habit:").substringBeforeLast(':'), LocalDate.ofEpochDay(day))
}

/** Projection only: these task-shaped inputs never enter the task database or task UI. */
fun TaskSnapshot.withHabitReminders(habits: HabitSnapshot, clock: Clock): TaskSnapshot {
    val today = LocalDate.now(clock)
    val projected = (0L..7L).flatMap { offset ->
        val day = today.plusDays(offset)
        habits.habitsFor(day).filter { habit -> habit.reminders.isNotEmpty() &&
            day !in habits.completedDates[habit.id].orEmpty() && !habits.configurationOn(habit, day).archived }.map { habit ->
            val rule = habits.configurationOn(habit, day)
            val identity = listOf(habit.reminderVersion, day, rule.startDate, rule.frequency,
                rule.weekdays.sortedBy { it.value }, rule.frequencyCount, rule.amount, rule.goalDays).joinToString("|")
            Task("habit:${habit.id}:${day.toEpochDay()}", habit.name, dueDate = day, dueTime = habit.reminders.first(),
                reminders = ReminderConfig(listOf(0), habit.constantReminder), revision = habit.revision,
                recurrence = TaskRecurrence(habit.reminderVersion, identity, day))
        }
    }
    return copy(tasks = tasks + projected)
}
