package com.niranjan.ticktick.feature.taskeditor

import com.niranjan.ticktick.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

internal fun TaskSchedule.toJson() = JSONObject().apply {
    put("date", date?.toString()); put("time", time?.toString())
    put("offsets", JSONArray(reminders.offsetsMinutes)); put("constant", reminders.constant)
    put("dateOnlyTime", reminders.dateOnlyTime.toString())
    put("repeat", JSONObject().apply {
        put("unit", repeat.unit.name); put("interval", repeat.interval); put("basis", repeat.basis.name)
        put("weekdays", JSONArray(repeat.weekdays.map { it.value })); put("dates", JSONArray(repeat.dates.map { it.toString() }))
    })
    duration?.let { range -> put("duration", JSONObject().apply {
        put("startDate", range.startDate.toString()); put("endDate", range.endDate.toString())
        put("startTime", range.startTime.toString()); put("endTime", range.endTime.toString()); put("allDay", range.allDay)
    }) }
}

internal fun scheduleFromJson(data: JSONObject): TaskSchedule {
    val offsets = data.optJSONArray("offsets") ?: JSONArray()
    val repeat = data.optJSONObject("repeat") ?: JSONObject()
    val weekdays = repeat.optJSONArray("weekdays") ?: JSONArray()
    val dates = repeat.optJSONArray("dates") ?: JSONArray()
    return TaskSchedule(
        date = data.optString("date").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
        time = data.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
        reminders = ReminderConfig((0 until offsets.length()).map { offsets.getLong(it) }, data.optBoolean("constant"), LocalTime.parse(data.optString("dateOnlyTime", "09:00"))),
        repeat = RepeatRule(RepeatUnit.valueOf(repeat.optString("unit", "None")), repeat.optInt("interval", 1),
            (0 until weekdays.length()).map { DayOfWeek.of(weekdays.getInt(it)) }.toSet(), RepeatBasis.valueOf(repeat.optString("basis", "DueDates")),
            (0 until dates.length()).map { LocalDate.parse(dates.getString(it)) }),
        duration = data.optJSONObject("duration")?.let { range -> TaskDuration(LocalDate.parse(range.getString("startDate")), LocalDate.parse(range.getString("endDate")),
            LocalTime.parse(range.getString("startTime")), LocalTime.parse(range.getString("endTime")), range.optBoolean("allDay")) },
    )
}
