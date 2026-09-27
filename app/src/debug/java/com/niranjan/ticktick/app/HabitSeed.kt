package com.niranjan.ticktick.app

import com.niranjan.ticktick.domain.model.Habit
import com.niranjan.ticktick.domain.model.HabitSnapshot
import java.time.Clock
import java.time.LocalDate

fun initialHabitSnapshot(clock: Clock): HabitSnapshot {
    val today = LocalDate.now(clock)
    return HabitSnapshot(
        habits = listOf(Habit("daily-check-in", "Daily Check-in", quote = "Take a chance and make a change.", startDate = today.minusDays(1))),
        progress = mapOf("daily-check-in" to mapOf(today.minusDays(1) to 1)),
    )
}
