package com.niranjan.ticktick.data.mapper

import com.niranjan.ticktick.domain.model.*
import org.json.JSONObject
internal fun FocusSession.encodeFocus(): String = JSONObject().apply {
    put("version", 1); put("id", id); put("mode", mode.name); put("phase", phase.name)
    put("durationMinutes", durationMinutes); put("elapsedMs", elapsedMs); put("startedAt", startedAt)
    put("bootCount", bootCount); put("wallStartedAt", wallStartedAt)
    put("expanded", expanded); put("keepScreenOn", keepScreenOn); put("note", note)
    put("taskId", taskId ?: JSONObject.NULL); put("soundUri", soundUri ?: JSONObject.NULL)
    put("completionPending", completionPending)
}.toString()
internal fun String.decodeFocus(): FocusSession = JSONObject(this).let { j ->
    require(j.getInt("version") == 1)
    FocusSession(id = j.getString("id"), mode = FocusMode.valueOf(j.getString("mode")),
        phase = FocusPhase.valueOf(j.getString("phase")), durationMinutes = j.getInt("durationMinutes"),
        elapsedMs = j.getLong("elapsedMs"), startedAt = j.getLong("startedAt"), bootCount = j.getInt("bootCount"),
        wallStartedAt = j.getLong("wallStartedAt"), expanded = j.getBoolean("expanded"),
        keepScreenOn = j.getBoolean("keepScreenOn"), note = j.getString("note"),
        taskId = if (j.isNull("taskId")) null else j.getString("taskId"),
        soundUri = if (j.isNull("soundUri")) null else j.getString("soundUri"), completionPending = j.getBoolean("completionPending"))
        .also { require(it.durationMinutes in 1..180 && it.elapsedMs in 0..1_577_880_000_000L && it.startedAt >= 0 &&
            it.wallStartedAt >= 0 && it.note.length <= 100_000 && (it.phase == FocusPhase.Ready || it.id.isNotBlank())) }
}
