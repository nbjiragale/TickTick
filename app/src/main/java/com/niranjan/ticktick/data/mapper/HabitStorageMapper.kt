package com.niranjan.ticktick.data.mapper

import com.niranjan.ticktick.data.local.*
import com.niranjan.ticktick.domain.model.*
import java.time.*

private fun HabitSettings.entity() = HabitSettingsEntity(ringtone = ringtone, ringtoneUri = ringtoneUri,
    sortByCheckInStatus = sortByCheckInStatus, showInTodayAndNext7Days = showInTodayAndNext7Days, countInAppBadge = countInAppBadge)

internal suspend fun HabitDao.initialize() {
    if (settings() == null) {
        putSections(HabitSnapshot().sections.mapIndexed { index, name -> HabitSectionEntity(name, index) })
        putSettings(HabitSettings().entity())
    }
}

internal suspend fun HabitDao.readHabits(): HabitSnapshot = HabitSnapshot(
    habits = habits().map { it.configuration.decodeHabit() }, sections = sections().map { it.name },
    progress = checkIns().groupBy { it.habitId }.mapValues { (_, rows) -> rows.associate { LocalDate.parse(it.date) to it.quantity } },
    settings = requireNotNull(settings()).let { HabitSettings(it.ringtone, it.ringtoneUri, it.sortByCheckInStatus, it.showInTodayAndNext7Days, it.countInAppBadge) },
    scheduleHistory = history().map { HabitScheduleRevision(it.habitId, LocalDate.parse(it.effectiveFrom), it.configuration.decodeHabit()) },
)

internal suspend fun HabitDao.writeHabits(before: HabitSnapshot, after: HabitSnapshot, clock: Clock) {
    deleteHabits(before.habits.filter { old -> after.habits.none { it.id == old.id } }.map { it.id })
    putHabits(after.habits.mapIndexedNotNull { index, habit ->
        if (before.habits.getOrNull(index) == habit) null else HabitEntity(habit.id, habit.encodeHabit(), index)
    })
    before.scheduleHistory.filter { old -> after.scheduleHistory.none { it.habitId == old.habitId && it.effectiveFrom == old.effectiveFrom } }
        .forEach { deleteHistory(it.habitId, it.effectiveFrom.toString()) }
    putHistory(after.scheduleHistory.filter { it !in before.scheduleHistory }.map {
        HabitScheduleEntity(it.habitId, it.effectiveFrom.toString(), it.configuration.encodeHabit())
    })
    before.progress.forEach { (id, dates) -> dates.keys.filter { it !in after.progress[id].orEmpty() }.forEach { deleteCheckIn(id, it.toString()) } }
    putCheckIns(after.progress.flatMap { (id, dates) -> dates.mapNotNull { (date, amount) ->
        if (before.progress[id]?.get(date) == amount) null else HabitCheckInEntity(id, date.toString(), amount, clock.instant().toString())
    } })
    if (before.sections != after.sections) putSections(after.sections.mapIndexed { index, name -> HabitSectionEntity(name, index) })
    if (before.settings != after.settings) putSettings(after.settings.entity())
}
