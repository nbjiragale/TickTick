package com.niranjan.ticktick.feature.tasks

import org.json.JSONObject

internal fun TaskPresentation.encode(): String = JSONObject().put("layout", layout.name).put("details", details)
    .put("grouping", grouping.name).put("sorting", sorting.name).put("descending", descending)
    .put("backdrop", backdrop.name).put("swatch", swatch).put("imageUri", imageUri)
    .put("showCompleted", showCompleted).toString()

internal fun taskPresentation(json: String?, fallback: TaskPresentation): TaskPresentation {
    if (json == null) return fallback
    val j = JSONObject(json)
    return TaskPresentation(TaskLayout.valueOf(j.getString("layout")), j.getBoolean("details"),
        TaskGrouping.valueOf(j.getString("grouping")), TaskSorting.valueOf(j.getString("sorting")), j.getBoolean("descending"),
        TaskBackdrop.valueOf(j.getString("backdrop")), j.getInt("swatch"), j.optString("imageUri").takeIf { it.isNotEmpty() },
        j.optBoolean("showCompleted"))
}
