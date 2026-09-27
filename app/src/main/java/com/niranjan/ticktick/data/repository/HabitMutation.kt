package com.niranjan.ticktick.data.repository

import com.niranjan.ticktick.domain.model.*
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

internal class HabitMutation(initial: HabitSnapshot, private val clock: Clock) {
    var snapshot = initial
        private set
    var completedSomething = false
        private set
    private val today get() = LocalDate.now(clock)
    private fun find(id: String) = requireNotNull(snapshot.habits.find { it.id == id }) { "This habit was removed." }
    private fun checkRevision(habit: Habit, revision: Long) {
        require(habit.revision == revision) { "This habit changed. Reopen it to review the latest record; your draft is still available." }
    }
    private fun replace(habit: Habit) { snapshot = snapshot.copy(habits = snapshot.habits.map { if (it.id == habit.id) habit else it }) }
    private fun pattern(h: Habit) = listOf(h.startDate, h.frequency, h.weekdays, h.frequencyCount, h.amount, h.unit, h.goalDays)

    fun save(habit: Habit) {
        val old = snapshot.habits.find { it.id == habit.id }
        require(if (old == null) habit.revision == 0L else old.revision == habit.revision) { "This habit changed or was removed. Reopen it; your draft is still available." }
        require(habit.name.isNotBlank() && habit.section in snapshot.sections)
        require(habit.weekdays.isNotEmpty() && habit.frequencyCount in 1..30)
        require(habit.frequency != HabitFrequency.Weekly || habit.frequencyCount <= 7)
        require(habit.amount == null || habit.amount in 1..999999 && habit.unit.isNotBlank())
        require(habit.recordAmount in 1..999999 && (habit.goalDays == null || habit.goalDays in 1..999))
        require(habit.reminders.size <= 32 && (!habit.constantReminder || habit.reminders.isNotEmpty()))
        val alarmsChanged = old == null || old.reminders != habit.reminders || old.constantReminder != habit.constantReminder
        val saved = habit.copy(revision = (old?.revision ?: 0) + 1, reminders = habit.reminders.distinct().sorted(),
            reminderVersion = if (alarmsChanged) UUID.randomUUID().toString() else old!!.reminderVersion,
            remindersChangedAt = if (alarmsChanged) clock.instant() else old!!.remindersChangedAt)
        var revisions = snapshot.scheduleHistory
        if (old == null || pattern(old) != pattern(saved)) {
            val effective = if (old == null) minOf(habit.startDate, today) else if (old.frequency == HabitFrequency.Weekly || habit.frequency == HabitFrequency.Weekly)
                today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(1) else today.plusDays(1)
            revisions = revisions.filterNot { it.habitId == habit.id && it.effectiveFrom >= effective } +
                HabitScheduleRevision(habit.id, effective, saved)
        }
        snapshot = snapshot.copy(habits = if (old == null) snapshot.habits + saved else snapshot.habits.map { if (it.id == habit.id) saved else it },
            scheduleHistory = revisions)
    }

    fun setArchived(id: String, archived: Boolean) {
        val old = find(id)
        if (old.archived == archived) return
        val updated = old.copy(archived = archived, revision = old.revision + 1)
        val rule = snapshot.configurationOn(old, today).copy(archived = archived)
        snapshot = snapshot.copy(scheduleHistory = snapshot.scheduleHistory
            .filterNot { it.habitId == id && it.effectiveFrom == today }
            .map { if (it.habitId == id && it.effectiveFrom > today) it.copy(configuration = it.configuration.copy(archived = archived)) else it } +
            HabitScheduleRevision(id, today, rule))
        replace(updated)
    }

    fun setProgress(id: String, date: LocalDate, amount: Int, expectedRevision: Long) {
        val habit = find(id)
        checkRevision(habit, expectedRevision)
        require(amount in 0..999999 && date <= today)
        require(amount == 0 || snapshot.canRecord(habit, date, today)) { "This date is unavailable or the habit goal is already achieved." }
        if (snapshot.progressFor(habit, date) == amount) return
        val target = snapshot.configurationOn(habit, date).amount ?: 1
        if (snapshot.progressFor(habit, date) < target && amount >= target) completedSomething = true
        val values = snapshot.progress[id].orEmpty().let { if (amount == 0) it - date else it + (date to amount) }
        snapshot = snapshot.copy(progress = if (values.isEmpty()) snapshot.progress - id else snapshot.progress + (id to values))
        replace(habit.copy(revision = habit.revision + 1))
    }

    fun delete(id: String) {
        find(id)
        snapshot = snapshot.copy(habits = snapshot.habits.filterNot { it.id == id }, progress = snapshot.progress - id,
            scheduleHistory = snapshot.scheduleHistory.filterNot { it.habitId == id })
    }
    fun updateSettings(settings: HabitSettings) { snapshot = snapshot.copy(settings = settings) }
    fun addSection(name: String): Boolean {
        val value = name.trim()
        if (value.isEmpty() || snapshot.sections.any { it.equals(value, true) }) return false
        snapshot = snapshot.copy(sections = snapshot.sections + value)
        return true
    }
    fun moveSection(name: String, direction: Int) {
        require(direction == -1 || direction == 1)
        val sections = snapshot.sections.toMutableList()
        val index = sections.indexOf(name)
        if (index !in sections.indices || index + direction !in sections.indices) return
        sections.add(index + direction, sections.removeAt(index))
        snapshot = snapshot.copy(sections = sections)
    }
}
