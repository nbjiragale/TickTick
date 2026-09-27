package com.niranjan.ticktick.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class HabitFrequency { Daily, Weekly, Interval }
enum class HabitRecordMode { Auto, Manual }

data class Habit(
    val id: String,
    val name: String,
    val icon: String = "😊",
    val quote: String = "Whatever you do, do it well.",
    val startDate: LocalDate,
    val section: String = "Others",
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val frequencyCount: Int = 2,
    val amount: Int? = null,
    val unit: String = "Count",
    val recordMode: HabitRecordMode = HabitRecordMode.Auto,
    val recordAmount: Int = 1,
    val goalDays: Int? = null,
    val reminders: List<LocalTime> = emptyList(),
    val autoPopUp: Boolean = false,
    // Bounded re-alerts use the shared reminder delivery engine.
    val constantReminder: Boolean = false,
    val archived: Boolean = false,
    val revision: Long = 0,
    val reminderVersion: String = "",
    val remindersChangedAt: java.time.Instant = java.time.Instant.EPOCH,
) : java.io.Serializable {
    fun isScheduledOn(date: LocalDate): Boolean = date >= startDate && when (frequency) {
        HabitFrequency.Daily -> date.dayOfWeek in weekdays
        HabitFrequency.Weekly -> true // Any day is available toward the weekly target.
        HabitFrequency.Interval -> ChronoUnit.DAYS.between(startDate, date) % frequencyCount.coerceAtLeast(1) == 0L
    }
}

data class HabitSnapshot(
    val habits: List<Habit> = emptyList(),
    val sections: List<String> = listOf("Morning", "Afternoon", "Night", "Others"),
    val progress: Map<String, Map<LocalDate, Int>> = emptyMap(),
    val settings: HabitSettings = HabitSettings(),
    val scheduleHistory: List<HabitScheduleRevision> = emptyList(),
) {
    val completedDates: Map<String, Set<LocalDate>> get() = habits.associate { habit -> habit.id to
        progress[habit.id].orEmpty().filter { (date, amount) -> amount >= (configurationOn(habit, date).amount ?: 1) }.keys }
}

data class HabitSettings(
    val ringtone: String = "TickTick Pop",
    val ringtoneUri: String? = "", // Empty: app default; null: silent; otherwise a system ringtone URI.
    val sortByCheckInStatus: Boolean = false,
    val showInTodayAndNext7Days: Boolean = true,
    val countInAppBadge: Boolean = false,
)

fun HabitSnapshot.habitsFor(date: LocalDate): List<Habit> {
    val scheduled = habits.filter { !it.archived && configurationOn(it, date).isScheduledOn(date) &&
        (!configurationOn(it, date).archived || date in completedDates[it.id].orEmpty()) &&
        (achievementDate(it, date)?.let { achieved -> date <= achieved } != false) &&
        (date in completedDates[it.id].orEmpty() || !weeklyTargetMet(it, date)) }
    return if (settings.sortByCheckInStatus) scheduled.sortedBy { date in completedDates[it.id].orEmpty() } else scheduled
}

fun HabitSnapshot.progressFor(habit: Habit, date: LocalDate): Int = progress[habit.id]?.get(date) ?: 0
