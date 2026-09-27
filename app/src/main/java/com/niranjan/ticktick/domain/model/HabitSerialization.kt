package com.niranjan.ticktick.domain.model

import java.time.*
import org.json.JSONArray
import org.json.JSONObject

internal fun Habit.encodeHabit(): String = JSONObject().put("version", 1).put("id", id).put("name", name)
    .put("icon", icon).put("quote", quote).put("start", startDate.toString()).put("section", section)
    .put("frequency", frequency.name).put("weekdays", JSONArray(weekdays.sortedBy { it.value }.map { it.name }))
    .put("frequencyCount", frequencyCount).put("amount", amount ?: JSONObject.NULL).put("unit", unit)
    .put("recordMode", recordMode.name).put("recordAmount", recordAmount).put("goalDays", goalDays ?: JSONObject.NULL)
    .put("reminders", JSONArray(reminders.map { it.toString() })).put("autoPopUp", autoPopUp)
    .put("constantReminder", constantReminder).put("archived", archived).put("revision", revision)
    .put("reminderVersion", reminderVersion).put("remindersChangedAt", remindersChangedAt.toString()).toString()

internal fun String.decodeHabit(): Habit = JSONObject(this).let { j ->
    require(j.getInt("version") == 1)
    fun strings(key: String) = j.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
    Habit(j.getString("id"), j.getString("name"), j.getString("icon"), j.getString("quote"),
        LocalDate.parse(j.getString("start")), j.getString("section"), HabitFrequency.valueOf(j.getString("frequency")),
        strings("weekdays").map(DayOfWeek::valueOf).toSet(), j.getInt("frequencyCount"),
        if (j.isNull("amount")) null else j.getInt("amount"), j.getString("unit"), HabitRecordMode.valueOf(j.getString("recordMode")),
        j.getInt("recordAmount"), if (j.isNull("goalDays")) null else j.getInt("goalDays"), strings("reminders").map(LocalTime::parse),
        j.getBoolean("autoPopUp"), j.getBoolean("constantReminder"), j.getBoolean("archived"), j.getLong("revision"),
        j.getString("reminderVersion"), Instant.parse(j.getString("remindersChangedAt")))
}
