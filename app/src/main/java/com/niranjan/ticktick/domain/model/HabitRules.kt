package com.niranjan.ticktick.domain.model

import java.time.LocalDate

data class HabitScheduleRevision(val habitId: String, val effectiveFrom: LocalDate, val configuration: Habit)
data class HabitStats(val currentStreak: Int, val bestStreak: Int, val totalDays: Int, val weekly: Boolean)

fun HabitSnapshot.configurationOn(habit: Habit, date: LocalDate): Habit = scheduleHistory
    .filter { it.habitId == habit.id && it.effectiveFrom <= date }.maxByOrNull { it.effectiveFrom }?.configuration ?: scheduleHistory.filter { it.habitId == habit.id }.minByOrNull { it.effectiveFrom }?.configuration ?: habit

internal fun LocalDate.habitWeek() = minusDays((dayOfWeek.value - 1).toLong())

fun HabitSnapshot.weeklyTargetMet(habit: Habit, date: LocalDate): Boolean {
    val rule = configurationOn(habit, date)
    if (rule.frequency != HabitFrequency.Weekly) return false
    val start = date.habitWeek()
    return completedDates[habit.id].orEmpty().count { it in start..start.plusDays(6) } >= weeklyTarget(habit, date)
}

private fun HabitSnapshot.weeklyTarget(habit: Habit, date: LocalDate): Int {
    val start = date.habitWeek()
    val available = (0L..6).map { start.plusDays(it) }.count {
        val rule = configurationOn(habit, it)
        !rule.archived && rule.isScheduledOn(it)
    }
    return minOf(configurationOn(habit, date).frequencyCount, available).coerceAtLeast(1)
}

fun HabitSnapshot.achievementDate(habit: Habit, asOf: LocalDate): LocalDate? {
    val goal = configurationOn(habit, asOf).goalDays ?: return null
    return completedDates[habit.id].orEmpty().filter { it <= asOf }.sorted().getOrNull(goal - 1)
}

fun HabitSnapshot.canRecord(habit: Habit, date: LocalDate, today: LocalDate): Boolean {
    val rule = configurationOn(habit, date)
    return !habit.archived && !rule.archived && date <= today && rule.isScheduledOn(date) &&
        achievementDate(habit, date)?.let { date <= it } != false &&
        (progressFor(habit, date) > 0 || !weeklyTargetMet(habit, date))
}

/** Schedule revisions protect past opportunities; Undo changes quantities, never the old rule. */
fun HabitSnapshot.stats(habit: Habit, today: LocalDate): HabitStats {
    val completed = completedDates[habit.id].orEmpty().filter { it <= today }.toSet()
    val start = minOf(habit.startDate, scheduleHistory.filter { it.habitId == habit.id }
        .minOfOrNull { it.configuration.startDate } ?: habit.startDate)
    val through = achievementDate(habit, today) ?: today
    var day = start
    var streak = 0
    var best = 0
    var weekly = configurationOn(habit, start).frequency == HabitFrequency.Weekly
    while (day <= through) {
        val rule = configurationOn(habit, day)
        val nextWeekly = rule.frequency == HabitFrequency.Weekly
        if (weekly != nextWeekly) {
            // Weeks and scheduled days are different units; start a new streak era.
            streak = 0
            best = 0
            weekly = nextWeekly
        }
        if (rule.frequency == HabitFrequency.Weekly) {
            val end = minOf(day.habitWeek().plusDays(6), through)
            val available = (0L..6L).map { day.habitWeek().plusDays(it) }.any {
                val configuration = configurationOn(habit, it)
                !configuration.archived && configuration.isScheduledOn(it)
            }
            if (available) {
                if (weeklyTargetMet(habit, day)) { streak++; best = maxOf(best, streak) }
                else if (end < through) streak = 0
            }
            day = end.plusDays(1)
        } else {
            if ((!rule.archived || day in completed) && rule.isScheduledOn(day)) {
                if (day in completed) { streak++; best = maxOf(best, streak) }
                else if (day < through) streak = 0
            }
            day = day.plusDays(1)
        }
    }
    return HabitStats(streak, best, completed.size, weekly)
}
