package com.niranjan.ticktick.data.mapper

import java.time.LocalDate
import org.json.JSONObject

internal fun validateSavedPreference(key: String, payload: String) {
    val j = JSONObject(payload)
    when {
        key == "app" -> com.niranjan.ticktick.core.settings.AppPreferences.decode(payload)
        key.startsWith("tasks:") -> com.niranjan.ticktick.feature.tasks.taskPresentation(payload, com.niranjan.ticktick.feature.tasks.TaskPresentation())
        key == "calendar" -> {
            require(j.optString("view", "Month") in setOf("List", "Day", "Week", "Month"))
            if (j.has("date")) LocalDate.ofEpochDay(j.getLong("date"))
        }
        key == "planning" -> {
            if (j.has("date") && !j.isNull("date")) LocalDate.parse(j.getString("date"))
            listOf("candidates", "reviewed").forEach { name ->
                val values = j.getJSONArray(name)
                for (i in 0 until values.length()) values.getString(i)
            }
            j.optJSONObject("occurrences")?.let { values -> values.keys().forEach { values.getString(it) } }
        }
    }
}
