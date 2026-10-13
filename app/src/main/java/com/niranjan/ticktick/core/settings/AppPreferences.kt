package com.niranjan.ticktick.core.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Stored in the existing Room UI preferences table. Defaults affect new editors only. */
data class AppPreferences(
    val tabs: List<String> = listOf("Tasks", "Calendar", "Habits", "Focus"),
    val mondayFirst: Boolean = false,
    val use24Hour: Boolean = false,
    val defaultReminderTime: LocalTime = LocalTime.of(9, 0),
    val defaultList: String = "inbox",
    val accent: Int = 0xFF4778FF.toInt(),
) {
    fun encode(): String = JSONObject().put("version", 1).put("tabs", JSONArray(tabs))
        .put("mondayFirst", mondayFirst).put("use24Hour", use24Hour)
        .put("defaultReminderTime", defaultReminderTime.toString()).put("defaultList", defaultList).put("accent", accent).toString()
    companion object {
        fun decode(payload: String?): AppPreferences {
            if (payload == null) return AppPreferences()
            val j = JSONObject(payload)
            require(j.getInt("version") == 1)
            val values = j.getJSONArray("tabs")
            val tabs = (0 until values.length()).map { values.getString(it) }
            require(tabs.firstOrNull() == "Tasks" && tabs.distinct() == tabs && tabs.all { it in setOf("Tasks", "Calendar", "Habits", "Focus") })
            val accent = j.getInt("accent")
            require(accent in listOf(0xFF4778FF.toInt(), 0xFF7C4DCC.toInt(), 0xFF248461.toInt()))
            val list = j.getString("defaultList"); require(list.isNotBlank())
            return AppPreferences(tabs, j.getBoolean("mondayFirst"), j.getBoolean("use24Hour"),
                LocalTime.parse(j.getString("defaultReminderTime")), list, accent)
        }
    }
}

/** Observable presentation mirror; Room remains the source of truth. */
object AppDisplay {
    var preferences by mutableStateOf(AppPreferences())
        internal set
    val timeFormatter: DateTimeFormatter get() = DateTimeFormatter.ofPattern(if (preferences.use24Hour) "HH:mm" else "h:mma", Locale.ENGLISH)
    val weekdayLabels get() = if (preferences.mondayFirst) listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    fun weekStart(date: LocalDate): LocalDate = date.minusDays(((date.dayOfWeek.value - if (preferences.mondayFirst) 1 else 0) % 7).toLong())
}
